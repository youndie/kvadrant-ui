package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantMetrics
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.scaled
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Scaling the metric set scales the text with it, the way the phone did.
 *
 * The canvas was 480 units wide whatever the screen was and the device stretched the whole of it, so
 * text could not be scaled apart from margins and tiles — it was measured in the same units.
 * `scaled()` used to leave the ramp alone on the reasoning that scaling both turns a faithful layout
 * into a merely large one, which is the Android convention and was recorded as though it were
 * Metro's. Research §1.6c.
 *
 * Measured as drawn ink rather than as a `fontSize` read back out of the theme: what could break
 * here is the wiring between the metric set, the theme and the text, and reading the property back
 * would test the arithmetic while skipping all three.
 */
@OptIn(ExperimentalTestApi::class)
class TypeScaleTest {
    private fun inkHeight(factor: Float): Int {
        var height = 0
        runComposeUiTest {
            setContent {
                KvadrantTheme(metrics = KvadrantMetrics().scaled(factor)) {
                    Box(Modifier.size(600.dp).background(Color.Black).testTag("frame")) {
                        KvadrantText("Ех", style = KvadrantTheme.typography.normal)
                    }
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)
            val rows =
                (0 until image.height).filter { y ->
                    (0 until image.width).any { x -> (pixels[y * image.width + x] shr 16 and 0xFF) > 0x40 }
                }
            assertTrue(rows.isNotEmpty(), "nothing was drawn at factor $factor")
            height = rows.last() - rows.first() + 1
        }
        return height
    }

    @Test
    fun the_ramp_grows_with_the_metric_set() {
        val one = inkHeight(1f)
        val two = inkHeight(2f)
        val ratio = two.toFloat() / one

        // Ink height is not exactly proportional to font size — hinting and rounding move the
        // outline by a pixel — so the tolerance is on the ratio, not on the pixels.
        assertTrue(
            abs(ratio - 2f) < 0.08f,
            "text at metric scale 2 drew $two px of ink against $one at scale 1, a ratio of " +
                "$ratio — the ramp is not scaling with the metric set",
        )
    }
}
