package io.github.youndie.kvadrant.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import io.github.youndie.kvadrant.indication.TiltIndication

/**
 * The theme is read on every piece of text and changes about twice in an application's life, which
 * is what `staticCompositionLocalOf` is for. The locals are internal on purpose: the public surface
 * is [KvadrantTheme], so the mechanism can change without breaking anyone.
 *
 * Animating the accent through a static local repaints the subtree every frame. Animate at the call
 * site instead — `animateColorAsState(KvadrantTheme.colors.accent)`.
 */
internal val LocalKvadrantColors = staticCompositionLocalOf { KvadrantColors.dark() }
internal val LocalKvadrantTypography =
    staticCompositionLocalOf { KvadrantTypography.default(FontFamily.SansSerif) }
internal val LocalKvadrantMetrics = staticCompositionLocalOf { KvadrantMetrics() }
internal val LocalKvadrantRemastered = staticCompositionLocalOf { false }

/** The current theme. */
public object KvadrantTheme {
    public val colors: KvadrantColors
        @Composable @ReadOnlyComposable
        get() = LocalKvadrantColors.current

    public val typography: KvadrantTypography
        @Composable @ReadOnlyComposable
        get() = LocalKvadrantTypography.current

    public val metrics: KvadrantMetrics
        @Composable @ReadOnlyComposable
        get() = LocalKvadrantMetrics.current

    /**
     * Whether this subtree may do things the phone did not.
     *
     * **Off, unless somebody asks.** The library's claim is that it reproduces Windows Phone, and a
     * default that quietly improves on it makes that claim unfalsifiable — you could no longer tell
     * a faithful component from a nicer one by looking. Someone reproducing the phone and someone
     * building a modern application in a Metro skin want opposite defaults, and both are legitimate;
     * this is the switch between them, and it is a theme value rather than a build flag so that it
     * can differ per subtree and be rendered both ways in one test.
     *
     * **Restoring behaviour the original had is not a deviation and is not gated by this.** That
     * distinction is the flag's whole value and blurring it would empty it: see
     * B-27, where finger-tracking is canon and therefore ungated.
     */
    public val remastered: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalKvadrantRemastered.current
}

/**
 * Wraps [content] in a Metro theme, and replaces the press indication with [TiltIndication] — the
 * plane leaning towards the finger is Metro's ripple, and it is the default rather than something
 * a caller remembers to apply.
 *
 * [remastered] switches on the things this library does that the phone did not. It is off by
 * default and every one of them is listed in research §2, D17.
 */
@Composable
public fun KvadrantTheme(
    colors: KvadrantColors = KvadrantColors.dark(),
    typography: KvadrantTypography = LocalKvadrantTypography.current,
    metrics: KvadrantMetrics = KvadrantMetrics(),
    remastered: Boolean = false,
    content: @Composable () -> Unit,
) {
    // The ramp scales with the metric set, and the theme does it rather than the caller: the two
    // live in different objects here, and Windows Phone had them in one — its text was measured in
    // the same canvas units as its margins, so it could not be scaled apart from them. Research
    // §1.6c. `remember` because this allocates ten TextStyles and the inputs change twice a run.
    val scaled = remember(typography, metrics.scale) { typography.scaled(metrics.scale) }

    CompositionLocalProvider(
        LocalKvadrantColors provides colors,
        LocalKvadrantTypography provides scaled,
        LocalKvadrantMetrics provides metrics,
        LocalKvadrantRemastered provides remastered,
        LocalIndication provides
            TiltIndication(maxDepression = metrics.tiltDepression, animatePress = remastered),
        LocalKvadrantTextStyle provides scaled.normal,
        content = content,
    )
}

/** The style [io.github.youndie.kvadrant.foundation.KvadrantText] uses when given none. */
public val LocalKvadrantTextStyle: androidx.compose.runtime.ProvidableCompositionLocal<TextStyle> =
    staticCompositionLocalOf { TextStyle.Default }
