package com.dicoding.asclepius.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yalantis.ucrop.UCrop
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    private var croppedImageUri: Uri? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    private var isAnalyzeButtonEnabled: Boolean = false

    companion object {
        const val TAG = "ImagePicker"
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                currentImageUri = uri
                showImage()
                startUCrop(uri)
            } ?: showToast("Failed to get image URI")
        }
    }

    private val launcherResultActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            showToast("Image analysis saved successfully")
        } else {
            showToast("Image analysis failed to save")
            // Clear the image view and reset image URIs when analysis fails
            binding.previewImageView.setImageURI(null)
            currentImageUri = null
            croppedImageUri = null
        }
    }

    private val cropActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                UCrop.getOutput(result.data!!)?.let { uri ->
                    showCroppedImage(uri)
                    isAnalyzeButtonEnabled = true
                    binding.analyzeButton.isEnabled = true  // active when analyze
                } ?: showToast("Failed to crop image")
            }
            RESULT_CANCELED -> {
                showToast("Cropping canceled")
                // Clear the image view when cropping is canceled
                binding.previewImageView.setImageURI(null)
                currentImageUri = null
                croppedImageUri = null
                isAnalyzeButtonEnabled = false
                binding.analyzeButton.isEnabled = false // nonactive when cancel analyze
            }
            UCrop.RESULT_ERROR -> {
                val cropError = UCrop.getError(result.data!!)
                showToast("Crop error: ${cropError?.message}")
                // Clear the image view in case of an error as well
                binding.previewImageView.setImageURI(null)
                currentImageUri = null
                croppedImageUri = null
                isAnalyzeButtonEnabled = false
                binding.analyzeButton.isEnabled = false // nonactive when analyze error
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAnalyzeButtonEnabled = savedInstanceState?.getBoolean("isAnalyzeButtonEnabled") ?: false
        binding.analyzeButton.isEnabled = isAnalyzeButtonEnabled

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.analyzeButton.setOnClickListener {
            currentImageUri?.let {
                analyzeImage()
            } ?: run {
                showToast(getString(R.string.image_classifier_failed))
            }
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigationView = findViewById(R.id.menuBar)
        bottomNavigationView.setupWithNavController(navController)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.news -> {
                    startActivity(Intent(this, NewsActivity::class.java))
                    true
                }
                R.id.history_menu -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.analyzeButton.setOnClickListener {
            currentImageUri?.let {
                analyzeImage()
            } ?: run {
                showToast(getString(R.string.image_classifier_failed))
            }
        }

        val launcherResultActivity = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
            }
        }

        launcherResultActivity.launch(Intent(this, ResultActivity::class.java))
    }

    private fun startGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        val chooser = Intent.createChooser(intent, "Choose a Picture")
        launcherIntentGallery.launch(chooser)
    }

    private fun startUCrop(sourceUri: Uri) {
        val fileName = "cropped_image_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, fileName))
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .getIntent(this@MainActivity)

        cropActivityLauncher.launch(uCropIntent)
    }

    private fun showImage() {
        currentImageUri?.let { uri ->
            Log.d(TAG, "Displaying image: $uri")
            binding.previewImageView.setImageURI(uri)
        } ?: Log.d(TAG, "No image to display")
    }

    private fun analyzeImage() {
        croppedImageUri?.let { uri ->
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra(ResultActivity.IMAGE_URI, uri.toString())
            launcherResultActivity.launch(intent)
        } ?: showToast(getString(R.string.image_classifier_failed))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showCroppedImage(uri: Uri) {
        binding.previewImageView.setImageURI(uri)
        croppedImageUri = uri
        binding.analyzeButton.isEnabled = true  // Set analyze button active
        isAnalyzeButtonEnabled = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentImageUri?.let { outState.putString("currentImageUri", it.toString()) }
        croppedImageUri?.let { outState.putString("croppedImageUri", it.toString()) }
        outState.putBoolean("isAnalyzeButtonEnabled", isAnalyzeButtonEnabled)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString("currentImageUri")?.let { uriString ->
            currentImageUri = Uri.parse(uriString)
            showImage()
        }
        savedInstanceState.getString("croppedImageUri")?.let { uriString ->
            croppedImageUri = Uri.parse(uriString)
            showCroppedImage(croppedImageUri!!)
        }
        isAnalyzeButtonEnabled = savedInstanceState.getBoolean("isAnalyzeButtonEnabled", false)
        binding.analyzeButton.isEnabled = isAnalyzeButtonEnabled
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.empty_menu, menu)
        return true
    }
}
