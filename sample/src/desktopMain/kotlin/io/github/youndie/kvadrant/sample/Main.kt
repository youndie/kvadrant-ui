package io.github.youndie.kvadrant.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

public fun main(): Unit =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kvadrant UI",
            // The phone's own canvas: 480x800 Metro pixels at the canonical 0.75.
            state = rememberWindowState(width = 560.dp, height = 860.dp),
        ) {
            KvadrantSampleApp()
        }
    }
