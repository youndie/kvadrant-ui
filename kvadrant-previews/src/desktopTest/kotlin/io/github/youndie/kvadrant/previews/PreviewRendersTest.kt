package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantColors
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every preview composes, and draws something.
 *
 * This is the guard the documentation site rests on, and it is deliberately not a golden. Goldens
 * for the previews are wanted and are held back: the suite already cannot pass on the Linux runner
 * over the Cyrillic companion (B-35), and adding forty images to a set that is red makes the red
 * larger rather than the signal stronger. What can be checked today is checked today.
 *
 * "Draws something" is a low bar and it is the *right* low bar, because the way a preview fails is
 * total: a composable that throws takes the page's canvas with it, and one that lays out to nothing
 * leaves a rectangle of background that a reader cannot tell from a component drawn in the
 * background colour. Both of those are caught here. Whether a component is drawn *correctly* is the
 * golden suite's question and is asked in `kvadrant-core`.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewRendersTest {
    private val width = 360

    /**
     * How many pixels differ from the page's own background.
     *
     * The clock is advanced first: several previews arrive through a transition, and a still taken
     * at frame zero would photograph the state before the entrance rather than the component.
     */
    private fun inkOf(preview: KvadrantPreview): Int {
        var lit = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Box(Modifier.size(width.dp, preview.heightDp.dp).testTag(TAG)) {
                    Preview(preview)
                }
            }
            mainClock.advanceTimeBy(SETTLE_MILLIS)
            val image = onNodeWithTag(TAG).captureToImage()
            val pixels = IntArray(image.width * image.height).also(image::readPixels)
            val background = KvadrantColors.dark().background.toArgb()
            lit = pixels.count { it != background }
        }
        return lit
    }

    @Composable
    private fun Preview(preview: KvadrantPreview) {
        KvadrantPreviewHost(preview, dark = true)
    }

    @Test
    fun every_preview_draws_something() {
        val blank = KvadrantPreviews.all.filter { inkOf(it) == 0 }
        assertTrue(
            blank.isEmpty(),
            "these previews render nothing but the page background, and a reader cannot tell that " +
                "from a broken component: ${blank.map { it.id }}",
        )
    }

    /**
     * The control for the test above.
     *
     * Counting pixels that differ from the background only means anything if a preview that draws
     * nothing actually scores nought — and a stray antialiased edge, a border the host itself paints
     * or an off-by-one in the background colour would put the floor above zero and make every
     * assertion above pass on its own. So an empty preview is measured, and it must come back
     * empty.
     */
    @Test
    fun an_empty_preview_measures_as_empty() {
        val empty =
            KvadrantPreview(
                id = "control-empty",
                group = "control",
                component = "none",
                summary = "the negative control for every_preview_draws_something",
                heightDp = 200,
            ) {}
        assertTrue(inkOf(empty) == 0, "an empty preview does not measure as empty, so the guard is vacuous")
    }

    private companion object {
        const val TAG = "preview"

        /** Long enough for the slowest entrance here — the turnstile's — to have finished. */
        const val SETTLE_MILLIS = 1_000L
    }
}
