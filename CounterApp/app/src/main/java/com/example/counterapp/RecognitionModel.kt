package com.example.counterapp

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File

object RecognitionModel {
    private lateinit var module: Module

    fun initialize(context: Context) {
        try {
            val modelPath = assetFilePath(context, "simclr_mobilenetv3.pt")
            module = Module.load(modelPath)
            println("✅ 模型載入成功")
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("模型載入失敗: ${e.message}")
        }
    }

    fun extractFeatureVector(bitmap: Bitmap): FloatArray {
        val inputTensor = preprocessImage(bitmap)
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        return outputTensor.dataAsFloatArray // 🔹 只輸出特徵向量，不執行比對！
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
