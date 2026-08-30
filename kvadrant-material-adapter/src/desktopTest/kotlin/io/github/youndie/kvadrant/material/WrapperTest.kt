package io.github.youndie.kvadrant.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The wrapped button is square, which the unwrapped one cannot be.
 *
 * Measured the way the pill was found: a rounded button is narrower at its first drawn row than at
 * its widest, and a square one is the same. The unwrapped case is the positive control and it is a
 * control that can fail — it did, at 75 against 97, which is what started this.
 */
@OptIn(ExperimentalTestApi::class)
class WrapperTest {
    private fun rowWidths(content: @Composable () -> Unit): Pair<Int, Int> {
        var result = 0 to 0
        runComposeUiTest {
            setContent {
                KvadrantTheme(colors = KvadrantColors.dark()) {
                    KvadrantMaterialAdapter {
                        Box(Modifier.size(300.dp).testTag("frame")) { content() }
                    }
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)

            fun widthAt(y: Int) = (0 until image.width).count { x -> (pixels[y * image.width + x] and 0xFFFFFF) != 0 }
            val rows = (0 until image.height).filter { widthAt(it) > 0 }
            result = widthAt(rows.first() + 1) to rows.maxOf { widthAt(it) }
        }
        return result
    }

    @Test
    fun the_wrapped_button_is_square_where_the_plain_one_is_a_pill() {
        val (plainTop, plainWidest) =
            rowWidths { androidx.compose.material3.Button(onClick = {}) { Text("готово") } }
        val (wrappedTop, wrappedWidest) =
            rowWidths { KvadrantMaterialButton(onClick = {}) { Text("готово") } }

        assertEquals(
            wrappedWidest,
            wrappedTop,
            "the wrapped button is $wrappedTop px wide at its top row and $wrappedWidest at its " +
                "widest — the shape did not take",
        )
        assert(plainTop < plainWidest) {
            "the plain button measured $plainTop/$plainWidest — it is no longer a pill, so this " +
                "test is comparing against nothing and the wrapper may be unnecessary"
        }
    }
}
