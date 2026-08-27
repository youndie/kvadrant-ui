package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The background has to return to where it started after exactly one content copy of scrolling.
 *
 * That is the wraparound's whole claim, and it was false: every layer was offset by `scroll * rate`
 * and drawn once, so the fold moved the background sideways by `copyWidth * rate` — a distance that
 * is a period of nothing — and the seam snapped on every wrap.
 *
 * **This does not photograph the fold, and the first version of it did, which is why it passed on
 * the defect.** The fold puts `scroll.value` back to zero *before* the next frame, so a capture
 * taken after it is by construction the frame at zero, jump or no jump. What is measured instead is
 * the position of a marker painted into the background at one pixel short of the fold: a layer that
 * is a proper cylinder has travelled one whole period by then and its marker is back under its own
 * starting column.
 */
@OptIn(ExperimentalTestApi::class)
class PanoramaFoldTest {
    /**
     * The column of the **leftmost** white stripe, and leftmost is the whole point.
     *
     * The background is drawn twice, so at some offsets two stripes are on screen and at others
     * one. Averaging every white pixel therefore moves when a second stripe merely enters the
     * viewport, and the first version of this measured exactly that: it reported a 150-pixel snap
     * for a background that had in fact closed its cycle to within a third of a pixel. One stripe,
     * chosen the same way every time.
     */
    private fun markerColumn(
        px: IntArray,
        width: Int,
        height: Int,
    ): Float {
        // The bottom band, which no section's body reaches, so nothing is drawn over the stripe.
        val band = (height - 40) until height
        for (x in 0 until width) {
            val lit =
                band.count { y ->
                    val p = px[y * width + x]
                    (p shr 16 and 0xFF) > 0xE0 && (p shr 8 and 0xFF) > 0xE0 && (p and 0xFF) > 0xE0
                }
            if (lit > band.count() / 2) return x.toFloat()
        }
        // Reached, in practice, by the very defect this test exists for: a layer whose rate is a
        // chosen coefficient rather than its own period drifts off the viewport entirely instead of
        // coming round again, and there is then no marker to measure.
        fail("no marker on screen — the background has drifted out of the viewport rather than wrapped")
    }

    @Test
    fun the_background_is_back_where_it_started_after_one_copy() {
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = KvadrantTypography.default(kvadrantLatin()),
                ) {
                    scroll = remember { ScrollState(0) }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
                            scroll = scroll,
                            background = { modifier ->
                                // One period, narrower than the viewport so that of the two copies
                                // laid out at least one marker is always on screen.
                                Row(modifier) {
                                    Box(Modifier.width(8.dp).fillMaxHeight().background(Color.White))
                                    Box(
                                        Modifier
                                            .width(292.dp)
                                            .fillMaxHeight()
                                            .background(KvadrantAccents.Cobalt),
                                    )
                                }
                            },
                            sections =
                                listOf(
                                    "one" to { KvadrantText("alpha", Modifier.width(500.dp)) },
                                    "two" to { KvadrantText("beta", Modifier.width(220.dp)) },
                                    "three" to { KvadrantText("gamma", Modifier.width(340.dp)) },
                                ),
                        )
                    }
                }
            }
            waitForIdle()

            fun markerAt(value: Int): Float {
                runBlocking { scroll.scrollTo(value) }
                waitForIdle()
                val image = onNodeWithTag("pano").captureToImage()
                val px = IntArray(image.width * image.height).also(image::readPixels)
                return markerColumn(px, image.width, image.height)
            }

            // Two copies are laid out, so `maxValue` is both of them less what the viewport shows.
            val viewport = onNodeWithTag("pano").captureToImage().width
            val copyWidth = (scroll.maxValue + viewport) / 2
            assertTrue(copyWidth > viewport, "the fixture must be wider than its viewport: $copyWidth")

            val start = markerAt(0)
            // The positive control. Part-way along, the marker has to have moved a long way —
            // otherwise a background that never drifted at all would satisfy the assertion below.
            val partway = markerAt(copyWidth / 3)
            assertTrue(abs(partway - start) > 20f, "the background did not drift: $start then $partway")

            // One pixel short of the fold, so the reading is of the last frame before the wrap
            // rather than of the frame the wrap produces.
            val almost = markerAt(copyWidth - 1)
            assertTrue(
                abs(almost - start) <= 2f,
                "the background does not close its cycle: $start at rest, $almost one pixel short " +
                    "of the fold, so the wrap snaps by ${abs(almost - start)} pixels",
            )
        }
    }
}
