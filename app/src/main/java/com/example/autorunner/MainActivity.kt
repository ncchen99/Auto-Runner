package com.example.autorunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.compose.ui.unit.dp
import com.example.autorunner.ui.theme.AutoRunnerTheme
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth

class MainActivity : ComponentActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val locationList = mutableListOf<LatLng>() // 儲存使用者新增的地點
    private var travelSpeed = 10.0 // 初始行駛時速
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var locationCallback: LocationCallback
    private var mockLocationService: MockLocationService? = null
    private var isBound = false
    private var isFirstStart = true // 新增變數來追蹤是否是第一次啟動

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MockLocationService.LocalBinder
            mockLocationService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mockLocationService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setContent {
            AutoRunnerTheme {
                var speedInput by remember { mutableStateOf("10") }
                var isMockServiceRunning by remember { mutableStateOf(false) }
                var showDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current

                Scaffold(
                    floatingActionButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
//                                .padding(horizontal = 16.dp), // 整個 Row 的水平內邊距
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    stopMockService()
                                    finish() // 結束應用程式
                                },
                                modifier = Modifier.padding(start = 32.dp), // 左邊按鈕的額外左邊距
                                containerColor = Color(0xFFFFC0CB) // 淡粉色
                            ) {
                                Text("Stop")
                            }
                            FloatingActionButton(
                                onClick = {
                                    if (isFirstStart) {
                                        showDialog = true
                                    } else {
                                        if (isMockServiceRunning) {
                                            // 傳遞暫停狀態到服務
                                            mockLocationService?.setPaused(true)
                                            isMockServiceRunning = false
                                        } else {
                                            // 恢復運行
                                            mockLocationService?.setPaused(false)
                                            isMockServiceRunning = true
                                        }
                                    }
                                },
//                                modifier = Modifier.padding(end = 16.dp), // 右邊按鈕的額外右邊距
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(if (isMockServiceRunning) "Pause" else "Start")
                            }
                        }
                    }
                ) { innerPadding ->
                    MapScreen(
                        mapView = mapView,
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("設定行駛時速") },
                            text = {
                                TextField(
                                    value = speedInput,
                                    onValueChange = { speedInput = it },
                                    label = { Text("行駛時速 (公里)") }
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    startMockService(context, speedInput.toDoubleOrNull())
                                    isMockServiceRunning = true
                                    isFirstStart = false // 更新狀態為非第一次啟動
                                    showDialog = false
                                }) {
                                    Text("確認")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showDialog = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startMockService(context: Context, speed: Double?) {
        if (speed != null && speed > 0) {
            travelSpeed = speed
            val intent = Intent(this, MockLocationService::class.java).apply {
                putExtra("locationList", ArrayList(locationList))
                putExtra("speed", travelSpeed)
            }
            startForegroundService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            setupLocationUpdates()
        } else {
            Toast.makeText(context, "請輸入有效的時速", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L // 每1秒更新一次
        ).apply {
            setMinUpdateIntervalMillis(1000) // 最小更新間隔為500毫秒
            setMaxUpdateDelayMillis(2000) // 最大延遲2秒
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val userLocation = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation)) // 更新地圖位置
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun stopMockService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, MockLocationService::class.java))
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(22.9967197, 120.2232919), 15f))
        enableUserLocation()

        // 載入並顯示已保存的地標
        val landmarks = getLandmarks()
        for (landmark in landmarks) {
            locationList.add(landmark)
            val marker = googleMap.addMarker(MarkerOptions().position(landmark).title("已保存地點"))
            marker?.tag = landmark
        }

        // 地圖點擊以新增地點
        googleMap.setOnMapClickListener { latLng ->
            locationList.add(latLng)
            val marker = googleMap.addMarker(MarkerOptions().position(latLng).title("地點 ${locationList.size}"))
            marker?.tag = latLng
            insertLandmark(latLng.latitude, latLng.longitude) // 保存新地點到資料庫
        }

        // 標記長按以刪除地點
        googleMap.setOnMarkerClickListener { marker ->
            val latLng = marker.tag as? LatLng
            latLng?.let {
                deleteLandmark(it.latitude, it.longitude)
                marker.remove()
                locationList.remove(it)
                Toast.makeText(this, "地標已刪除", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMockService()
        mapView.onDestroy()
    }

    private fun insertLandmark(latitude: Double, longitude: Double) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_LATITUDE, latitude)
            put(DatabaseHelper.COLUMN_LONGITUDE, longitude)
        }

        db.insert(DatabaseHelper.TABLE_NAME, null, values)
        db.close()
    }

    private fun getLandmarks(): List<LatLng> {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            arrayOf(DatabaseHelper.COLUMN_LATITUDE, DatabaseHelper.COLUMN_LONGITUDE),
            null, null, null, null, null
        )

        val landmarks = mutableListOf<LatLng>()
        with(cursor) {
            while (moveToNext()) {
                val latitude = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE))
                val longitude = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE))
                landmarks.add(LatLng(latitude, longitude))
            }
        }
        cursor.close()
        db.close()
        return landmarks
    }

    private fun deleteLandmark(latitude: Double, longitude: Double) {
        val dbHelper = DatabaseHelper(this)
        dbHelper.deleteLandmark(latitude, longitude)
    }
}

@Composable
fun MapScreen(
    mapView: MapView,
    modifier: Modifier = Modifier
) {
    AndroidView(factory = { mapView }, modifier = modifier)
}
