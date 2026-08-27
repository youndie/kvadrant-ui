package io.github.youndie.kvadrant.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG's contrast ratio between two opaque colours. */
public fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

/** WCAG AA for body text. */
public const val WCAG_AA: Float = 4.5f

/**
 * The nearest colour to [accent] that reaches [target] against the text colour Metro would put on
 * it, found by walking towards black or white.
 *
 * Nine of the twenty Windows Phone accents fall below AA at their authentic text colour, `cyan` —
 * the phone's own default — among them at 2.90:1. This is what the opt-in palette applies, and it
 * is computed rather than hand-picked so that a caller's own accent gets the same treatment.
 *
 * The hue is preserved; only the distance to black or white changes.
 */
public fun accessibleAccent(
    accent: Color,
    target: Float = WCAG_AA,
): Color {
    val text = contrastOn(accent)
    if (contrastRatio(accent, text) >= target) return accent

    // Text is white when the accent is dark, so the accent has to get darker still, and vice versa.
    val towards = if (text == Color.White) Color.Black else Color.White
    var low = 0f
    var high = 1f
    repeat(STEPS) {
        val mid = (low + high) / 2f
        if (contrastRatio(accent.mix(towards, mid), text) >= target) high = mid else low = mid
    }
    return accent.mix(towards, high)
}

/**
 * The theme with every colour that carries text raised to WCAG AA.
 *
 * **Opt-in, and that is the whole design.** Metro's palette is below AA in nine of twenty accents
 * and its subtle text is at roughly 2.8:1 in the light theme; a library that quietly fixed this
 * would not be reproducing Metro, and one that ignored it would be unusable wherever an
 * accessibility bar exists. So the authentic palette is the default and this is one call away.
 *
 * Touch targets are **not** part of the opt-in: they are extended to 48 dp always, because a
 * larger invisible hit area costs the visual nothing. See D7.
 */
public fun KvadrantColors.accessible(target: Float = WCAG_AA): KvadrantColors =
    copy(
        accent = accessibleAccent(accent, target),
        subtle = raiseAgainst(subtle, background, target),
        disabled = raiseAgainst(disabled, background, target = 3f),
    )

/**
 * Opacity is what makes Metro's subtle and disabled tokens what they are, so this raises the
 * opacity rather than shifting the hue — `#99FFFFFF` stays white, it just stops being 60 %.
 */
private fun raiseAgainst(
    color: Color,
    background: Color,
    target: Float,
): Color {
    var low = color.alpha
    var high = 1f
    repeat(STEPS) {
        val mid = (low + high) / 2f
        val flattened = background.mix(color.copy(alpha = 1f), mid)
        if (contrastRatio(flattened, background) >= target) high = mid else low = mid
    }
    return color.copy(alpha = high)
}

private fun Color.mix(
    other: Color,
    amount: Float,
) = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
    alpha = alpha,
)

private const val STEPS = 20
