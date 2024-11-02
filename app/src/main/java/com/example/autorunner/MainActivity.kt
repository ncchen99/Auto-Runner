package com.example.autorunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.autorunner.ui.theme.AutoRunnerTheme
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : ComponentActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val locationList = mutableListOf<LatLng>() // 儲存使用者新增的地點
    private var travelSpeed = 10.0 // 初始行駛時速
    private var isMockServiceRunning = false
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)


        enableEdgeToEdge()

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setContent {
            AutoRunnerTheme {
                var speedInput by remember { mutableStateOf("10") }
                val context = LocalContext.current

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            if (isMockServiceRunning) {
                                stopMockService()
                            } else {
                                startMockService(context, speedInput.toDoubleOrNull())
                            }
                        }) {
                            Text(if (isMockServiceRunning) "Stop" else "Start")
                        }
                    }
                ) { innerPadding ->
                    MapScreen(
                        mapView = mapView,
                        modifier = Modifier.padding(innerPadding),
                        speedInput = speedInput,
                        onSpeedChange = { speedInput = it }
                    )
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
            isMockServiceRunning = true
            setupLocationUpdates() // 設置位置更新回呼
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
        stopService(Intent(this, MockLocationService::class.java))
        isMockServiceRunning = false
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

    fun insertLandmark(latitude: Double, longitude: Double) {
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

    fun deleteLandmark(latitude: Double, longitude: Double) {
        val dbHelper = DatabaseHelper(this)
        dbHelper.deleteLandmark(latitude, longitude)
    }
}

@Composable
fun MapScreen(
    mapView: MapView,
    modifier: Modifier = Modifier,
    speedInput: String,
    onSpeedChange: (String) -> Unit
) {
    AndroidView(factory = { mapView }, modifier = modifier)
    TextField(
        value = speedInput,
        onValueChange = onSpeedChange,
        label = { Text("行駛時速 (公里)") }
    )
}
