package com.example.autorunner

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.util.Timer
import java.util.TimerTask

class MockLocationService : Service() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val initialLocation = LatLng(25.0330, 121.5654) // 台北101初始位置
    private var latitudeOffset = 0.0
    private var longitudeOffset = 0.0
    private val timer = Timer()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMockLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startMockLocationUpdates() {
        fusedLocationClient.setMockMode(true) // 啟用 Mock 模式

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                latitudeOffset += 0.0001
                longitudeOffset += 0.0001

                val mockLocation = Location("mockProvider").apply {
                    latitude = initialLocation.latitude + latitudeOffset
                    longitude = initialLocation.longitude + longitudeOffset
                    accuracy = 1.0f
                    time = System.currentTimeMillis()
                }

                fusedLocationClient.setMockLocation(mockLocation)
            }
        }, 0, 2000) // 每2秒更新一次位置
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.setMockMode(false)
        timer.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
