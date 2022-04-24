package com.pc12

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.DataInputStream
import java.net.Socket
import java.time.Instant.now
import java.util.concurrent.TimeUnit

/**
 * Interface via Aspen CG100 Gateway Wi-Fi.
 * HTTP GET + ARINC-429 over TCP socket.
 */
class AspenAvionicsInterface : AvionicsInterface {
    private val TAG = AspenAvionicsInterface::class.qualifiedName
    private val ASPEN_IP = "10.22.44.1"
    private val WSDL_PORT = 8188
    private val SOCKET_PORT = 9399
    private val NETWORK_TIMEOUT_SEC = 1L
    private val SOCKET_TIMEOUT_SEC = 3L
    private val CREDENTIALS = "SG9uZXl3ZWxsUDpYUmZ0UFprUXkyZVpiSmphNjVuc0pVMis="
    private val INT_NAN = -99

    private var altitude = INT_NAN
    private var outsideTemp = INT_NAN

    override suspend fun requestData(): AvionicsData? {
        if (probeService()) {
            // Encapsulation: 2-byte len | 0x00 0x02 | ARINC-429 32-bit words
            try {
                val socket = Socket(ASPEN_IP, SOCKET_PORT)
                socket.soTimeout = SOCKET_TIMEOUT_SEC.toInt()

                val input = DataInputStream(socket.getInputStream())
                val start = now().epochSecond
                val buf = ByteArray(4)

                do {
                    val lenBytes = ByteArray(2)
                    input.readFully(lenBytes)

                    val padBytes = ByteArray(2)
                    input.readFully(padBytes)

                    val len = (lenBytes[0].toInt() shl 8 and lenBytes[1].toInt())
                    Log.e(TAG, "Data length %len found")
                    if (padBytes[0] == 0x00.toByte() && padBytes[1] == 0x02.toByte() && len % 4 == 0) {
                        for (i in 0 until len/4) {
                            input.readFully(buf)
                            parseArinc429(buf)
                        }
                    } else {
                        Log.e(TAG, "Invalid length/padding")
                        break
                    }
                } while (altitude == INT_NAN || outsideTemp == INT_NAN &&
                    (now().epochSecond - start) < SOCKET_TIMEOUT_SEC)

                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Connection error $e")
            }
        }

        return if (altitude != INT_NAN && outsideTemp != INT_NAN) {
            AvionicsData(altitude, outsideTemp)
        } else {
            null
        }
    }

    private fun probeService(): Boolean {
        val client = OkHttpClient.Builder()
            .callTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("http://$ASPEN_IP:$WSDL_PORT/wdls/ping")
            .addHeader("Authorization", "basic $CREDENTIALS")
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
        val label = (buf[3].toInt() shr 6 and 0x03).toByte() * 100 +
                    (buf[3].toInt() shr 3 and 0x07).toByte() * 10 +
                    (buf[3].toInt() and 0x07).toByte()  // octal to decimal
        Log.i(TAG, "ARINC-429 label: $label")

        // Data field in bits 28 to 11
        // Example data field: 1110...1 is (1/2 + 1/4 + 1/8 + ... 1/2^18) times a max range
        // Bit 29 is sign bit (two's complement)
        val data = (buf[0].toInt() shl 16 + buf[1].toInt() shl 8 + buf[2].toInt()) shr 2 and 0x3FFFF
        val negative = buf[0].toInt() and 0x80 == 0x80 //

        if (label == 213) {  // SAT
            outsideTemp = data / 0x40000 * 512 + if (negative) -512 else 0
        } else if (label == 203 ) {  // altitude
            altitude = data / 0x40000 * 131072 + (if (negative) -131072 else 0)
        }
    }
}