package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The same press the device was measured with, so the two renderers can be compared as geometry
 * rather than as reasoning about what each backend does with `cameraDistance`.
 *
 * A press on the centre of the bottom edge is pure rotation about the x-axis — no rotation about y,
 * and almost no depression — so the tile draws as a symmetric trapezoid whose two horizontal edges
 * are magnified by the perspective in opposite directions. From those two widths the camera's
 * effective depth follows: `depth = s(r + 1) / (r - 1)`, with `s = (H / 2) sin θ`.
 */
@OptIn(ExperimentalTestApi::class)
class CameraProbeTest {
    @Test
    fun the_desktop_camera_sits_where_the_arithmetic_says() =
        runComposeUiTest {
            val tile = 497
            var top = 0
            var bottom = 0
            setContent {
                Box(
                    Modifier.size(900.dp).background(Color.Black).testTag("frame"),
                    contentAlignment = Alignment.Center,
                ) {
                    val source = remember { MutableInteractionSource() }
                    LaunchedEffect(Unit) {
                        // The device was pressed 2 px above the bottom edge; match it.
                        source.emit(PressInteraction.Press(Offset(tile / 2f, tile - 2f)))
                    }
                    Box(
                        Modifier
                            .size(tile.dp)
                            .indication(source, TiltIndication())
                            .background(Color.White),
                    )
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)
            val widths =
                (0 until image.height).map { y ->
                    (0 until image.width).count { x -> (pixels[y * image.width + x] shr 16 and 0xFF) > 0x80 }
                }
            val rows = widths.withIndex().filter { it.value > 150 }.map { it.index }
            top = widths[rows.first() + 2]
            bottom = widths[rows.last() - 2]

            // depth = s(r + 1) / (r - 1), the trapezoid solved for the camera.
            val s = (tile / 2f) * kotlin.math.sin(0.484f * 0.3f)
            val r = top.toFloat() / bottom
            val solved = s * (r + 1) / (r - 1)
            val declared = TiltIndication.DEFAULT_CAMERA_DISTANCE.value

            // 4% covers reading two antialiased edges two rows in from the ends of the shape; it
            // does not cover being wrong about the unit, which is the thing this is here to catch.
            // The same arithmetic on a Pixel 6a screenshot gave 593 against the desktop's 588.
            assertTrue(
                kotlin.math.abs(solved - declared) / declared < 0.04f,
                "the tilt camera solves to $solved px from a $top/$bottom trapezoid, where " +
                    "$declared px was declared - `cameraDistance` is not reaching the layer in the " +
                    "unit this thinks it is",
            )
        }
}
