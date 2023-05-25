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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant.now

private const val MISSING_DATA = "---"
private const val DATA_MAX_AGE = 60 // 1min
private const val REQUEST_DATA_PERIOD_MSEC = 5000L
private const val REQUEST_DATA_RETRY_MSEC = 1000L
private val ROUND_ROBIN_AVIONICS = arrayOf(
    SettingsStore.ASPEN_INTERFACE,
    SettingsStore.ECONNECT_INTERFACE,
    SettingsStore.GOGO_INTERFACE
)

fun UIState(
    avionicsData: AvionicsData,
    perfData: PerfData,
    avionicsInterface: String,
    age: Long
): UIState {
    val deltaIsaTemp = avionicsData.outsideTemp + (avionicsData.altitude + 500) / 1000 * 2 - 15

    val altitudeStr =
        if (avionicsInterface == "") MISSING_DATA else avionicsData.altitude.toString()
    val outsideTempStr =
        if (avionicsInterface == "") MISSING_DATA else avionicsData.outsideTemp.toString()
    val deltaIsaTempStr = when {
        avionicsInterface == "" -> ""
        deltaIsaTemp > 0 -> "(ISA +$deltaIsaTemp)"
        deltaIsaTemp < 0 -> "(ISA $deltaIsaTemp)"
        else -> ""
    }

    val torqueStr =
        if (perfData.torque.isNaN() || age > DATA_MAX_AGE) MISSING_DATA else perfData.torque.toString()
    val fuelFlowStr =
        if (perfData.torque.isNaN() || age > DATA_MAX_AGE || perfData.fuelFlow == 0) MISSING_DATA else perfData.fuelFlow.toString()
    val airspeedStr =
        if (perfData.torque.isNaN() || age > DATA_MAX_AGE || perfData.airspeed == 0) MISSING_DATA else perfData.airspeed.toString()

    val isOldData = isDataOld(age, avionicsInterface)
    return UIState(
        altitudeStr,
        outsideTempStr,
        deltaIsaTempStr,
        torqueStr,
        fuelFlowStr,
        airspeedStr,
        getAvionicsLabel(age, avionicsInterface),
        isOldData
    )
}

data class UIState(
    val altitude: String = MISSING_DATA,
    val outsideTemp: String = MISSING_DATA,
    val deltaIsaTemp: String = "",
    val torqueStr: String = MISSING_DATA,
    val fuelFlowStr: String = MISSING_DATA,
    val airspeed: String = MISSING_DATA,
    val avionicsLabel: String = "",
    val isDataOld: Boolean = true
)

class FlightDataViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = FlightDataViewModel::class.qualifiedName

    private val theApp = application
    private val settingsStore = SettingsStore(application)
    private var isNetworkJobRunning : Boolean = false
    private lateinit var networkJob : Job
    private var userAgreedTerms = false
    private var roundRobinAvionicsIndex = 0
    private var lastRequestSuccessful = true
    private var lastSuccessTime: Long = 0
    private var wifiNetwork: Network? = null
    private var wifiNetworkCallback: NetworkCallback? = null

    var uiState by mutableStateOf(UIState())
        private set

    fun startNetworkRequests() {
        if (!isNetworkJobRunning && userAgreedTerms) {
            Log.i(TAG, "Starting network I/O coroutine...")
            registerWiFiNetworkCallback()
            networkRequestLoop()
            isNetworkJobRunning = true

        }
    }

    fun stopNetworkRequests() {
        if (isNetworkJobRunning) {
            Log.i(TAG, "Stopping network I/O coroutine...")
            networkJob.cancel()
            unregisterWiFiNetworkCallback()
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
            while (isActive) {
                if (wifiNetwork == null) {
                    Log.w(TAG, "Wi-Fi network not ready")
                    delay(REQUEST_DATA_RETRY_MSEC)
                    continue
                }

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

                val avionicsInterfaceStr = SettingsStore.avionicsInterfaceToString(interfaceType)
                Log.i(TAG, "Requesting data via $avionicsInterfaceStr")
                val data : AvionicsData?
                withContext(Dispatchers.IO) {
                    data = avionicsInterface.requestData(wifiNetwork!!)
                }  // end I/O CoroutineScope

                if (data != null) {
                    val aircraftType = settingsStore.aircraftTypeFlow.first()
                    val weight = settingsStore.aircraftWeightFlow.first()

                    Log.i(TAG, "Calculating torque for: $data, " +
                            SettingsStore.aircraftWeightToString(weight) + ", " +
                            SettingsStore.aircraftTypeToString(aircraftType))

                    val perfData = PerfCalculator.compute(data, aircraftType, weight)

                    uiState = UIState(
                        data,
                        perfData,
                        avionicsInterfaceStr,
                        0
                    )
                    lastSuccessTime = now().epochSecond
                    lastRequestSuccessful = true
                    delay(REQUEST_DATA_PERIOD_MSEC)
                } else {
                    val age = now().epochSecond - lastSuccessTime
                    uiState = uiState.copy(
                        avionicsLabel = getAvionicsLabel(age, avionicsInterfaceStr),
                        isDataOld = isDataOld(age, avionicsInterfaceStr)
                    )
                    lastRequestSuccessful = false
                    delay(REQUEST_DATA_RETRY_MSEC)
                }
            }
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
            wifiNetworkCallback = null
            wifiNetwork = null
        }
    }
}

private fun getAvionicsLabel(age: Long, avionicsInterface: String): String {
    var avionicsLabel = ""
    val ageStr = if (age > 60) (age / 60).toString() + "m" else "$age" + "s"

    if (avionicsInterface != "") {
        avionicsLabel += " - $avionicsInterface"
        if (age > 0) avionicsLabel += " ($ageStr old)"
    } else {
        avionicsLabel += " - Searching..."
    }

    return avionicsLabel
}

private fun isDataOld(
    age: Long,
    avionicsInterface: String
) = age > DATA_MAX_AGE || avionicsInterface == ""