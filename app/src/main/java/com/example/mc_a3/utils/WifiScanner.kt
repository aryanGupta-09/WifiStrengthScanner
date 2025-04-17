package com.example.mc_a3.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.example.mc_a3.model.LocationData
import com.example.mc_a3.model.WifiSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiScanner(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults = _scanResults.asStateFlow()
    
    // Broadcast receiver for WiFi scan results
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    _scanResults.value = wifiManager.scanResults
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        }
        context.registerReceiver(wifiScanReceiver, intentFilter)
    }

    // Start a WiFi scan
    fun startScan(): Boolean {
        return wifiManager.startScan()
    }

    // Convert scan results to a signal matrix of size 100
    fun createSignalMatrix(): List<Int> {
        val results = _scanResults.value
        
        // If we have too few or too many networks, we need to normalize to size 100
        return when {
            results.size >= LocationData.MATRIX_SIZE -> {
                // Too many networks, take the strongest ones
                results
                    .sortedByDescending { it.level }
                    .take(LocationData.MATRIX_SIZE)
                    .map { it.level }
            }
            results.isEmpty() -> {
                // No networks found, return matrix with minimum signal levels
                List(LocationData.MATRIX_SIZE) { -100 }
            }
            else -> {
                // Too few networks, repeat the pattern until we reach 100
                val networkLevels = results.map { it.level }
                List(LocationData.MATRIX_SIZE) { index -> networkLevels[index % networkLevels.size] }
            }
        }
    }

    // Create a LocationData object from current scan results
    fun createLocationData(locationName: String): LocationData {
        val wifiSignals = _scanResults.value.map { scanResult ->
            WifiSignal(
                ssid = scanResult.SSID.ifEmpty { "<Hidden Network>" },
                bssid = scanResult.BSSID,
                level = scanResult.level,
                frequency = scanResult.frequency
            )
        }
        
        return LocationData(
            name = locationName,
            signalMatrix = createSignalMatrix(),
            scanResults = wifiSignals
        )
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}