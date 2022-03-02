package com.pc12

/**
 * Interface to request avionics data.
 */
interface AvionicsInterface {
    suspend fun requestData(): AvionicsData?
}