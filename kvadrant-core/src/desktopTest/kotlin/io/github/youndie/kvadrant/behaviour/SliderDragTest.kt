package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Dragging a slider must move the slider, and it must not turn the page underneath it.
 *
 * The two are the same defect seen from two sides. A slider that only listens for taps does not
 * simply fail to drag: the horizontal movement is still a gesture, an enclosing Pivot is still a
 * pager, and so sliding the thumb swipes to the next page. That is what a person reported, and it
 * is why the test puts the slider inside a real Pivot rather than alone in a Box — alone, the drag
 * has nothing to lose the gesture to and the interesting half of the bug cannot happen.
 */
@OptIn(ExperimentalTestApi::class)
class SliderDragTest {
    @Test
    fun dragging_the_thumb_moves_the_value_and_leaves_the_page_alone() =
        runComposeUiTest {
            val pagesSeen = mutableListOf<Int>()
            var value = 0.9f
            setContent {
                KvadrantTheme {
                    KvadrantPivot(titles = listOf("one", "two", "three")) { page ->
                        if (pagesSeen.lastOrNull() != page) pagesSeen += page
                        var v by remember { mutableStateOf(0.9f) }
                        Box(Modifier.size(300.dp)) {
                            KvadrantSlider(
                                value = v,
                                onValueChange = {
                                    v = it
                                    value = it
                                },
                                modifier = Modifier.fillMaxWidth().testTag("slider"),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            val pageBefore = pagesSeen.last()

            onNodeWithTag("slider").performTouchInput { swipeLeft() }
            waitForIdle()

            assertTrue(value < 0.5f, "the drag did not move the value: it is still $value")
            assertEquals(
                pageBefore,
                pagesSeen.last(),
                "dragging the slider turned the pivot page — the gesture reached the pager",
            )
        }
}
