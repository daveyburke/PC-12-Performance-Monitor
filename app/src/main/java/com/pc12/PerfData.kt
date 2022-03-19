package com.pc12

/**
 * Performance data (computed from AvionicsData).
 * torque in psi, fuelFlow in lb/h, true airspeed in kts
 */
data class PerfData(var torque: Float, var fuelFlow: Int, var airspeed: Int)
