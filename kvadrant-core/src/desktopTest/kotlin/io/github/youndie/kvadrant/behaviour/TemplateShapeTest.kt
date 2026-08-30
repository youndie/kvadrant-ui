package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.components.KvadrantTextBox
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Two things `System.Windows.xaml` says that this library had been drawing the other way round.
 *
 * Both were in components whose templates B-32 listed as unread, and both are visible in a still —
 * which is the point: they had been on screen, in goldens, for months, and nobody had a source to
 * compare them against.
 */
@OptIn(ExperimentalTestApi::class)
class TemplateShapeTest {
    private fun pixels(content: @Composable () -> Unit): Triple<IntArray, Int, Int> {
        var result = Triple(IntArray(0), 0, 0)
        runComposeUiTest {
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    Box(Modifier.size(300.dp, 120.dp).background(Color.Black).testTag("frame")) { content() }
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            result = Triple(IntArray(image.width * image.height).also(image::readPixels), image.width, image.height)
        }
        return result
    }

    /**
     * `PhoneSimpleThumb` is `<Rectangle Fill="Transparent"/>`, and the thumb is one pixel wide with
     * a `ScaleTransform ScaleX="32"` — a handle for the finger and nothing to look at. This drew a
     * foreground-coloured block, named in its own KDoc as this project's own on the grounds that
     * the template had not been recovered.
     */
    @Test
    fun the_slider_draws_no_thumb() {
        val (px, _, _) = pixels { KvadrantSlider(0.5f, {}, Modifier.fillMaxWidth()) }
        // The theme's foreground is pure white and nothing in a slider is entitled to it: the fill
        // is the accent and the track is white at a fifth, which lands far below this.
        val white = px.count { (it shr 16 and 0xFF) > 0xE0 && (it shr 8 and 0xFF) > 0xE0 && (it and 0xFF) > 0xE0 }
        assertEquals(0, white, "$white pixels of foreground in a slider that should have no thumb")
        // The positive control: the bar itself has to be there, or the assertion above is satisfied
        // by a slider that drew nothing at all.
        val accent = px.count { (it and 0xFF) > 0xA0 && (it shr 16 and 0xFF) < 0x80 }
        assertTrue(accent > 100, "the slider drew no accent fill either: $accent pixels")
    }

    /**
     * `Background` and `BorderBrush` are both `PhoneTextBoxBrush` — 75 % white on a dark page. A
     * Metro text box is a light box with dark text in *both* themes, and this drew a transparent
     * field with the page's own foreground in it.
     */
    @Test
    fun the_text_box_is_filled_before_it_is_focused() {
        val (px, _, _) = pixels { KvadrantTextBox("готово", {}, Modifier.fillMaxWidth()) }
        // Counted rather than sampled: the field sits where the layout puts it, and a single
        // coordinate chosen by hand lands outside it the moment anything above it changes height —
        // which is how the first version of this failed, on a field that was drawn correctly.
        val filled = px.count { (it shr 16 and 0xFF) in 0x90..0xF0 && (it and 0xFF) in 0x90..0xF0 }
        assertTrue(filled > 2000, "the field is not filled: only $filled pixels of PhoneTextBoxBrush")
    }
}
