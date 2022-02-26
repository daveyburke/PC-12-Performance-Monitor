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
    val perfData: PerfData = PerfData(NaN),
    val age: Long = 0
)

class FlightDataViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = FlightDataViewModel::class.qualifiedName
    private val REQUEST_DATA_PERIOD_SEC = 5

    private val aircraftTypeStore = AircraftTypeStore(application)
    private var avionicsInterface = GogoAvionicsInterface()  // TODO: Add EConnect option
    private var isNetworkRunning : Boolean = false
    private lateinit var networkJob : Job
    private var userAgreedTerms = false

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
            var lastSuccessTime: Long = now().getEpochSecond()
            while (isActive) {
                val data:AvionicsData?
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "Requesting data...")
                    data = avionicsInterface.requestData()
                }

                if (data != null) {
                    val aircraftType = aircraftTypeStore.aircraftTypeFlow.first()
                    Log.i(TAG, "Using aircraft model: " + aircraftTypeStore.aircraftTypeToString(aircraftType))

                    Log.i(TAG, "Calculating torque for: " + data.altitude + "ft, " + data.outsideTemp + "c")
                    val newAvionicsData = AvionicsData(data.altitude, data.outsideTemp)
                    val newPerfData = PerfCalculator.compute(aircraftType, newAvionicsData)

                    uiState = uiState.copy(avionicsData = newAvionicsData, perfData = newPerfData, age = 0)
                    lastSuccessTime = now().getEpochSecond()
                } else {
                    val elapsedTime = now().getEpochSecond() - lastSuccessTime
                    uiState = uiState.copy(age = elapsedTime)
                }

                delay(REQUEST_DATA_PERIOD_SEC * 1000L)
            }
        }
    }
}
