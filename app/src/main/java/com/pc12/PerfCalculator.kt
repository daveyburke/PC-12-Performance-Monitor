package com.pc12

import android.util.Log
import java.lang.Float.NaN

object PerfCalculator {
    private val TAG = PerfCalculator::class.qualifiedName

    fun compute(aircraftType : Int, avionicsData: AvionicsData /*, weight: Int */) : PerfData {
        val perfData = PerfData(NaN)

        if (avionicsData.altitude >= 10000 && avionicsData.altitude <= 30000 &&
            avionicsData.outsideTemp >= -55 && avionicsData.outsideTemp <= 24) {

            val i = ((avionicsData.altitude + 500) / 1000f).toInt() - 10
            var j = 0
            var interpolate = false
            for (outsideTemp: Int in SAT_TEMP_INDEX) {
                if (outsideTemp == avionicsData.outsideTemp) {  // bullseye
                    break
                } else if (outsideTemp > avionicsData.outsideTemp) {  // one beyond
                    interpolate = true
                    break
                }
                j++
            }

            val data = when(aircraftType) {
                AircraftTypeStore.PC_12_47E_MSN_1451_1942_4_Blade -> TORQUE_1451_1942_4_MAX_CRUISE
                AircraftTypeStore.PC_12_47E_MSN_1576_1942_5_Blade -> TORQUE_DATA_1576_1942_5_MAX_CRUISE
                AircraftTypeStore.PC_12_47E_MSN_2001_5_Blade -> TORQUE_2001_5_1700_RPM_MAX_CRUISE
                else -> TORQUE_DATA_1576_1942_5_MAX_CRUISE
            }

            if (interpolate) {  // and round to 2 decimal places
                perfData.torque = (data[i][j] + data[i][j - 1]) / 2f
                perfData.torque = (perfData.torque * 100.0f + 0.5f).toInt() / 100.0f
            } else {
                perfData.torque = data[i][j]
            }
        } else {
            Log.e(TAG, "Values out of range: " + avionicsData.altitude + " " +
                        avionicsData.outsideTemp)
        }

        return perfData
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // DATA - consistent with eQRH v1.3.2

    val SAT_TEMP_INDEX = arrayOf(-55,-53,-51,-49,-47,-45,-43,-41,-39,-37,-35,-33,-31,-29,-27,-25,-23,-21,-20,-19,-18,-17,-16,-15,-14,-13,-12,-11,-10,-9,-8,-7,-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,10,12,14,16,18,20,22,24)

    // Format: pressure altitude [10000:1000:30000] x static air temp [SAT_INDEX]. Values are PSI

    val TORQUE_1451_1942_4_MAX_CRUISE = arrayOf(
        arrayOf(NaN,NaN,NaN,NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.8f,36.8f,36.3f,35.2f,34.0f,32.9f,31.7f),
        arrayOf(NaN,NaN,NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.8f,36.7f,36.6f,36.5f,36.4f,36.2f,35.7f,34.6f,33.5f,32.4f,31.2f,NaN),
        arrayOf(NaN,NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.7f,36.5f,36.4f,36.2f,36.1f,36.0f,35.7f,35.1f,34.0f,32.9f,31.8f,30.7f,NaN,NaN),
        arrayOf(NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.7f,36.5f,36.3f,36.1f,35.9f,35.7f,35.5f,35.3f,35.1f,34.5f,33.4f,32.3f,31.3f,30.2f,NaN,NaN,NaN),
        arrayOf(NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.7f,36.4f,36.2f,35.9f,35.6f,35.4f,35.1f,34.8f,34.6f,34.3f,33.8f,32.8f,31.7f,30.7f,29.6f,NaN,NaN,NaN,NaN),
        arrayOf(NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.8f,36.8f,36.7f,36.7f,36.7f,36.7f,36.6f,36.4f,36.1f,35.8f,35.4f,35.1f,34.8f,34.5f,34.1f,33.8f,33.5f,33.0f,32.5f,32.0f,31.0f,30.0f,29.0f,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.8f,36.7f,36.6f,36.6f,36.5f,36.5f,36.4f,36.4f,36.1f,35.7f,35.3f,35.0f,34.6f,34.2f,33.8f,33.5f,33.1f,32.7f,32.2f,31.8f,31.3f,30.8f,30.3f,29.3f,28.4f,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.7f,36.5f,36.4f,36.2f,36.1f,35.9f,35.8f,35.6f,35.5f,35.2f,34.8f,34.5f,34.1f,33.7f,33.4f,33.0f,32.6f,32.2f,31.9f,31.4f,31.0f,30.5f,30.0f,29.6f,29.1f,28.6f,27.7f,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.7f,36.5f,36.3f,36.0f,35.8f,35.6f,35.3f,35.1f,34.9f,34.6f,34.3f,33.9f,33.6f,33.2f,32.9f,32.5f,32.1f,31.8f,31.4f,31.0f,30.6f,30.2f,29.7f,29.3f,28.8f,28.3f,27.9f,27.4f,27.0f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.6f,36.5f,36.4f,36.3f,36.2f,35.8f,35.5f,35.3f,35.0f,34.8f,34.5f,34.3f,34.0f,33.7f,33.4f,33.1f,32.7f,32.4f,32.0f,31.7f,31.3f,31.0f,30.6f,30.3f,29.8f,29.4f,29.0f,28.5f,28.1f,27.7f,27.2f,26.8f,26.3f,25.9f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.3f,36.1f,35.9f,35.7f,35.5f,35.1f,34.5f,34.2f,34.0f,33.7f,33.4f,33.1f,32.9f,32.6f,32.2f,31.9f,31.5f,31.2f,30.8f,30.5f,30.2f,29.8f,29.5f,29.1f,28.7f,28.2f,27.8f,27.4f,27.0f,26.5f,26.1f,25.7f,25.3f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(35.7f,35.8f,35.9f,35.9f,36.0f,36.0f,36.0f,36.0f,36.0f,36.0f,35.8f,35.5f,35.2f,34.9f,34.6f,34.1f,33.5f,33.0f,32.8f,32.5f,32.2f,32.0f,31.7f,31.3f,31.0f,30.7f,30.4f,30.0f,29.7f,29.4f,29.1f,28.7f,28.4f,27.9f,27.5f,27.1f,26.7f,26.3f,25.9f,25.4f,25.0f,24.6f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(35.1f,35.3f,35.4f,35.5f,35.6f,35.6f,35.6f,35.6f,35.6f,35.2f,34.8f,34.4f,34.0f,33.6f,33.1f,32.6f,32.1f,31.6f,31.3f,31.1f,30.8f,30.5f,30.2f,29.9f,29.6f,29.2f,28.9f,28.6f,28.3f,28.0f,27.6f,27.2f,26.8f,26.4f,26.0f,25.6f,25.2f,24.8f,24.4f,24.0f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(34.1f,34.3f,34.4f,34.5f,34.5f,34.6f,34.6f,34.6f,34.3f,33.9f,33.4f,33.0f,32.6f,32.1f,31.6f,31.2f,30.7f,30.2f,30.0f,29.7f,29.4f,29.1f,28.8f,28.5f,28.2f,27.9f,27.6f,27.3f,26.9f,26.5f,26.1f,25.7f,25.3f,24.9f,24.5f,24.1f,23.7f,23.3f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(33.2f,33.4f,33.4f,33.5f,33.6f,33.6f,33.7f,33.4f,32.9f,32.5f,32.0f,31.6f,31.1f,30.7f,30.2f,29.8f,29.4f,28.8f,28.5f,28.2f,28.0f,27.7f,27.4f,27.1f,26.8f,26.5f,26.2f,25.8f,25.4f,25.0f,24.6f,24.2f,23.8f,23.5f,23.1f,22.7f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(32.2f,32.3f,32.4f,32.5f,32.5f,32.6f,32.3f,31.9f,31.4f,31.0f,30.6f,30.1f,29.7f,29.3f,28.8f,28.4f,27.9f,27.3f,27.1f,26.8f,26.5f,26.2f,26.0f,25.7f,25.4f,25.0f,24.6f,24.2f,23.8f,23.5f,23.1f,22.7f,22.3f,21.9f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(31.2f,31.3f,31.4f,31.4f,31.5f,31.2f,30.8f,30.4f,30.0f,29.5f,29.1f,28.7f,28.3f,27.9f,27.4f,26.9f,26.4f,25.9f,25.6f,25.4f,25.1f,24.8f,24.5f,24.1f,23.8f,23.4f,23.0f,22.7f,22.3f,21.9f,21.6f,21.2f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(30.1f,30.2f,30.3f,30.5f,30.2f,29.8f,29.3f,28.9f,28.5f,28.1f,27.7f,27.3f,26.9f,26.4f,26.0f,25.5f,25.0f,24.4f,24.2f,23.9f,23.6f,23.3f,22.9f,22.6f,22.2f,21.9f,21.5f,21.2f,20.8f,20.5f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(29.1f,29.2f,29.4f,29.1f,28.7f,28.3f,27.9f,27.5f,27.1f,26.7f,26.3f,25.9f,25.5f,25.0f,24.5f,24.0f,23.5f,23.1f,22.8f,22.4f,22.1f,21.7f,21.4f,21.1f,20.7f,20.4f,20.0f,19.7f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(28.1f,28.3f,28.1f,27.7f,27.3f,26.9f,26.5f,26.1f,25.7f,25.3f,24.9f,24.5f,24.1f,23.6f,23.2f,22.7f,22.2f,21.6f,21.3f,21.0f,20.7f,20.3f,20.0f,19.7f,19.3f,19.0f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(27.3f,27.0f,26.7f,26.3f,25.9f,25.5f,25.1f,24.7f,24.3f,24.0f,23.6f,23.1f,22.7f,22.3f,21.9f,21.4f,20.8f,20.2f,19.9f,19.6f,19.3f,19.0f,18.6f,18.3f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN))

    val TORQUE_DATA_1576_1942_5_MAX_CRUISE = arrayOf(
        arrayOf(NaN,NaN,NaN,NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.5f,35.4f,34.4f,33.3f,32.2f),
        arrayOf(NaN,NaN,NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.8f,36.8f,36.7f,36.6f,36.1f,35.0f,34.0f,32.9f,31.8f,NaN),
        arrayOf(NaN,NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.7f,36.6f,36.6f,36.5f,36.4f,36.3f,35.7f,34.6f,33.5f,32.4f,31.3f,NaN,NaN),
        arrayOf(NaN,NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.7f,36.5f,36.4f,36.3f,36.1f,36.0f,35.8f,35.7f,35.1f,34.0f,32.9f,31.8f,30.7f,NaN,NaN,NaN),
        arrayOf(NaN,NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.6f,36.3f,36.1f,35.9f,35.7f,35.5f,35.3f,35.1f,34.9f,34.4f,33.4f,32.3f,31.2f,30.2f,NaN,NaN,NaN,NaN),
        arrayOf(NaN,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.7f,36.4f,36.1f,35.8f,35.5f,35.2f,34.9f,34.6f,34.4f,34.1f,33.6f,33.1f,32.6f,31.6f,30.6f,29.5f,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.6f,36.3f,35.9f,35.5f,35.1f,34.7f,34.4f,34.0f,33.6f,33.2f,32.9f,32.4f,31.9f,31.4f,30.9f,29.9f,28.9f,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.7f,36.6f,36.5f,36.5f,36.4f,36.3f,36.1f,36.0f,35.8f,35.4f,35.0f,34.6f,34.3f,33.9f,33.5f,33.2f,32.8f,32.4f,32.0f,31.5f,31.1f,30.6f,30.1f,29.6f,29.2f,28.2f,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.9f,36.8f,36.7f,36.5f,36.3f,36.1f,36.0f,35.8f,35.6f,35.5f,35.3f,34.9f,34.5f,34.2f,33.8f,33.4f,33.0f,32.7f,32.3f,31.9f,31.6f,31.1f,30.7f,30.2f,29.7f,29.4f,28.9f,28.4f,28.0f,27.5f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.7f,36.6f,36.6f,36.6f,36.2f,36.0f,35.8f,35.5f,35.3f,35.1f,34.9f,34.6f,34.4f,34.1f,33.7f,33.3f,32.9f,32.6f,32.2f,31.9f,31.5f,31.1f,30.8f,30.4f,29.9f,29.5f,29.0f,28.5f,28.1f,27.7f,27.3f,26.8f,26.4f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.5f,36.4f,36.4f,36.3f,36.3f,36.2f,35.8f,35.2f,34.9f,34.6f,34.4f,34.1f,33.8f,33.5f,33.2f,32.9f,32.5f,32.2f,31.7f,31.4f,31.0f,30.7f,30.3f,30.0f,29.6f,29.1f,28.7f,28.3f,27.8f,27.4f,26.9f,26.5f,26.2f,25.7f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(35.9f,36.0f,36.0f,36.0f,36.0f,36.0f,36.0f,36.0f,36.0f,36.0f,35.9f,35.7f,35.6f,35.4f,35.2f,34.8f,34.2f,33.7f,33.4f,33.2f,32.9f,32.6f,32.3f,32.0f,31.6f,31.3f,31.0f,30.6f,30.2f,29.9f,29.6f,29.2f,28.9f,28.4f,28.0f,27.6f,27.1f,26.7f,26.3f,25.8f,25.4f,25.0f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(35.5f,35.5f,35.5f,35.6f,35.6f,35.6f,35.6f,35.6f,35.6f,35.3f,35.1f,34.8f,34.5f,34.2f,33.8f,33.2f,32.7f,32.2f,32.0f,31.7f,31.4f,31.1f,30.8f,30.5f,30.1f,29.8f,29.5f,29.2f,28.8f,28.5f,28.1f,27.7f,27.3f,26.8f,26.4f,26.0f,25.6f,25.1f,24.7f,24.3f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(34.2f,34.3f,34.3f,34.4f,34.5f,34.6f,34.7f,34.8f,34.6f,34.2f,33.9f,33.6f,33.2f,32.8f,32.3f,31.8f,31.3f,30.8f,30.5f,30.2f,29.9f,29.6f,29.3f,29.0f,28.7f,28.4f,28.1f,27.8f,27.3f,26.9f,26.5f,26.1f,25.7f,25.3f,24.9f,24.4f,24.0f,23.6f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(33.0f,33.0f,33.2f,33.4f,33.6f,33.8f,34.0f,33.8f,33.4f,33.0f,32.6f,32.2f,31.8f,31.3f,30.9f,30.4f,29.9f,29.4f,29.1f,28.8f,28.5f,28.2f,27.9f,27.6f,27.3f,27.0f,26.7f,26.3f,25.7f,25.3f,24.9f,24.5f,24.1f,23.7f,23.3f,22.9f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(31.9f,32.1f,32.3f,32.4f,32.6f,32.8f,32.5f,32.2f,31.9f,31.5f,31.2f,30.8f,30.3f,29.9f,29.4f,29.0f,28.4f,27.9f,27.6f,27.3f,27.0f,26.7f,26.4f,26.1f,25.8f,25.4f,25.0f,24.7f,24.1f,23.7f,23.4f,23.0f,22.6f,22.2f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(31.0f,31.1f,31.2f,31.4f,31.5f,31.3f,31.0f,30.7f,30.4f,30.1f,29.7f,29.3f,28.9f,28.4f,28.0f,27.5f,26.9f,26.4f,26.1f,25.8f,25.6f,25.3f,25.0f,24.6f,24.2f,23.9f,23.5f,23.1f,22.6f,22.2f,21.8f,21.4f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(30.0f,30.1f,30.3f,30.5f,30.3f,30.0f,29.7f,29.4f,29.1f,28.7f,28.3f,27.8f,27.4f,27.0f,26.5f,26.0f,25.4f,24.9f,24.7f,24.4f,24.1f,23.7f,23.4f,23.0f,22.7f,22.3f,21.9f,21.6f,21.0f,20.6f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(29.1f,29.2f,29.4f,29.2f,28.9f,28.6f,28.3f,28.0f,27.6f,27.2f,26.8f,26.4f,26.0f,25.5f,25.0f,24.5f,24.0f,23.5f,23.2f,22.9f,22.5f,22.2f,21.8f,21.5f,21.1f,20.8f,20.4f,20.1f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(28.2f,28.4f,28.2f,27.9f,27.6f,27.3f,27.0f,26.6f,26.2f,25.8f,25.4f,25.0f,24.5f,24.1f,23.6f,23.1f,22.7f,22.1f,21.7f,21.4f,21.0f,20.7f,20.4f,20.0f,19.7f,19.4f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(27.3f,27.1f,26.9f,26.6f,26.3f,26.0f,25.6f,25.2f,24.8f,24.4f,24.0f,23.6f,23.1f,22.7f,22.3f,21.8f,21.2f,20.6f,20.3f,19.9f,19.6f,19.3f,19.0f,18.7f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN))

    val TORQUE_2001_5_1700_RPM_MAX_CRUISE = arrayOf(
        arrayOf(NaN,NaN,NaN,NaN,NaN,NaN,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.4f,40.1f,39.7f,39.1f,38.5f,37.9f,37.0f,35.9f,34.7f,33.6f,32.4f),
        arrayOf(NaN,NaN,NaN,NaN,NaN,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.3f,39.9f,39.6f,39.2f,38.8f,38.1f,37.3f,36.4f,35.3f,34.1f,33.0f,31.9f,NaN),
        arrayOf(NaN,NaN,NaN,NaN,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.3f,39.8f,39.4f,39.0f,38.5f,38.1f,37.6f,36.7f,35.8f,34.7f,33.6f,32.5f,31.4f,NaN,NaN),
        arrayOf(NaN,NaN,NaN,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.5f,40.5f,40.4f,40.3f,40.3f,40.2f,40.1f,40.1f,40.0f,39.6f,39.2f,38.8f,38.3f,37.9f,37.4f,37.0f,36.5f,36.1f,35.1f,34.1f,33.0f,31.9f,30.8f,NaN,NaN,NaN),
        arrayOf(NaN,NaN,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.7f,40.7f,40.7f,40.7f,40.7f,40.7f,40.7f,40.7f,40.7f,40.6f,40.4f,40.3f,40.1f,40.0f,39.9f,39.7f,39.6f,39.5f,39.3f,39.0f,38.5f,38.1f,37.7f,37.2f,36.8f,36.4f,35.9f,35.5f,35.0f,34.5f,33.5f,32.4f,31.3f,30.2f,NaN,NaN,NaN,NaN),
        arrayOf(NaN,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.5f,40.3f,40.1f,39.9f,39.7f,39.5f,39.2f,39.0f,38.8f,38.6f,38.2f,37.8f,37.4f,36.9f,36.5f,36.0f,35.6f,35.1f,34.7f,34.3f,33.8f,33.3f,32.7f,31.7f,30.7f,29.7f,NaN,NaN,NaN,NaN,NaN),
        arrayOf(40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.4f,40.2f,39.9f,39.6f,39.3f,39.0f,38.8f,38.5f,38.2f,37.9f,37.5f,37.1f,36.6f,36.2f,35.7f,35.3f,34.8f,34.4f,33.9f,33.5f,33.0f,32.5f,32.0f,31.6f,31.1f,30.1f,29.1f,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.5f,40.5f,40.3f,39.9f,39.6f,39.3f,38.9f,38.6f,38.2f,37.9f,37.5f,37.2f,36.8f,36.4f,35.9f,35.5f,35.0f,34.6f,34.2f,33.7f,33.3f,32.9f,32.4f,31.9f,31.4f,31.0f,30.5f,30.0f,29.5f,28.6f,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.5f,40.5f,40.4f,40.1f,39.7f,39.3f,38.9f,38.5f,38.1f,37.7f,37.3f,36.9f,36.5f,36.1f,35.7f,35.2f,34.8f,34.4f,33.9f,33.5f,33.1f,32.7f,32.2f,31.8f,31.3f,30.8f,30.4f,29.9f,29.4f,29.0f,28.5f,28.0f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(40.2f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.4f,40.2f,40.0f,39.7f,39.5f,38.8f,38.4f,38.1f,37.7f,37.3f,36.9f,36.5f,36.1f,35.7f,35.3f,34.9f,34.5f,34.1f,33.6f,33.2f,32.8f,32.4f,31.9f,31.5f,31.1f,30.6f,30.2f,29.7f,29.3f,28.8f,28.4f,27.9f,27.5f,27.0f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(40.5f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.6f,40.3f,39.9f,39.4f,39.0f,38.6f,37.9f,37.2f,36.8f,36.4f,36.1f,35.7f,35.3f,35.0f,34.6f,34.1f,33.7f,33.3f,32.9f,32.5f,32.1f,31.6f,31.2f,30.8f,30.4f,29.9f,29.5f,29.1f,28.6f,28.2f,27.8f,27.3f,26.9f,26.4f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(38.8f,38.8f,38.8f,38.8f,38.8f,39.0f,39.2f,39.4f,39.6f,39.9f,39.5f,39.0f,38.5f,38.0f,37.5f,36.9f,36.2f,35.5f,35.1f,34.8f,34.4f,34.1f,33.7f,33.3f,32.9f,32.5f,32.1f,31.7f,31.3f,30.9f,30.5f,30.1f,29.7f,29.3f,28.8f,28.4f,28.0f,27.6f,27.1f,26.7f,26.3f,25.8f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.9f,36.9f,36.9f,36.9f,37.3f,37.7f,38.2f,38.6f,39.1f,38.7f,38.2f,37.6f,37.0f,36.4f,35.8f,35.2f,34.5f,33.9f,33.5f,33.2f,32.9f,32.5f,32.1f,31.7f,31.3f,30.9f,30.6f,30.2f,29.8f,29.4f,29.0f,28.6f,28.2f,27.7f,27.3f,26.9f,26.5f,26.1f,25.7f,25.2f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(36.1f,36.2f,36.3f,36.5f,36.8f,37.1f,37.4f,37.7f,37.4f,36.9f,36.4f,35.9f,35.4f,34.8f,34.2f,33.5f,32.9f,32.3f,31.9f,31.6f,31.2f,30.8f,30.5f,30.1f,29.7f,29.3f,29.0f,28.6f,28.2f,27.8f,27.4f,27.0f,26.6f,26.2f,25.8f,25.3f,24.9f,24.5f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(35.4f,35.6f,35.8f,35.9f,36.1f,36.2f,36.4f,36.1f,35.7f,35.2f,34.8f,34.4f,33.8f,33.2f,32.6f,32.0f,31.4f,30.7f,30.3f,29.9f,29.6f,29.2f,28.9f,28.5f,28.2f,27.8f,27.4f,27.0f,26.6f,26.2f,25.8f,25.4f,25.0f,24.6f,24.2f,23.8f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(34.3f,34.5f,34.6f,34.8f,34.9f,35.1f,34.8f,34.4f,34.1f,33.7f,33.3f,32.8f,32.2f,31.6f,31.0f,30.4f,29.7f,29.0f,28.7f,28.4f,28.0f,27.7f,27.3f,27.0f,26.6f,26.2f,25.9f,25.5f,25.1f,24.7f,24.3f,23.9f,23.5f,23.2f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(33.1f,33.3f,33.4f,33.6f,33.7f,33.5f,33.2f,32.9f,32.6f,32.2f,31.7f,31.1f,30.6f,30.0f,29.4f,28.8f,28.1f,27.5f,27.2f,26.8f,26.5f,26.2f,25.8f,25.5f,25.1f,24.7f,24.4f,24.0f,23.6f,23.2f,22.9f,22.5f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(32.1f,32.2f,32.3f,32.5f,32.3f,32.1f,31.8f,31.5f,31.2f,30.7f,30.2f,29.6f,29.0f,28.5f,27.9f,27.2f,26.6f,26.0f,25.7f,25.3f,25.0f,24.7f,24.3f,24.0f,23.6f,23.2f,22.9f,22.5f,22.2f,21.8f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(31.0f,31.1f,31.3f,31.1f,30.9f,30.7f,30.5f,30.2f,29.8f,29.2f,28.6f,28.1f,27.5f,26.9f,26.3f,25.7f,25.1f,24.5f,24.2f,23.8f,23.5f,23.2f,22.8f,22.5f,22.2f,21.8f,21.5f,21.2f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(30.0f,30.1f,30.0f,29.8f,29.6f,29.4f,29.2f,28.8f,28.3f,27.7f,27.2f,26.6f,26.0f,25.5f,24.9f,24.3f,23.7f,23.1f,22.8f,22.5f,22.1f,21.8f,21.5f,21.2f,20.9f,20.5f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN),
        arrayOf(29.0f,28.9f,28.8f,28.6f,28.4f,28.3f,27.8f,27.3f,26.8f,26.2f,25.7f,25.1f,24.6f,24.0f,23.5f,22.9f,22.4f,21.7f,21.4f,21.1f,20.8f,20.5f,20.2f,19.9f,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN))

    // Additional data - this requires weight as input, i.e. 3D array. Not impl

    // val AIRSPEED_1451_1942_4_MAX_CRUISE = ...
    // val AIRSPEED_1576_1942_5_MAX_CRUISE = ...
    // val AIRSPEED_2001_5_MAX_CRUISE = ...

    // val FUEL_FLOW_1451_1942_4_MAX_CRUISE = ...
    // val FUEL_FLOW_1576_1942_5_MAX_CRUISE = ...
    // val FUEL_FLOW_2001_5_MAX_CRUISE = ...

    ////////////////////////////////////////////////////////////////////////////////////////////////
}