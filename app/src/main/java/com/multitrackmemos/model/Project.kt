package com.multitrackmemos.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val CURRENT_SCHEMA_VERSION = 1

@Serializable
data class Project(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val name: String = "Untitled project",
    val tempoBpm: Int = 90,
    val timeSignature: TimeSignature = TimeSignature.FOUR_FOUR,
    val tracks: List<Track> = emptyList(),
    val calibrations: List<CalibrationRecord> = emptyList(),
    val export: ExportSettings = ExportSettings(),
) {
    init {
        require(tempoBpm in 40..240) { "Tempo must be between 40 and 240 BPM" }
    }
}

@Serializable
enum class TimeSignature { FOUR_FOUR, THREE_FOUR }

@Serializable
data class Track(
    val id: String,
    val name: String = "Track",
    val colorArgb: Long = 0xFF6750A4,
    val inputTrimDb: Float = 0f,
    val processing: ProcessingSettings = ProcessingSettings(),
    val faderDb: Float = 0f,
    val pan: Float = 0f,
    val mute: Boolean = false,
    val solo: Boolean = false,
    val armed: Boolean = false,
    val recordSafe: Boolean = false,
    val regions: List<Region> = emptyList(),
)

@Serializable
data class Region(
    val id: String,
    val sourceFile: String,
    val startMs: Long,
    val durationMs: Long,
    val timelineOffsetMs: Long = 0,
)

@Serializable
data class ProcessingSettings(
    val gainDb: Float = 0f,
    val eq: EqSettings = EqSettings(),
    val compressor: CompressorSettings = CompressorSettings(),
)

@Serializable
data class EqSettings(
    val highPassEnabled: Boolean = false,
    val lowShelfFrequencyHz: Float = 120f,
    val lowShelfDb: Float = 0f,
    val midFrequencyHz: Float = 1000f,
    val midDb: Float = 0f,
    val highShelfFrequencyHz: Float = 6000f,
    val highShelfDb: Float = 0f,
)

@Serializable
data class CompressorSettings(
    val amountPercent: Float = 0f,
    val advanced: Boolean = false,
    val thresholdDb: Float = -18f,
    val ratio: Int = 4,
    val makeupDb: Float = 0f,
)

@Serializable
data class CalibrationRecord(
    val deviceKey: String,
    val codec: String,
    val sampleRateHz: Int,
    val offsetMs: Long,
    val driftPpm: Float = 0f,
    val measuredAtEpochMs: Long = 0,
)

@Serializable
data class ExportSettings(
    val format: ExportFormat = ExportFormat.WAV_PCM16,
    val m4aBitrateKbps: Int = 256,
)

@Serializable
enum class ExportFormat { WAV_PCM16, M4A_AAC }

object ProjectCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun encode(project: Project): String = json.encodeToString(project)

    /** Decodes current projects and migrates the schema-0 shape used by the first prototype. */
    fun decode(raw: String): Project {
        val element = json.parseToJsonElement(raw)
        val objectValue = element as? JsonObject ?: error("Project must be a JSON object")
        val schema = objectValue["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 0
        require(schema in 0..CURRENT_SCHEMA_VERSION) { "Unsupported project schema: $schema" }
        val migrated = if (schema == 0) migrateV0(objectValue) else element
        return json.decodeFromJsonElement<Project>(migrated)
    }

    private fun migrateV0(source: JsonObject): JsonElement = buildJsonObject {
        source.forEach { (key, value) -> put(key, value) }
        put("schemaVersion", JsonPrimitive(CURRENT_SCHEMA_VERSION))
    }
}
