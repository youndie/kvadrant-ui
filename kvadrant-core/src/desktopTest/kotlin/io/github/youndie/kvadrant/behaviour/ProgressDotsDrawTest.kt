package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantProgressDots
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the composable draws, sampled through the cycle — not what its exported keyframes say.
 *
 * [KvadrantProgressTest] reads `dotPath()` and `dotOpacity()`, which are specifications. A component
 * can export a correct specification and animate something else, and this one did the equivalent
 * for a long time: the opacity storyboard was simply absent from the drawing while the position was
 * right. So this drives the clock and counts pixels.
 */
@OptIn(ExperimentalTestApi::class)
class ProgressDotsDrawTest {
    @Test
    fun the_bar_empties_between_sweeps() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Box(Modifier.size(400.dp, 40.dp).background(Color.Black).testTag("dots")) {
                    KvadrantProgressDots(color = Color.Red)
                }
            }

            var elapsed = 0L

            fun litAt(millis: Long): Int {
                mainClock.advanceTimeBy(millis - elapsed)
                elapsed = millis
                val image = onNodeWithTag("dots").captureToImage()
                val px = IntArray(image.width * image.height).also(image::readPixels)
                return px.count { (it shr 16 and 0xFF) > 0x60 && (it shr 8 and 0xFF) < 0x40 }
            }

            // Five dots of three dp square, all still stacked at the left where they start.
            assertTrue(litAt(16) > 0, "nothing is drawn at the start of a sweep")
            // Mid-run: everything is travelling and everything is visible.
            assertTrue(litAt(1500) > 0, "the dots vanished mid-sweep")
            // The first dot lands at 2.5 s and goes out; the last starts at 0.8 and lands at 3.3.
            val late = litAt(3200)
            val empty = litAt(3800)
            assertTrue(late > 0, "the last dots went out before they landed")
            assertEquals(0, empty, "the bar is not empty between sweeps: $empty lit pixels at 3.8 s")
            // And the next sweep begins.
            assertTrue(litAt(4500) > 0, "the cycle did not come round")
        }
    }
}
