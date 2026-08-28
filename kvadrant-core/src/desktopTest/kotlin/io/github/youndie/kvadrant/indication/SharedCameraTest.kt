package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Two identical surfaces, pressed identically, in different places on the screen, look the same.
 *
 * **The opposite of what this file asserted yesterday, and the reversal is the finding.** B-26
 * argued for one camera over the whole display and got it by moving `transformOrigin` to the root's
 * centre. `graphicsLayer` uses that property for the projection centre *and* the rotation pivot, so
 * an element away from the middle stopped leaning and started swinging: a 60 dp bar pressed at the
 * same point in its own coordinates came out 65 px tall at the centre of the screen and **84 px** at
 * the top. It reached a device as a push notification being pressed far too hard.
 *
 * The evidence that argued for it does not survive either. The comparison fixture rotated nine tiles
 * at once, and the shared version bent them into one sheet — which looked like Metro and is not a
 * thing a press does. A press rotates one tile. The sheet was the fixture's construction.
 *
 * So the camera is per element again, and this test holds it there rather than holding the change
 * that was reverted. A genuine shared camera needs the projection centre and the rotation pivot to
 * be separate points, which one layer cannot express; that is back in B-26 as the shape of the work.
 */
@OptIn(ExperimentalTestApi::class)
class SharedCameraTest {
    private fun pressedRow(): Pair<IntArray, Int> {
        var pixels = IntArray(0)
        var width = 0
        runComposeUiTest {
            lateinit var left: MutableInteractionSource
            lateinit var right: MutableInteractionSource
            setContent {
                KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
                    left = remember { MutableInteractionSource() }
                    right = remember { MutableInteractionSource() }
                    Box(Modifier.size(420.dp, 200.dp).background(Color.Black).testTag("frame")) {
                        Row(Modifier.align(Alignment.Center)) {
                            KvadrantTile(TileSize.Medium, color = Color.White, interactionSource = left) {}
                            KvadrantTile(TileSize.Medium, color = Color.White, interactionSource = right) {}
                        }
                    }
                }
            }
            waitForIdle()
            // The same press, in each tile's own coordinates.
            left.tryEmit(PressInteraction.Press(Offset(20f, 20f)))
            right.tryEmit(PressInteraction.Press(Offset(20f, 20f)))
            waitForIdle()
            val image = onNodeWithTag("frame").captureToImage()
            width = image.width
            pixels = IntArray(image.width * image.height).also(image::readPixels)
        }
        return pixels to width
    }

    @Test
    fun two_tiles_pressed_alike_in_different_places_render_alike() {
        val (px, width) = pressedRow()
        val height = px.size / width

        fun white(
            x: Int,
            y: Int,
        ) = (px[y * width + x] shr 16 and 0xFF) > 0xC0

        // Each half holds one tile; each is described by its own column profile, measured from its
        // own left edge so the two are comparable without depending on where they sit.
        fun profileOfTileIn(
            from: Int,
            to: Int,
        ): List<Int> {
            val columns = (from until to).map { x -> (0 until height).count { white(x, it) } }
            val first = columns.indexOfFirst { it > 0 }
            val last = columns.indexOfLast { it > 0 }
            return if (first < 0) emptyList() else columns.subList(first, last + 1)
        }

        val leftTile = profileOfTileIn(0, width / 2)
        val rightTile = profileOfTileIn(width / 2, width)
        assertTrue(leftTile.isNotEmpty() && rightTile.isNotEmpty(), "one of the tiles is not drawn")

        // The control: a shape, not a blank. Two empty profiles would agree perfectly.
        assertTrue(leftTile.max() > 50, "the tiles are not drawn as shapes: ${leftTile.max()}")

        val shorter = minOf(leftTile.size, rightTile.size)
        val apart =
            (0 until shorter).sumOf { kotlin.math.abs(leftTile[it] - rightTile[it]) } +
                kotlin.math.abs(leftTile.size - rightTile.size) * 100
        assertTrue(
            apart < leftTile.sum() / 50,
            "two surfaces pressed alike render differently depending on where they are: $apart",
        )
    }
}
