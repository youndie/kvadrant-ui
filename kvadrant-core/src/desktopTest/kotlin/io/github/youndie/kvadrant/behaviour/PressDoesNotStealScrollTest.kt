package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.indication.kvadrantTilt
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A finger that starts on a pressable surface and then moves scrolls the list under it.
 *
 * A surface that takes the touch on the way down and keeps it is a surface that cannot be scrolled
 * past: the press follows the finger and the list stands still. `clickable` gets this right — it
 * gives the gesture up at the touch slop — and anything that handles pointers itself has to be shown
 * to, rather than assumed to.
 */
@OptIn(ExperimentalTestApi::class)
class PressDoesNotStealScrollTest {
    private fun scrolledAfterDrag(surface: @androidx.compose.runtime.Composable () -> Unit): Int {
        var moved = 0
        runComposeUiTest {
            setContent {
                KvadrantTheme(KvadrantColors.dark(), KvadrantTypography.default(kvadrantLatin())) {
                    val scroll = rememberScrollState()
                    Box(Modifier.size(300.dp, 400.dp).testTag("list")) {
                        Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
                            // Tagged, because the gesture has to *start on the surface*. Swiping the
                            // list instead starts at the centre of a 400 dp box, below a 158 dp tile
                            // at the top of it — the finger never touches the thing under test and
                            // both cases pass, which is what the first version of this did.
                            Box(Modifier.testTag("surface")) { surface() }
                            Box(Modifier.fillMaxWidth().height(1200.dp).background(Color.DarkGray))
                        }
                    }
                    moved = scroll.value
                }
            }
            onNodeWithTag("surface").performTouchInput { swipeUp() }
            waitForIdle()
        }
        return moved
    }

    @Test
    fun a_tile_lets_the_list_scroll_under_the_finger() {
        val scrolled =
            scrolledAfterDrag {
                KvadrantTile(TileSize.Medium, color = Color.White, onClick = {}) {}
            }
        assertTrue(scrolled > 100, "the list did not scroll: the tile kept the touch ($scrolled px)")
    }

    @Test
    fun the_finger_tracking_modifier_lets_the_list_scroll_too() {
        val scrolled =
            scrolledAfterDrag {
                Box(Modifier.size(158.dp).kvadrantTilt {}.background(Color.White))
            }
        assertTrue(
            scrolled > 100,
            "the list did not scroll: kvadrantTilt kept the touch and dragged the press with it " +
                "($scrolled px)",
        )
    }

    /**
     * The control, and the file is worth little without it.
     *
     * Two green assertions about something *not* happening prove nothing until the harness has been
     * shown to notice when it does. This surface consumes every pointer change it sees, which is
     * exactly the failure the other two are asserting the absence of.
     */
    @Test
    fun a_surface_that_swallows_the_gesture_stops_the_list() {
        val scrolled =
            scrolledAfterDrag {
                Box(
                    Modifier
                        .size(158.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        }.background(Color.White),
                )
            }
        assertEquals(0, scrolled, "the greedy surface did not stop the list, so this test cannot see stealing")
    }
}
