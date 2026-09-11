package com.multitrackmemos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ProjectTest {
    @Test fun roundTripPreservesRegionsAndOffsets() {
        val source = Project(tracks = listOf(Track("t1", "Voice", regions = listOf(Region("r1", "audio/r1.wav", 0, 1200, -183)))))
        assertEquals(source, ProjectCodec.decode(ProjectCodec.encode(source)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsFutureSchema() { ProjectCodec.decode("{\"schemaVersion\":99}") }

    @Test fun migratesPrototypeWithoutSchema() {
        val migrated = ProjectCodec.decode("{\"name\":\"Old\",\"tempoBpm\":100,\"tracks\":[]}")
        assertEquals(CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals("Old", migrated.name)
        assertEquals(100, migrated.tempoBpm)
    }

    @Test fun storeCreatesProjectAndAudioDirectories() {
        val directory = Files.createTempDirectory("sketch-project").toFile()
        val store = ProjectStore(directory)
        assertEquals(Project(), store.loadOrCreate())
        assertTrue(directory.resolve("project.json").isFile)
        assertTrue(directory.resolve("audio").isDirectory)
        assertTrue(directory.resolve("peaks").isDirectory)
        store.save(Project(name = "Saved"))
        assertEquals("Saved", store.load().name)
    }
}
