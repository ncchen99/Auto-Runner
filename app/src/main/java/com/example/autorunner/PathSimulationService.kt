package com.example.autorunner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PathSimulationService : Service() {

    private val startLocation = LatLng(25.0330, 121.5654) // 起始位置
    private val endLocation = LatLng(25.0375, 121.5637) // 目的地位置
    private var currentLocation = startLocation

    private var coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationId = 1
    private val channelId = "PathSimulationChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, createNotification("模擬移動中..."))

        coroutineScope.launch {
            startSimulation(50.0) // 設定速度，50 公里/小時
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "路徑模擬通知",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("路徑模擬")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private suspend fun startSimulation(speed: Double) {
        while (calculateDistance(currentLocation, endLocation) > 0.01) { // 檢查是否到達終點
            val distance = (speed * 1000 / 3600) * 1 // 每秒的移動距離 (單位: 米)
            currentLocation = moveTowards(currentLocation, endLocation, distance)

            // 更新通知
            val notification = createNotification("當前位置: ${currentLocation.latitude}, ${currentLocation.longitude}")
            startForeground(notificationId, notification)

            delay(1000L) // 每秒更新一次位置
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000 // 米
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun moveTowards(start: LatLng, end: LatLng, distance: Double): LatLng {
        val heading = atan2(
            end.longitude - start.longitude,
            end.latitude - start.latitude
        )
        val newLat = start.latitude + distance * cos(heading) / 111320
        val newLng = start.longitude + distance * sin(heading) / (111320 * cos(Math.toRadians(start.latitude)))
        return LatLng(newLat, newLng)
    }
}
