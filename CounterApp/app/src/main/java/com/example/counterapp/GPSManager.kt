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
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
