package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * The same press — dead centre, so it is pure depression with no rotation — on three sizes.
 *
 * If the sinking is proportional, all three shrink by the same fraction and the effect reads the
 * same at any size. If it is not, a large tile appears to sink further than a small control does,
 * which is a thing a per-element camera can easily cause and a global one cannot.
 */
@Composable
private fun CentrePressed(size: Int) {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { source.emit(PressInteraction.Press(Offset(size / 2f, size / 2f))) }
    Box(
        Modifier
            .size(size.dp)
            .indication(source, TiltIndication())
            .background(Color(0xFF1BA1E2)),
    )
}

@ViddikScreenshot(name = "scale ladder", group = "tilt", width = 320, height = 460)
@Composable
internal fun TiltScaleLadder() {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF101010)).padding(12.dp),
    ) {
        listOf(40, 100, 240).forEach { size ->
            Box(Modifier.padding(bottom = 12.dp)) { CentrePressed(size) }
        }
    }
}
