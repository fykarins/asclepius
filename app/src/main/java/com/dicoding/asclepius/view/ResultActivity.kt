package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.data.local.AsclepiusDatabase
import com.dicoding.asclepius.data.local.PredictionHistory
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    companion object {
        const val IMAGE_URI = "img_uri"
        const val TAG = "ResultActivity"
        const val RESULT_TEXT = "result_text"
        const val REQUEST_HISTORY_UPDATE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra(IMAGE_URI)
        imageUriString?.let {
            val imageUri = Uri.parse(it)
            displayImage(imageUri)
            classifyImage(imageUri)
        } ?: run {
            Log.e(TAG, "No image URI provided")
            showToast(getString(R.string.image_not_available))
            finish()
        }

        binding.saveButton.setOnClickListener {
            val resultText = binding.resultText.text.toString()
            imageUriString?.let {
                savePredictionToDatabase(Uri.parse(it), resultText)
            } ?: showToast(getString(R.string.image_not_available))
        }
    }

    private fun displayImage(uri: Uri) {
        Log.d(TAG, "Displaying image: $uri")
        binding.resultImage.setImageURI(uri)
    }

    private fun classifyImage(uri: Uri) {
        val imageClassifierHelper = ImageClassifierHelper(
            contextValue = this,
            classifierListenerValue = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(errorMsg: String) {
                    Log.e(TAG, "Classification error: $errorMsg")
                    showToast(getString(R.string.analysis_error, errorMsg))
                }

                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    results?.let { showResults(it) }
                }
            }
        )
        imageClassifierHelper.classifyImage(uri)
    }

    private fun showResults(results: List<Classifications>) {
        val topResult = results[0]
        val label = topResult.categories[0].label
        val score = topResult.categories[0].score

        binding.resultText.text = getString(R.string.result_text, label, score.formatToString())
    }

    private fun savePredictionToDatabase(imageUri: Uri, result: String) {
        if (result.isNotEmpty()) {
            val fileName = "cropped_image_${System.currentTimeMillis()}.jpg"
            val destinationUri = Uri.fromFile(File(cacheDir, fileName))
            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(File(cacheDir, fileName)).use { output ->
                    input.copyTo(output)
                }
            }
            val prediction = PredictionHistory(imagePath = destinationUri.toString(), result = result)
            lifecycleScope.launch(Dispatchers.IO) {
                val database = AsclepiusDatabase.getDatabase(applicationContext)
                try {
                    database.predictionHistoryDao().insertPrediction(prediction)
                    Log.d(TAG, "Prediction saved successfully: $prediction")
                    moveToHistory(destinationUri, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save prediction", e)
                }
            }
        } else {
            showToast(getString(R.string.empty_result_cannot_save))
            Log.e(TAG, "Result is empty, cannot save prediction to database.")
        }
    }

    private fun moveToHistory(imageUri: Uri, result: String) {
        val intent = Intent(this, HistoryActivity::class.java)
        intent.putExtra(RESULT_TEXT, result)
        intent.putExtra(IMAGE_URI, imageUri.toString())
        setResult(RESULT_OK, intent)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Float.formatToString(): String {
        return String.format(Locale.getDefault(), "%.2f%%", this * 100)
    }
}
