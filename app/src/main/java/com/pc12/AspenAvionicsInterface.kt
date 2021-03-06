package com.pc12

import android.net.Network
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant.now
import java.util.concurrent.TimeUnit

/**
 * Interface via Aspen CG100 Gateway.
 * HTTP GET + ARINC-429 over TCP.
 * (Credit Chad Brubaker for assistance with protocol analysis)
 */
class AspenAvionicsInterface : AvionicsInterface {
    private val TAG = AspenAvionicsInterface::class.qualifiedName
    private val ASPEN_IP = "10.22.44.1"
    private val ASPEN_WSDL_PORT = 8188
    private val ASPEN_SOCKET_PORT = 9399
    private val NETWORK_TIMEOUT_SEC = 1L
    private val SOCKET_TIMEOUT_MSEC = 3000
    private val CREDENTIALS = "SG9uZXl3ZWxsUDpYUmZ0UFprUXkyZVpiSmphNjVuc0pVMis="
    private val ARINC_SAT_LABEL = 213
    private val ARINC_ALTITUDE_LABEL = 204  // (baro corrected altitude)
    private val INT_NAN = -99

    private var altitude = INT_NAN
    private var outsideTemp = INT_NAN

    override suspend fun requestData(network: Network): AvionicsData? {
        if (probeService(network)) {
            val socket = Socket()

            try {
                Log.i(TAG, "Connecting socket")
                network.bindSocket(socket)
                socket.connect(InetSocketAddress(ASPEN_IP, ASPEN_SOCKET_PORT), SOCKET_TIMEOUT_MSEC)
                socket.soTimeout = SOCKET_TIMEOUT_MSEC
                val input = DataInputStream(socket.getInputStream())
                val buf = ByteArray(4)
                val start = now().toEpochMilli()

                do {
                    // Encapsulation: 2-byte len | 0x00 0x02 | ARINC-429 32-bit words
                    val lenBytes = ByteArray(2)
                    input.readFully(lenBytes)

                    val padBytes = ByteArray(2)
                    input.readFully(padBytes)

                    val len = (lenBytes[0].toInt() shl 8) + lenBytes[1].toInt()
                    if (padBytes[0] == 0x00.toByte() && padBytes[1] == 0x02.toByte() && len % 4 == 0) {
                        for (i in 0 until len/4) {
                            input.readFully(buf)
                            parseArinc429(buf)
                        }
                    } else {
                        Log.e(TAG, "Invalid length/padding")
                        break
                    }
                } while ((altitude == INT_NAN || outsideTemp == INT_NAN) &&
                    (now().toEpochMilli() - start) < SOCKET_TIMEOUT_MSEC)
            } catch (e: Exception) {
                Log.e(TAG, "Socket connection error $e")
            } finally {
                try {
                    if (!socket.isClosed) {
                        Log.i(TAG, "Closing socket")
                        socket.shutdownOutput()
                        socket.close()
                    }
                } catch (e: Exception) {}
            }
        }

        return if (altitude != INT_NAN && outsideTemp != INT_NAN) {
            AvionicsData(altitude, outsideTemp)
        } else {
            null
        }
    }

    private fun probeService(network: Network): Boolean {
        val client = OkHttpClient.Builder()
            .callTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
            .socketFactory(network.socketFactory)
            .build()
        val request = Request.Builder()
            .url("http://$ASPEN_IP:$ASPEN_WSDL_PORT/wdls/ping")
            .addHeader("Authorization", " basic $CREDENTIALS")
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Found gateway")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error $e")
        }

        return false
    }

    private fun parseArinc429(buf: ByteArray) {
        // https://en.wikipedia.org/wiki/ARINC_429
        // ARINC-429 in network order but label byte is reversed:
        // Bit 32 - Bit 25 | Bit 24 - Bit 17 | Bit 16 - Bit 9 | Bit 1 - Bit 8
        val label = ((buf[3].toInt() shr 6) and 0x03) * 100 +
                    ((buf[3].toInt() shr 3 and 0x07)) * 10 +
                    (buf[3].toInt() and 0x07) // octal to decimal
        Log.i(TAG, "ARINC-429 label: $label (${labelToText(label)})")

        // Data field in bits 28 to 11. Data interpretation:
        // 1110...1 is (1/2 + 1/4 + 1/8 + 0/16 + ... + 1/2^18) * RANGE
        // Bit 29 is the sign bit (i.e. two's complement)
        val data = ((buf[0].toInt() and 0x0F) shl 14) +
                   ((buf[1].toInt() shl 6) and 0x3FFF) +
                   ((buf[2].toInt() shr 2) and 0x3F)
        val signBit = (buf[0].toInt() ushr 4) and 0x01

        if (label == ARINC_SAT_LABEL) {
            outsideTemp = signBit * -512 + (data.toFloat() / 0x40000.toFloat() * 512f).toInt()
            Log.i(TAG, "ARINC-429 SAT: $outsideTemp")
        } else if (label == ARINC_ALTITUDE_LABEL) {
            altitude = signBit * -131072 + (data.toFloat() / 0x40000.toFloat() * 131072f).toInt()
            Log.i(TAG, "ARINC-429 altitude: $altitude")
        }
    }

    private fun labelToText(label: Int) : String {
        return when (label) {  // from ARINC-429 spec, not confirmed
            1 -> "Distance To Go"
            2 -> "Time To Go"
            10 -> "Latitude"
            11 -> "Longitude"
            12 -> "Ground Speed"
            14 -> "Magnetic Heading"
            15 -> "Wind Speed"
            16 -> "Wind Direction"
            56 -> "ETA"
            74 -> "Zero Fuel Weight"
            75 -> "Gross Weight"
            113 -> "Spare"
            114 -> "Desired Track"
            115 -> "Waypoint Bearing"
            125 -> "Universal Time Coordinated (UTC)"
            147 -> "Discrete Status 4 EFIS"
            150 -> "Universal Time Constant (UTC)"
            152 -> "Cabin Pressure"
            203 -> "Altitude"
            204 -> "Baro Altitude"
            205 -> "Indicated Airspeed"
            210 -> "True Airspeed"
            213 -> "Static Air Temperature"
            244 -> "Fuel Flow"
            247 -> "Fuel Flow"
            251 -> "Distance to Go"
            252 -> "Time to Go"
            260 -> "Date"
            303 -> "Application Dependent"
            304 -> "Application Dependent"
            305 -> "Application Dependent"
            306 -> "Application Dependent"
            307 -> "Application Dependent"
            310 -> "Present Position - Latitude"
            311 -> "Present Position - Longitude"
            312 -> "Ground Speed"
            313 -> "Track Angle True"
            314 -> "True Heading"
            315 -> "Wind Speed"
            316 -> "Wind Direction"
            320 -> "Magnetic Heading"
            351 -> "Maintenance Data"
            352 -> "Maintenance Data"
            371 -> "General Aviation Equipment Identifier"
            else -> "Unknown"
        }
    }
}