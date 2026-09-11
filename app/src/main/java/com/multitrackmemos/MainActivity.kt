package com.multitrackmemos

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multitrackmemos.model.Project
import com.multitrackmemos.model.ProjectStore
import java.io.File

class MainActivity : ComponentActivity() {
    companion object { init { System.loadLibrary("sketch_audio") } }
    private external fun nativeEngineStatus(): String
    private val requestRecordPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startForegroundService(Intent(this, RecordingService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val project = ProjectStore(File(filesDir, "projects/default")).loadOrCreate()
        setContent { SketchApp(project, nativeEngineStatus()) { requestRecordPermission.launch(Manifest.permission.RECORD_AUDIO) } }
    }
}

@Composable
fun SketchApp(project: Project, engineStatus: String, onRecord: () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(project.name, style = MaterialTheme.typography.headlineMedium)
                Text("${project.tempoBpm} BPM · ${project.tracks.size}/8 tracks")
                Text("Engine $engineStatus", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onRecord) { Text("Record") }
            }
        }
    }
}
