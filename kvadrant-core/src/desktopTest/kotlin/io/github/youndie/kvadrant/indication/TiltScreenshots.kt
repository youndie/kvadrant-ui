package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * The tilt spike's evidence: one 120 dp square pressed in five places. If the geometry is right,
 * the centre press is a plain shrink and each corner press is a trapezoid leaning towards that
 * corner.
 */
@Composable
private fun PressedTile(
    x: Float,
    y: Float,
) {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { source.emit(PressInteraction.Press(Offset(x, y))) }
    Box(Modifier.fillMaxSize().background(Color(0xFF101010)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .indication(source, TiltIndication())
                .background(Color(0xFF1BA1E2)),
        )
    }
}

// The square is 120 dp; the coordinates below are in its own pixels at density 1.
@ViddikScreenshot(name = "centre", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltCentre(): Unit = PressedTile(60f, 60f)

@ViddikScreenshot(name = "top left", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltTopLeft(): Unit = PressedTile(0f, 0f)

@ViddikScreenshot(name = "top right", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltTopRight(): Unit = PressedTile(120f, 0f)

@ViddikScreenshot(name = "bottom left", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltBottomLeft(): Unit = PressedTile(0f, 120f)

@ViddikScreenshot(name = "bottom right", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltBottomRight(): Unit = PressedTile(120f, 120f)

/**
 * The one that actually settles the spike. `Modifier.indication` alone does not exercise the risk:
 * the question was whether a layout-modifying node survives being wrapped by `clickable`, which
 * delegates the indication node inside its own `AbstractClickableNode`. Same press, same geometry
 * expected — a difference here means tilt cannot be the default indication.
 */
@ViddikScreenshot(name = "centre through clickable", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltCentreThroughClickable() {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { source.emit(PressInteraction.Press(Offset(60f, 60f))) }
    Box(Modifier.fillMaxSize().background(Color(0xFF101010)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .clickable(interactionSource = source, indication = TiltIndication()) {}
                .background(Color(0xFF1BA1E2)),
        )
    }
}

@ViddikScreenshot(name = "corner through clickable", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltCornerThroughClickable() {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { source.emit(PressInteraction.Press(Offset(0f, 0f))) }
    Box(Modifier.fillMaxSize().background(Color(0xFF101010)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .clickable(interactionSource = source, indication = TiltIndication()) {}
                .background(Color(0xFF1BA1E2)),
        )
    }
}

@ViddikScreenshot(name = "at rest", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltAtRest() {
    val source = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize().background(Color(0xFF101010)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .indication(source, TiltIndication())
                .background(Color(0xFF1BA1E2)),
        )
    }
}

/** Pure horizontal: only rotationY is non-zero, so the left edge must recede and shorten. */
@ViddikScreenshot(name = "left edge", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltLeftEdge(): Unit = PressedTile(0f, 60f)

/** Pure horizontal, mirrored. */
@ViddikScreenshot(name = "right edge", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltRightEdge(): Unit = PressedTile(120f, 60f)

/** Pure vertical: only rotationX is non-zero, so the top edge must recede and narrow. */
@ViddikScreenshot(name = "top edge", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltTopEdge(): Unit = PressedTile(60f, 0f)

@ViddikScreenshot(name = "bottom edge", group = "tilt", width = 200, height = 200)
@Composable
internal fun TiltBottomEdge(): Unit = PressedTile(60f, 120f)
