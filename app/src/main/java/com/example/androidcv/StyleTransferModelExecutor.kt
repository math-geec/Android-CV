package com.example.androidcv

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.collections.set
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

@SuppressWarnings("GoodTime")
class StyleTransferModelExecutor(
    context: Context,
    private var useGPU: Boolean = false
) {
    private var gpuDelegate: GpuDelegate? = null
    private var numberThreads = 4

    private val interpreterPredict: Interpreter
    private val interpreterTransform: Interpreter

    private var fullExecutionTime = 0L
    private var preProcessTime = 0L
    private var stylePredictTime = 0L
    private var styleTransferTime = 0L
    private var postProcessTime = 0L

    // Note: TensorFlow Lite Interpreter must be created on the same thread as where it is run
    init {
        if (useGPU) {
            // float16 quantized model for GPU inference
            interpreterPredict = getInterpreter(context, STYLE_PREDICT_FLOAT16_MODEL, true)
            interpreterTransform = getInterpreter(context, STYLE_TRANSFER_FLOAT16_MODEL, true)
        } else {
            // int8 quantized model for CPU inference
            interpreterPredict = getInterpreter(context, STYLE_PREDICT_INT8_MODEL, false)
            interpreterTransform = getInterpreter(context, STYLE_TRANSFER_INT8_MODEL, false)
        }
    }

    companion object {
        private const val TAG = "StyleTransferMExec"
        private const val STYLE_IMAGE_SIZE = 256
        private const val CONTENT_IMAGE_SIZE = 384
        private const val BOTTLENECK_SIZE = 100
        private const val STYLE_PREDICT_INT8_MODEL = "style_predict_quantized_256.tflite"
        private const val STYLE_TRANSFER_INT8_MODEL = "style_transfer_quantized_384.tflite"
        private const val STYLE_PREDICT_FLOAT16_MODEL = "style_predict_f16_256.tflite"
        private const val STYLE_TRANSFER_FLOAT16_MODEL = "style_transfer_f16_384.tflite"
    }

    fun execute(
        contentImagePath: String,
        styleImageName: String,
        context: Context
    ): ModelExecutionResult {
        try {
            Log.i(TAG, "running models")

            fullExecutionTime = SystemClock.uptimeMillis()
            preProcessTime = SystemClock.uptimeMillis()

            val contentImage = ImageUtils.decodeBitmap(File(contentImagePath))
            // Content Image Shape: (1, 384, 384, 3)
            val contentArray =
                ImageUtils.bitmapToByteBuffer(contentImage, CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE)
            val styleBitmap =
                ImageUtils.loadBitmapFromResources(context, "thumbnails/$styleImageName")
            // Style Image Shape: (1, 256, 256, 3)
            val input = ImageUtils.bitmapToByteBuffer(styleBitmap, STYLE_IMAGE_SIZE, STYLE_IMAGE_SIZE)

            // Set inputs & outputs for style predict model
            val inputsForPredict = arrayOf<Any>(input)
            val outputsForPredict = HashMap<Int, Any>()
            // Style Bottleneck Shape: (1, 1, 1, 100)
            val styleBottleneck = Array(1) { Array(1) { Array(1) { FloatArray(BOTTLENECK_SIZE) } } }
            outputsForPredict[0] = styleBottleneck

            preProcessTime = SystemClock.uptimeMillis() - preProcessTime
            stylePredictTime = SystemClock.uptimeMillis()

            // Style predict model converts the style image to a style embedding vector
            interpreterPredict.runForMultipleInputsOutputs(inputsForPredict, outputsForPredict)
            stylePredictTime = SystemClock.uptimeMillis() - stylePredictTime
            Log.d(TAG, "Style Predict Time to run: $stylePredictTime")

            // Set inputs & outputs for style transform model
            val inputsForStyleTransfer = arrayOf(contentArray, styleBottleneck)
            val outputsForStyleTransfer = HashMap<Int, Any>()
            val outputImage =
                Array(1) { Array(CONTENT_IMAGE_SIZE) { Array(CONTENT_IMAGE_SIZE) { FloatArray(3) } } }
            outputsForStyleTransfer[0] = outputImage

            styleTransferTime = SystemClock.uptimeMillis()

            // Style transform model applies the style embedding vector on the content image to generate a stylized image
            interpreterTransform.runForMultipleInputsOutputs(
                inputsForStyleTransfer,
                outputsForStyleTransfer
            )
            styleTransferTime = SystemClock.uptimeMillis() - styleTransferTime
            Log.d(TAG, "Style apply Time to run: $styleTransferTime")
            postProcessTime = SystemClock.uptimeMillis()

            // Final Stylized Image Shape: (1, 384, 384, 3)
            var styledImage =
                ImageUtils.convertArrayToBitmap(outputImage, CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE)
            postProcessTime = SystemClock.uptimeMillis() - postProcessTime

            fullExecutionTime = SystemClock.uptimeMillis() - fullExecutionTime
            Log.d(TAG, "Time to run everything: $fullExecutionTime")

            return ModelExecutionResult(
                styledImage,
                preProcessTime,
                stylePredictTime,
                styleTransferTime,
                postProcessTime,
                fullExecutionTime,
                formatExecutionLog()
            )
        } catch (e: Exception) {
            val exceptionLog = "something went wrong: ${e.message}"
            Log.d(TAG, exceptionLog)

            val emptyBitmap =
                ImageUtils.createEmptyBitmap(
                    CONTENT_IMAGE_SIZE,
                    CONTENT_IMAGE_SIZE
                )
            return ModelExecutionResult(
                emptyBitmap, errorMessage = e.message!!
            )
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }

    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGpu: Boolean = false
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()

        gpuDelegate = null
        // if the device has a supported GPU, add the GPU delegate
        if (useGpu) {
            gpuDelegate = GpuDelegate()
            tfliteOptions.addDelegate(gpuDelegate)
        }

        // if the GPU is not supported, run on multiple CPU threads (4 in this case)
        tfliteOptions.setNumThreads(numberThreads)
        return Interpreter(loadModelFile(context, modelName), tfliteOptions)
    }

    private fun formatExecutionLog(): String {
        val sb = StringBuilder()
        sb.append("Input Image Size: $CONTENT_IMAGE_SIZE x $CONTENT_IMAGE_SIZE\n")
        sb.append("GPU enabled: $useGPU\n")
        sb.append("Number of threads: $numberThreads\n")
        sb.append("Pre-process execution time: $preProcessTime ms\n")
        sb.append("Predicting style execution time: $stylePredictTime ms\n")
        sb.append("Transferring style execution time: $styleTransferTime ms\n")
        sb.append("Post-process execution time: $postProcessTime ms\n")
        sb.append("Full execution time: $fullExecutionTime ms\n")
        return sb.toString()
    }

    fun close() {
        interpreterPredict.close()
        interpreterTransform.close()
        if (gpuDelegate != null) {
            gpuDelegate!!.close()
        }
    }
}
