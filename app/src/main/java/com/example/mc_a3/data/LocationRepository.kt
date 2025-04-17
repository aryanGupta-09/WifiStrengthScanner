package com.example.mc_a3.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mc_a3.model.LocationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("wifi_locations")

class LocationRepository(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Predefined location names for the app
    val predefinedLocations = listOf("Location 1", "Location 2", "Location 3")
    
    // Save location data
    suspend fun saveLocationData(locationData: LocationData) {
        val key = stringPreferencesKey(locationData.name)
        context.dataStore.edit { preferences ->
            preferences[key] = json.encodeToString(locationData)
        }
    }
    
    // Get location data for a specific location
    fun getLocationData(locationName: String): Flow<LocationData?> {
        val key = stringPreferencesKey(locationName)
        return context.dataStore.data.map { preferences ->
            preferences[key]?.let { json.decodeFromString<LocationData>(it) }
        }
    }
    
    // Get all saved location data
    fun getAllLocationData(): Flow<Map<String, LocationData>> {
        return context.dataStore.data.map { preferences ->
            predefinedLocations.associateWith { locationName ->
                val key = stringPreferencesKey(locationName)
                preferences[key]?.let { json.decodeFromString<LocationData>(it) }
            }.filterValues { it != null } as Map<String, LocationData>
        }
    }
    
    // Check if location data exists
    fun hasLocationData(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            predefinedLocations.any { locationName ->
                val key = stringPreferencesKey(locationName)
                preferences.contains(key)
            }
        }
    }
}