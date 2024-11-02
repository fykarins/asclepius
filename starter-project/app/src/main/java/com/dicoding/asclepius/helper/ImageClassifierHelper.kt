package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import com.dicoding.asclepius.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File
import java.io.FileOutputStream

class ImageClassifierHelper(
    private var threshold: Float = 0.1f,
    private var maxResults: Int = 3,
    private val modelName: String = "cancer_classification.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(4)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            val modelFile = loadModelFile()

            imageClassifier = ImageClassifier.createFromFileAndOptions(
                modelFile,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    fun classifyImage(image: ImageProxy) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.UINT8))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(toBitmap(image)))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(image.imageInfo.rotationDegrees))
            .build()

        var inferenceTime = SystemClock.uptimeMillis()
        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        classifierListener?.onResult(
            results,
            inferenceTime
        )
    }

    private fun loadModelFile(): File {
        val modelFile = File(context.filesDir, modelName)
        context.assets.open(modelName).use { inputStream ->
            FileOutputStream(modelFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return modelFile
    }

    fun classifyStaticImage(imageUri: Uri) {
        val bitmap = toBitmap(imageUri) ?: return

        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val image = TensorImage.fromBitmap(argbBitmap)
        val outputs = imageClassifier?.classify(image)
        var inferenceTime = SystemClock.uptimeMillis()
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        outputs?.forEach { classification ->
            classification.categories.forEach { category ->
                Log.d(
                    "Classification Result",
                    "Label: ${category.label}, Confidence: ${category.score}"
                )
            }
        }
        classifierListener?.onResult(outputs, inferenceTime)
    }

    private fun toBitmap(imageUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to Bitmap: ${e.message}")
            null
        }
    }

    private fun toBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResult(
            result: MutableList<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}
