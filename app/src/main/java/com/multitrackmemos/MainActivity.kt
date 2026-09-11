package com.multitrackmemos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    companion object { init { System.loadLibrary("sketch_audio") } }
    private external fun nativeEngineStatus(): String
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SketchApp(nativeEngineStatus()) } }
}

@Composable fun SketchApp(engineStatus: String) {
    MaterialTheme { Surface { Text("Sketch\nOne empty project · Engine $engineStatus") } }
}
