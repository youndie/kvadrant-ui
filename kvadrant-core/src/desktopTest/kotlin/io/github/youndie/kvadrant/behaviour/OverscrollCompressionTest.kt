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
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A list held past its end **compresses**, which is how a Windows Phone list ended.
 *
 * [B-38](../../../../../../../../docs/backlog/B-38-the-theme-leaves-the-platform-overscroll.md):
 * `KvadrantTheme` replaced the ripple and left the platform's overscroll, so on Android a Metro list
 * finished with Android's stretch. Compression is Microsoft's own word for what it did instead —
 * Windows Phone 7.1 added `HorizontalCompression` and `VerticalCompression` visual states so an
 * application could react to it.
 *
 * **Measured rather than photographed**, because the two candidate behaviours look similar in a
 * still and are opposites in a gesture. A *translation* — iOS's rubber band, and what a naive
 * implementation produces — slides the content away from the edge and leaves a gap, so the boundary
 * moves and the far edge retreats. A *compression* keeps the boundary where it is and squeezes what
 * is behind it. So the assertion is about which edge moved: at the bottom of a list, the content's
 * **top** comes down and its bottom stays put.
 */
@OptIn(ExperimentalTestApi::class)
class OverscrollCompressionTest {
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
            edges = firstBlue to lastLit
        }
        return edges
    }

    @Test
    fun a_list_held_past_its_end_squeezes_towards_that_end() {
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
            heldTop > restTop,
            "the band boundary did not move while the finger pushed past the end " +
                "($heldTop against $restTop), so nothing compressed and the theme is still handing " +
                "out the platform's overscroll",
        )
        // And the edge being pushed against stays where it is: that is what makes this a
        // compression rather than the translation an easier implementation would produce.
        assertTrue(
            heldBottom >= restBottom - 1,
            "the bottom edge retreated from $restBottom to $heldBottom, so the content is sliding " +
                "away from the boundary rather than squeezing towards it — that is a rubber band",
        )
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
