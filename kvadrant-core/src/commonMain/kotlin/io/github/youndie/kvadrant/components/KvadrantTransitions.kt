package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.theme.KvadrantEasing

/** Which way a slide travels. */
public enum class SlideDirection { Left, Right, Up, Down }

/**
 * A page sliding 200 px in one of four directions.
 *
 * **The four durations are not one number.** Left and right take 500 ms in and 300 out; up and down
 * take 350 and 250. Horizontal movement is given more time than vertical, which is the toolkit's
 * choice and reads as the difference between changing place and changing level.
 */
@Composable
public fun KvadrantSlide(
    visible: Boolean,
    direction: SlideDirection,
    modifier: Modifier = Modifier,
    distance: Dp = SLIDE_DISTANCE,
    content: @Composable () -> Unit,
) {
    val horizontal = direction == SlideDirection.Left || direction == SlideDirection.Right
    val sign = if (direction == SlideDirection.Left || direction == SlideDirection.Up) -1f else 1f
    val offset = remember { Animatable(sign) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visible, direction) {
        val millis =
            when {
                horizontal && visible -> LEFT_RIGHT_IN
                horizontal -> LEFT_RIGHT_OUT
                visible -> UP_DOWN_IN
                else -> UP_DOWN_OUT
            }
        val spec = tween<Float>(millis, easing = KvadrantEasing.ExponentialOut6)
        alpha.animateTo(if (visible) 1f else 0f, tween(millis))
        offset.animateTo(if (visible) 0f else sign, spec)
    }

    Box(
        modifier.graphicsLayer {
            val travel = distance.toPx() * offset.value
            if (horizontal) translationX = travel else translationY = travel
            this.alpha = alpha.value
        },
    ) { content() }
}

/**
 * A page tipping away from you around its top edge.
 *
 * Forward: in from **-45°** over 350 ms, out to **+90°** over 250 ms — and that exit is the one
 * place `exponentialIn(15)` appears outside the ToggleSwitch, which is why it snaps. Going back is
 * gentler: out to **+60°** on an exponential-in(6).
 */
@Composable
public fun KvadrantSwivel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    backward: Boolean = false,
    content: @Composable () -> Unit,
) {
    val angle = remember { Animatable(SWIVEL_IN_FROM) }

    LaunchedEffect(visible, backward) {
        if (visible) {
            angle.animateTo(0f, tween(SWIVEL_IN_MILLIS, easing = KvadrantEasing.ExponentialOut6))
        } else if (backward) {
            angle.animateTo(
                SWIVEL_BACKWARD_OUT_TO,
                tween(SWIVEL_OUT_MILLIS, easing = KvadrantEasing.ExponentialIn6),
            )
        } else {
            angle.animateTo(
                SWIVEL_FORWARD_OUT_TO,
                tween(SWIVEL_OUT_MILLIS, easing = KvadrantEasing.ExponentialIn15),
            )
        }
    }

    Box(
        modifier.graphicsLayer {
            rotationX = angle.value
            transformOrigin = TransformOrigin(0.5f, 0f)
            cameraDistance = kvadrantCameraUnits()
        },
    ) { content() }
}

private val SLIDE_DISTANCE = 150.dp // 200 px
private const val LEFT_RIGHT_IN = 500
private const val LEFT_RIGHT_OUT = 300
private const val UP_DOWN_IN = 350
private const val UP_DOWN_OUT = 250

private const val SWIVEL_IN_FROM = -45f
private const val SWIVEL_FORWARD_OUT_TO = 90f
private const val SWIVEL_BACKWARD_OUT_TO = 60f

/**
 * How long a swivel takes, in and out.
 *
 * Public because anything that swivels *itself out* has to stay composed for exactly this long — a
 * surface removed on the frame its flag moves has nothing to animate, and hard-coding 250 at each
 * call site duplicates a number that lives here.
 */
public const val KVADRANT_SWIVEL_IN_MILLIS: Int = 350

/** @see KVADRANT_SWIVEL_IN_MILLIS */
public const val KVADRANT_SWIVEL_OUT_MILLIS: Int = 250

private const val SWIVEL_IN_MILLIS = KVADRANT_SWIVEL_IN_MILLIS
private const val SWIVEL_OUT_MILLIS = KVADRANT_SWIVEL_OUT_MILLIS

/**
 * A page turning in its own plane, a quarter or a half turn either way.
 *
 * All eight variants take **250 ms** — the same number regardless of how far it turns, which is the
 * toolkit's decision and reads as the turn being one gesture rather than a distance travelled.
 * Opacity rides a sine rather than the exponential the transform uses, so the page fades evenly
 * while it accelerates.
 */
@Composable
public fun KvadrantRotate(
    visible: Boolean,
    degrees: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val angle = remember { Animatable(degrees) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visible, degrees) {
        angle.animateTo(
            if (visible) 0f else degrees,
            tween(ROTATE_MILLIS, easing = KvadrantEasing.Exponential1),
        )
        alpha.animateTo(if (visible) 1f else 0f, tween(ROTATE_MILLIS, easing = KvadrantEasing.SineOut))
    }

    Box(
        modifier.graphicsLayer {
            rotationZ = angle.value
            this.alpha = alpha.value
        },
    ) { content() }
}

/**
 * The roll: ninety degrees in **two** phases rather than one.
 *
 * Nought to forty-five over 300 ms on an exponential-out(6), then forty-five to ninety over another
 * 300 **linearly** — six hundred in total. The change of curve halfway is the whole character of it:
 * the page eases into the turn and then completes it at a constant rate, which reads as something
 * being carried rather than something falling. Opacity is not animated at all.
 */
@Composable
public fun KvadrantRoll(
    visible: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Where the turn is anchored. **This number is not Microsoft's** — the specification gives the
     * roll's two angles and its two durations and says nothing about the axis, so a default had to
     * be chosen and it is a parameter because of that.
     *
     * The bottom-left corner, because the centre is worse in a way that is obvious once seen: a
     * wide element rotated ninety degrees about its middle sweeps a circle the width of the page and
     * ends up standing across everything else. Anchored at a corner it reads as a panel swinging on
     * a hinge, which is what "roll" is meant to describe.
     */
    transformOrigin: TransformOrigin = TransformOrigin(0f, 1f),
    content: @Composable () -> Unit,
) {
    val angle = remember { Animatable(if (visible) ROLL_END else 0f) }

    LaunchedEffect(visible) {
        if (visible) {
            angle.animateTo(ROLL_MID, tween(ROLL_PHASE_MILLIS, easing = KvadrantEasing.ExponentialOut6))
            angle.animateTo(0f, tween(ROLL_PHASE_MILLIS, easing = LinearEasing))
        } else {
            angle.animateTo(ROLL_MID, tween(ROLL_PHASE_MILLIS, easing = LinearEasing))
            angle.animateTo(ROLL_END, tween(ROLL_PHASE_MILLIS, easing = KvadrantEasing.ExponentialOut6))
        }
    }

    Box(
        modifier.graphicsLayer {
            rotationZ = angle.value
            this.transformOrigin = transformOrigin
        },
    ) { content() }
}

private const val ROTATE_MILLIS = 250
private const val ROLL_MID = 45f
private const val ROLL_END = 90f
private const val ROLL_PHASE_MILLIS = 300
