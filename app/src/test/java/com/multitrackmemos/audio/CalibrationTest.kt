package com.multitrackmemos.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {
    @Test fun rejectsTapOutlierBeforeTakingMedian() {
        assertEquals(202, CalibrationMath.medianResidualMs(listOf(198, 201, 202, 204, 760)))
    }

    @Test fun combinesPipelineAndSinkLatencyAndAppliesNudge() {
        val offset = CalibrationMath.totalOffsetMs(PipelineLatency(4, 190, 3), 7)
        assertEquals(204, offset)
        assertEquals(796, CalibrationMath.applyToRegionStartMs(1000, offset))
        assertEquals(806, CalibrationMath.applyToRegionStartMs(1000, offset, 10))
    }

    @Test fun routePolicyKeepsBluetoothPlaybackSeparateFromMic() {
        assertTrue(RoutingPolicy.isCaptureRouteValid(AudioRoute(InputRoute.BUILT_IN_MIC, OutputRoute.BLUETOOTH_A2DP, false)))
        assertFalse(RoutingPolicy.isCaptureRouteValid(AudioRoute(InputRoute.OTHER, OutputRoute.BLUETOOTH_A2DP, false)))
        assertFalse(RoutingPolicy.isCaptureRouteValid(AudioRoute(InputRoute.BUILT_IN_MIC, OutputRoute.BLUETOOTH_A2DP, true)))
        assertFalse(RoutingPolicy.monitoringAllowed(AudioRoute(InputRoute.BUILT_IN_MIC, OutputRoute.BLUETOOTH_A2DP, false), 10))
        assertTrue(RoutingPolicy.monitoringAllowed(AudioRoute(InputRoute.BUILT_IN_MIC, OutputRoute.WIRED, false), 10))
    }
}
