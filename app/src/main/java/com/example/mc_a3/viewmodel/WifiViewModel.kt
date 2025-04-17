package com.example.mc_a3.viewmodel

import android.app.Application
import android.net.wifi.ScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mc_a3.data.LocationRepository
import com.example.mc_a3.model.LocationData
import com.example.mc_a3.utils.WifiScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val wifiScanner = WifiScanner(application)
    private val locationRepository = LocationRepository(application)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private val _currentLocationName = MutableStateFlow<String?>(null)
    val currentLocationName = _currentLocationName.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults = _scanResults.asStateFlow()
    
    private val _locationData = MutableStateFlow<Map<String, LocationData>>(emptyMap())
    val locationData = _locationData.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    init {
        viewModelScope.launch {
            wifiScanner.scanResults.collectLatest {
                _scanResults.value = it
                _isScanning.value = false
            }
        }
        
        // Load saved location data
        viewModelScope.launch {
            locationRepository.getAllLocationData().collectLatest {
                _locationData.value = it
                _uiState.value = UiState.Ready
            }
        }
    }

    fun startScan() {
        _isScanning.value = true
        wifiScanner.startScan()
    }
    
    fun setCurrentLocation(locationName: String) {
        _currentLocationName.value = locationName
    }
    
    fun captureLocationData() {
        val locationName = _currentLocationName.value ?: return
        val locationData = wifiScanner.createLocationData(locationName)
        
        viewModelScope.launch {
            locationRepository.saveLocationData(locationData)
            val updatedLocationData = _locationData.value.toMutableMap()
            updatedLocationData[locationName] = locationData
            _locationData.value = updatedLocationData
        }
    }
    
    fun getPredefinedLocations(): List<String> = locationRepository.predefinedLocations
    
    override fun onCleared() {
        super.onCleared()
        wifiScanner.cleanup()
    }
    
    sealed class UiState {
        data object Loading : UiState()
        data object Ready : UiState()
        data class Error(val message: String) : UiState()
    }
}