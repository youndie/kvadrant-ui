package io.github.youndie.kvadrant.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-14's first criterion, on the component it names: a Material `DatePicker` under the adapter.
 *
 * It is measured rather than assumed because a Material `Button` is not squared by theming at all —
 * `ButtonDefaults.shape` is a token — and after finding that, "the theme sets rectangular shapes,
 * therefore the date picker is rectangular" is a sentence that has already been wrong once.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class DatePickerTest {
    @Test
    fun a_date_picker_draws_in_the_accent_with_no_rounded_container() =
        runComposeUiTest {
            var containerShape = ""
            setContent {
                KvadrantTheme(colors = KvadrantColors.dark(KvadrantAccents.Magenta)) {
                    KvadrantMaterialAdapter {
                        containerShape = DatePickerDefaults.shape.toString()
                        Box(Modifier.size(420.dp, 560.dp).testTag("frame")) {
                            DatePicker(state = rememberDatePickerState(initialSelectedDateMillis = 0L))
                        }
                    }
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)

            assertTrue(pixels.any { (it and 0xFFFFFF) != 0 }, "the date picker drew nothing at all")

            val accent = KvadrantAccents.Magenta
            val hits =
                pixels.count { p ->
                    val r = p shr 16 and 0xFF
                    val g = p shr 8 and 0xFF
                    val b = p and 0xFF
                    kotlin.math.abs(r - (accent.red * 255).toInt()) < 8 &&
                        kotlin.math.abs(b - (accent.blue * 255).toInt()) < 8 &&
                        g < 60
                }
            assertTrue(hits > 100, "only $hits pixels of the accent — the selection is not wearing it")

            // Whatever this reports is the truth about the container, and the message carries it
            // either way: the point of the test is that the answer is read off the component rather
            // than inferred from the theme.
            assertTrue(
                "0.0.dp" in containerShape,
                "DatePickerDefaults.shape is $containerShape — it does not follow the theme, so " +
                    "the date picker is one more component that needs a shape at the call site",
            )
        }
}
