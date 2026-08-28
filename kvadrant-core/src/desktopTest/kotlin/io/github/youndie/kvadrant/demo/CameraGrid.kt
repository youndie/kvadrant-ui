package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantCamera
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * B-26's question, as two pictures: the same nine tiles, leaning by the same angles, under a camera
 * per tile and under one camera over the grid.
 *
 * **The angles here are fixed rather than taken from the tilt**, and the fixture is smaller for it.
 * What is being compared is where the camera's axis sits, not how far a press leans, and borrowing
 * `tiltFor` would have put a second copy of the tilt's arithmetic in a fixture whose subject is not
 * the arithmetic.
 *
 * `graphicsLayer` puts the axis at each element's own centre. A camera shared across the grid is the
 * same thing with `transformOrigin` moved to the grid's centre, expressed in each tile's own
 * fractional coordinates — which is what a `TransformOrigin` outside 0..1 means, and is why the
 * comparison needs no new machinery.
 */
@Composable
private fun Grid(shared: Boolean) {
    KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    repeat(3) { column ->
                        Box(
                            Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    cameraDistance = kvadrantCameraUnits(KvadrantCamera.Distance)
                                    // A tile is 100 dp with a 9 dp gap, so the grid's centre is
                                    // 1.09 of a tile away per step out from the middle.
                                    transformOrigin =
                                        if (!shared) {
                                            TransformOrigin(0.5f, 0.5f)
                                        } else {
                                            TransformOrigin(
                                                0.5f + (1 - column) * STEP,
                                                0.5f + (1 - row) * STEP,
                                            )
                                        }
                                    rotationX = -ANGLE
                                    rotationY = -ANGLE
                                }.background(KvadrantAccents.Cyan),
                        )
                    }
                }
            }
        }
    }
}

/** Half of the tilt's 17.19°, so the perspective is plain without the quads overlapping. */
private const val ANGLE = 8.6f

/** One tile plus one gap, as a fraction of a tile: 109 dp over 100 dp. */
private const val STEP = 1.09f

@ViddikScreenshot(name = "camera per layer", group = "tilt", width = 340, height = 340)
@Composable
internal fun CameraPerLayer(): Unit = Grid(shared = false)

@ViddikScreenshot(name = "camera shared", group = "tilt", width = 340, height = 340)
@Composable
internal fun CameraShared(): Unit = Grid(shared = true)
