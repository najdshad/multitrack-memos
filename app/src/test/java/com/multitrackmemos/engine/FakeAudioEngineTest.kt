package com.multitrackmemos.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeAudioEngineTest {
    @Test fun recordingRequiresAnArmedTrack() {
        val engine = FakeAudioEngine()
        assertEquals(AudioEvent.Error("Arm a track before recording"), engine.dispatch(AudioCommand.StartRecording))
        engine.dispatch(AudioCommand.ArmTrack(0))
        assertEquals(AudioEvent.TransportChanged(TransportState.RECORDING), engine.dispatch(AudioCommand.StartRecording))
    }

    @Test fun stopReturnsPlayheadToStart() {
        val engine = FakeAudioEngine()
        engine.dispatch(AudioCommand.Seek(1200))
        engine.dispatch(AudioCommand.Stop)
        assertEquals(0, engine.positionMs)
        assertEquals(TransportState.STOPPED, engine.state)
    }
}
