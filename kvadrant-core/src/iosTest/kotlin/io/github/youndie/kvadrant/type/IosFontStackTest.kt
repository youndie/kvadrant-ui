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
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Both scripts reach the third renderer, out of the files this library bundles.
 *
 * **This is the last of [B-07](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-07-font-stack.md)'s
 * criteria, and it could not be worked harder at** — it said so itself: iOS was unverified *because
 * the target did not exist*. So the target exists, under D14's own rule that one arrives when
 * something runs on it, and this is that something.
 *
 * The assertion is `AndroidFontStackTest`'s and it is deliberately a second copy rather than a
 * shared one. The two live in source sets that share nothing: `androidDeviceTest` is an
 * instrumentation compilation driven by a connected phone, this is a Kotlin/Native binary Gradle
 * boots a simulator for. Threading one function between them means a source set that exists only to
 * hold it, and the thing being asserted is sixty lines that read the same on both — the *claim* is
 * shared, and it is stated twice on purpose, once per renderer.
 *
 * **What could differ here, and why the desktop cannot answer it.** compose-resources replaced a
 * classpath read so that one declaration would serve every target; a classpath is a JVM idea, and on
 * native the fonts arrive through a different mechanism entirely. A resource that fails to load does
 * not throw — the text still appears, in whatever the platform substitutes, looking like a slightly
 * different design decision rather than a missing asset.
 *
 *     ./gradlew :kvadrant-core:iosSimulatorArm64Test
 */
@OptIn(ExperimentalTestApi::class)
class IosFontStackTest {
    @Test
    fun the_bundled_families_draw_and_are_not_the_platform_substitution() {
        val latin = render("settings") { kvadrantLatin() }
        val cyrillic = render("настройки") { kvadrantCyrillic() }
        // A family with no Cyrillic in it at all, so this is whatever iOS puts in the gap.
        val substituted = render("настройки") { kvadrantLatin() }

        assertTrue(latin > 0, "Latin drew nothing, so the bundled family did not load")
        assertTrue(cyrillic > 0, "Cyrillic drew nothing")
        // The control: three equal numbers would also be what a harness that draws nothing produces.
        assertTrue(substituted > 0, "the control drew nothing, so this cannot tell the cases apart")

        assertNotEquals(
            substituted,
            cyrillic,
            "kvadrantCyrillic() drew exactly what a family with no Cyrillic draws ($cyrillic px), " +
                "so the bundled companion did not load on this simulator and the platform is " +
                "drawing this text",
        )
    }

    /** Lit pixels. A count rather than a picture, because viddik cannot photograph this renderer. */
    private fun render(
        text: String,
        family: @Composable () -> FontFamily,
    ): Int {
        var ink = 0
        runComposeUiTest {
            setContent {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color.Black).testTag(TAG)) {
                    BasicText(
                        text,
                        style = TextStyle(Color.White, SIZE.sp, FontWeight.W300, fontFamily = family()),
                    )
                }
            }
            // compose-resources loads asynchronously, and a frame taken too early is the platform
            // substitution wearing the right label — the confusion this test exists to catch.
            var previous = -1
            repeat(SETTLE_ATTEMPTS) {
                waitForIdle()
                val image = onNodeWithTag(TAG).captureToImage()
                val pixels = IntArray(image.width * image.height).also(image::readPixels)
                ink = pixels.count { it and 0xFFFFFF != 0 }
                if (ink == previous) return@repeat
                previous = ink
            }
        }
        return ink
    }

    private companion object {
        const val TAG = "fonts"
        const val WIDTH = 320
        const val HEIGHT = 100
        const val SIZE = 36
        const val SETTLE_ATTEMPTS = 8
    }
}
