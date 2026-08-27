package io.github.youndie.kvadrant.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
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
}

/**
 * Wraps [content] in a Metro theme, and replaces the press indication with [TiltIndication] — the
 * plane leaning towards the finger is Metro's ripple, and it is the default rather than something
 * a caller remembers to apply.
 */
@Composable
public fun KvadrantTheme(
    colors: KvadrantColors = KvadrantColors.dark(),
    typography: KvadrantTypography = LocalKvadrantTypography.current,
    metrics: KvadrantMetrics = KvadrantMetrics(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKvadrantColors provides colors,
        LocalKvadrantTypography provides typography,
        LocalKvadrantMetrics provides metrics,
        LocalIndication provides TiltIndication(maxDepression = metrics.tiltDepression),
        LocalKvadrantTextStyle provides typography.normal,
        content = content,
    )
}

/** The style [io.github.youndie.kvadrant.foundation.KvadrantText] uses when given none. */
public val LocalKvadrantTextStyle: androidx.compose.runtime.ProvidableCompositionLocal<TextStyle> =
    staticCompositionLocalOf { TextStyle.Default }
