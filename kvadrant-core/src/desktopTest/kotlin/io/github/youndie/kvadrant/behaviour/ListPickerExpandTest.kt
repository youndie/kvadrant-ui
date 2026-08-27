package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantListPicker
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opening a picker pushes what is below it apart over the 200 ms, rather than shoving it aside in
 * one frame and then animating something else.
 *
 * The control used to open with `graphicsLayer { scaleY }`, which is a *visual* scale: the list is
 * at full height in layout from the first frame, so a neighbour is already in its final place before
 * the animation has drawn anything. `ListPicker.cs` animates `FrameworkElement.HeightProperty` — a
 * layout property — and this is the difference, seen from the neighbour's side.
 *
 * The clock is stopped because the whole claim is about the frames in between; at either end the two
 * implementations are identical, which is exactly why nothing noticed.
 */
@OptIn(ExperimentalTestApi::class)
class ListPickerExpandTest {
    @Test
    fun the_neighbour_below_moves_gradually_rather_than_in_one_frame() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var expanded by mutableStateOf(false)
            setContent {
                KvadrantTheme {
                    Column {
                        KvadrantListPicker(
                            items = listOf("никогда", "каждые 15 минут", "каждый час"),
                            selectedIndex = 1,
                            onSelect = {},
                            expanded = expanded,
                            onExpandRequest = {},
                        )
                        Box(Modifier.size(40.dp).testTag("below"))
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            val closed = onNodeWithTag("below").getUnclippedBoundsInRoot().top.value

            expanded = true
            mainClock.advanceTimeBy(100) // half of the 200 ms
            val half = onNodeWithTag("below").getUnclippedBoundsInRoot().top.value

            mainClock.advanceTimeBy(500)
            val open = onNodeWithTag("below").getUnclippedBoundsInRoot().top.value

            assertTrue(open > closed + 1f, "the picker never opened: $closed -> $open")
            assertTrue(
                half > closed + 1f && half < open - 1f,
                "half way through the 200 ms the neighbour sits at $half, with $closed closed and " +
                    "$open open — it is not being pushed, it has already been moved",
            )
        }
}
