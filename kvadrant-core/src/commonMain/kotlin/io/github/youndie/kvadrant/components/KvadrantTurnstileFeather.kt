package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.theme.KvadrantEasing

/**
 * A list arriving one row at a time, all of them swinging on the **same** axis.
 *
 * This is the effect people remember and the one reimplementations get wrong, always the same way:
 * they rotate each row about its own centre, which looks like a stack of cards flipping. The
 * original rotates every row about one axis running down the screen — the toolkit puts the centre of
 * rotation at **-0.2** of each element's width, off to the left of all of them — so the list opens
 * like a venetian blind rather than a deck.
 *
 * Rows enter 40 ms apart and leave 50 ms apart. Both numbers are the toolkit's.
 */
@Composable
public fun KvadrantTurnstileFeather(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val angle by animateFloatAsState(
        targetValue = if (visible) 0f else ENTER_ANGLE,
        animationSpec =
            tween(
                durationMillis = DURATION_MILLIS,
                delayMillis = index * (if (visible) STAGGER_IN_MILLIS else STAGGER_OUT_MILLIS),
                easing = if (visible) KvadrantEasing.ExponentialOut6 else KvadrantEasing.ExponentialIn6,
            ),
        label = "feather",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = DURATION_MILLIS,
                delayMillis = index * (if (visible) STAGGER_IN_MILLIS else STAGGER_OUT_MILLIS),
            ),
        label = "alpha",
    )

    Column(
        modifier.graphicsLayer {
            rotationY = angle
            this.alpha = alpha
            // The shared axis. Not the row's own edge, and emphatically not its centre.
            transformOrigin = TransformOrigin(SHARED_AXIS_X, 0.5f)
            cameraDistance = kvadrantCameraUnits()
        },
    ) { content() }
}

private const val ENTER_ANGLE = -80f
private const val DURATION_MILLIS = 350
private const val STAGGER_IN_MILLIS = 40
private const val STAGGER_OUT_MILLIS = 50

/** `centerOfRotationX = -0.2` in the Windows Phone Toolkit. */
private const val SHARED_AXIS_X = -0.2f
