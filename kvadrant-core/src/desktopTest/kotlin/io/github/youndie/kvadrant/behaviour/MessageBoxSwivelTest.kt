package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantMessageBox
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The message box swivels in, and stays composed long enough to swivel out.
 *
 * `CustomMessageBox.cs` shows itself with `SwivelTransitionMode.BackwardIn` and dismisses with
 * `BackwardOut` — it tips in around its top edge rather than sliding. Before this it appeared and
 * vanished on the frame its flag moved.
 *
 * Measured on the title's own bounds mid-flight against where they settle, because the swivel is a
 * rotation about the top edge with perspective: the further the box has tipped, the narrower and
 * shorter its content draws. Both ends are identical whether or not anything animated, which is why
 * the clock is stopped in between.
 */
@OptIn(ExperimentalTestApi::class)
class MessageBoxSwivelTest {
    @Test
    fun it_tips_in_rather_than_appearing() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var open by mutableStateOf(false)
            setContent {
                KvadrantTheme {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        KvadrantMessageBox(
                            visible = open,
                            title = "удалить письмо?",
                            message = "его нельзя будет вернуть",
                            onConfirm = {},
                            onCancel = {},
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(500)

            open = true
            // 350 ms is the whole entry.
            mainClock.advanceTimeBy(80)
            val midFlight = onNodeWithText("удалить письмо?").getUnclippedBoundsInRoot()

            mainClock.advanceTimeBy(2_000)
            val settled = onNodeWithText("удалить письмо?").getUnclippedBoundsInRoot()

            val midHeight = midFlight.bottom.value - midFlight.top.value
            val settledHeight = settled.bottom.value - settled.top.value
            assertTrue(
                abs(midHeight - settledHeight) > 1f || abs(midFlight.top.value - settled.top.value) > 1f,
                "the title was $midHeight tall at ${midFlight.top} 80 ms in and $settledHeight at " +
                    "${settled.top} when settled — the box appeared rather than tipping in",
            )
        }

    @Test
    fun it_is_still_there_while_it_leaves() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var open by mutableStateOf(true)
            setContent {
                KvadrantTheme {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        KvadrantMessageBox(
                            visible = open,
                            title = "удалить письмо?",
                            message = "его нельзя будет вернуть",
                            onConfirm = {},
                            onCancel = {},
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(2_000)

            open = false
            // 250 ms is the whole exit; at 80 the box must still be on screen to be leaving.
            mainClock.advanceTimeBy(80)
            onNodeWithText("удалить письмо?").assertExists()
        }
}
