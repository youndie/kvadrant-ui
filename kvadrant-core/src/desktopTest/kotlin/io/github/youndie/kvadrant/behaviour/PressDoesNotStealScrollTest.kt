package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.indication.kvadrantTilt
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
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
                KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
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
     * And it lets go, which "the list scrolled" does not say.
     *
     * A modifier that watches pointers without consuming them lets a scroll win *and* keeps its own
     * press: the list moves, the tile stays leaning, and the assertion above is satisfied by a
     * defect. What the phone did was give up at the same moment the scroll took over.
     *
     * **This constrains the outcome and not the mechanism, and that was checked rather than
     * assumed.** Removing `kvadrantTilt`'s explicit `isConsumed` yield leaves this test green: the
     * press is released anyway, by the node ceasing to receive that pointer once the scroll owns it.
     * The explicit yield stays because relying on that is relying on when Compose stops delivering
     * events to a loser, which is an implementation detail and not a contract — but nobody should
     * read a green run here as evidence that the line is load-bearing.
     */
    @Test
    fun the_press_is_released_when_the_scroll_takes_the_gesture() {
        var released = false
        runComposeUiTest {
            setContent {
                KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
                    val scroll = rememberScrollState()
                    val source = remember { MutableInteractionSource() }
                    LaunchedEffect(source) {
                        source.interactions.collect {
                            if (it is PressInteraction.Cancel || it is PressInteraction.Release) {
                                released = true
                            }
                        }
                    }
                    Box(Modifier.size(300.dp, 400.dp)) {
                        Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
                            Box(
                                Modifier
                                    .size(158.dp)
                                    .testTag("surface")
                                    .kvadrantTilt(interactionSource = source) {}
                                    .background(Color.White),
                            )
                            Box(Modifier.fillMaxWidth().height(1200.dp).background(Color.DarkGray))
                        }
                    }
                }
            }
            // Held down, then dragged far enough that the scroll claims it — and *not* lifted, so
            // an ordinary release cannot be what sets the flag.
            onNodeWithTag("surface").performTouchInput {
                down(
                    androidx.compose.ui.geometry
                        .Offset(79f, 140f),
                )
                moveTo(
                    androidx.compose.ui.geometry
                        .Offset(79f, 20f),
                )
            }
            waitForIdle()
        }
        assertTrue(released, "the scroll took the gesture and the tile is still leaning")
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

    /**
     * A drag that scrolled must not also count as a tap when the finger comes up.
     *
     * Reported from the phone: grab a tile, drag sideways to page the pivot, let go — and the tile's
     * page opens. The release is the whole of it. A gesture that has been taken by somebody else is
     * over, and lifting the finger afterwards is not a click on anything.
     */
    @Test
    fun a_drag_that_scrolled_is_not_a_click() {
        var clicks = 0
        runComposeUiTest {
            setContent {
                KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
                    val scroll = rememberScrollState()
                    Box(Modifier.size(300.dp, 400.dp)) {
                        Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
                            Box(
                                Modifier
                                    .size(158.dp)
                                    .testTag("surface")
                                    .kvadrantTilt { clicks++ }
                                    .background(Color.White),
                            )
                            Box(Modifier.fillMaxWidth().height(1200.dp).background(Color.DarkGray))
                        }
                    }
                }
            }
            // Down on the surface, dragged far enough for the scroll to claim it, then lifted.
            onNodeWithTag("surface").performTouchInput {
                down(
                    androidx.compose.ui.geometry
                        .Offset(79f, 140f),
                )
                moveTo(
                    androidx.compose.ui.geometry
                        .Offset(79f, 100f),
                )
                moveTo(
                    androidx.compose.ui.geometry
                        .Offset(79f, 40f),
                )
                up()
            }
            waitForIdle()
        }
        assertEquals(0, clicks, "lifting the finger after a scroll opened the tile")
    }
}
