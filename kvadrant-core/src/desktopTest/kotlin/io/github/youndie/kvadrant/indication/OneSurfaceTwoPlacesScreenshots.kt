package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantCamera
import io.github.youndie.kvadrant.foundation.KvadrantHomography
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.theme.KvadrantAccents
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * **One** surface, pressed at the same point in its own coordinates, in two places on the screen —
 * under each camera. This is
 * [B-26](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-26-per-layer-camera-versus-a-global-one.md)'s
 * first criterion, in the shape the item's own reopening asked for.
 *
 * The criterion has been attempted twice and answered neither time. The first fixture rotated **nine
 * tiles at once**, and the shared version bent them into a sheet that looked unmistakably like
 * Metro — a picture answering a question nobody had asked, because a press rotates one tile and not
 * nine. It was convincing enough that the change shipped, and a device reported it within a day: an
 * element away from the middle had stopped leaning and started swinging, because the only lever
 * available then, `transformOrigin`, moves the rotation pivot and the projection centre together.
 *
 * These are the same surface twice in each frame. Under the per-layer camera the two are identical —
 * every element carries its own camera at its own centre, so where it sits on the screen cannot
 * matter. Under one camera over the display the lower-left one is seen from the side and the upper
 * one nearly head-on, which is the behaviour research §1.6 says the original had and this does not.
 *
 * **Neither frame is the shipping behaviour.** The tilt still uses `graphicsLayer`; this draws the
 * alternative so it can be looked at rather than argued about, which is the only thing the item has
 * ever been short of.
 */
private const val SURFACE = 150
private const val FRAME_WIDTH = 400
private const val FRAME_HEIGHT = 560

/** The press: the surface's own top-left corner, so the lean has a direction to have. */
private val ROTATION_X = 9f
private val ROTATION_Y = -9f

@Composable
private fun Frame(shared: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Surface(x = 125, y = 40, shared = shared)
        Surface(x = 10, y = 350, shared = shared)
    }
}

@Composable
private fun Surface(
    x: Int,
    y: Int,
    shared: Boolean,
) {
    var eye by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val frameCentreX = with(density) { (FRAME_WIDTH / 2).dp.toPx() }
    val frameCentreY = with(density) { (FRAME_HEIGHT / 2).dp.toPx() }
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(SURFACE.dp)
            .onGloballyPositioned { coordinates ->
                // Where the screen's centre is, relative to this surface's own centre. Under the
                // per-layer camera this number exists and is ignored, which is precisely the
                // difference the two frames are here to show.
                val topLeft = coordinates.positionInRoot()
                eye =
                    Offset(
                        x = frameCentreX - (topLeft.x + coordinates.size.width / 2f),
                        y = frameCentreY - (topLeft.y + coordinates.size.height / 2f),
                    )
            }.then(if (shared) Modifier.sharedCamera(eye) else Modifier.perLayerCamera())
            .background(KvadrantAccents.Cobalt),
    )
}

private fun Modifier.perLayerCamera(): Modifier =
    graphicsLayer {
        cameraDistance = kvadrantCameraUnits()
        rotationX = ROTATION_X
        rotationY = ROTATION_Y
    }

private fun Modifier.sharedCamera(eye: Offset): Modifier =
    drawWithContent {
        val quad =
            KvadrantHomography.quadUnderCamera(
                size = size,
                rotationXDegrees = ROTATION_X,
                rotationYDegrees = ROTATION_Y,
                cameraDistance = KvadrantCamera.Distance.toPx(),
                eye = eye,
            )
        drawContext.canvas.save()
        drawContext.canvas.concat(KvadrantHomography.homographyFromRect(size, quad))
        drawContent()
        drawContext.canvas.restore()
    }

@ViddikScreenshot(name = "one surface per layer", group = "camera", width = FRAME_WIDTH, height = FRAME_HEIGHT)
@Composable
internal fun OneSurfacePerLayer(): Unit = Frame(shared = false)

@ViddikScreenshot(name = "one surface shared", group = "camera", width = FRAME_WIDTH, height = FRAME_HEIGHT)
@Composable
internal fun OneSurfaceShared(): Unit = Frame(shared = true)
