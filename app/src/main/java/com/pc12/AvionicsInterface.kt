package com.pc12

import android.net.Network

/**
 * Interface to request avionics data.
 */
interface AvionicsInterface {
    suspend fun requestData(network: Network): AvionicsData?
}