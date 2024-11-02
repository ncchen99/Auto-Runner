package com.example.autorunner

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.util.Timer
import java.util.TimerTask

class MockLocationService : Service() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var currentLocation = LatLng(25.0330, 121.5654) // 台北101初始位置
    private var latitudeOffset = 0.0001
    private var longitudeOffset = 0.0001
    private val timer = Timer()
    private var nanoOffset: Long = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initNanoOffset() // 計算 nanoOffset
        startMockLocationUpdates()
        return START_STICKY
    }

    // 計算 elapsedRealtimeNanos 與 System.nanoTime() 之間的 offset
    @SuppressLint("MissingPermission")
    private fun initNanoOffset() {
        fusedLocationClient.lastLocation.addOnSuccessListener { current ->
            current?.let {
                nanoOffset = it.elapsedRealtimeNanos - System.nanoTime()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMockLocationUpdates() {
        fusedLocationClient.setMockMode(true) // 啟用 Mock 模式

        timer.schedule(object : TimerTask() {
            override fun run() {
                currentLocation = LatLng(
                    currentLocation.latitude + latitudeOffset,
                    currentLocation.longitude + longitudeOffset
                )

                val mockLocation = Location("mockProvider").apply {
                    latitude = currentLocation.latitude
                    longitude = currentLocation.longitude
                    accuracy = 1.0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = System.nanoTime() + nanoOffset // 設置偏移後的時間
                }

                // 模擬位置
                fusedLocationClient.setMockLocation(mockLocation)
                    .addOnSuccessListener { Log.d("setMockLocation", "Location mocked at ${mockLocation.latitude}, ${mockLocation.longitude}") }
                    .addOnFailureListener { Log.d("setMockLocation", "Mocking failed") }
            }
        }, 0, 1100) // 每 1100 毫秒更新一次位置
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
