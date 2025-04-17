package com.example.mc_a3.model

import android.net.wifi.ScanResult
import kotlinx.serialization.Serializable

// Data class to hold WiFi signal information
@Serializable
data class WifiSignal(
    val ssid: String,
    val bssid: String,
    val level: Int, // Signal strength in dBm
    val frequency: Int
)

// Data class to represent a location with its WiFi signal matrix
@Serializable
data class LocationData(
    val name: String,
    val signalMatrix: List<Int>,
    val scanResults: List<WifiSignal> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val MATRIX_SIZE = 100
    }
}