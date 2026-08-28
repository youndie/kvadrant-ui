package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * The button's three states in one frame, which is the only way any of them gets looked at.
 *
 * The button appeared in `gallery` and in `start`, once each and at rest, so neither the inversion
 * on press nor the disabled state was in a golden at all — and the disabled one did not exist.
 *
 * The press is a real [PressInteraction.Press] through the hoisted interaction source, not a
 * hand-drawn approximation of what a press looks like: it goes through `collectIsPressedAsState`
 * and through the theme's tilt exactly as a finger does, so the frame also carries the depression.
 */
@Composable
private fun States(colors: KvadrantColors) {
    KvadrantTheme(colors = colors, typography = portableTypography(kvadrantLatin())) {
        val pressing = remember { MutableInteractionSource() }
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KvadrantText("rest", style = KvadrantTheme.typography.subtle)
            KvadrantButton("save", {})
            KvadrantText("pressed", style = KvadrantTheme.typography.subtle)
            KvadrantButton("save", {}, interactionSource = pressing)
            KvadrantText("disabled", style = KvadrantTheme.typography.subtle)
            KvadrantButton("save", {}, enabled = false)

            // Last on purpose. `MutableInteractionSource` replays nothing, and `LaunchedEffect`s
            // run in composition order, so a press emitted above the button reaches the tilt — the
            // indication node attaches with the layout — and misses `collectIsPressedAsState`,
            // whose collector is itself a `LaunchedEffect` further down. The first recording caught
            // exactly that: a button shrunk by the depression with its fill still transparent.
            LaunchedEffect(Unit) { pressing.emit(PressInteraction.Press(Offset.Zero)) }
        }
    }
}

@ViddikScreenshot(name = "button states dark", group = "button", width = 320, height = 400)
@Composable
internal fun ButtonStatesDark(): Unit = States(KvadrantColors.dark())

@ViddikScreenshot(name = "button states light", group = "button", width = 320, height = 400)
@Composable
internal fun ButtonStatesLight(): Unit = States(KvadrantColors.light())
