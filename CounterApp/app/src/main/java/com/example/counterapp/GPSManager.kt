package com.example.counterapp

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.Surface
import android.os.Build
import android.view.WindowManager


class GPSManager(private val context: Context) : LocationListener, SensorEventListener {

    private var locationManager: LocationManager? = null
    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null

    var latitude: Double? = null
    var longitude: Double? = null
    var gyroX: Float = 0f
    var gyroY: Float = 0f
    var gyroZ: Float = 0f

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    fun startLocationUpdates(activity: Activity) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1f, this)
        gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            gyroX = it.values[0]
            gyroY = it.values[1]
            gyroZ = it.values[2]

            // --- 獲取螢幕旋轉狀態的最新做法 ---
            val rotation: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) 或更新版本
                // 使用 Context.display (較新的 API)
                // 使用 try-catch 或 ?. 確保 context.display 不是 null
                rotation = try {
                    context.display?.rotation ?: Surface.ROTATION_0 // 如果 display 為 null，給予預設值
                } catch (e: UnsupportedOperationException) {
                    // 某些 Context 可能不支援 display 操作，例如 Application Context
                    // 在 Activity Context 中通常是安全的，但加上保護更好
                    // 如果真的出錯， fallback 到舊方法或預設值
                    getLegacyRotation(context)
                }

            } else { // Android 11 (API 30) 以下的版本
                // 使用舊的、已棄用的 API，並抑制警告
                rotation = getLegacyRotation(context)
            }
            // --- 結束獲取旋轉狀態 ---

            // 計算修正後的方向角（Yaw）
            var adjustedYaw = Math.toDegrees(gyroZ.toDouble()).toFloat() // 原始方向角

            // 根據螢幕旋轉狀態調整方向角
            adjustedYaw = when (rotation) {
                Surface.ROTATION_0 -> adjustedYaw // 直立
                Surface.ROTATION_90 -> adjustedYaw - 90 // 橫向（左側朝上）
                Surface.ROTATION_180 -> adjustedYaw - 180 // 反轉
                Surface.ROTATION_270 -> adjustedYaw + 90 // 橫向（右側朝上）
                else -> adjustedYaw
            }

//            println("調整後的方向角（Yaw）：$adjustedYaw")
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    @Suppress("DEPRECATION")
    private fun getLegacyRotation(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay.rotation
    }
}
