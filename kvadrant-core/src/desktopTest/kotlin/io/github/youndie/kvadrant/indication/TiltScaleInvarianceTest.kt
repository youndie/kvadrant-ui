package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantMetrics
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.scaled
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A pressed surface sinks by the same *fraction of itself* whatever the metric scale is — and this
 * holds because [io.github.youndie.kvadrant.theme.scaled] leaves the depression alone, not in spite
 * of it.
 *
 * The reasoning that says otherwise is short and wrong: every other screen distance in the metric
 * set scales, so a scaled-up tile should surely sink further. It already does. What reaches the
 * screen is `depth / (depth + depression)` — a ratio with no term for the size of the thing — so a
 * 24 dp checkbox and a 210 dp tile both draw at 0.9685 of themselves, and a bigger tile therefore
 * loses proportionally the same and absolutely more. Add a `* factor` to the depression and the
 * 1.6x tile sinks 4.9% of its width against the 1x tile's 3.1%: the disproportion the change was
 * meant to remove, newly introduced.
 *
 * Measured off a rendered frame rather than recomputed from the constants the renderer is supposed
 * to be using.
 */
class TiltScaleInvarianceTest {
    /** The rows of the pressed tile, in pixels of width, top to bottom, after the transform. */
    @OptIn(ExperimentalTestApi::class)
    private fun rows(
        tile: Int,
        factor: Float,
        press: (Float) -> Offset,
    ): List<Int> {
        var measured = emptyList<Int>()
        runComposeUiTest {
            setContent {
                KvadrantTheme(metrics = KvadrantMetrics().scaled(factor)) {
                    Box(
                        Modifier.size(400.dp).background(Color.Black).testTag("frame"),
                        contentAlignment = Alignment.Center,
                    ) {
                        val source = remember { MutableInteractionSource() }
                        val px = with(LocalDensity.current) { tile.dp.toPx() }
                        LaunchedEffect(Unit) { source.emit(PressInteraction.Press(press(px))) }
                        Box(
                            Modifier
                                .size(tile.dp)
                                .indication(source, LocalIndication.current)
                                .background(Color.White),
                        )
                    }
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)
            measured =
                (0 until image.height)
                    .map { y -> (0 until image.width).count { x -> pixels[y * image.width + x].isWhite() } }
                    .filter { it > 0 }
        }
        return measured
    }

    private fun Int.isWhite(): Boolean = (this shr 16 and 0xFF) > 0x80

    /** Dead centre: pure depression, no rotation. */
    private fun sink(
        tile: Int,
        factor: Float,
    ): Float = rows(tile, factor) { px -> Offset(px / 2f, px / 2f) }.max() / tile.toFloat()

    @Test
    fun `a pressed tile sinks by the same fraction of itself at every metric scale`() {
        val atOne = sink(tile = 100, factor = 1f)
        val atOnePointSix = sink(tile = 160, factor = 1.6f)

        assertTrue(
            abs(atOne - atOnePointSix) < 0.005f,
            "1x drew $atOne of the tile, 1.6x drew $atOnePointSix - the depression is not " +
                "proportional under the metric scale",
        )
    }
}
