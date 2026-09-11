package com.multitrackmemos.engine

/** Commands are immutable messages from the UI/service to the audio engine. */
sealed interface AudioCommand {
    data class ArmTrack(val trackIndex: Int, val armed: Boolean = true) : AudioCommand
    data object StartRecording : AudioCommand
    data object Stop : AudioCommand
    data object Play : AudioCommand
    data object Pause : AudioCommand
    data class Seek(val positionMs: Long) : AudioCommand
}

sealed interface AudioEvent {
    data class TransportChanged(val state: TransportState) : AudioEvent
    data class PositionChanged(val positionMs: Long) : AudioEvent
    data class TrackArmed(val trackIndex: Int) : AudioEvent
    data class Error(val message: String) : AudioEvent
}

enum class TransportState { STOPPED, PLAYING, RECORDING, PAUSED }

interface AudioEngine {
    val state: TransportState
    val positionMs: Long
    fun dispatch(command: AudioCommand): AudioEvent
}

/** Deterministic engine used by the Compose shell and JVM tests until the native graph lands. */
class FakeAudioEngine(private val trackCount: Int = 8) : AudioEngine {
    init { require(trackCount in 1..8) { "The engine supports one to eight tracks" } }

    private var transport = TransportState.STOPPED
    private var position = 0L
    private val armedTracks = BooleanArray(trackCount)

    override val state: TransportState get() = transport
    override val positionMs: Long get() = position

    override fun dispatch(command: AudioCommand): AudioEvent = when (command) {
        is AudioCommand.ArmTrack -> {
            if (command.trackIndex !in armedTracks.indices) return AudioEvent.Error("Track ${command.trackIndex + 1} is unavailable")
            armedTracks[command.trackIndex] = command.armed
            AudioEvent.TrackArmed(command.trackIndex)
        }
        AudioCommand.StartRecording -> {
            if (armedTracks.none { it }) return AudioEvent.Error("Arm a track before recording")
            transport = TransportState.RECORDING
            AudioEvent.TransportChanged(transport)
        }
        AudioCommand.Play -> {
            transport = TransportState.PLAYING
            AudioEvent.TransportChanged(transport)
        }
        AudioCommand.Pause -> {
            transport = TransportState.PAUSED
            AudioEvent.TransportChanged(transport)
        }
        AudioCommand.Stop -> {
            transport = TransportState.STOPPED
            position = 0
            AudioEvent.TransportChanged(transport)
        }
        is AudioCommand.Seek -> {
            position = command.positionMs.coerceAtLeast(0)
            AudioEvent.PositionChanged(position)
        }
    }
}
