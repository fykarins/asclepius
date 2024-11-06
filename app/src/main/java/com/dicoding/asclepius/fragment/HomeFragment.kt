package com.dicoding.asclepius.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.FragmentHomeBinding
import com.dicoding.asclepius.view.ResultActivity
import com.yalantis.ucrop.UCrop
import java.io.File
import android.content.res.Configuration


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            homeViewModel.setImageUri(uri)
            startCrop(uri)
        } else {
            Log.d("Photo Picker", "No media selected")
            showToast("No image selected")
        }
    }

    private val cropResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                homeViewModel.setImageUri(it)
                showImage(it)
                moveToResultActivity(it)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            cropError?.let {
                showToast("Crop error: ${it.message}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        homeViewModel.currentImageUri.value?.let { uri ->
            outState.putString("currentImageUri", uri.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore image URI if available
        savedInstanceState?.getString("currentImageUri")?.let { uriString ->
            homeViewModel.setImageUri(Uri.parse(uriString))
        }

        // Observe currentImageUri and display the image if available
        homeViewModel.currentImageUri.observe(viewLifecycleOwner) { uri ->
            uri?.let { showImage(it) }
        }

        // Set up button click to open gallery
        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        // Adjust layout based on orientation
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Update constraints for landscape mode
            constraintSet.connect(R.id.previewImageView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.previewImageView, ConstraintSet.BOTTOM, R.id.galleryButton, ConstraintSet.TOP)
        } else {
            // Restore constraints for portrait mode
            constraintSet.connect(R.id.previewImageView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.previewImageView, ConstraintSet.BOTTOM, R.id.galleryButton, ConstraintSet.TOP)
        }

        constraintSet.applyTo(binding.root)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "croppedImage.png"))
        val uCrop = UCrop.of(uri, destinationUri)
        uCrop.withAspectRatio(1f, 1f)
        uCrop.withMaxResultSize(1080, 1080)

        val cropIntent = uCrop.getIntent(requireContext())
        cropResultLauncher.launch(cropIntent)
    }

    private fun showImage(resultUri: Uri) {
        binding.previewImageView.setImageURI(resultUri)
    }

    private fun moveToResultActivity(uri: Uri) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra(ResultActivity.IMAGE_URI, uri.toString())
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }
}
