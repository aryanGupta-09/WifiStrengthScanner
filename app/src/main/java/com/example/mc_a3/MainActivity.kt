package com.example.mc_a3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mc_a3.model.LocationData
import com.example.mc_a3.ui.components.SignalMatrixVisualization
import com.example.mc_a3.ui.theme.MC_A3Theme
import com.example.mc_a3.viewmodel.WifiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Initialize scanning if permissions are granted
            val viewModel: WifiViewModel by viewModels()
            viewModel.startScan()
        } else {
            // Handle permission denial
            // In a real app, you'd show a dialog explaining why permissions are needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check and request permissions
        checkAndRequestPermissions()
        
        setContent {
            MC_A3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WifiSignalApp()
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSignalApp(
    viewModel: WifiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLocationName by viewModel.currentLocationName.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    
    val predefinedLocations = viewModel.getPredefinedLocations()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("WiFi Signal Strength Scanner") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Location selection
            Text(
                text = "Select Location",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                predefinedLocations.forEach { location ->
                    Button(
                        onClick = { viewModel.setCurrentLocation(location) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLocationName == location) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(location.split(" ").last())
                    }
                }
            }
            
            // Single button for scanning and capturing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.scanAndCaptureData()
                    },
                    enabled = !isScanning && currentLocationName != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isScanning) "Processing..." else "Record WiFi Fingerprint")
                }
            }
            
            // Clear button with Compose dialog
            var showClearConfirmDialog by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = locationData.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Data")
                }
            }
            
            // Confirmation dialog
            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = { Text("Clear All Data") },
                    text = { 
                        Text("Are you sure you want to clear all the location data?") 
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllLocationData()
                                scope.launch {
                                    snackbarHostState.showSnackbar("All the location data has been cleared")
                                }
                                showClearConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Location Data Visualizations
            if (locationData.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Text(
                        text = "No data captured yet. Select a location, scan WiFi networks, and capture data.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Comparison of WiFi Signal Strengths Across Locations",
                    style = MaterialTheme.typography.headlineMedium,  // Upgraded from headlineSmall to headlineMedium
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                // Display each location in a separate card
                locationData.values.forEachIndexed { index, data ->
                    // Use a lighter surface color for better visibility in dark mode
                    val cardBackgroundColor = if (isSystemInDarkTheme()) {
                        // Use a lighter gray in dark mode for better contrast
                        Color(0xFF2C2C2C)
                    } else {
                        // In light mode, use the default surface color (white)
                        MaterialTheme.colorScheme.surface
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            SignalMatrixVisualization(
                                locationData = data,
                                barColor = getColorForLocation(data),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                if (locationData.size >= 2) {
                    // Show statistics across locations
                    val stats = calculateStatsBetweenLocations(locationData.values.toList())
                    
                    Text(
                        text = "Cross-Location Statistics",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Display cross-location stats directly without a card
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Average difference between locations: ${stats.averageDifference} dBm",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Text(
                            text = "Max difference between locations: ${stats.maxDifference} dBm",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getColorForLocation(locationData: LocationData): Color {
    // Extract the location number from the location name
    val locationName = locationData.name
    val locationNumber = locationName.split(" ").lastOrNull()?.toIntOrNull()
    
    return when (locationNumber) {
        1 -> Color(0xFF1976D2) // Blue for Location 1
        2 -> Color(0xFFF44336) // Red for Location 2
        3 -> Color(0xFF4CAF50) // Green for Location 3
        else -> Color(0xFF9C27B0) // Purple for any other location
    }
}

private data class LocationStats(
    val averageDifference: Int,
    val maxDifference: Int
)

private fun calculateStatsBetweenLocations(locations: List<LocationData>): LocationStats {
    if (locations.size < 2) {
        return LocationStats(0, 0)
    }
    
    val diffs = mutableListOf<Int>()
    
    // Compare each location with every other location
    for (i in 0 until locations.size - 1) {
        for (j in i + 1 until locations.size) {
            val matrix1 = locations[i].signalMatrix
            val matrix2 = locations[j].signalMatrix
            
            // Calculate element-wise absolute differences
            val elementDiffs = matrix1.zip(matrix2) { a, b -> Math.abs(a - b) }
            diffs.addAll(elementDiffs)
        }
    }
    
    return LocationStats(
        averageDifference = diffs.average().toInt(),
        maxDifference = diffs.maxOrNull() ?: 0
    )
}