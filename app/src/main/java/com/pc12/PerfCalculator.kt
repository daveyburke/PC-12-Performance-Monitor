package com.pc12

import android.util.Log
import java.lang.Float.NaN
import java.lang.Math.abs

object PerfCalculator {
    private val TAG = PerfCalculator::class.qualifiedName

    fun compute(avionicsData: AvionicsData, aircraftType: Int, weightType: Int) : PerfData {
        return PerfData(
            calculateTorque(avionicsData, aircraftType),
            calculateFuelFlow(avionicsData, aircraftType),
            calculateTrueAirspeed(avionicsData, aircraftType, weightType)
        )
    }

    private fun calculateTorque(avionicsData: AvionicsData, aircraftType: Int) : Float {
        val data = when(aircraftType) {
            SettingsStore.PC_12_47E_MSN_1451_1942_4_Blade -> TORQUE_1451_1942_4_MAX_CRUISE
            SettingsStore.PC_12_47E_MSN_1576_1942_5_Blade -> TORQUE_DATA_1576_1942_5_MAX_CRUISE
            SettingsStore.PC_12_47E_MSN_2001_5_Blade -> TORQUE_2001_5_1700_RPM_MAX_CRUISE
            else -> TORQUE_DATA_1576_1942_5_MAX_CRUISE
        }

        val roundedAltitude = ((avionicsData.altitude + 500) / 1000f).toInt() * 1000
        if ((roundedAltitude !in 10000..30000) || (avionicsData.outsideTemp !in -55..24)) {
            Log.i(TAG, "Values out of range: " + roundedAltitude + " " +
                    avionicsData.outsideTemp)
            return NaN
        }

        val i = roundedAltitude / 1000 - 10  // 0 corresponds to 10000 ft
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

        var torque : Float
        if (interpolate) {  // and round to 2 decimal places
            torque = (data[i][j] + data[i][j - 1]) / 2f
            torque = (torque * 100.0f + 0.5f).toInt() / 100.0f
        } else {
            torque = data[i][j]
        }
        return torque
    }

    private fun calculateFuelFlow(avionicsData: AvionicsData, aircraftType: Int) : Int {
        val data = when(aircraftType) {
            SettingsStore.PC_12_47E_MSN_1451_1942_4_Blade ->  FUEL_FLOW_1001_1942_4_MAX_CRUISE
            SettingsStore.PC_12_47E_MSN_1576_1942_5_Blade -> FUEL_FLOW_1576_1942_5_MAX_CRUISE
            SettingsStore.PC_12_47E_MSN_2001_5_Blade -> FUEL_FLOW_2001_5_MAX_CRUISE
            else -> FUEL_FLOW_1576_1942_5_MAX_CRUISE
        }

        // Table is sparse: interpolate over altitude and ISA temp
        return interpolateOverAltitudeAndTemp(avionicsData.altitude,
            avionicsData.outsideTemp, data)
    }

    private fun calculateTrueAirspeed(avionicsData: AvionicsData, aircraftType: Int, weightType: Int) : Int {
        val data = when(aircraftType) {
            SettingsStore.PC_12_47E_MSN_1451_1942_4_Blade -> AIRSPEED_1001_1942_4_MAX_CRUISE
            SettingsStore.PC_12_47E_MSN_1576_1942_5_Blade -> AIRSPEED_1576_1942_5_MAX_CRUISE
            SettingsStore.PC_12_47E_MSN_2001_5_Blade -> AIRSPEED_2001_5_MAX_CRUISE
            else -> AIRSPEED_1576_1942_5_MAX_CRUISE
        }

        // Table is sparse: interpolate over altitude and ISA temp. We could interpolate
        // over weight, but that's a manually entered value and not usually recalculated
        // accurately and continually by the pilot during the flight.
        return interpolateOverAltitudeAndTemp(avionicsData.altitude,
            avionicsData.outsideTemp, reshape(data, weightType))
    }

    private fun interpolateOverAltitudeAndTemp(altitude: Int, outsideTemp: Int, data: Array<Array<Int>>) : Int {
        val roundedAltitude = ((altitude + 500) / 1000f).toInt() * 1000
        val isa = outsideTemp + (altitude + 500) / 1000 * 2 - 15

        if ((roundedAltitude !in 10000..30000)  || (isa !in -40..39)) {
            Log.i(TAG, "Values out of range: " + roundedAltitude + " " + isa)
            return 0
        }

        val out: Int
        val i = roundedAltitude / 2000 - 5   // 0 corresponds to 10000 ft
        val j = isa / 10 + 4                 // 0 corresponds to -40 celsius
        val jStep = if (isa < 0) -1 else +1  // look back for neg, forward for pos

        if (isa >-40 && isa < 30) {
            val w2 = (abs(isa) % 10) / 10.0f
            val w1 = 1.0f - w2
            out = if (roundedAltitude % 2000 != 0) {  // interp over alts and temps
                (w1 * (data[i][j] + data[i+1][j]) / 2 +
                        w2 * (data[i][j+jStep] + data[i+1][j+jStep]) / 2).toInt()
            } else { // interp over temps only
                (w1 * data[i][j] + w2 * data[i][j+jStep]).toInt()
            }
        } else {
            out = if (roundedAltitude % 2000 != 0) { // interp over alts only
                (data[i][j] + data[i+1][j]) / 2
            } else {  // no interp
                data[i][j]
            }
        }

        return out
    }

    private fun reshape(input: Array<Array<Array<Int>>>, k: Int) : Array<Array<Int>> {
        val out = Array(input.size) { Array(input[0].size) { 0 } }
        for(i: Int in input.indices) {
            for(j: Int in 0 until input[i].size) {
                out[i][j] = input[i][j][k]
            }
        }
        return out
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // DATA LOOKUP TABLES
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Temperature index (celsius)
    private val SAT_TEMP_INDEX = arrayOf(-55, -53, -51, -49, -47, -45, -43, -41, -39, -37, -35, -33, -31, -29, -27, -25, -23, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 24)

    // Format: pressure altitude [10000:1000:30000] x static air temp [SAT_TEMP_INDEX]. Values are PSI
    // Consistent with eQRH v1.3.2
    private val TORQUE_1451_1942_4_MAX_CRUISE = arrayOf(
        arrayOf(NaN, NaN, NaN, NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.8f, 36.8f, 36.3f, 35.2f, 34.0f, 32.9f, 31.7f),
        arrayOf(NaN, NaN, NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.8f, 36.7f, 36.6f, 36.5f, 36.4f, 36.2f, 35.7f, 34.6f, 33.5f, 32.4f, 31.2f, NaN),
        arrayOf(NaN, NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.7f, 36.5f, 36.4f, 36.2f, 36.1f, 36.0f, 35.7f, 35.1f, 34.0f, 32.9f, 31.8f, 30.7f, NaN, NaN),
        arrayOf(NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.7f, 36.5f, 36.3f, 36.1f, 35.9f, 35.7f, 35.5f, 35.3f, 35.1f, 34.5f, 33.4f, 32.3f, 31.3f, 30.2f, NaN, NaN, NaN),
        arrayOf(NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.7f, 36.4f, 36.2f, 35.9f, 35.6f, 35.4f, 35.1f, 34.8f, 34.6f, 34.3f, 33.8f, 32.8f, 31.7f, 30.7f, 29.6f, NaN, NaN, NaN, NaN),
        arrayOf(NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.8f, 36.8f, 36.7f, 36.7f, 36.7f, 36.7f, 36.6f, 36.4f, 36.1f, 35.8f, 35.4f, 35.1f, 34.8f, 34.5f, 34.1f, 33.8f, 33.5f, 33.0f, 32.5f, 32.0f, 31.0f, 30.0f, 29.0f, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.8f, 36.7f, 36.6f, 36.6f, 36.5f, 36.5f, 36.4f, 36.4f, 36.1f, 35.7f, 35.3f, 35.0f, 34.6f, 34.2f, 33.8f, 33.5f, 33.1f, 32.7f, 32.2f, 31.8f, 31.3f, 30.8f, 30.3f, 29.3f, 28.4f, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.7f, 36.5f, 36.4f, 36.2f, 36.1f, 35.9f, 35.8f, 35.6f, 35.5f, 35.2f, 34.8f, 34.5f, 34.1f, 33.7f, 33.4f, 33.0f, 32.6f, 32.2f, 31.9f, 31.4f, 31.0f, 30.5f, 30.0f, 29.6f, 29.1f, 28.6f, 27.7f, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.7f, 36.5f, 36.3f, 36.0f, 35.8f, 35.6f, 35.3f, 35.1f, 34.9f, 34.6f, 34.3f, 33.9f, 33.6f, 33.2f, 32.9f, 32.5f, 32.1f, 31.8f, 31.4f, 31.0f, 30.6f, 30.2f, 29.7f, 29.3f, 28.8f, 28.3f, 27.9f, 27.4f, 27.0f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.6f, 36.5f, 36.4f, 36.3f, 36.2f, 35.8f, 35.5f, 35.3f, 35.0f, 34.8f, 34.5f, 34.3f, 34.0f, 33.7f, 33.4f, 33.1f, 32.7f, 32.4f, 32.0f, 31.7f, 31.3f, 31.0f, 30.6f, 30.3f, 29.8f, 29.4f, 29.0f, 28.5f, 28.1f, 27.7f, 27.2f, 26.8f, 26.3f, 25.9f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.3f, 36.1f, 35.9f, 35.7f, 35.5f, 35.1f, 34.5f, 34.2f, 34.0f, 33.7f, 33.4f, 33.1f, 32.9f, 32.6f, 32.2f, 31.9f, 31.5f, 31.2f, 30.8f, 30.5f, 30.2f, 29.8f, 29.5f, 29.1f, 28.7f, 28.2f, 27.8f, 27.4f, 27.0f, 26.5f, 26.1f, 25.7f, 25.3f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(35.7f, 35.8f, 35.9f, 35.9f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 35.8f, 35.5f, 35.2f, 34.9f, 34.6f, 34.1f, 33.5f, 33.0f, 32.8f, 32.5f, 32.2f, 32.0f, 31.7f, 31.3f, 31.0f, 30.7f, 30.4f, 30.0f, 29.7f, 29.4f, 29.1f, 28.7f, 28.4f, 27.9f, 27.5f, 27.1f, 26.7f, 26.3f, 25.9f, 25.4f, 25.0f, 24.6f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(35.1f, 35.3f, 35.4f, 35.5f, 35.6f, 35.6f, 35.6f, 35.6f, 35.6f, 35.2f, 34.8f, 34.4f, 34.0f, 33.6f, 33.1f, 32.6f, 32.1f, 31.6f, 31.3f, 31.1f, 30.8f, 30.5f, 30.2f, 29.9f, 29.6f, 29.2f, 28.9f, 28.6f, 28.3f, 28.0f, 27.6f, 27.2f, 26.8f, 26.4f, 26.0f, 25.6f, 25.2f, 24.8f, 24.4f, 24.0f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(34.1f, 34.3f, 34.4f, 34.5f, 34.5f, 34.6f, 34.6f, 34.6f, 34.3f, 33.9f, 33.4f, 33.0f, 32.6f, 32.1f, 31.6f, 31.2f, 30.7f, 30.2f, 30.0f, 29.7f, 29.4f, 29.1f, 28.8f, 28.5f, 28.2f, 27.9f, 27.6f, 27.3f, 26.9f, 26.5f, 26.1f, 25.7f, 25.3f, 24.9f, 24.5f, 24.1f, 23.7f, 23.3f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(33.2f, 33.4f, 33.4f, 33.5f, 33.6f, 33.6f, 33.7f, 33.4f, 32.9f, 32.5f, 32.0f, 31.6f, 31.1f, 30.7f, 30.2f, 29.8f, 29.4f, 28.8f, 28.5f, 28.2f, 28.0f, 27.7f, 27.4f, 27.1f, 26.8f, 26.5f, 26.2f, 25.8f, 25.4f, 25.0f, 24.6f, 24.2f, 23.8f, 23.5f, 23.1f, 22.7f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(32.2f, 32.3f, 32.4f, 32.5f, 32.5f, 32.6f, 32.3f, 31.9f, 31.4f, 31.0f, 30.6f, 30.1f, 29.7f, 29.3f, 28.8f, 28.4f, 27.9f, 27.3f, 27.1f, 26.8f, 26.5f, 26.2f, 26.0f, 25.7f, 25.4f, 25.0f, 24.6f, 24.2f, 23.8f, 23.5f, 23.1f, 22.7f, 22.3f, 21.9f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(31.2f, 31.3f, 31.4f, 31.4f, 31.5f, 31.2f, 30.8f, 30.4f, 30.0f, 29.5f, 29.1f, 28.7f, 28.3f, 27.9f, 27.4f, 26.9f, 26.4f, 25.9f, 25.6f, 25.4f, 25.1f, 24.8f, 24.5f, 24.1f, 23.8f, 23.4f, 23.0f, 22.7f, 22.3f, 21.9f, 21.6f, 21.2f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(30.1f, 30.2f, 30.3f, 30.5f, 30.2f, 29.8f, 29.3f, 28.9f, 28.5f, 28.1f, 27.7f, 27.3f, 26.9f, 26.4f, 26.0f, 25.5f, 25.0f, 24.4f, 24.2f, 23.9f, 23.6f, 23.3f, 22.9f, 22.6f, 22.2f, 21.9f, 21.5f, 21.2f, 20.8f, 20.5f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(29.1f, 29.2f, 29.4f, 29.1f, 28.7f, 28.3f, 27.9f, 27.5f, 27.1f, 26.7f, 26.3f, 25.9f, 25.5f, 25.0f, 24.5f, 24.0f, 23.5f, 23.1f, 22.8f, 22.4f, 22.1f, 21.7f, 21.4f, 21.1f, 20.7f, 20.4f, 20.0f, 19.7f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(28.1f, 28.3f, 28.1f, 27.7f, 27.3f, 26.9f, 26.5f, 26.1f, 25.7f, 25.3f, 24.9f, 24.5f, 24.1f, 23.6f, 23.2f, 22.7f, 22.2f, 21.6f, 21.3f, 21.0f, 20.7f, 20.3f, 20.0f, 19.7f, 19.3f, 19.0f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(27.3f, 27.0f, 26.7f, 26.3f, 25.9f, 25.5f, 25.1f, 24.7f, 24.3f, 24.0f, 23.6f, 23.1f, 22.7f, 22.3f, 21.9f, 21.4f, 20.8f, 20.2f, 19.9f, 19.6f, 19.3f, 19.0f, 18.6f, 18.3f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN))

    private val TORQUE_DATA_1576_1942_5_MAX_CRUISE = arrayOf(
        arrayOf(NaN, NaN, NaN, NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.5f, 35.4f, 34.4f, 33.3f, 32.2f),
        arrayOf(NaN, NaN, NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.8f, 36.8f, 36.7f, 36.6f, 36.1f, 35.0f, 34.0f, 32.9f, 31.8f, NaN),
        arrayOf(NaN, NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.7f, 36.6f, 36.6f, 36.5f, 36.4f, 36.3f, 35.7f, 34.6f, 33.5f, 32.4f, 31.3f, NaN, NaN),
        arrayOf(NaN, NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.7f, 36.5f, 36.4f, 36.3f, 36.1f, 36.0f, 35.8f, 35.7f, 35.1f, 34.0f, 32.9f, 31.8f, 30.7f, NaN, NaN, NaN),
        arrayOf(NaN, NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.6f, 36.3f, 36.1f, 35.9f, 35.7f, 35.5f, 35.3f, 35.1f, 34.9f, 34.4f, 33.4f, 32.3f, 31.2f, 30.2f, NaN, NaN, NaN, NaN),
        arrayOf(NaN, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.7f, 36.4f, 36.1f, 35.8f, 35.5f, 35.2f, 34.9f, 34.6f, 34.4f, 34.1f, 33.6f, 33.1f, 32.6f, 31.6f, 30.6f, 29.5f, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.6f, 36.3f, 35.9f, 35.5f, 35.1f, 34.7f, 34.4f, 34.0f, 33.6f, 33.2f, 32.9f, 32.4f, 31.9f, 31.4f, 30.9f, 29.9f, 28.9f, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.7f, 36.6f, 36.5f, 36.5f, 36.4f, 36.3f, 36.1f, 36.0f, 35.8f, 35.4f, 35.0f, 34.6f, 34.3f, 33.9f, 33.5f, 33.2f, 32.8f, 32.4f, 32.0f, 31.5f, 31.1f, 30.6f, 30.1f, 29.6f, 29.2f, 28.2f, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.9f, 36.8f, 36.7f, 36.5f, 36.3f, 36.1f, 36.0f, 35.8f, 35.6f, 35.5f, 35.3f, 34.9f, 34.5f, 34.2f, 33.8f, 33.4f, 33.0f, 32.7f, 32.3f, 31.9f, 31.6f, 31.1f, 30.7f, 30.2f, 29.7f, 29.4f, 28.9f, 28.4f, 28.0f, 27.5f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.7f, 36.6f, 36.6f, 36.6f, 36.2f, 36.0f, 35.8f, 35.5f, 35.3f, 35.1f, 34.9f, 34.6f, 34.4f, 34.1f, 33.7f, 33.3f, 32.9f, 32.6f, 32.2f, 31.9f, 31.5f, 31.1f, 30.8f, 30.4f, 29.9f, 29.5f, 29.0f, 28.5f, 28.1f, 27.7f, 27.3f, 26.8f, 26.4f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.5f, 36.4f, 36.4f, 36.3f, 36.3f, 36.2f, 35.8f, 35.2f, 34.9f, 34.6f, 34.4f, 34.1f, 33.8f, 33.5f, 33.2f, 32.9f, 32.5f, 32.2f, 31.7f, 31.4f, 31.0f, 30.7f, 30.3f, 30.0f, 29.6f, 29.1f, 28.7f, 28.3f, 27.8f, 27.4f, 26.9f, 26.5f, 26.2f, 25.7f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(35.9f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 36.0f, 35.9f, 35.7f, 35.6f, 35.4f, 35.2f, 34.8f, 34.2f, 33.7f, 33.4f, 33.2f, 32.9f, 32.6f, 32.3f, 32.0f, 31.6f, 31.3f, 31.0f, 30.6f, 30.2f, 29.9f, 29.6f, 29.2f, 28.9f, 28.4f, 28.0f, 27.6f, 27.1f, 26.7f, 26.3f, 25.8f, 25.4f, 25.0f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(35.5f, 35.5f, 35.5f, 35.6f, 35.6f, 35.6f, 35.6f, 35.6f, 35.6f, 35.3f, 35.1f, 34.8f, 34.5f, 34.2f, 33.8f, 33.2f, 32.7f, 32.2f, 32.0f, 31.7f, 31.4f, 31.1f, 30.8f, 30.5f, 30.1f, 29.8f, 29.5f, 29.2f, 28.8f, 28.5f, 28.1f, 27.7f, 27.3f, 26.8f, 26.4f, 26.0f, 25.6f, 25.1f, 24.7f, 24.3f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(34.2f, 34.3f, 34.3f, 34.4f, 34.5f, 34.6f, 34.7f, 34.8f, 34.6f, 34.2f, 33.9f, 33.6f, 33.2f, 32.8f, 32.3f, 31.8f, 31.3f, 30.8f, 30.5f, 30.2f, 29.9f, 29.6f, 29.3f, 29.0f, 28.7f, 28.4f, 28.1f, 27.8f, 27.3f, 26.9f, 26.5f, 26.1f, 25.7f, 25.3f, 24.9f, 24.4f, 24.0f, 23.6f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(33.0f, 33.0f, 33.2f, 33.4f, 33.6f, 33.8f, 34.0f, 33.8f, 33.4f, 33.0f, 32.6f, 32.2f, 31.8f, 31.3f, 30.9f, 30.4f, 29.9f, 29.4f, 29.1f, 28.8f, 28.5f, 28.2f, 27.9f, 27.6f, 27.3f, 27.0f, 26.7f, 26.3f, 25.7f, 25.3f, 24.9f, 24.5f, 24.1f, 23.7f, 23.3f, 22.9f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(31.9f, 32.1f, 32.3f, 32.4f, 32.6f, 32.8f, 32.5f, 32.2f, 31.9f, 31.5f, 31.2f, 30.8f, 30.3f, 29.9f, 29.4f, 29.0f, 28.4f, 27.9f, 27.6f, 27.3f, 27.0f, 26.7f, 26.4f, 26.1f, 25.8f, 25.4f, 25.0f, 24.7f, 24.1f, 23.7f, 23.4f, 23.0f, 22.6f, 22.2f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(31.0f, 31.1f, 31.2f, 31.4f, 31.5f, 31.3f, 31.0f, 30.7f, 30.4f, 30.1f, 29.7f, 29.3f, 28.9f, 28.4f, 28.0f, 27.5f, 26.9f, 26.4f, 26.1f, 25.8f, 25.6f, 25.3f, 25.0f, 24.6f, 24.2f, 23.9f, 23.5f, 23.1f, 22.6f, 22.2f, 21.8f, 21.4f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(30.0f, 30.1f, 30.3f, 30.5f, 30.3f, 30.0f, 29.7f, 29.4f, 29.1f, 28.7f, 28.3f, 27.8f, 27.4f, 27.0f, 26.5f, 26.0f, 25.4f, 24.9f, 24.7f, 24.4f, 24.1f, 23.7f, 23.4f, 23.0f, 22.7f, 22.3f, 21.9f, 21.6f, 21.0f, 20.6f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(29.1f, 29.2f, 29.4f, 29.2f, 28.9f, 28.6f, 28.3f, 28.0f, 27.6f, 27.2f, 26.8f, 26.4f, 26.0f, 25.5f, 25.0f, 24.5f, 24.0f, 23.5f, 23.2f, 22.9f, 22.5f, 22.2f, 21.8f, 21.5f, 21.1f, 20.8f, 20.4f, 20.1f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(28.2f, 28.4f, 28.2f, 27.9f, 27.6f, 27.3f, 27.0f, 26.6f, 26.2f, 25.8f, 25.4f, 25.0f, 24.5f, 24.1f, 23.6f, 23.1f, 22.7f, 22.1f, 21.7f, 21.4f, 21.0f, 20.7f, 20.4f, 20.0f, 19.7f, 19.4f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(27.3f, 27.1f, 26.9f, 26.6f, 26.3f, 26.0f, 25.6f, 25.2f, 24.8f, 24.4f, 24.0f, 23.6f, 23.1f, 22.7f, 22.3f, 21.8f, 21.2f, 20.6f, 20.3f, 19.9f, 19.6f, 19.3f, 19.0f, 18.7f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN))

    private val TORQUE_2001_5_1700_RPM_MAX_CRUISE = arrayOf(
        arrayOf(NaN, NaN, NaN, NaN, NaN, NaN, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.4f, 40.1f, 39.7f, 39.1f, 38.5f, 37.9f, 37.0f, 35.9f, 34.7f, 33.6f, 32.4f),
        arrayOf(NaN, NaN, NaN, NaN, NaN, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.3f, 39.9f, 39.6f, 39.2f, 38.8f, 38.1f, 37.3f, 36.4f, 35.3f, 34.1f, 33.0f, 31.9f, NaN),
        arrayOf(NaN, NaN, NaN, NaN, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.3f, 39.8f, 39.4f, 39.0f, 38.5f, 38.1f, 37.6f, 36.7f, 35.8f, 34.7f, 33.6f, 32.5f, 31.4f, NaN, NaN),
        arrayOf(NaN, NaN, NaN, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.5f, 40.5f, 40.4f, 40.3f, 40.3f, 40.2f, 40.1f, 40.1f, 40.0f, 39.6f, 39.2f, 38.8f, 38.3f, 37.9f, 37.4f, 37.0f, 36.5f, 36.1f, 35.1f, 34.1f, 33.0f, 31.9f, 30.8f, NaN, NaN, NaN),
        arrayOf(NaN, NaN, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.7f, 40.7f, 40.7f, 40.7f, 40.7f, 40.7f, 40.7f, 40.7f, 40.7f, 40.6f, 40.4f, 40.3f, 40.1f, 40.0f, 39.9f, 39.7f, 39.6f, 39.5f, 39.3f, 39.0f, 38.5f, 38.1f, 37.7f, 37.2f, 36.8f, 36.4f, 35.9f, 35.5f, 35.0f, 34.5f, 33.5f, 32.4f, 31.3f, 30.2f, NaN, NaN, NaN, NaN),
        arrayOf(NaN, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.5f, 40.3f, 40.1f, 39.9f, 39.7f, 39.5f, 39.2f, 39.0f, 38.8f, 38.6f, 38.2f, 37.8f, 37.4f, 36.9f, 36.5f, 36.0f, 35.6f, 35.1f, 34.7f, 34.3f, 33.8f, 33.3f, 32.7f, 31.7f, 30.7f, 29.7f, NaN, NaN, NaN, NaN, NaN),
        arrayOf(40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.4f, 40.2f, 39.9f, 39.6f, 39.3f, 39.0f, 38.8f, 38.5f, 38.2f, 37.9f, 37.5f, 37.1f, 36.6f, 36.2f, 35.7f, 35.3f, 34.8f, 34.4f, 33.9f, 33.5f, 33.0f, 32.5f, 32.0f, 31.6f, 31.1f, 30.1f, 29.1f, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.5f, 40.5f, 40.3f, 39.9f, 39.6f, 39.3f, 38.9f, 38.6f, 38.2f, 37.9f, 37.5f, 37.2f, 36.8f, 36.4f, 35.9f, 35.5f, 35.0f, 34.6f, 34.2f, 33.7f, 33.3f, 32.9f, 32.4f, 31.9f, 31.4f, 31.0f, 30.5f, 30.0f, 29.5f, 28.6f, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.5f, 40.5f, 40.4f, 40.1f, 39.7f, 39.3f, 38.9f, 38.5f, 38.1f, 37.7f, 37.3f, 36.9f, 36.5f, 36.1f, 35.7f, 35.2f, 34.8f, 34.4f, 33.9f, 33.5f, 33.1f, 32.7f, 32.2f, 31.8f, 31.3f, 30.8f, 30.4f, 29.9f, 29.4f, 29.0f, 28.5f, 28.0f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(40.2f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.4f, 40.2f, 40.0f, 39.7f, 39.5f, 38.8f, 38.4f, 38.1f, 37.7f, 37.3f, 36.9f, 36.5f, 36.1f, 35.7f, 35.3f, 34.9f, 34.5f, 34.1f, 33.6f, 33.2f, 32.8f, 32.4f, 31.9f, 31.5f, 31.1f, 30.6f, 30.2f, 29.7f, 29.3f, 28.8f, 28.4f, 27.9f, 27.5f, 27.0f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(40.5f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.6f, 40.3f, 39.9f, 39.4f, 39.0f, 38.6f, 37.9f, 37.2f, 36.8f, 36.4f, 36.1f, 35.7f, 35.3f, 35.0f, 34.6f, 34.1f, 33.7f, 33.3f, 32.9f, 32.5f, 32.1f, 31.6f, 31.2f, 30.8f, 30.4f, 29.9f, 29.5f, 29.1f, 28.6f, 28.2f, 27.8f, 27.3f, 26.9f, 26.4f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(38.8f, 38.8f, 38.8f, 38.8f, 38.8f, 39.0f, 39.2f, 39.4f, 39.6f, 39.9f, 39.5f, 39.0f, 38.5f, 38.0f, 37.5f, 36.9f, 36.2f, 35.5f, 35.1f, 34.8f, 34.4f, 34.1f, 33.7f, 33.3f, 32.9f, 32.5f, 32.1f, 31.7f, 31.3f, 30.9f, 30.5f, 30.1f, 29.7f, 29.3f, 28.8f, 28.4f, 28.0f, 27.6f, 27.1f, 26.7f, 26.3f, 25.8f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.9f, 36.9f, 36.9f, 36.9f, 37.3f, 37.7f, 38.2f, 38.6f, 39.1f, 38.7f, 38.2f, 37.6f, 37.0f, 36.4f, 35.8f, 35.2f, 34.5f, 33.9f, 33.5f, 33.2f, 32.9f, 32.5f, 32.1f, 31.7f, 31.3f, 30.9f, 30.6f, 30.2f, 29.8f, 29.4f, 29.0f, 28.6f, 28.2f, 27.7f, 27.3f, 26.9f, 26.5f, 26.1f, 25.7f, 25.2f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(36.1f, 36.2f, 36.3f, 36.5f, 36.8f, 37.1f, 37.4f, 37.7f, 37.4f, 36.9f, 36.4f, 35.9f, 35.4f, 34.8f, 34.2f, 33.5f, 32.9f, 32.3f, 31.9f, 31.6f, 31.2f, 30.8f, 30.5f, 30.1f, 29.7f, 29.3f, 29.0f, 28.6f, 28.2f, 27.8f, 27.4f, 27.0f, 26.6f, 26.2f, 25.8f, 25.3f, 24.9f, 24.5f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(35.4f, 35.6f, 35.8f, 35.9f, 36.1f, 36.2f, 36.4f, 36.1f, 35.7f, 35.2f, 34.8f, 34.4f, 33.8f, 33.2f, 32.6f, 32.0f, 31.4f, 30.7f, 30.3f, 29.9f, 29.6f, 29.2f, 28.9f, 28.5f, 28.2f, 27.8f, 27.4f, 27.0f, 26.6f, 26.2f, 25.8f, 25.4f, 25.0f, 24.6f, 24.2f, 23.8f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(34.3f, 34.5f, 34.6f, 34.8f, 34.9f, 35.1f, 34.8f, 34.4f, 34.1f, 33.7f, 33.3f, 32.8f, 32.2f, 31.6f, 31.0f, 30.4f, 29.7f, 29.0f, 28.7f, 28.4f, 28.0f, 27.7f, 27.3f, 27.0f, 26.6f, 26.2f, 25.9f, 25.5f, 25.1f, 24.7f, 24.3f, 23.9f, 23.5f, 23.2f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(33.1f, 33.3f, 33.4f, 33.6f, 33.7f, 33.5f, 33.2f, 32.9f, 32.6f, 32.2f, 31.7f, 31.1f, 30.6f, 30.0f, 29.4f, 28.8f, 28.1f, 27.5f, 27.2f, 26.8f, 26.5f, 26.2f, 25.8f, 25.5f, 25.1f, 24.7f, 24.4f, 24.0f, 23.6f, 23.2f, 22.9f, 22.5f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(32.1f, 32.2f, 32.3f, 32.5f, 32.3f, 32.1f, 31.8f, 31.5f, 31.2f, 30.7f, 30.2f, 29.6f, 29.0f, 28.5f, 27.9f, 27.2f, 26.6f, 26.0f, 25.7f, 25.3f, 25.0f, 24.7f, 24.3f, 24.0f, 23.6f, 23.2f, 22.9f, 22.5f, 22.2f, 21.8f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(31.0f, 31.1f, 31.3f, 31.1f, 30.9f, 30.7f, 30.5f, 30.2f, 29.8f, 29.2f, 28.6f, 28.1f, 27.5f, 26.9f, 26.3f, 25.7f, 25.1f, 24.5f, 24.2f, 23.8f, 23.5f, 23.2f, 22.8f, 22.5f, 22.2f, 21.8f, 21.5f, 21.2f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(30.0f, 30.1f, 30.0f, 29.8f, 29.6f, 29.4f, 29.2f, 28.8f, 28.3f, 27.7f, 27.2f, 26.6f, 26.0f, 25.5f, 24.9f, 24.3f, 23.7f, 23.1f, 22.8f, 22.5f, 22.1f, 21.8f, 21.5f, 21.2f, 20.9f, 20.5f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN),
        arrayOf(29.0f, 28.9f, 28.8f, 28.6f, 28.4f, 28.3f, 27.8f, 27.3f, 26.8f, 26.2f, 25.7f, 25.1f, 24.6f, 24.0f, 23.5f, 22.9f, 22.4f, 21.7f, 21.4f, 21.1f, 20.8f, 20.5f, 20.2f, 19.9f, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN))

    // Format: Alt [10000:2000:30000] x ISA [-40:10:30], based on 8000 lb weight
    // Values are lb/h
    // Consistent with POH tables, Revision 15
    private val FUEL_FLOW_1001_1942_4_MAX_CRUISE = arrayOf(
        arrayOf(529, 534, 540, 545, 550, 555, 559, 507),
        arrayOf(521, 526, 531, 536, 541, 546, 537, 487),
        arrayOf(513, 518, 523, 529, 533, 539, 515, 465),
        arrayOf(  0, 510, 515, 520, 524, 523, 488, 442),
        arrayOf(  0, 501, 506, 511, 515, 493, 460, 416),
        arrayOf(  0, 489, 494, 498, 491, 465, 432, 392),
        arrayOf(  0,   0, 478, 482, 461, 437, 408, 368),
        arrayOf(  0,   0, 448, 455, 432, 411, 383, 345),
        arrayOf(  0,   0,   0, 425, 404, 383, 358, 322),
        arrayOf(  0,   0,   0, 397, 376, 356, 333, 300),
        arrayOf(  0,   0,   0, 369, 349, 330, 309, 278))

    private val FUEL_FLOW_1576_1942_5_MAX_CRUISE = arrayOf(
        arrayOf(528, 533, 538, 544, 549, 554, 560, 512),
        arrayOf(519, 524, 530, 535, 539, 545, 542, 491),
        arrayOf(511, 517, 522, 527, 532, 537, 520, 470),
        arrayOf(  0, 508, 513, 518, 522, 528, 493, 446),
        arrayOf(  0, 499, 504, 509, 518, 499, 465, 421),
        arrayOf(  0, 487, 492, 496, 497, 470, 438, 396),
        arrayOf(  0,   0, 476, 480, 467, 443, 412, 372),
        arrayOf(  0,   0, 442, 457, 438, 416, 387, 349),
        arrayOf(  0,   0,   0, 423, 409, 388, 362, 326),
        arrayOf(  0,   0,   0, 396, 381, 361, 336, 302),
        arrayOf(  0,   0,   0, 368, 354, 334, 313, 281))

    private val FUEL_FLOW_2001_5_MAX_CRUISE = arrayOf(  // TODO: Get data for the NGX
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0))

    // Format: Alt [10000:2000:30000] x ISA [-40:10:30] x weight [7000, 8000, 9000, 10000, 10400]
    // Values are TAS kts
    // Consistent with POH tables, Revision 15
    private val AIRSPEED_1001_1942_4_MAX_CRUISE = arrayOf(
        arrayOf(arrayOf(245, 244, 243, 242, 241), arrayOf(248, 247, 246, 244, 244), arrayOf(251, 250, 249, 247, 246), arrayOf(253, 253, 251, 250, 249), arrayOf(256, 255, 254, 252, 251), arrayOf(259, 258, 256, 254, 254), arrayOf(261, 260, 258, 256, 255), arrayOf(247, 245, 243, 241, 240)),
        arrayOf(arrayOf(250, 249, 248, 246, 246), arrayOf(253, 252, 251, 249, 249), arrayOf(256, 255, 253, 252, 251), arrayOf(259, 258, 256, 254, 254), arrayOf(261, 260, 259, 257, 256), arrayOf(264, 263, 261, 259, 258), arrayOf(263, 261, 259, 257, 256), arrayOf(249, 247, 245, 242, 241)),
        arrayOf(arrayOf(255, 254, 253, 251, 251), arrayOf(258, 257, 256, 254, 253), arrayOf(261, 260, 258, 257, 256), arrayOf(264, 263, 261, 259, 259), arrayOf(267, 266, 264, 262, 261), arrayOf(269, 268, 266, 264, 263), arrayOf(264, 262, 260, 258, 258), arrayOf(250, 248, 246, 242, 241)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(264, 263, 261, 259, 258), arrayOf(267, 265, 264, 262, 261), arrayOf(269, 268, 266, 264, 264), arrayOf(272, 271, 269, 267, 266), arrayOf(273, 272, 270, 267, 267), arrayOf(265, 263, 260, 258, 257), arrayOf(251, 249, 246, 242, 241)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(269, 268, 266, 264, 263), arrayOf(272, 271, 269, 267, 266), arrayOf(275, 274, 272, 270, 269), arrayOf(278, 276, 274, 272, 271), arrayOf(274, 272, 270, 267, 266), arrayOf(265, 263, 260, 257, 256), arrayOf(251, 249, 245, 241, 240)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(273, 272, 270, 268, 267), arrayOf(277, 275, 273, 271, 270), arrayOf(280, 278, 276, 274, 273), arrayOf(280, 278, 275, 273, 272), arrayOf(274, 272, 269, 266, 265), arrayOf(265, 262, 260, 256, 254), arrayOf(251, 248, 244, 240, 238)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(280, 278, 276, 274, 273), arrayOf(283, 281, 279, 276, 275), arrayOf(279, 277, 275, 272, 270), arrayOf(274, 271, 269, 265, 263), arrayOf(265, 263, 259, 255, 253), arrayOf(251, 247, 243, 238, 236)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(277, 277, 277, 277, 275), arrayOf(283, 281, 278, 275, 274), arrayOf(279, 276, 273, 270, 268), arrayOf(273, 271, 267, 263, 261), arrayOf(265, 262, 257, 253, 251), arrayOf(250, 246, 241, 236, 233)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(281, 279, 277, 273, 271), arrayOf(277, 275, 271, 267, 265), arrayOf(272, 269, 264, 260, 258), arrayOf(263, 259, 255, 249, 247), arrayOf(248, 243, 237, 231, 228)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(279, 278, 274, 269, 268), arrayOf(275, 272, 268, 263, 261), arrayOf(270, 265, 261, 255, 253), arrayOf(261, 256, 250, 245, 242), arrayOf(245, 239, 233, 224, 220)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(276, 275, 270, 265, 263), arrayOf(273, 269, 264, 258, 256), arrayOf(267, 262, 256, 250, 247), arrayOf(258, 252, 246, 239, 235), arrayOf(242, 235, 227, 215, 208)))

    private val AIRSPEED_1576_1942_5_MAX_CRUISE = arrayOf(
        arrayOf(arrayOf(243, 243, 241, 240, 239), arrayOf(246, 245, 244, 242, 242), arrayOf(249, 248, 247, 245, 244), arrayOf(251, 251, 249, 247, 247), arrayOf(254, 253, 252, 250, 249), arrayOf(257, 256, 255, 253, 252), arrayOf(261, 260, 258, 256, 255), arrayOf(249, 247, 245, 243, 242)),
        arrayOf(arrayOf(249, 248, 247, 245, 244), arrayOf(252, 251, 249, 248, 247), arrayOf(254, 253, 252, 250, 249), arrayOf(257, 256, 254, 253, 252), arrayOf(260, 259, 257, 256, 255), arrayOf(264, 262, 261, 259, 258), arrayOf(265, 264, 262, 260, 259), arrayOf(252, 249, 247, 245, 243)),
        arrayOf(arrayOf(254, 253, 252, 250, 249), arrayOf(257, 256, 255, 253, 252), arrayOf(260, 259, 257, 255, 255), arrayOf(263, 262, 260, 258, 257), arrayOf(266, 265, 263, 262, 261), arrayOf(270, 269, 267, 265, 264), arrayOf(267, 266, 263, 261, 260), arrayOf(253, 251, 249, 246, 244)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(263, 262, 260, 258, 257), arrayOf(266, 265, 263, 261, 260), arrayOf(269, 268, 266, 264, 264), arrayOf(273, 272, 270, 268, 267), arrayOf(277, 275, 273, 271, 270), arrayOf(269, 267, 264, 262, 261), arrayOf(255, 252, 250, 246, 245)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(268, 267, 265, 263, 262), arrayOf(272, 271, 269, 267, 266), arrayOf(276, 274, 273, 271, 270), arrayOf(280, 278, 276, 274, 273), arrayOf(278, 276, 274, 272, 271), arrayOf(270, 267, 265, 262, 260), arrayOf(256, 253, 250, 246, 244)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(272, 271, 269, 267, 266), arrayOf(277, 275, 273, 271, 271), arrayOf(282, 280, 278, 276, 275), arrayOf(285, 283, 280, 278, 277), arrayOf(279, 277, 274, 272, 270), arrayOf(270, 268, 265, 261, 260), arrayOf(257, 254, 250, 245, 243)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(280, 278, 276, 274, 274), arrayOf(285, 283, 281, 279, 278), arrayOf(285, 282, 280, 277, 275), arrayOf(279, 276, 274, 270, 268), arrayOf(270, 268, 264, 260, 258), arrayOf(256, 253, 248, 243, 241)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(277, 277, 277, 277, 276), arrayOf(284, 284, 282, 280, 278), arrayOf(284, 281, 278, 275, 273), arrayOf(279, 276, 272, 268, 266), arrayOf(270, 267, 263, 258, 256), arrayOf(256, 252, 246, 241, 239)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(281, 281, 281, 277, 275), arrayOf(282, 279, 276, 272, 270), arrayOf(277, 274, 270, 265, 263), arrayOf(269, 265, 260, 254, 252), arrayOf(254, 249, 243, 236, 233)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(279, 279, 277, 273, 271), arrayOf(280, 277, 272, 268, 266), arrayOf(275, 271, 266, 260, 258), arrayOf(267, 261, 256, 250, 247), arrayOf(251, 245, 238, 230, 226)),
        arrayOf(arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(  0,   0,   0,   0,   0), arrayOf(276, 276, 274, 269, 266), arrayOf(277, 273, 268, 263, 260), arrayOf(272, 267, 261, 255, 252), arrayOf(263, 258, 251, 244, 240), arrayOf(248, 241, 232, 221, 213)))

    private val AIRSPEED_2001_5_MAX_CRUISE = arrayOf(  // TODO: Get data for the NGX
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)),
        arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0)))
}