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
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var currentLocation = LatLng(25.0330, 121.5654) // 台北101初始位置
    private var latitudeOffset = 0.0001
    private var longitudeOffset = 0.0001
    private val timer = Timer()
    private var nanoOffset: Long = 0


    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "模擬位置服務",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 設置前景通知
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("模擬位置服務")
            .setContentText("位置模擬中...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 替換成您自己的圖標
            .build()

        startForeground(1, notification)

        initNanoOffset() // 計算 nanoOffset
        startMockLocationUpdates()
        return START_STICKY
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mock Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Simulation Running")
            .setContentText("Simulating user location in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with an actual icon resource
            .build()
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

    // Constant values
    companion object {
        private const val CHANNEL_ID = "MockLocationServiceChannel"
        private const val NOTIFICATION_ID = 1
    }
}
