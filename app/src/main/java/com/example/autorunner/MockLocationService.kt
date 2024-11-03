package com.example.autorunner

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.util.Timer
import java.util.TimerTask
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.MediaPlayer

class MockLocationService : Service() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var currentLocation = LatLng(22.9967197, 120.2232919) // 自強校區初始位置 ,
    private val timer = Timer()
    private var nanoOffset: Long = 0
    private var fractionSum = 0.0

    private var locationList = listOf<LatLng>()
    private var currentIndex = 0
    private var speed = 10.0 // 默認行駛時速 (公里/小時)

    private var mediaPlayer: MediaPlayer? = null

    private var isPaused = false // 新增布林值來表示暫��狀態

    private val binder = LocalBinder()

    // 提供方法來更新暫停狀態
    fun setPaused(paused: Boolean) {
        Log.d("setPaused", "Paused status changed to $paused")
        isPaused = paused
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "模擬位置服務",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        createNotificationChannel()
        mediaPlayer = MediaPlayer.create(this, R.raw.notification_sound) // 確保有一個名為 notification_sound 的音效文件
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        locationList = intent?.getParcelableArrayListExtraCompat("locationList") ?: emptyList()
        speed = intent?.getDoubleExtra("speed", 10.0) ?: 10.0

        Log.d("onStartCommand", "locationList: $locationList")
        Log.d("onStartCommand", "speed: $speed")
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mock Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

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
        fusedLocationClient.setMockMode(true)

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (isPaused) return // 如果是暫停狀態，則不執行任何動作

                if (currentIndex < locationList.size - 1) {
                    val startLocation = locationList[currentIndex]
                    val endLocation = locationList[currentIndex + 1]

                    // 計算兩點之間的距離
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        startLocation.latitude, startLocation.longitude,
                        endLocation.latitude, endLocation.longitude,
                        results
                    )
                    val distance = results[0]

                    // 計算每秒需要行走的距離
                    val distancePerSecond = (speed * 1000) / 3600

                    // 如果累積比例超過或等於 1，則移動到下一個地點
                    if (fractionSum >= 1) {
                        currentIndex++
                        currentLocation = endLocation
                        fractionSum = 0.0

                        // 播放提示音並暫停 20 秒
                        mediaPlayer?.start()
                        Thread.sleep(20000)
                    } else {
                        // 計算新的位置
                        val fraction = distancePerSecond / distance
                        fractionSum += fraction
                        val newLatitude = startLocation.latitude + fractionSum * (endLocation.latitude - startLocation.latitude)
                        val newLongitude = startLocation.longitude + fractionSum * (endLocation.longitude - startLocation.longitude)
                        currentLocation = LatLng(newLatitude, newLongitude)
                    }
                }

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
        }, 0, 1000) // 每秒更新一次位置
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.setMockMode(false)
        timer.cancel()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): MockLocationService = this@MockLocationService
    }

    // Constant values
    companion object {
        private const val CHANNEL_ID = "MockLocationServiceChannel"
//        private const val NOTIFICATION_ID = 1
    }

    private inline fun <reified T : android.os.Parcelable> Intent.getParcelableArrayListExtraCompat(key: String): ArrayList<T>? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            extras?.getParcelableArrayList(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras?.getParcelableArrayList(key)
        }
    }
}
