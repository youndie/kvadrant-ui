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
import androidx.compose.ui.test.v2.runComposeUiTest
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
 * Both scripts reach the renderer this library will mostly ship on, out of the files it bundles.
 *
 * **[B-07](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-07-font-stack.md)'s last
 * open criterion, and it cannot be answered on the desktop.** The fonts moved to compose-resources
 * so that one declaration would serve every target, and every check of that so far has run on the
 * JVM — where the loader it replaced, a classpath read, also worked. A resource that fails to load
 * on Android does not throw: the text still appears, in whatever the platform substitutes, looking
 * like a slightly different design decision rather than a missing asset. `FontFallbackTest` guards
 * that on the desktop; this asks the same question where the answer could differ.
 *
 * **It does not run today**, and not for any reason of its own:
 * [B-36](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-36-the-on-device-guard-does-not-execute.md)
 * — the whole device suite fails at its first `captureToImage` with "No compose hierarchies found
 * in the app". It is written now because the check is what B-07 needs and writing it later means
 * rediscovering why; it is *not* evidence of anything until that item closes.
 *
 *     ./gradlew :kvadrant-core:connectedAndroidDeviceTest
 */
@OptIn(ExperimentalTestApi::class)
class AndroidFontStackTest {
    @Test
    fun the_bundled_families_draw_and_are_not_the_platform_substitution() {
        val latin = render("settings") { kvadrantLatin() }
        val cyrillic = render("настройки") { kvadrantCyrillic() }
        // A family with no Cyrillic in it at all, so this is whatever Android puts in the gap.
        val substituted = render("настройки") { kvadrantLatin() }

        assertTrue(latin > 0, "Latin drew nothing, so the bundled family did not load")
        assertTrue(cyrillic > 0, "Cyrillic drew nothing")
        // The control: three equal numbers would also be what a harness that draws nothing produces.
        assertTrue(substituted > 0, "the control drew nothing, so this cannot tell the cases apart")

        assertNotEquals(
            substituted,
            cyrillic,
            "kvadrantCyrillic() drew exactly what a family with no Cyrillic draws ($cyrillic px), " +
                "so the bundled companion did not load on this device and the platform is drawing " +
                "this text",
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
