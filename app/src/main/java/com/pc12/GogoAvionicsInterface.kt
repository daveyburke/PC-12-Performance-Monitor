package com.pc12

import android.util.Log
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Validated with L3 Avance 4.3
 */
class GogoAvionicsInterface() : AvionicsInterface {
    private val TAG = GogoAvionicsInterface::class.qualifiedName
    private var TIMEOUT = 1000

    private val GOGO_URI =
        "https://fp3d.gogo.aero/fp3d_fcgi-php/portal/public/index.php?_url=/index/getFile&path=last"

    override suspend fun requestData(): AvionicsData? {
        try {
            val url = URL(GOGO_URI)
            val httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.setConnectTimeout(TIMEOUT)
            httpURLConnection.setReadTimeout(TIMEOUT)
            httpURLConnection.requestMethod = "GET"
            httpURLConnection.doInput = true
            httpURLConnection.doOutput = false

            val responseCode = httpURLConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = httpURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }
                Log.d(TAG, "HTTP response body: " + response)
                val jsonObject = JSONTokener(response).nextValue() as JSONObject

                val altitude = jsonObject.getInt("altitudeFeet")
                val outsideTemp = jsonObject.getInt("outsideTemp")
                return AvionicsData(altitude, outsideTemp)
            } else {
                Log.e(TAG, "Bad HTTP response code " + responseCode)
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }
        return null
    }
}