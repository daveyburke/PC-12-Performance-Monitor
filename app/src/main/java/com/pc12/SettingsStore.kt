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
        val AVIONICS_INTERFACE = intPreferencesKey("avionics_interface")
        val AIRCRAFT_WEIGHT= intPreferencesKey("aircraft_weight")

        const val PC_12_47E_MSN_1001_1942_4_Blade: Int = 0
        const val PC_12_47E_MSN_1576_1942_5_Blade: Int = 1
        const val PC_12_47E_MSN_2001_5_Blade: Int = 2  // NGX

        const val ASPEN_INTERFACE: Int = 0
        const val ECONNECT_INTERFACE: Int = 1
        const val GOGO_INTERFACE: Int = 2
        const val AUTO_DETECT_INTERFACE: Int = 3

        const val WEIGHT_7000: Int = 0
        const val WEIGHT_8000: Int = 1
        const val WEIGHT_9000: Int = 2
        const val WEIGHT_10000: Int = 3
        const val WEIGHT_10400: Int = 4

        fun aircraftTypeToString(type : Int) : String {
            return when (type) {
                PC_12_47E_MSN_1001_1942_4_Blade -> "PC-12/47E MSN 1001-1942 4 Blade"
                PC_12_47E_MSN_1576_1942_5_Blade -> "PC-12/47E MSN 1576-1942 5 Blade"
                PC_12_47E_MSN_2001_5_Blade -> "PC-12/47E MSN 2001+ 5 Blade"
                else -> "Unknown"
            }
        }
        fun avionicsInterfaceToString(type : Int) : String {
            return when (type) {
                ASPEN_INTERFACE -> "Aspen"
                ECONNECT_INTERFACE -> "eConnect"
                GOGO_INTERFACE -> "Gogo"
                AUTO_DETECT_INTERFACE -> "Auto-detect"
                else -> "Unknown"
            }
        }
        fun aircraftWeightToString(type : Int) : String {
            return when (type) {
                WEIGHT_7000 -> "7000 lbs"
                WEIGHT_8000 -> "8000 lbs"
                WEIGHT_9000 -> "9000 lbs"
                WEIGHT_10000 -> "10000 lbs"
                WEIGHT_10400 -> "10400 lbs"
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

    val avionicsInterfaceFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AVIONICS_INTERFACE] ?: ASPEN_INTERFACE
        }

    suspend fun saveAvionicsInterface(avionics: Int) {
        context.dataStore.edit { preferences ->
            preferences[AVIONICS_INTERFACE] = avionics
        }
    }

    val aircraftWeightFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AIRCRAFT_WEIGHT] ?: WEIGHT_8000
        }

    suspend fun saveAircraftWeight(weight: Int) {
        context.dataStore.edit { preferences ->
            preferences[AIRCRAFT_WEIGHT] = weight
        }
    }
}