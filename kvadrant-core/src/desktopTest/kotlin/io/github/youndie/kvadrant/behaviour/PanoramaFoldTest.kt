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
     * Which block of the background is under a fixed screen column, decoded from its own colour.
     *
     * Two measurements were discarded before this one, and both were wrong in the same way — they
     * were readings of something that is not always there. Photographing the fold reads the frame
     * after `scroll.value` is already back at zero, so it is the frame at zero whatever the layer
     * did. Averaging every white pixel of a stripe moves when a *second* copy of the stripe merely
     * enters the viewport, and reported a 150-pixel snap for a layer that had closed its cycle to
     * within a third of a pixel.
     *
     * A background painted as [BLOCKS] blocks of distinct colour has no such gaps: whatever the
     * offset, some block is under the column being read, and its colour says which.
     */
    private fun blockUnderColumn(
        px: IntArray,
        width: Int,
        height: Int,
    ): Int {
        // The bottom band, which no section's body reaches, so nothing is drawn over the background.
        val p = px[(height - 10) * width + width / 2]
        assertTrue(p and 0xFF == 0x80, "column is not showing the background at all: %08X".format(p))
        return (p shr 16 and 0xFF) / STRIDE
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
                                // **Wider than the viewport, and that is the point of the fixture.**
                                // The first version of this was 300 dp against a 400 dp viewport, so
                                // the bounded measurement it was meant to catch happened to return
                                // the right number and the test passed over the defect the sample
                                // was showing on a phone.
                                Row(modifier) {
                                    repeat(BLOCKS) { i ->
                                        Box(
                                            Modifier
                                                .width(8.dp)
                                                .fillMaxHeight()
                                                .background(Color(i * STRIDE, 0, 0x80)),
                                        )
                                    }
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

            fun markerAt(value: Int): Int {
                runBlocking { scroll.scrollTo(value) }
                waitForIdle()
                val image = onNodeWithTag("pano").captureToImage()
                val px = IntArray(image.width * image.height).also(image::readPixels)
                return blockUnderColumn(px, image.width, image.height)
            }

            // Two copies are laid out, so `maxValue` is both of them less what the viewport shows.
            val viewport = onNodeWithTag("pano").captureToImage().width
            val copyWidth = (scroll.maxValue + viewport) / 2
            assertTrue(copyWidth > viewport, "the fixture must be wider than its viewport: $copyWidth")

            // Blocks are a ring, so the distance between two of them is the shorter way round.
            fun apart(
                a: Int,
                b: Int,
            ): Int = minOf(abs(a - b), BLOCKS - abs(a - b))

            val start = markerAt(0)
            // The positive control. Part-way along, the background has to have moved a long way —
            // otherwise one that never drifted at all would satisfy the assertion below.
            val partway = markerAt(copyWidth / 3)
            assertTrue(apart(partway, start) > 5, "the background did not drift: $start then $partway")

            // One pixel short of the fold, so the reading is of the last frame before the wrap
            // rather than of the frame the wrap produces.
            val almost = markerAt(copyWidth - 1)
            assertTrue(
                apart(almost, start) <= 1,
                "the background does not close its cycle: block $start at rest, block $almost one " +
                    "pixel short of the fold, so the wrap snaps by ${apart(almost, start)} blocks",
            )
        }
    }
}

/** Seventy-five blocks of eight dp: a 600 dp period against the fixture's 400 dp viewport. */
private const val BLOCKS = 75

/** Red channel per block, so seventy-five of them stay inside a byte and decode exactly. */
private const val STRIDE = 3
