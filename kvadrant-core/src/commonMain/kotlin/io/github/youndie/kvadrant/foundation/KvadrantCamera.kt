package io.github.youndie.kvadrant.foundation

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How far the eye is from the drawing plane, for everything in this library that turns in 3D.
 *
 * `GraphicsLayerScope.cameraDistance` places the camera at a fixed number of **pixels** — measured
 * at `value * 72` px on both skiko and hwui — while every piece of geometry handed to it is in dp.
 * Left alone, that makes the perspective a property of the screen: on a 2.625x device a 100 dp tile
 * subtends 0.456 of a 576 px camera where the same tile subtends 0.174 on a desktop, so it turns
 * visibly harder there. Holding the distance in dp and converting per density removes that, and the
 * default reproduces Compose's own camera exactly at density 1 — so nothing about the desktop moves.
 *
 * It lives in one place because it was in six: the tilt, both turnstiles, the tile flip, the swivel
 * transition and the looping selector each wrote `cameraDistance = 8f`, and fixing the tilt alone
 * left five surfaces turning by a different rule from the one they share a screen with.
 */
public object KvadrantCamera {
    /** Compose's own default camera restated in dp: `8 * 72 = 576` px at density 1. */
    public val Distance: Dp = 576.dp

    /**
     * Pixels of depth per unit of `GraphicsLayerScope.cameraDistance`.
     *
     * skiko says so in its source — `val depth = cameraDistance * 72f`, under the comment "The
     * camera location is passed in inches, set in pt". Android's path says nothing usable: it hands
     * the value to `RenderNode.setCameraDistance` untouched, whose documentation calls the unit
     * pixels, which would put the default camera 8 px away and is plainly not what a device draws.
     * So the Android figure is **measured, not read** — the same tile pressed on the centre of its
     * bottom edge draws a trapezoid of 507/449 px on a Pixel 6a and 521/461 on the desktop, ratios
     * of 1.1292 and 1.1301. `CameraProbeTest` keeps the desktop half of that honest.
     */
    internal const val PIXELS_PER_UNIT: Float = 72f
}

/** [distance] expressed in the unit `GraphicsLayerScope.cameraDistance` wants, at this density. */
public fun Density.kvadrantCameraUnits(distance: Dp = KvadrantCamera.Distance): Float =
    distance.toPx() / KvadrantCamera.PIXELS_PER_UNIT
