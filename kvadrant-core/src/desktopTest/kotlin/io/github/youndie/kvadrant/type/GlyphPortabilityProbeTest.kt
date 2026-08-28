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
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.resources.Res
import io.github.youndie.kvadrant.resources.selawik_semilight
import io.github.youndie.kvadrant.resources.source_sans_3_variable
import org.jetbrains.compose.resources.Font
import kotlin.test.Test

/**
 * Why a golden that is identical on macOS and on Linux stops being identical when the text is
 * Cyrillic — measured, and measured in a way that separates the two answers.
 *
 * **The two answers look the same in a diff and need opposite fixes.** If the glyphs land in the
 * same places and only their edges differ, the cause is the rasteriser and the fix is somewhere
 * near hinting and smoothing. If the *ink box moves or changes width*, the cause is shaping — a
 * different face was selected, or the same face was measured differently — and no amount of
 * antialiasing settings will touch it. B-35 was opened claiming the first and one diff showed a
 * word drawn twice at two widths, which is the second.
 *
 * So this prints a bounding box, not a similarity. It asserts nothing: a single run sees one
 * platform, and the comparison is between two runs. Read it by running the suite here and reading
 * the same lines out of the CI log.
 *
 *     ./gradlew :kvadrant-core:desktopTest --tests '*GlyphPortabilityProbeTest*' -i | grep PROBE
 *
 * The cases are chosen to make the answer a difference between two of them rather than a judgement
 * about one:
 *
 *  - `latin-selawik` is the control. It is clean in every diff so far, so a run where *this* moves
 *    means the probe is measuring something other than what it claims to.
 *  - `cyrillic-source-sans-370` is the shipping stack — a variable font instanced on `wght` at run
 *    time.
 *  - `cyrillic-source-sans-default` is the same file with no axis setting at all. If instancing is
 *    the problem, these two differ from each other differently on the two platforms.
 *  - `cyrillic-fira` and `cyrillic-inter` are static faces with real Cyrillic, bundled in this
 *    source set. They carry no axis, so a difference in them cannot be about instancing. One of
 *    them already fails the golden suite on Linux and the other does not, which is the single most
 *    informative fact available and the reason both are here.
 *  - `cyrillic-fallback` has no Cyrillic in its family at all, so the host chooses. It is the
 *    expected-to-differ case, and it is here to prove the probe can see a difference when there
 *    certainly is one.
 */
@OptIn(ExperimentalTestApi::class)
class GlyphPortabilityProbeTest {
    @Test
    fun probe() {
        println("PROBE os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}")
        cases().forEach { (label, content) -> measure(label, content) }
    }

    private fun cases(): List<Pair<String, @Composable () -> FontFamily>> =
        listOf(
            "latin-selawik" to { FontFamily(Font(Res.font.selawik_semilight, WEIGHT)) },
            "cyrillic-source-sans-370" to {
                FontFamily(
                    Font(
                        Res.font.source_sans_3_variable,
                        WEIGHT,
                        variationSettings = FontVariation.Settings(FontVariation.weight(370)),
                    ),
                )
            },
            "cyrillic-source-sans-default" to {
                FontFamily(Font(Res.font.source_sans_3_variable, WEIGHT))
            },
            "cyrillic-fira" to { FontFamily(Font(resource = "fonts/FiraSans-Light.ttf", weight = WEIGHT)) },
            "cyrillic-inter" to { FontFamily(Font(resource = "fonts/Inter-Light.ttf", weight = WEIGHT)) },
            "cyrillic-fallback" to { FontFamily(Font(Res.font.selawik_semilight, WEIGHT)) },
        )

    private fun measure(
        label: String,
        family: @Composable () -> FontFamily,
    ) {
        val text = if (label.startsWith("latin")) "settings" else "настройки"
        runComposeUiTest {
            setContent {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color.Black).testTag(TAG)) {
                    BasicText(
                        text,
                        style =
                            TextStyle(
                                color = Color.White,
                                fontSize = SIZE.sp,
                                fontWeight = WEIGHT,
                                fontFamily = family(),
                            ).portable(),
                    )
                }
            }
            // compose-resources loads a font asynchronously, and a frame taken before it lands
            // measures the *host fallback* while looking like a successful measurement. Capturing
            // until two consecutive frames agree is what separates "the font is loaded" from "the
            // font has not arrived yet", and it costs one extra capture in the settled case.
            var previous = -1
            var pixels = IntArray(0)
            var image = onNodeWithTag(TAG).captureToImage()
            repeat(SETTLE_ATTEMPTS) {
                waitForIdle()
                image = onNodeWithTag(TAG).captureToImage()
                pixels = IntArray(image.width * image.height).also(image::readPixels)
                val digest = pixels.fold(0) { acc, pixel -> acc * 31 + pixel }
                if (digest == previous) return@repeat
                previous = digest
            }

            var ink = 0
            var left = image.width
            var right = -1
            var top = image.height
            var bottom = -1
            pixels.forEachIndexed { index, pixel ->
                // Anything at all above the black ground. A threshold would hide exactly the faint
                // edge pixels that tell antialiasing apart from a moved glyph.
                if (pixel and 0xFFFFFF != 0) {
                    ink++
                    val x = index % image.width
                    val y = index / image.width
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
            println(
                "PROBE $label ink=$ink box=$left,$top..$right,$bottom " +
                    "w=${right - left + 1} h=${bottom - top + 1}",
            )
        }
    }

    private companion object {
        const val TAG = "probe"

        /** Enough frames for an asynchronous font load to land; two agreeing frames end it early. */
        const val SETTLE_ATTEMPTS = 8
        const val WIDTH = 480
        const val HEIGHT = 120

        /** The Metro header weight, which is where the golden suite's largest differences are. */
        val WEIGHT = FontWeight.W300
        const val SIZE = 54
    }
}
