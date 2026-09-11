package com.multitrackmemos.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectTest {
    @Test fun roundTripPreservesRegionsAndOffsets() {
        val source = Project(tracks = listOf(Track("t1", "Voice", listOf(Region("r1", "audio/r1.wav", 0, 1200, -183)))) )
        assertEquals(source, ProjectCodec.decode(ProjectCodec.encode(source)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsFutureSchema() { ProjectCodec.decode("{\"schemaVersion\":99}") }
}
