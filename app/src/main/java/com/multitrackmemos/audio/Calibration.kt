package com.multitrackmemos.audio

import kotlin.math.abs

data class PipelineLatency(val outputMs: Long, val sinkMs: Long, val inputMs: Long) {
    val totalMs: Long get() = outputMs + sinkMs + inputMs
}

object CalibrationMath {
    /** Returns a robust median residual after discarding taps far from the central estimate. */
    fun medianResidualMs(residualsMs: List<Long>, outlierThresholdMs: Long = 80): Long {
        require(residualsMs.isNotEmpty()) { "At least one tap is required" }
        require(outlierThresholdMs >= 0) { "Outlier threshold cannot be negative" }
        val initial = median(residualsMs)
        val inliers = residualsMs.filter { abs(it - initial) <= outlierThresholdMs }
        return median(if (inliers.isEmpty()) listOf(initial) else inliers)
    }

    fun totalOffsetMs(pipeline: PipelineLatency, sinkCalibrationMs: Long): Long = pipeline.totalMs + sinkCalibrationMs

    /** Regions are moved earlier by latency; nudge is a deliberate user correction. */
    fun applyToRegionStartMs(captureStartMs: Long, totalOffsetMs: Long, nudgeMs: Long = 0): Long =
        captureStartMs - totalOffsetMs + nudgeMs

    private fun median(values: List<Long>): Long {
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }
}

enum class InputRoute { BUILT_IN_MIC, OTHER }
enum class OutputRoute { BUILT_IN, WIRED, BLUETOOTH_A2DP, BLUETOOTH_LE }

data class AudioRoute(val input: InputRoute, val output: OutputRoute, val scoActive: Boolean)

object RoutingPolicy {
    fun isCaptureRouteValid(route: AudioRoute): Boolean = route.input == InputRoute.BUILT_IN_MIC && !route.scoActive

    fun monitoringAllowed(route: AudioRoute, measuredRoundTripMs: Long?): Boolean =
        route.output !in setOf(OutputRoute.BLUETOOTH_A2DP, OutputRoute.BLUETOOTH_LE) &&
            (measuredRoundTripMs == null || measuredRoundTripMs < 25)
}
