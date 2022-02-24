package com.pc12

import java.io.IOException

/**
 * Interface to request avionics data.
 */
interface AvionicsInterface {
    @Throws(IOException::class)
    suspend fun requestData(): AvionicsData?
}