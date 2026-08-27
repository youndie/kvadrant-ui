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
import io.github.youndie.kvadrant.theme.KvadrantTypography
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Two identical tiles, pressed identically, in different places on the screen, do not look the same.
 *
 * That sentence is the whole of B-26. `graphicsLayer` gives every element its own camera at its own
 * centre, so a grid of tiles renders as identically deformed copies of one shape; Metro had one
 * camera over the screen, under which a grid bends as a single sheet. The measurement that settled
 * it is a pair of stills — `tilt_camera_per_layer` and `tilt_camera_shared` — and this holds the
 * behaviour they argued for.
 *
 * **No existing test or golden caught the change**, and the reason is worth keeping: every fixture
 * that presses something centres it in the frame, where a shared axis and an element's own axis are
 * the same point. A property that only shows off-centre needs a fixture that is off-centre.
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
                KvadrantTheme(KvadrantColors.dark(), KvadrantTypography.default(kvadrantLatin())) {
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
    fun two_tiles_pressed_alike_in_different_places_render_differently() {
        val (px, width) = pressedRow()
        val height = px.size / width

        fun white(
            x: Int,
            y: Int,
        ) = (px[y * width + x] shr 16 and 0xFF) > 0xC0

        // Each half holds one tile; each tile is described by its own column profile, measured
        // from its own left edge so that the two are comparable without depending on where they sit.
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

        // **Not a mirror.** The first version of this asserted the two profiles were reflections of
        // each other, which is the wrong physics: both tiles are pressed in *their own* top-left
        // corner, so both lean the same way. Only the camera's axis differs between them.
        //
        // Under a camera per element the two would be the same shape in two places, and their
        // profiles identical. Under one camera over the screen they are not.
        val shorter = minOf(leftTile.size, rightTile.size)
        val apart =
            (0 until shorter).sumOf { kotlin.math.abs(leftTile[it] - rightTile[it]) } +
                kotlin.math.abs(leftTile.size - rightTile.size) * 100
        assertTrue(
            apart > leftTile.sum() / 50,
            "two tiles pressed alike in different places render identically, so the camera is " +
                "still per element: $apart",
        )
    }
}
