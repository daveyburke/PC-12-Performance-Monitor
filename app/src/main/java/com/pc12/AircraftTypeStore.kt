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
 * Persist the chosen aircraft type.
 */
class AircraftTypeStore(private val context: Context) {
    companion object {  // singleton
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        val AIRCRAFT_TYPE = intPreferencesKey("aircraft_type")
        val PC_12_47E_MSN_1451_1942_4_Blade: Int = 0
        val PC_12_47E_MSN_1576_1942_5_Blade: Int = 1
        val PC_12_47E_MSN_2001_5_Blade: Int = 2
    }

    val aircraftTypeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AIRCRAFT_TYPE] ?: PC_12_47E_MSN_1576_1942_5_Blade
        }

    suspend fun saveAircraftModel(model: Int) {
        context.dataStore.edit { preferences ->
            preferences[AIRCRAFT_TYPE] = model
        }
    }

    fun aircraftTypeToString(type : Int) : String {
        return when (type) {
            PC_12_47E_MSN_1451_1942_4_Blade -> "MSN 1451-1942 4B"
            PC_12_47E_MSN_1576_1942_5_Blade -> "MSN 1576-1942 5B"
            PC_12_47E_MSN_2001_5_Blade -> "MSN 2001+ 5B"
            else -> "Unknown"
        }
    }
}