package com.pc12

import android.net.Network
import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.lang.NumberFormatException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Interface via Emteq eConnect Wi-Fi.
 * HTTP GET + Websocket protocol.
 */
class EConnectAvionicsInterface : AvionicsInterface, WebSocketListener() {
    private val TAG = EConnectAvionicsInterface::class.qualifiedName
    private val ECONNECT_IP = "10.0.9.1"
    private val NETWORK_TIMEOUT_SEC = 1L
    private val WEBSOCKET_TIMEOUT_SEC = 3L
    private val NORMAL_CLOSURE_STATUS = 1000
    private val INT_NAN = -99

    private val responseLock = ReentrantLock()
    private val responseSignal = responseLock.newCondition()
    private var altitude = INT_NAN
    private var outsideTemp = INT_NAN

    override suspend fun requestData(network: Network): AvionicsData? {
        val url = getWebSocketAddr(network)
        if (url != null) {
            val client = OkHttpClient.Builder()
                .callTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                .socketFactory(network.socketFactory)
                .build()
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                client.newWebSocket(request, this)
            } catch (e: Exception) {
                Log.e(TAG, "Connection error $e")
            }

            responseLock.withLock {  // wait for Websocket protocol to complete
                responseSignal.await(WEBSOCKET_TIMEOUT_SEC, TimeUnit.SECONDS)
            }
        }

        return if (altitude != INT_NAN && outsideTemp != INT_NAN) {
            AvionicsData(altitude, outsideTemp)
        } else {
            null
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Websocket received: $text")

        if (text == "5:::{\"name\":\"connected\"}") {
            webSocket.send("5:::{\"name\":\"fms_data:code\",\"args\":[\"2\"]}")  // alt
            webSocket.send("5:::{\"name\":\"fms_data:code\",\"args\":[\"13\"]}") // temp
        } else {
            if (text.contains("fms_data:code:2")) {
                altitude = getJsonValueToInt(text)
            } else if (text.contains("fms_data:code:13")) {
                outsideTemp = getJsonValueToInt(text)
                webSocket.close(NORMAL_CLOSURE_STATUS, null)  // causes socket error
            }
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d(TAG, "Websocket closed $code")
        responseLock.withLock {
            responseSignal.signal()
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        Log.e(TAG,"Websocket error : " + t.message)
        responseLock.withLock {
            responseSignal.signal()
        }
    }

    private fun getWebSocketAddr(network: Network): String? {
        val BASE = "ws://$ECONNECT_IP/socket.io/1/websocket/"
        val client = OkHttpClient.Builder()
            .callTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
            .socketFactory(network.socketFactory)
            .build()
        val request = Request.Builder()
            .url("http://$ECONNECT_IP/socket.io/1/?t=0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body!!.string()
                    return BASE + body.substringBefore(":")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error $e")
        }
        return null
    }

    @Throws (NumberFormatException::class)
    private fun getJsonValueToInt(text: String): Int {
        try {
            val jsonObject = JSONTokener(text.substring(4)).nextValue() as JSONObject
            val args = jsonObject.getJSONArray("args") as JSONArray
            val jsonInnerObject = args[0] as JSONObject
            val valueStr = jsonInnerObject.getString("value")
            return valueStr.toInt()
        } catch (e: JSONException) {
            Log.e(TAG, "Could not parse: $text")
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Could not parse: $text")
        }
        return INT_NAN
    }
}

/*
 * Sample response over Websocket:
 * 5:::{"name":"fms_data:code:2","args":[{"id":2,"gui_code":2,"label":"Altitude","units":"Feet","value":"24999"}]}
 */
