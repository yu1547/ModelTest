package com.example.counterapp

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.nio.FloatBuffer
import java.io.File

object RecognitionModel {
    private const val MODEL_FILE = "simclr_mobilenetv3.pt"
    private lateinit var module: Module

    fun initialize(context: Context) {
        try {
            val modelPath = assetFilePath(context, MODEL_FILE)
            module = Module.load(modelPath)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("模型載入失敗: ${e.message}")
        }
    }


    fun classifyImage(bitmap: Bitmap): Float {
        // 預處理圖片
        val inputTensor = preprocessImage(bitmap)

        // 模型推論
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        // 返回第一個類別的相似分數（假設 ntou_donut 是第一個類別）
        return scores[0]
    }

    private fun preprocessImage(bitmap: Bitmap): Tensor {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        return TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file.absolutePath
    }
}
