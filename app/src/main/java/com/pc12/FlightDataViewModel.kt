package com.pc12

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.util.Log
import android.widget.Toast
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
    private val REQUEST_DATA_PERIOD_MSEC = 5000L
    private val REQUEST_DATA_RETRY_MSEC = 1000L

    private val theApp = application
    private val settingsStore = SettingsStore(application)
    private var isNetworkJobRunning : Boolean = false
    private lateinit var networkJob : Job
    private var lastRequestSuccessful = true
    private var lastSuccessTime: Long = 0
    private var wifiNetwork: Network? = null
    private var wifiNetworkCallback: NetworkCallback? = null

    var uiState by mutableStateOf(UIState())
        private set

    fun startNetworkRequests() {
        if (!isNetworkJobRunning) {
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

    private fun networkRequestLoop() {
        networkJob = viewModelScope.launch {
            registerWiFiNetworkCallback()
            while (isActive) {
                if (wifiNetwork == null) {
                    Log.w(TAG, "Wi-Fi network not ready")
                    delay(REQUEST_DATA_RETRY_MSEC)
                    continue
                }

                val interfaceType = settingsStore.avionicsInterfaceFlow.first()
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
            unregisterWiFiNetworkCallback()
        }
    }

    private suspend fun registerWiFiNetworkCallback() {
        if (wifiNetworkCallback == null) {
            wifiNetworkCallback = object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Network available: $network")
                    Toast.makeText(
                        theApp,
                        "Connected to network",
                        Toast.LENGTH_LONG
                    ).show()
                    wifiNetwork = network
                }
                override fun onLost(network: Network) {
                    if (network == wifiNetwork) {
                        Log.i(TAG, "Network lost: $network")
                        wifiNetwork = null
                    }
                }
                override fun onUnavailable() {
                    Log.e(TAG, "Network onUnavailable")
                    Toast.makeText(
                        theApp,
                        "Could not connect to network",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            val rb = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

            // If user has set an app-specific Wi-Fi network then add this to request
            val networkSsid = settingsStore.networkSsidFlow.first()
            val networkPassword = settingsStore.networkPasswordFlow.first()
            if (!networkSsid.isEmpty()) {
                Log.i(TAG, "Specific network requested: $networkSsid")
                Toast.makeText(
                    theApp,
                    "Connecting to $networkSsid",
                    Toast.LENGTH_LONG
                ).show()
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(networkSsid)
                    .setWpa2Passphrase(networkPassword)
                    .build()
                rb.setNetworkSpecifier(specifier)
            }

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