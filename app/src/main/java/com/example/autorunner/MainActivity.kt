package com.example.autorunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.autorunner.ui.theme.AutoRunnerTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Timer
import java.util.TimerTask

class MainActivity : ComponentActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val initialLocation = LatLng(25.0330, 121.5654) // 台北101初始位置
    private var latitudeOffset = 0.0 // 緯度增量
    private var longitudeOffset = 0.0 // 經度增量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setContent {
            AutoRunnerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(mapView = mapView, modifier = Modifier.padding(innerPadding))
                }
            }
        }

        // 如果權限被授予，則在 onMapReady 中初始化位置顯示
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startMockLocationUpdates() // 啟動模擬位置更新
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.addMarker(MarkerOptions().position(initialLocation).title("起始位置"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))

        enableUserLocation() // 在 onMapReady 中呼叫
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLocation = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                    googleMap.addMarker(MarkerOptions().position(userLocation).title("當前位置"))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMockLocationUpdates() {
        fusedLocationClient.setMockMode(true) // 啟用 Mock 模式

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // 模擬位置的增量變化
                latitudeOffset += 0.0001
                longitudeOffset += 0.0001

                val mockLocation = Location("mockProvider").apply {
                    latitude = initialLocation.latitude + latitudeOffset
                    longitude = initialLocation.longitude + longitudeOffset
                    accuracy = 1.0f
                    time = System.currentTimeMillis()
                }

                // 更新模擬位置
                fusedLocationClient.setMockLocation(mockLocation).addOnSuccessListener {
                    runOnUiThread {
                        val newLocation = LatLng(mockLocation.latitude, mockLocation.longitude)
                        googleMap.clear() // 清除地圖標記
                        googleMap.addMarker(MarkerOptions().position(newLocation).title("模擬位置"))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(newLocation))
                    }
                }
            }
        }, 0, 2000) // 每2秒更新一次位置
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
        mapView.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.setMockMode(false) // 停止 Mock 模式
    }
}


@Composable
fun MapScreen(mapView: MapView, modifier: Modifier = Modifier) {
    AndroidView(factory = { mapView }, modifier = modifier)
}
