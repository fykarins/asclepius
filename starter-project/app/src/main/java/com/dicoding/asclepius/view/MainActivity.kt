package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.mycamera.data.api.ApiConfig
import com.dicoding.asclepius.mycamera.data.api.FileUploadResponse
import com.dicoding.asclepius.utils.Utils
import com.dicoding.asclepius.utils.reduceFileImage
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.tensorflow.lite.task.vision.classifier.Classifications
import retrofit2.HttpException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    showToast(error)
                }

                override fun onResult(result: MutableList<Classifications>?, inferenceTime: Long) {
                    moveToResult(result)
                }
            }
        )
        binding.galleryButton.setOnClickListener{ startGallery() }
        binding.analyzeButton.setOnClickListener{ analyzeImage() }
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d( "Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun analyzeImage() {
        currentImageUri?.let {
            imageClassifierHelper.classifyStaticImage(it)
        } ?: run {
            showToast("Please choose the image first")
        }
    }

    private fun moveToResult(result: MutableList<Classifications>?) {
        val resultString = StringBuilder()
        result?.forEach { classification ->
            classification.categories.forEach { category ->
                resultString.append("Label: ${category.label}, Confidence: ${category.score * 100}%\n")
            }
        }

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("IMAGE_URI", currentImageUri.toString())
            putExtra("RESULT", resultString.toString())
        }
        startActivity(intent)
    }

    private fun uploadImage() {
        currentImageUri?.let { uri ->
            val imageFile = Utils().uriToFile(uri, this).reduceFileImage()
            Log.d("Image Classification File", "showImage: ${imageFile.path}")
            showLoading(true)
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "photo",
                imageFile.name,
                requestImageFile
            )
            lifecycleScope.launch {
                try {
                    val apiService = ApiConfig.getApiService()
                    val successResponse = apiService.uploadImage(multipartBody)
                    with(successResponse.data) {
                        binding.resultTextView.text = if (isAboveThreshold == true) {
                            showToast(successResponse.message.toString())
                            String.format(Locale.getDefault(), "%s with %.2f%%", result, confidenceScore)
                        } else {
                            showToast("Model is predicted successfully but under threshold.")
                            String.format(Locale.getDefault(), "Please use the correct picture because the confidence score is %.2f%%", confidenceScore)
                        }
                    }
                    showLoading(false)
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, FileUploadResponse::class.java)
                    showToast(errorResponse.message.toString())
                    showLoading(false)
                } catch (e: Exception) {
                    showToast("An unexpected error occurred")
                    showLoading(false)
                }
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}