package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import io.github.youndie.kvadrant.theme.KvadrantEasing

/**
 * The page transition Windows Phone is remembered for after the tilt: content swinging in around
 * its left edge like a turnstile.
 *
 * In at **-80°** over 350 ms on an exponential-out(6), out at **+50°** over 250 ms on an
 * exponential-in(6), both about the left edge. The asymmetry is the original's, and it is what makes
 * leaving feel quicker than arriving. Going backwards swaps the pair: in from +50°, out to -80°.
 */
@Composable
public fun KvadrantTurnstile(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = updateTransition(visible, label = "turnstile")
    val angle by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(IN_MILLIS, easing = KvadrantEasing.ExponentialOut6)
            } else {
                tween(OUT_MILLIS, easing = KvadrantEasing.ExponentialIn6)
            }
        },
        label = "angle",
    ) { shown ->
        if (shown) {
            0f
        } else if (transition.targetState) {
            IN_ANGLE
        } else {
            OUT_ANGLE
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) IN_MILLIS else OUT_MILLIS) },
        label = "alpha",
    ) { shown -> if (shown) 1f else 0f }

    Column(
        modifier.graphicsLayer {
            rotationY = angle
            this.alpha = alpha
            // The axis is the left edge of the content, which is what makes it a turnstile rather
            // than a card flipping about its middle.
            transformOrigin = TransformOrigin(0f, 0.5f)
            cameraDistance = DEFAULT_CAMERA
        },
    ) { content() }
}

private const val IN_ANGLE = -80f
private const val OUT_ANGLE = 50f
private const val IN_MILLIS = 350
private const val OUT_MILLIS = 250

/**
 * Compose's own default. Note that it means different things per backend — skiko reads it as inches
 * at 72 px each, Android hands it to `RenderNode` where the display's dpi applies. See B-25; the
 * same caveat that applies to the tilt applies here.
 */
private const val DEFAULT_CAMERA = 8f
