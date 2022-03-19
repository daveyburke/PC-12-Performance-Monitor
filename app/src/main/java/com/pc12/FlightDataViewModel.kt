package com.pc12

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.lang.Float.NaN
import java.time.Instant.now

data class UIState (
    val avionicsData: AvionicsData = AvionicsData(0, 0),
    val perfData: PerfData = PerfData(NaN, 0, 0),
    val avionicsInterface: String = "",
    val age: Long = 0
)

class FlightDataViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = FlightDataViewModel::class.qualifiedName
    private val REQUEST_DATA_PERIOD_SEC = 5

    private val settingsStore = SettingsStore(application)
    private var isNetworkRunning : Boolean = false
    private lateinit var networkJob : Job
    private var userAgreedTerms = false
    private var autoAvionicsInterfaceType = SettingsStore.ECONNECT_INTERFACE
    private var lastRequestSuccessful = false
    private var lastSuccessTime: Long = 0

    var uiState by mutableStateOf(UIState())
        private set

    fun startNetworkRequests() {
        if (!isNetworkRunning && userAgreedTerms) {
            Log.i(TAG, "Starting network I/O coroutine...")
            networkRequestLoop()
            isNetworkRunning = true
        }
    }

    fun stopNetworkRequests() {
        if (isNetworkRunning) {
            Log.i(TAG, "Stopping network I/O coroutine...")
            networkJob.cancel()
            isNetworkRunning = false
        }
    }

    fun setUserAgreedTerms() {
        userAgreedTerms = true
    }

    fun getUserAgreedTerms() : Boolean {
        return userAgreedTerms
    }

    private fun networkRequestLoop() {
        networkJob = viewModelScope.launch {
            while (isActive) {
                var interfaceType = settingsStore.avionicsInterfaceFlow.first()
                if (interfaceType == SettingsStore.AUTO_DETECT_INTERFACE) {
                    Log.d(TAG, "Auto-detecting avionics interface")
                    interfaceType = autoDetectAvionicsInterfaceType()
                }
                val avionicsInterface = when (interfaceType) {
                    SettingsStore.GOGO_INTERFACE -> GogoAvionicsInterface()
                    SettingsStore.ECONNECT_INTERFACE -> EConnectAvionicsInterface()
                    else -> EConnectAvionicsInterface()  // should never get here
                }

                val data:AvionicsData?
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "Requesting data via " + SettingsStore.avionicsInterfaceToString(interfaceType))
                    data = avionicsInterface.requestData()
                }

                if (data != null) {
                    val aircraftType = settingsStore.aircraftTypeFlow.first()
                    val weight = settingsStore.aircraftWeightFlow.first()
                    Log.i(TAG, "Calculating torque for: " + data.altitude + " ft, " + data.outsideTemp + " celsius " +
                            "with aircraft type " + SettingsStore.aircraftTypeToString(aircraftType))
                    val newAvionicsData = AvionicsData(data.altitude, data.outsideTemp)
                    val newPerfData = PerfCalculator.compute(newAvionicsData, aircraftType, weight)

                    uiState = uiState.copy(
                        avionicsData = newAvionicsData,
                        perfData = newPerfData,
                        avionicsInterface = SettingsStore.avionicsInterfaceToString(interfaceType),
                        age = 0)
                    lastSuccessTime = now().epochSecond
                    lastRequestSuccessful = true
                } else {
                    uiState = uiState.copy(age = now().epochSecond - lastSuccessTime)
                    lastRequestSuccessful = false
                }

                delay(REQUEST_DATA_PERIOD_SEC * 1000L)
            }
        }
    }

    private fun autoDetectAvionicsInterfaceType(): Int {
        // In auto mode, we just round-robin across avionics interfaces until one works,
        // then stick with it until it doesn't
        if (!lastRequestSuccessful) {
            if (autoAvionicsInterfaceType == SettingsStore.ECONNECT_INTERFACE) {
                autoAvionicsInterfaceType = SettingsStore.GOGO_INTERFACE
            } else if (autoAvionicsInterfaceType == SettingsStore.GOGO_INTERFACE) {
                autoAvionicsInterfaceType = SettingsStore.ECONNECT_INTERFACE
            }
        }
        return autoAvionicsInterfaceType
    }
}
