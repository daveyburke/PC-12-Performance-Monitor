package com.pc12

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val REQUEST_DATA_PERIOD_MSEC = 5000L
    private val REQUEST_DATA_RETRY_MSEC = 1000L
    private val ROUND_ROBIN_AVIONICS = arrayOf(SettingsStore.ASPEN_INTERFACE,
                                               SettingsStore.ECONNECT_INTERFACE,
                                               SettingsStore.GOGO_INTERFACE )

    private val theApp = application
    private val settingsStore = SettingsStore(application)
    private var isNetworkJobRunning : Boolean = false
    private lateinit var networkJob : Job
    private var userAgreedTerms = false
    private var roundRobinAvionicsIndex = 0
    private var lastRequestSuccessful = false
    private var lastSuccessTime: Long = 0
    private var wifiNetwork: Network? = null
    private var wifiNetworkCallback: NetworkCallback? = null

    var uiState by mutableStateOf(UIState())
        private set

    fun startNetworkRequests() {
        if (!isNetworkJobRunning && userAgreedTerms) {
            Log.i(TAG, "Starting network I/O coroutine...")
            networkRequestLoop()
            isNetworkJobRunning = true
        }
    }

    fun stopNetworkRequests() {
        if (isNetworkJobRunning) {
            Log.i(TAG, "Stopping network I/O coroutine...")
            networkJob.cancel()
            isNetworkJobRunning = false
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
            registerWiFiNetworkCallback()
            while (isActive) {
                var interfaceType = settingsStore.avionicsInterfaceFlow.first()
                if (interfaceType == SettingsStore.AUTO_DETECT_INTERFACE) {
                    Log.d(TAG, "Auto-detecting avionics interface")
                    interfaceType = autoDetectAvionicsInterfaceType()
                }
                val avionicsInterface = when (interfaceType) {
                    SettingsStore.ASPEN_INTERFACE -> AspenAvionicsInterface()
                    SettingsStore.ECONNECT_INTERFACE -> EConnectAvionicsInterface()
                    SettingsStore.GOGO_INTERFACE -> GogoAvionicsInterface()
                    else -> AspenAvionicsInterface()  // should never get here
                }

                Log.i(TAG, "Requesting data via " +
                        SettingsStore.avionicsInterfaceToString(interfaceType))

                val data : AvionicsData?
                withContext(Dispatchers.IO) {
                    data = if (wifiNetwork != null) {
                        avionicsInterface.requestData(wifiNetwork!!)
                    } else {
                        Log.e(TAG, "Unable to get Wi-Fi network")
                        null
                    }
                }  // end I/O CoroutineScope

                if (data != null) {
                    val aircraftType = settingsStore.aircraftTypeFlow.first()
                    val weight = settingsStore.aircraftWeightFlow.first()

                    Log.i(TAG, "Calculating torque for: $data, " +
                            SettingsStore.aircraftWeightToString(weight) + ", " +
                            SettingsStore.aircraftTypeToString(aircraftType))

                    val perfData = PerfCalculator.compute(data, aircraftType, weight)

                    uiState = uiState.copy(
                        avionicsData = data,
                        perfData = perfData,
                        avionicsInterface = SettingsStore.avionicsInterfaceToString(interfaceType),
                        age = 0)
                    lastSuccessTime = now().epochSecond
                    lastRequestSuccessful = true
                    delay(REQUEST_DATA_PERIOD_MSEC)
                } else {
                    uiState = uiState.copy(age = now().epochSecond - lastSuccessTime)
                    lastRequestSuccessful = false
                    delay(REQUEST_DATA_RETRY_MSEC)
                }
            }
            unregisterWiFiNetworkCallback()
        }
    }

    private fun autoDetectAvionicsInterfaceType(): Int {
        // In auto mode, we just round-robin across avionics interfaces until one works,
        // then stick with it until it doesn't
        if (!lastRequestSuccessful) {
            if (++roundRobinAvionicsIndex == ROUND_ROBIN_AVIONICS.size) {
                roundRobinAvionicsIndex = 0
            }
        }
        return ROUND_ROBIN_AVIONICS[roundRobinAvionicsIndex]
    }

    private fun registerWiFiNetworkCallback() {
        if (wifiNetworkCallback == null) {
            wifiNetworkCallback = object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Network available: $network")
                    wifiNetwork = network
                }
                override fun onLost(network: Network) {
                    if (network == wifiNetwork) {
                        Log.i(TAG, "Network lost: $network")
                        wifiNetwork = null
                    }
                }
            }
            val rb = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            val cm = theApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.requestNetwork(rb.build(), wifiNetworkCallback as NetworkCallback)
        }
    }

    private fun unregisterWiFiNetworkCallback() {
        if (wifiNetworkCallback != null) {
            val cm = theApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(wifiNetworkCallback as NetworkCallback)
        }
    }
}
