package io.github.youndie.kvadrant.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.foundation.CYRILLIC_BOLD_WEIGHT
import io.github.youndie.kvadrant.foundation.CYRILLIC_LIGHT_WEIGHT
import io.github.youndie.kvadrant.foundation.CYRILLIC_NORMAL_WEIGHT
import io.github.youndie.kvadrant.foundation.CYRILLIC_SEMIBOLD_WEIGHT
import io.github.youndie.kvadrant.foundation.CYRILLIC_SEMILIGHT_WEIGHT
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.resources.Res
import io.github.youndie.kvadrant.resources.source_sans_3_variable
import org.jetbrains.compose.resources.Font
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * How much of its own box a line of text fills, so two scripts can be compared as ink rather than as
 * numbers out of a font's metadata.
 *
 * Selawik's SemiLight was matched to Source Sans 3 at **370** by eye during B-03, and every other
 * Metro weight is still uncalibrated — which matters more than it sounds, because the Cyrillic
 * companion is built as a family with a single instanced weight, so a `SemiBold` heading renders
 * Latin bold and Cyrillic at 370 beside it.
 *
 * **This one is deliberately *not* pinned to viddik's hinting, and the goldens around it are.** A
 * golden is compared between machines and has to be identical; a measurement is about how the
 * renderer actually renders, and pinning it measures a condition nothing ships in. Pinning it was
 * tried by accident and moved SemiLight from 370 to **380** — which is not a better answer, it is an
 * answer to a different question.
 *
 * **The known 370 is this measurement's positive control.** If the metric cannot rediscover it at
 * `W300`, it has no business proposing values for the other four.
 */
@OptIn(ExperimentalTestApi::class)
class InkParityTest {
    /** Lit pixels over the area of the drawn line: darker faces fill more of the same box. */
    private fun coverage(
        text: String,
        weight: FontWeight,
        family: @Composable () -> FontFamily,
    ): Float {
        var value = 0f
        runComposeUiTest {
            setContent {
                Box(Modifier.size(600.dp, 120.dp).background(Color.Black).testTag("frame")) {
                    BasicText(
                        text,
                        style = TextStyle(Color.White, 64.sp, weight, fontFamily = family()),
                    )
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val px = IntArray(image.width * image.height)
            image.readPixels(px)
            var lit = 0
            var minX = image.width
            var maxX = -1
            var minY = image.height
            var maxY = -1
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    if ((px[y * image.width + x] shr 16 and 0xFF) > 0x40) {
                        lit++
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            value = if (maxX < 0) 0f else lit.toFloat() / ((maxX - minX + 1) * (maxY - minY + 1))
        }
        return value
    }

    @Composable
    private fun cyrillicAt(axis: Int) =
        FontFamily(
            Font(
                Res.font.source_sans_3_variable,
                FontWeight(axis),
                variationSettings = FontVariation.Settings(FontVariation.weight(axis)),
            ),
        )

    @Test
    fun every_calibrated_weight_is_still_the_closest_match() {
        val calibrated =
            listOf(
                Triple("Light", FontWeight.W200, CYRILLIC_LIGHT_WEIGHT),
                Triple("SemiLight", FontWeight.W300, CYRILLIC_SEMILIGHT_WEIGHT),
                Triple("Normal", FontWeight.W400, CYRILLIC_NORMAL_WEIGHT),
                Triple("SemiBold", FontWeight.W600, CYRILLIC_SEMIBOLD_WEIGHT),
                Triple("Bold", FontWeight.W700, CYRILLIC_BOLD_WEIGHT),
            )
        calibrated.forEach { (name, slot, axis) ->
            val latin = coverage("settings", slot) { kvadrantLatin() }
            // A window rather than the whole axis: the sweep that produced these took eighteen
            // seconds, and re-running it on every `check` would buy nothing a window does not.
            val best =
                ((axis - 30)..(axis + 30) step 10).minByOrNull { candidate ->
                    abs(coverage("настройки", FontWeight(candidate)) { cyrillicAt(candidate) } - latin)
                }!!
            assertTrue(
                best == axis,
                "$name (${slot.weight}) now matches Source Sans at $best, not the calibrated $axis " +
                    "— either a font file changed under us or the measurement did",
            )
        }
    }

    @Test
    fun the_metric_rediscovers_the_calibrated_semilight() {
        val latin = coverage("settings", FontWeight.W300) { kvadrantLatin() }
        val candidates =
            (240..520 step 10).associateWith { axis ->
                abs(coverage("настройки", FontWeight(axis)) { cyrillicAt(axis) } - latin)
            }
        val best = candidates.minByOrNull { it.value }!!.key

        assertTrue(
            abs(best - 370) <= 40,
            "the metric puts Selawik SemiLight's Cyrillic match at $best, where B-03 measured 370 " +
                "by eye. Off by more than the 10-step grid, so it is measuring something else and " +
                "must not be used to calibrate the other four weights. Latin coverage $latin.",
        )
    }
}
