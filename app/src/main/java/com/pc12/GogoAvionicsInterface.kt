package com.pc12

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Interface via Gogo Wi-Fi.
 * Validated with L3 Avance 4.3
 * HTTP GET / JSON response protocol.
 */
class GogoAvionicsInterface : AvionicsInterface {
    private val TAG = GogoAvionicsInterface::class.qualifiedName
    private val NETWORK_TIMEOUT_SEC = 1L
    private val POSITION_TIMEOUT_SEC = 60L
    private val GOGO_URI =
        "https://fp3d.gogo.aero/fp3d_fcgi-php/portal/public/index.php?_url=/index/getFile&path=last"

    companion object {
        var lastKnownLat: String = ""
        var lastKnownLon: String = ""
        var lastChange: Long = 0
    }

    override suspend fun requestData(): AvionicsData? {
        val client = OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(GOGO_URI)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body!!.string()
                    try {
                        val jsonObject = JSONTokener(body).nextValue() as JSONObject
                        val altitude = jsonObject.getInt("altitudeFeet")
                        val outsideTemp = jsonObject.getInt("outsideTemp")
                        val presentLat = jsonObject.getString("presentLat")
                        val presentLon = jsonObject.getString("presentLon")
                        if (livenessCheck(presentLat, presentLon)) {
                            return AvionicsData(altitude, outsideTemp)
                        }
                    } catch (e: JSONException) {
                        Log.e(TAG, "Could not parse: $body")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error $e")
        }

        return null  // fail
    }

    // Noticed bug where video.gogo.aero data can freeze so detect this here
    // TODO: File bug against Gogo to fix
    private fun livenessCheck(presentLat: String, presentLon: String): Boolean {
        var positionChanged = false
        if (presentLat != lastKnownLat) {
            lastKnownLat = presentLat
            positionChanged = true

        }
        if (presentLon != lastKnownLon) {
            lastKnownLon = presentLon
            positionChanged = true

        }

        if (positionChanged) {
            lastChange = Instant.now().getEpochSecond()
        } else {
            if (Instant.now().getEpochSecond() - lastChange > POSITION_TIMEOUT_SEC) {
                Log.w(TAG, "Liveness check failed")
                return false
            }
        }

        return true
    }
}

/*
 * Sample response (note outsideTemp doesn't appear when on ground):
 * {"altitudeFeet":23005,"departBaggageId":"BFI","departureId":"KBFI",
 * "departureLat":47.53329849243164,"departureLon":-122.3000030517578,
 * "destBaggageId":"PAO","destinationId":"KPAO","destinationLat":37.383300781250,
 * "destinationLon":-122.0670013427734,"distanceToDestinationNauticalMiles":80,"doorsClosed":0,
 * "estimatedTimeOfArrival":879,"flightNumber":"","fltRev":1,"groundSpeedKnots":272,"mach":0,
 * "outsideTemp":-28,"positionValid":true,"presentLat":38.54950,"presentLon":-122.91160,
 * "presentPhase":"Cruise","tailNumber":"","time":1645309110,"timeToDestination":21,
 * "trueHeading":175,"weightOnWheels":0,"windDirection":249}
 */