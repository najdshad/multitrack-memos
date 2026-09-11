package com.multitrackmemos.model

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** File-backed project storage. Writes are atomic at the project.json boundary. */
class ProjectStore(private val projectDirectory: File) {
    private val projectFile get() = File(projectDirectory, "project.json")

    fun loadOrCreate(name: String = "Untitled project"): Project {
        if (!projectFile.exists()) {
            val project = Project(name = name)
            save(project)
            return project
        }
        return ProjectCodec.decode(projectFile.readText())
    }

    fun load(): Project = ProjectCodec.decode(projectFile.readText())

    fun save(project: Project) {
        require(project.tracks.size <= 8) { "A project cannot contain more than eight tracks" }
        projectDirectory.mkdirs()
        File(projectDirectory, "audio").mkdirs()
        File(projectDirectory, "peaks").mkdirs()
        val temporary = File(projectDirectory, "project.json.tmp")
        temporary.writeText(ProjectCodec.encode(project))
        try {
            Files.move(temporary.toPath(), projectFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), projectFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
