package com.pc12

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persist settings
 */
class SettingsStore(private val context: Context) {
    companion object {  // singleton
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        val AIRCRAFT_TYPE = intPreferencesKey("aircraft_type")
        val WIFI_TYPE = intPreferencesKey("wifi_type")

        val PC_12_47E_MSN_1451_1942_4_Blade: Int = 0
        val PC_12_47E_MSN_1576_1942_5_Blade: Int = 1
        val PC_12_47E_MSN_2001_5_Blade: Int = 2

        val GOGO_WIFI: Int = 0
        val ECONNECT_WIFI: Int = 1

        fun aircraftTypeToString(type : Int) : String {
            return when (type) {
                PC_12_47E_MSN_1451_1942_4_Blade -> "PC-12/47E MSN 1451-1942 4 Blade"
                PC_12_47E_MSN_1576_1942_5_Blade -> "PC-12/47E MSN 1576-1942 5 Blade"
                PC_12_47E_MSN_2001_5_Blade -> "PC-12/47E MSN 2001+ 5 Blade"
                else -> "Unknown"
            }
        }
        fun wifiTypeToString(type : Int) : String {
            return when (type) {
                GOGO_WIFI -> "Gogo Wi-Fi"
                ECONNECT_WIFI -> "Emteq eConnect"
                else -> "Unknown"
            }
        }
    }

    val aircraftTypeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AIRCRAFT_TYPE] ?: PC_12_47E_MSN_1576_1942_5_Blade
        }

    suspend fun saveAircraftType(type: Int) {
        context.dataStore.edit { preferences ->
            preferences[AIRCRAFT_TYPE] = type
        }
    }

    val wifiTypeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[WIFI_TYPE] ?: GOGO_WIFI
        }

    suspend fun saveWifiType(type: Int) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_TYPE] = type
        }
    }
}