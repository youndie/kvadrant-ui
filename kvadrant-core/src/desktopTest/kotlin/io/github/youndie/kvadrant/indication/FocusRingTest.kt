package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.theme.KvadrantMetrics
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.scaled
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The dotted ring appears for a keyboard and for nothing else.
 *
 * Two claims, and the second is the one that keeps this library's screenshots still. Windows 8's
 * template draws its focus rectangle from `Focused` and leaves `PointerFocused` empty; Compose has
 * no such pair, and on desktop `Modifier.clickable` takes focus on click — so a ring drawn on every
 * focus would sit around every tile a mouse had ever touched. The condition that stops it is
 * `InputModeManager`, and this test is what says the condition is actually wired: the pointer case
 * asserts the surface **is focused** before asserting that nothing was drawn, so a green result
 * cannot come from focus never having been taken.
 *
 * [B-40](../../../../../../../../docs/backlog/B-40-keyboard-and-focus-on-desktop-and-wasm.md).
 */
@OptIn(ExperimentalTestApi::class)
class FocusRingTest {
    @Test
    fun a_keyboard_focus_draws_a_black_and_white_dotted_ring() =
        runComposeUiTest {
            surface()
            val resting = pixels()
            assertTrue(BLACK !in resting && WHITE !in resting, "the surface is not a flat colour to start with")

            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            assertEquals(true, focused(), "tab did not focus the surface")

            val ringed = pixels()
            assertTrue(BLACK in ringed, "no black dashes: the ring is not drawn")
            assertTrue(WHITE in ringed, "no white dashes: only one of the template's two rectangles is drawn")

            // Alternating rather than one long side of each colour: the two rectangles are a dash apart,
            // and drawing them at the same phase would put the white exactly under the black and leave
            // a plain white line. Count the runs along the top edge.
            val top = (0 until SIDE).map { x -> ringed[x] }
            val changes = (1 until SIDE).count { top[it] != top[it - 1] }
            assertTrue(
                changes > SIDE / 4,
                "the top edge changes colour $changes times in $SIDE pixels, which is not a dashed line",
            )

            // And nothing but the border moved. The ring is a ring, not a wash over the content.
            val inside = { p: IntArray ->
                (2 until SIDE - 2).flatMap { y -> (2 until SIDE - 2).map { x -> p[y * SIDE + x] } }
            }
            assertContentEquals(inside(resting), inside(ringed), "the ring bled into the content")
        }

    /**
     * The tile is a separate claim because it is a separate path: it is the one surface in the
     * library that does not use `clickable`, so its `focusable` and the indication that draws the
     * ring are two modifiers this project wrote and ordered itself. Getting that order wrong puts
     * the focus target above the indication instead of below it, and the indication then never
     * hears about the focus — with no error anywhere, because a ring that is not drawn looks
     * exactly like a ring that was not asked for.
     */
    @Test
    fun the_tile_gets_the_ring_too() =
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantTile(onClick = {}, modifier = Modifier.testTag(TAG).size(SIDE.dp)) { }
                }
            }
            waitForIdle()
            val resting = pixels()
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            assertEquals(true, focused(), "tab did not focus the tile")
            val ringed = pixels()
            assertTrue(ringed.toList() != resting.toList(), "the tile draws no focus ring")
            assertTrue(BLACK in ringed && WHITE in ringed, "the tile's ring is missing one of its two rectangles")
        }

    /**
     * A scaled theme scales the ring, which is a claim about **wiring** rather than about
     * arithmetic. `scaled()` multiplying one more field is covered where the rest of the set is;
     * what is not covered anywhere else is that `KvadrantTheme` hands the value to the indication at
     * all. Leave that argument off and the ring keeps the default thickness for ever, in a theme
     * where every other measurement has moved — with nothing failing.
     */
    @Test
    fun a_scaled_theme_draws_a_thicker_ring() =
        runComposeUiTest {
            setContent {
                KvadrantTheme(metrics = KvadrantMetrics().scaled(4f)) {
                    Box(
                        Modifier
                            .testTag(TAG)
                            .size(SIDE.dp)
                            .background(GREY)
                            .clickable {},
                    )
                }
            }
            waitForIdle()
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            val thick = pixels().count { it == BLACK || it == WHITE }

            var thin = 0
            runComposeUiTest {
                surface()
                onRoot().performKeyInput { pressKey(Key.Tab) }
                waitForIdle()
                thin = pixels().count { it == BLACK || it == WHITE }
            }

            assertTrue(thin > 0, "the unscaled ring is not drawn, so there is nothing to compare against")
            assertTrue(
                thick > thin * 2,
                "a theme scaled fourfold drew $thick ring pixels against $thin unscaled — the thickness " +
                    "is not reaching the indication from the metric set",
            )
        }

    @Test
    fun a_mouse_focus_draws_nothing() =
        runComposeUiTest {
            surface()
            val resting = pixels()

            onNodeWithTag(TAG).performMouseInput {
                press()
                release()
            }
            // The press leans the surface and the lean unwinds over the return delay and duration. A
            // capture taken before that finishes would differ for a reason that has nothing to do with
            // focus, and the difference would read as a ring.
            mainClock.advanceTimeBy(SETTLE_MILLIS)
            waitForIdle()

            // The positive control, and the whole reason this test is not vacuous: the mouse really did
            // leave the surface focused. Without this line an assertion that nothing is drawn would pass
            // just as happily if clicking had never focused anything.
            assertEquals(true, focused(), "the mouse did not leave the surface focused — this test proves nothing")
            assertContentEquals(resting.toList(), pixels().toList(), "a mouse click drew a focus ring")
        }

    private fun ComposeUiTest.surface() {
        setContent {
            KvadrantTheme {
                Box(
                    Modifier
                        .testTag(TAG)
                        .size(SIDE.dp)
                        .background(GREY)
                        .clickable {},
                )
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.focused(): Boolean? =
        onNodeWithTag(TAG).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused)

    private fun ComposeUiTest.pixels(): IntArray {
        val image = onNodeWithTag(TAG).captureToImage()
        return IntArray(image.width * image.height).also(image::readPixels)
    }

    private companion object {
        const val TAG = "surface"

        /** Density is 1 in the test environment, so a side in dp is a side in pixels. */
        const val SIDE = 60
        const val SETTLE_MILLIS = 500L
        val GREY = Color(0xFF808080)
        val BLACK = Color.Black.toArgb()
        val WHITE = Color.White.toArgb()
    }
}
