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
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.resources.Res
import io.github.youndie.kvadrant.resources.selawik_semilight
import org.jetbrains.compose.resources.Font
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A `FontFamily` list is not a glyph-fallback chain, asserted rather than photographed.
 *
 * This is research §1.7's correction to the brief, and it used to be verified by three golden
 * images having one MD5 between them. That verification could not survive leaving one machine:
 * **the Cyrillic in all three comes from the host's own font manager**, which is the finding, so
 * the pictures are pictures of whichever font the host happened to have. They differed on the CI
 * runner by 5.36 % — correctly, and there is nothing to fix, because the variable is the operating
 * system.
 *
 * The claim itself is portable, because it is a comparison *within* one run: three families render
 * the same, a fourth renders differently. That holds on any machine and says exactly what the
 * images were there to say.
 *
 * What is lost is a picture of what the substituted font looks like. That belongs in the research
 * document as prose, and is there.
 */
@OptIn(ExperimentalTestApi::class)
class FontFallbackTest {
    @Test
    fun a_family_list_does_not_rescue_a_missing_script() {
        val selawik = render { FontFamily(Font(Res.font.selawik_semilight, WEIGHT)) }
        val selawikThenInter = render { FontFamily(selawikFonts() + interLight()) }
        val selawikThenFira = render { FontFamily(selawikFonts() + firaLight()) }

        // Selawik has no Cyrillic at all. Naming a second family after it changes nothing: Compose
        // selects among weight and style variants of *one* family, and the missing glyphs are
        // filled in by the host either way.
        assertEquals(
            selawik,
            selawikThenInter,
            "adding Inter after Selawik changed the render, so the family list now does fall back " +
                "— research §1.7 is out of date and the font stack needs revisiting",
        )
        assertEquals(selawik, selawikThenFira, "adding Fira after Selawik changed the render")
    }

    /**
     * The control for the test above.
     *
     * Three identical renders would also be what a broken harness produces — one that draws nothing,
     * or ignores the family argument entirely. So a family that *does* carry Cyrillic has to come
     * out different, or the equality above means nothing.
     */
    @Test
    fun a_family_that_has_the_glyphs_renders_differently() {
        val selawik = render { FontFamily(Font(Res.font.selawik_semilight, WEIGHT)) }
        val inter = render { FontFamily(interLight()) }
        val fira = render { FontFamily(firaLight()) }

        assertNotEquals(selawik, inter, "Inter rendered the same as the host substitution")
        assertNotEquals(selawik, fira, "Fira rendered the same as the host substitution")
        assertNotEquals(inter, fira, "Inter and Fira rendered identically, which they are not")
    }

    @Composable
    private fun selawikFonts() = listOf(Font(Res.font.selawik_semilight, WEIGHT))

    private fun interLight() = listOf(Font(resource = "fonts/Inter-Light.ttf", weight = WEIGHT))

    private fun firaLight() = listOf(Font(resource = "fonts/FiraSans-Light.ttf", weight = WEIGHT))

    /** A digest of the rendered pixels: what is compared is sameness, never a stored value. */
    private fun render(family: @Composable () -> FontFamily): Int {
        var digest = 0
        runComposeUiTest {
            setContent {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color.Black).testTag(TAG)) {
                    BasicText(
                        TEXT,
                        style = TextStyle(Color.White, SIZE.sp, WEIGHT, fontFamily = family()).portable(),
                    )
                }
            }
            // compose-resources loads a font asynchronously; a frame taken before it lands is the
            // host substitution wearing the right label, which is precisely the confusion this file
            // is about. Capture until two frames agree.
            var previous = 0
            repeat(SETTLE_ATTEMPTS) {
                waitForIdle()
                val image = onNodeWithTag(TAG).captureToImage()
                val pixels = IntArray(image.width * image.height).also(image::readPixels)
                digest = pixels.fold(0) { acc, pixel -> acc * 31 + pixel }
                if (digest == previous) return@repeat
                previous = digest
            }
        }
        return digest
    }

    private companion object {
        const val TAG = "fallback"
        const val TEXT = "настройки"
        const val WIDTH = 480
        const val HEIGHT = 120
        const val SIZE = 54
        const val SETTLE_ATTEMPTS = 8
        val WEIGHT = FontWeight.W300
    }
}
