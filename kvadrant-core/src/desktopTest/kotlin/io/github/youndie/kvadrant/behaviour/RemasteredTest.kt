package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The flag is off unless asked for, and its first citizen behaves differently on each setting.
 *
 * **A golden pair is the wrong instrument here and the item's criterion asking for one is amended
 * rather than met.** The difference between a press that snaps and a press that sinks over a tenth
 * of a second exists only *during* that tenth of a second: a still of either setting, taken after
 * it, is the same image. Building a fixture that captures mid-animation is exactly what B-31 warns
 * against — a golden whose pixels are decided by when the camera fired. So the clock is held and
 * the geometry is read instead.
 */
@OptIn(ExperimentalTestApi::class)
class RemasteredTest {
    /** How many pixels of tile are lit one frame after the press; the depression shrinks it. */
    private fun litJustAfterPress(remastered: Boolean): Int {
        var lit = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var press: MutableInteractionSource
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                    remastered = remastered,
                ) {
                    press = remember { MutableInteractionSource() }
                    Box(Modifier.size(300.dp).background(Color.Black).testTag("frame")) {
                        KvadrantTile(TileSize.Medium, color = Color.White, interactionSource = press) {}
                    }
                }
            }
            mainClock.advanceTimeBy(16)
            press.tryEmit(PressInteraction.Press(Offset.Zero))
            // One frame past the press. Canon has already arrived at the bottom of it; a remastered
            // press is a tenth of a second long and has barely started.
            mainClock.advanceTimeBy(16)
            val image = onNodeWithTag("frame").captureToImage()
            val px = IntArray(image.width * image.height).also(image::readPixels)
            lit = px.count { (it shr 16 and 0xFF) > 0xC0 && (it shr 8 and 0xFF) > 0xC0 }
        }
        return lit
    }

    /**
     * Canon snaps to the bottom of the press; remastered is still on its way down.
     *
     * The depression is a shrink, so "further down" is "fewer lit pixels" — one frame in, the
     * canonical tile is already at its smallest and the remastered one is still nearly full size.
     */
    @Test
    fun a_remastered_press_sinks_where_a_canonical_one_snaps() {
        val canon = litJustAfterPress(remastered = false)
        val remastered = litJustAfterPress(remastered = true)
        assertTrue(canon > 0 && remastered > 0, "no tile was drawn: $canon and $remastered")
        assertTrue(
            remastered > canon,
            "one frame after the press both settings are the same size: $canon canon, " +
                "$remastered remastered - the flag is not reaching the indication",
        )
    }

    @Test
    fun the_flag_is_off_unless_it_is_asked_for() {
        var seen = true
        runComposeUiTest {
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    seen = KvadrantTheme.remastered
                }
            }
        }
        assertEquals(false, seen, "a theme built with no argument came up remastered")
    }
}
