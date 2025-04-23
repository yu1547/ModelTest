package com.example.counterapp

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private lateinit var gpsManager: GPSManager

    // 目標位置（地點 A）：這裡以 ntou_donut 為例
//    private val targetLat = 25.15085156936951
//    private val targetLon = 121.77400613690929

    // 目標位置：這裡以 test_now 當下地點 為例
    private val targetLat = 25.1342536
    private val targetLon = 121.7891466

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gpsManager = GPSManager(this)
        RecognitionModel.initialize(this) // 初始化模型
        requestPermissions()

        textViewResult = findViewById(R.id.textViewResult)
        val buttonDetect = findViewById<Button>(R.id.buttonDetect) // 開啟辨識（原本的開啟鏡頭）
        val buttonGPS = findViewById<Button>(R.id.buttonGPS)       // 定位按鈕

        imageView = findViewById(R.id.imageView)

        // 按下「開啟辨識」：直接開啟相機拍照
        buttonDetect.setOnClickListener {
            if (hasPermissions()) {
                CameraManager.openCamera(this)
            } else {
                requestPermissions()
            }
        }

        // 按下「GPS 定位」：先更新 GPS，再進行驗證邏輯
        buttonGPS.setOnClickListener {
            gpsManager.startLocationUpdates(this)
            // 延遲2秒讓GPSManager有足夠的時間更新資料
            buttonGPS.postDelayed({ validatePosition() }, 2000)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        CameraManager.handleCameraResult(this, requestCode, resultCode, data) { imageBitmap ->
            if (imageBitmap != null) {
                imageView.setImageBitmap(imageBitmap)
                // 執行模型辨識
                val similarity = RecognitionModel.classifyImage(imageBitmap)
                textViewResult.text = "相似度：$similarity"
            } else {
                textViewResult.text = "拍照失敗"
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 200)
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            textViewResult.text = if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                "權限已授予，請使用功能！"
            } else {
                "需要權限才能使用功能！"
            }
        }
    }

    // 位置驗證邏輯：顯示GPS數據及驗證結果；通過時開啟相機拍照
    private fun validatePosition() {
        val userLat = gpsManager.latitude
        val userLon = gpsManager.longitude

        // 檢查是否已取得GPS資訊
        if (userLat == null || userLon == null) {
            textViewResult.text = "尚未取得GPS位置，請稍候……"
            return
        }

        // 將當前GPS與陀螺儀原始資料先行存入結果字串
        var resultText = "目前GPS資料：\n經度: $userLon, 緯度: $userLat\n"
        resultText += "陀螺儀資料：X=${gpsManager.gyroX}, Y=${gpsManager.gyroY}, Z=${gpsManager.gyroZ}\n"

        // 驗證 1：距離檢查
        val distance = calculateDistance(userLat, userLon, targetLat, targetLon)
        if (distance > 20) {
            resultText += "位置驗證失敗：您離目標超過20公尺（實際距離：${distance.toInt()}公尺）。"
            textViewResult.text = resultText
            return
        } else {
            resultText += "位置驗證通過！（距離：${distance.toInt()}公尺）\n"
        }

        // 驗證 2：方向檢查
        val targetBearing = calculateBearing(userLat, userLon, targetLat, targetLon)
        // 假設 gyroZ 為當前使用者朝向
        val userYaw = gpsManager.gyroZ
        val directionDifference = angleDifference(userYaw, targetBearing)
        if (directionDifference > 45) {
            resultText += "方向驗證失敗：偏差 ${directionDifference.toInt()}°（允許±45°）。"
            textViewResult.text = resultText
            return
        } else {
            resultText += "方向驗證通過！（目標方向：${targetBearing.toInt()}°，您的方向：${userYaw.toInt()}，偏差：${directionDifference.toInt()}°）\n"
        }

        // 若兩項皆通過，顯示成功資訊並開啟相機拍照
        resultText += "GPS 驗證成功！"
        textViewResult.text = resultText
        CameraManager.openCamera(this)
    }

    // 利用 Location.distanceBetween() 計算距離（單位：公尺）
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    // 計算從玩家位置（B）到目標位置（A）的方向角（範圍 0～360°）
    private fun calculateBearing(latB: Double, lonB: Double, latA: Double, lonA: Double): Float {
        val phiB = Math.toRadians(latB)
        val phiA = Math.toRadians(latA)
        val deltaLambda = Math.toRadians(lonA - lonB)
        val x = sin(deltaLambda) * cos(phiA)
        val y = cos(phiB) * sin(phiA) - sin(phiB) * cos(phiA) * cos(deltaLambda)
        val initialBearing = Math.atan2(x, y)
        return (((Math.toDegrees(initialBearing)) + 360) % 360).toFloat()
    }

    // 計算兩角度間最小的差值（處理環狀情形）
    private fun angleDifference(angle1: Float, angle2: Float): Float {
        val diff = abs(angle1 - angle2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }
}
