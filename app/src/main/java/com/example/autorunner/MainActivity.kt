package com.example.autorunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private lateinit var locationCallback: LocationCallback

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startService(Intent(this, MockLocationService::class.java))
            setupLocationUpdates() // 設置位置更新回呼
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.0330, 121.5654), 15f))
        enableUserLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
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


    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MockLocationService::class.java))
        fusedLocationClient.removeLocationUpdates(locationCallback) // 停止位置更新
        mapView.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}

@Composable
fun MapScreen(mapView: MapView, modifier: Modifier = Modifier) {
    AndroidView(factory = { mapView }, modifier = modifier)
}
