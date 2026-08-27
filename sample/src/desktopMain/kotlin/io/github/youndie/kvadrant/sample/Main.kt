package io.github.youndie.kvadrant.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kvadrant UI",
            // The phone's own canvas: 480x800 Metro pixels at the canonical 0.75.
            state = rememberWindowState(width = 560.dp, height = 860.dp),
        ) {
            // A desktop window is not a phone: Metro's metrics were drawn for 480 px and read as
            // cramped here, so the demo opens with them scaled. On a device it opens at 1.0.
            KvadrantSampleApp(initialScale = 1.6f)
        }
    }
