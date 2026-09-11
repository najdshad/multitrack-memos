package com.multitrackmemos.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CURRENT_SCHEMA_VERSION = 1

@Serializable data class Project(val schemaVersion: Int = CURRENT_SCHEMA_VERSION, val name: String = "Untitled project", val tempoBpm: Int = 90, val tracks: List<Track> = emptyList())
@Serializable data class Track(val id: String, val name: String = "Track", val regions: List<Region> = emptyList())
@Serializable data class Region(val id: String, val sourceFile: String, val startMs: Long, val durationMs: Long, val timelineOffsetMs: Long = 0)

object ProjectCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    fun encode(project: Project): String = json.encodeToString(project)
    fun decode(raw: String): Project {
        val decoded = json.decodeFromString<Project>(raw)
        require(decoded.schemaVersion in 1..CURRENT_SCHEMA_VERSION) { "Unsupported project schema: ${decoded.schemaVersion}" }
        return decoded
    }
}
