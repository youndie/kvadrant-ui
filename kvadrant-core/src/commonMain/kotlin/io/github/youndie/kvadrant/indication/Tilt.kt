package io.github.youndie.kvadrant.indication

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

/**
 * The tilt geometry of Windows Phone, transcribed from `TiltEffect.cs` (the RTM algorithm, "direct
 * from the shell team"): a press rotates the plane towards the finger and pushes it away from the
 * viewer, by an amount that trades off against the rotation.
 *
 * A press in the exact centre is pure depression and no rotation; a press in a corner is the full
 * 17.188° and no depression at all.
 */
public object Tilt {
    /** Maximum rotation, in radians. `MaxAngle` in the original. */
    public const val MAX_ANGLE_RADIANS: Float = 0.3f

    /**
     * Maximum push-away. `MaxDepression = 25` in Metro pixels at the canonical 0.75.
     *
     * The default only. What a themed surface actually uses is `KvadrantMetrics.tiltDepression`,
     * which the metric scale reaches — a tilt that ignored the scale would read as shallower on
     * everything that grew.
     */
    public val maxDepression: Dp = 18.75.dp

    /** The delay before the plane springs back, and how long the return takes. */
    public const val RETURN_DELAY_MILLIS: Int = 200
    public const val RETURN_DURATION_MILLIS: Int = 100
}

/**
 * Where the plane ends up for a press at [position] on a surface of [size].
 *
 * [rotationX] and [rotationY] are degrees about the x- and y-*axis*, which is the swap the original
 * comments on: a rotation to the left in the x-*direction* is a rotation about the y-axis.
 */
public data class TiltTransform(
    val rotationX: Float,
    val rotationY: Float,
    val depression: Dp,
) {
    public companion object {
        public val None: TiltTransform = TiltTransform(0f, 0f, 0.dp)
    }
}

public fun tiltFor(
    position: Offset,
    size: Size,
    maxDepression: Dp = Tilt.maxDepression,
): TiltTransform {
    if (size.width <= 0f || size.height <= 0f) return TiltTransform.None

    val nx = (position.x / size.width).coerceIn(0f, 1f)
    val ny = (position.y / size.height).coerceIn(0f, 1f)

    val xMagnitude = abs(nx - 0.5f)
    val yMagnitude = abs(ny - 0.5f)
    val xDirection = -sign(nx - 0.5f)
    val yDirection = sign(ny - 0.5f)

    val angleMagnitude = xMagnitude + yMagnitude
    val xContribution = if (angleMagnitude > 0f) xMagnitude / angleMagnitude else 0f

    val angle = (angleMagnitude * Tilt.MAX_ANGLE_RADIANS * 180f / PI).toFloat()

    return TiltTransform(
        rotationX = angle * (1f - xContribution) * yDirection,
        rotationY = angle * xContribution * xDirection,
        depression = maxDepression * (1f - angleMagnitude),
    )
}

/**
 * How much of the tilt is still applied, [millisSinceRelease] after the finger left.
 *
 * The press is instant and the return is not: the plane holds where it was for
 * [Tilt.RETURN_DELAY_MILLIS], then unwinds linearly over [Tilt.RETURN_DURATION_MILLIS]. The pause is
 * the part that matters — it is what makes a tap feel acknowledged rather than merely registered,
 * and it is why a naive spring-back reads as twitchy.
 *
 * A pure function because a still frame cannot show a return, and something has to be checkable.
 */
public fun tiltReturn(millisSinceRelease: Long): Float =
    when {
        millisSinceRelease < Tilt.RETURN_DELAY_MILLIS -> {
            1f
        }

        else -> {
            val elapsed = millisSinceRelease - Tilt.RETURN_DELAY_MILLIS
            (1f - elapsed.toFloat() / Tilt.RETURN_DURATION_MILLIS).coerceIn(0f, 1f)
        }
    }
