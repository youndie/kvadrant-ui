package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantRoll
import io.github.youndie.kvadrant.components.KvadrantRotate
import io.github.youndie.kvadrant.components.KvadrantScrim
import io.github.youndie.kvadrant.components.KvadrantSlide
import io.github.youndie.kvadrant.components.KvadrantSwivel
import io.github.youndie.kvadrant.components.KvadrantTurnstile
import io.github.youndie.kvadrant.components.KvadrantTurnstileFeather
import io.github.youndie.kvadrant.components.SlideDirection
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme

internal fun motionPreviews(): List<KvadrantPreview> =
    listOf(
        KvadrantPreview(
            id = "turnstile",
            component = "KvadrantTurnstile",
            summary = "the page transition the whole system was built around — tap to play it",
            heightDp = 320,
        ) { TurnstilePreview() },
        KvadrantPreview(
            id = "turnstile-feather",
            component = "KvadrantTurnstileFeather",
            summary = "the same rotation, staggered per row, which is how a list arrived",
            heightDp = 360,
        ) { FeatherPreview() },
        KvadrantPreview(
            id = "swivel",
            component = "KvadrantSwivel",
            summary = "the dialog's arrival: a rotation about the horizontal axis",
            heightDp = 300,
        ) { SwivelPreview() },
        KvadrantPreview(
            id = "slide",
            component = "KvadrantSlide",
            summary = "in from an edge, on the phone's own exponential curves",
            heightDp = 300,
        ) { SlidePreview() },
        KvadrantPreview(
            id = "roll",
            component = "KvadrantRoll",
            summary = "a rotation about the bottom-left corner",
            heightDp = 300,
        ) { RollPreview() },
        KvadrantPreview(
            id = "rotate",
            component = "KvadrantRotate",
            summary = "a plain rotation with a fade, by whatever angle the caller asks for",
            heightDp = 300,
        ) { RotatePreview() },
        KvadrantPreview(
            id = "scrim",
            component = "KvadrantScrim",
            summary = "the dimmed layer a modal sits on",
            heightDp = 240,
        ) { ScrimPreview() },
    )

/** Every motion preview is the same shape: a button that flips one boolean, and the thing it moves. */
@Composable
private fun Stage(content: @Composable (Boolean) -> Unit) {
    var visible by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KvadrantButton(if (visible) "out" else "in", { visible = !visible })
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content(visible) }
    }
}

@Composable
private fun Card(label: String) {
    Box(
        Modifier.size(160.dp, 90.dp).background(KvadrantAccents.Cobalt),
        contentAlignment = Alignment.Center,
    ) {
        KvadrantText(label, style = KvadrantTheme.typography.normal)
    }
}

@Composable
private fun TurnstilePreview(): Unit = Stage { visible -> KvadrantTurnstile(visible) { Card("turnstile") } }

@Composable
private fun FeatherPreview() {
    Stage { visible ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                KvadrantTurnstileFeather(visible, index) { Card("row $index") }
            }
        }
    }
}

@Composable
private fun SwivelPreview(): Unit = Stage { visible -> KvadrantSwivel(visible) { Card("swivel") } }

@Composable
private fun SlidePreview(): Unit = Stage { visible -> KvadrantSlide(visible, SlideDirection.Up) { Card("slide") } }

@Composable
private fun RollPreview(): Unit = Stage { visible -> KvadrantRoll(visible) { Card("roll") } }

@Composable
private fun RotatePreview(): Unit = Stage { visible -> KvadrantRotate(visible, degrees = 80f) { Card("rotate") } }

@Composable
private fun ScrimPreview() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { KvadrantText("page content", style = KvadrantTheme.typography.normal) }
        }
        KvadrantScrim(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Card("on top") }
        }
    }
}
