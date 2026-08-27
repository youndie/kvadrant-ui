package io.github.youndie.kvadrant.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Metro's timing, from the WinJS animation library and the Windows Phone Toolkit.
 *
 * Only the curves something here uses. Two of them are exponential rather than cubic-bezier and
 * have no bezier equivalent, so they are the formula.
 */
public object KvadrantEasing {
    /** The curve behind roughly four fifths of the WinJS library. */
    public val Primary: Easing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f)

    /** `exponentialOut(6)`: the Windows Phone canon for things settling into place. */
    public val ExponentialOut6: Easing = Easing { t -> (1f - exp(-6f * t)) / (1f - exp(-6f)) }

    /** `exponentialOut(15)`: the ToggleSwitch's snap, straight out of the Toolkit's template. */
    public val ExponentialOut15: Easing = Easing { t -> (1f - exp(-15f * t)) / (1f - exp(-15f)) }

    /** `exponentialIn(6)`: a page leaving on the turnstile. */
    public val ExponentialIn6: Easing = Easing { t -> exp(6f * (t - 1f)) }

    /** `exponentialIn(15)`: the snap of a ToggleSwitch, and Swivel's forward-out. */
    public val ExponentialIn15: Easing = Easing { t -> exp(15f * (t - 1f)) }

    /** `exponential(1)`: the rotate transition's transform, gentler than anything else here. */
    public val Exponential1: Easing = Easing { t -> (1f - exp(-1f * t)) / (1f - exp(-1f)) }

    /** `sineOut`: the rotate's opacity, which fades evenly while the transform accelerates. */
    public val SineOut: Easing = Easing { t -> sin(t * PI.toFloat() / 2f) }

    /** `exponentialInOut(4)`: a ListPicker opening, a ContextMenu pushing its page back. */
    public val ExponentialInOut4: Easing =
        Easing { t ->
            if (t < 0.5f) {
                (1f - exp(-8f * t)) / (1f - exp(-4f)) / 2f
            } else {
                1f - (1f - exp(-8f * (1f - t))) / (1f - exp(-4f)) / 2f
            }
        }
}
