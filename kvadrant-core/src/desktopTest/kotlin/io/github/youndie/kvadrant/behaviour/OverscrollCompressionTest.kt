package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A list held past its end **slides away from it**, showing the page behind, which is how a Windows
 * Phone list ended.
 *
 * [B-38](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-38-the-theme-leaves-the-platform-overscroll.md):
 * `KvadrantTheme` replaced the ripple and left the platform's overscroll, so on Android a Metro list
 * finished with Android's stretch.
 *
 * **This file used to assert the opposite, and the file name is the last of it.** The effect was
 * built as a *squeeze* — the boundary held still and the content behind it compressed — on the
 * strength of Microsoft's name for the visual states, `HorizontalCompression` and
 * `VerticalCompression`. The design guidelines describe the behaviour and say the reverse: "when the
 * end of the list is reached, it will then **scroll up to display the empty section** and rubber
 * band back to rest in place" (`jj735577`). An empty section is precisely what a squeeze cannot
 * produce, and "compression" turns out to name the damping of the *manipulation* rather than
 * anything about pixels — which is also why those visual states carry no storyboard.
 *
 * So the assertion here is inverted from what it was, and it was **written to exclude the correct
 * behaviour**: it failed a run in which "the content is sliding away from the boundary rather than
 * squeezing towards it — that is a rubber band". A guard is only as good as the reading it was
 * written from.
 *
 * **Measured rather than photographed**, because the two candidates look similar in a still and are
 * opposites in a gesture: a slide moves the far edge and opens a gap, a squeeze holds the far edge
 * and moves everything behind it. At the bottom of a list the content's **bottom** comes up and the
 * page shows beneath it.
 */
@OptIn(ExperimentalTestApi::class)
class OverscrollCompressionTest {
    /** The frame the last [contentEdges] call read, so a second measurement need not re-render it. */
    private var lastPixels: IntArray = IntArray(0)
    private var lastWidth: Int = 0

    /**
     * The first and last rows carrying content, in pixels from the top of the viewport.
     *
     * [hold] leaves the finger down, which is the only state the effect exists in: releasing starts
     * the spring and a frame taken after that is a photograph of the return.
     */
    private fun contentEdges(hold: Boolean): Pair<Int, Int> {
        var edges = 0 to 0
        runComposeUiTest {
            setContent {
                KvadrantTheme(colors = KvadrantColors.dark()) {
                    val scroll = rememberScrollState()
                    // Start at the end, so the very next drag is entirely overscroll.
                    LaunchedEffect(Unit) { scroll.scrollTo(scroll.maxValue) }
                    Box(
                        Modifier
                            .size(WIDTH.dp, VIEWPORT.dp)
                            .background(Color.Black)
                            .testTag(TAG)
                            .verticalScroll(scroll),
                    ) {
                        // Bands rather than one block. A solid rectangle taller than the viewport
                        // cannot show a compression at all: whatever is squeezed away at the far
                        // edge is replaced by more of the same colour, and the first version of
                        // this test measured exactly that — no change, from an effect that was
                        // working.
                        androidx.compose.foundation.layout.Column {
                            repeat(BANDS) { index ->
                                Box(
                                    Modifier
                                        .size(WIDTH.dp, BAND.dp)
                                        .background(if (index % 2 == 0) Color.White else Color.Blue),
                                )
                            }
                        }
                    }
                }
            }
            waitForIdle()

            if (hold) {
                onNodeWithTag(TAG).performTouchInput {
                    down(center)
                    // Upwards, which at the end of a list is against the stop.
                    moveBy(
                        androidx.compose.ui.geometry
                            .Offset(0f, -PUSH),
                    )
                    moveBy(
                        androidx.compose.ui.geometry
                            .Offset(0f, -PUSH),
                    )
                }
                waitForIdle()
            }

            val image = onNodeWithTag(TAG).captureToImage()
            val pixels = IntArray(image.width * image.height).also(image::readPixels)

            // The first band boundary from the top, and the last lit row. The boundary is the
            // thing that moves: it sits a whole viewport away from the pivot, so a six per cent
            // compression displaces it by pixels rather than by a fraction of one.
            fun rowIsBlue(y: Int): Boolean {
                val pixel = pixels[y * image.width + image.width / 2]
                return (pixel and 0xFF) > 0x80 && (pixel shr 16 and 0xFF) < 0x40
            }
            val firstBlue = (0 until image.height).firstOrNull(::rowIsBlue) ?: -1
            val lastLit =
                (0 until image.height).lastOrNull { y ->
                    pixels[y * image.width + image.width / 2] and 0xFFFFFF != 0
                } ?: -1
            lastPixels = pixels
            lastWidth = image.width
            edges = firstBlue to lastLit
        }
        return edges
    }

    @Test
    fun a_list_held_past_its_end_slides_away_from_it() {
        val (restTop, restBottom) = contentEdges(hold = false)
        val (heldTop, heldBottom) = contentEdges(hold = true)

        // The control. At rest the content fills the viewport, so if it does not the harness is
        // measuring something other than a list at its stop and neither number below means anything.
        assertTrue(
            restTop >= 0 && restBottom >= VIEWPORT - 2,
            "at rest the first band boundary is at $restTop and the content ends at $restBottom in a " +
                "$VIEWPORT px viewport. A list at its stop fills the viewport and shows a band; this " +
                "is measuring something else",
        )

        assertTrue(
            heldBottom < restBottom - 1,
            "the content still reaches $heldBottom of $restBottom while the finger pushed past the " +
                "end, so no gap opened and the theme is still handing out the platform's overscroll",
        )
        // **And the content did not change size**, which is what separates a slide from a squeeze
        // and is measured on the last band: under a slide the whole band travels and keeps its
        // hundred pixels, under a squeeze it loses some of them.
        //
        // Not on the boundary nearest the top, which is where this looked first: at rest that
        // boundary is already row zero, so it has nowhere to move and the comparison holds for
        // anything.
        val restBand = lastBandHeight(hold = false)
        val heldBand = lastBandHeight(hold = true)
        assertTrue(
            heldBand in (restBand - 1)..(restBand + 1),
            "the last band is $heldBand px while the finger pushes past the end and $restBand at " +
                "rest, so the content is being squeezed rather than moved",
        )
    }

    /** How many rows of the last band are visible: the run of one colour up from the content's end. */
    private fun lastBandHeight(hold: Boolean): Int {
        val (_, bottom) = contentEdges(hold)
        val pixels = lastPixels
        val width = lastWidth

        fun blue(y: Int): Boolean {
            val pixel = pixels[y * width + width / 2]
            return (pixel and 0xFF) > 0x80 && (pixel shr 16 and 0xFF) < 0x40
        }
        val bottomIsBlue = blue(bottom)
        var run = 0
        var y = bottom
        while (y >= 0 && blue(y) == bottomIsBlue) {
            run++
            y--
        }
        return run
    }

    private companion object {
        const val TAG = "list"
        const val WIDTH = 200
        const val VIEWPORT = 300
        const val BANDS = 6
        const val BAND = 100
        const val PUSH = 120f
    }
}
