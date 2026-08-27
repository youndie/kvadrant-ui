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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The **step** from one weight to the next has to be the same size in both scripts.
 *
 * [InkParityTest] asks whether Cyrillic at a given axis value matches Latin at the corresponding
 * slot; this asks the question a reader actually notices, which is whether *going bolder* costs the
 * same in each. Two scripts can each be individually plausible and still make a bold heading look
 * like two different decisions, because the eye compares a word to its neighbours rather than to a
 * font it has never seen.
 *
 * Measured: Latin steps ×1.271 from Normal to SemiBold, Cyrillic ×1.280 — seven parts in a thousand
 * apart. The tolerance below is far looser than that on purpose; it is there to catch a
 * recalibration that moves one script and not the other, not to pin the current numbers.
 *
 * **What this does not settle, and cannot.** SemiBold lands at 0.915 of Selawik's own Bold — nearly
 * at the top of its ramp — and Selawik is a clone of **Segoe UI**, which has six faces. The phone
 * had **Segoe WP**, whose theme dictionary names four and no Bold at all, so its Semibold *was* the
 * top. Same word, different position in two different families, and no measurement here can bridge
 * that without a font this repository will not hold. See D8.
 */
@OptIn(ExperimentalTestApi::class)
class WeightStepTest {
    private fun coverage(
        text: String,
        weight: FontWeight,
        family: @Composable () -> FontFamily,
    ): Float {
        var value = 0f
        runComposeUiTest {
            setContent {
                Box(Modifier.size(600.dp, 120.dp).background(Color.Black).testTag("frame")) {
                    BasicText(text, style = TextStyle(Color.White, 64.sp, weight, fontFamily = family()))
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

    @Test
    fun the_semibold_step_is_the_same_size_in_both_scripts() {
        val steps =
            listOf<Pair<String, @Composable () -> FontFamily>>(
                "settings" to { kvadrantLatin() },
                "диалог" to { kvadrantCyrillic() },
            ).map { (word, family) ->
                val normal = coverage(word, FontWeight.W400, family)
                val semi = coverage(word, FontWeight.W600, family)
                val bold = coverage(word, FontWeight.W700, family)
                // The positive control. Without it a family that ignored the requested weight
                // entirely — which is exactly the defect the five instanced faces were added to
                // fix — would give three identical numbers and one very stable ratio of 1.0.
                assertTrue(normal < semi && semi < bold, "$word: $normal / $semi / $bold is not a ramp")
                semi / normal
            }
        assertTrue(
            abs(steps[0] - steps[1]) < 0.05f,
            "the SemiBold step differs between scripts: latin ${steps[0]}, cyrillic ${steps[1]}",
        )
    }
}
