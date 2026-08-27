package io.github.youndie.kvadrant.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.youndie.kvadrant.sample.KvadrantSampleApp

/**
 * The whole Android application: host the shared demo and get out of the way.
 *
 * The scale is 1.0 and not the desktop's 1.6 because Metro's numbers were drawn for a phone in the
 * first place — this is the surface they are correct on, and anything else here would be a second
 * opinion about a measured figure.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { KvadrantSampleApp(initialScale = 1f) }
    }
}
