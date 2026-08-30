package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The wrap, checked by driving it rather than by looking at it.
 *
 * This was skipped once on the grounds that a still cannot show whether a wrap is seamless. That was
 * true of screenshots and stopped being true the moment there was a way to run a composition and
 * move it — so the reason expired and the work came back.
 */
@OptIn(ExperimentalTestApi::class)
class PanoramaWrapTest {
    @Test
    fun scrolling_to_the_end_folds_back_into_the_first_copy() =
        runComposeUiTest {
            lateinit var state: ScrollState

            setContent {
                KvadrantTheme {
                    state = rememberScrollState()
                    KvadrantPanorama(
                        title = "почта",
                        scroll = state,
                        // Wide enough that one copy overflows any test window: a panorama narrower
                        // than the screen has nothing to wrap, and the test would pass for that reason.
                        sections = List(3) { index -> "s$index" to { Box(Modifier.size(800.dp)) } },
                    )
                }
            }
            waitForIdle()

            val extent = state.maxValue
            assertTrue(extent > 0, "the panorama has to be scrollable at all; extent was $extent")

            // Drive it to the far end. With the fold in place the position comes back inside the first
            // copy of the sections; without it, the scroll simply stops there and stays.
            runBlocking { state.scrollTo(extent) }
            waitForIdle()

            assertTrue(
                state.value < extent,
                "scrolling to $extent should have folded back, but the position stayed at ${state.value}",
            )
        }
}
