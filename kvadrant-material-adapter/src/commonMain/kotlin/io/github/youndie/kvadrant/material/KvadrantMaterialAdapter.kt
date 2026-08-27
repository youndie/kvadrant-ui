package io.github.youndie.kvadrant.material

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.indication.TiltIndication
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography

/**
 * Raises a [MaterialTheme] derived from the surrounding [KvadrantTheme], so a consumer's existing
 * Material widgets stop looking foreign inside a Metro application.
 *
 * **This is for foreign widgets, not for building a Metro kit out of Material.** Of roughly
 * twenty-two Material 3 components, about four survive on theming alone, ten need a wrapper and
 * eleven need replacing outright — the disagreements are structural rather than stylistic and
 * research §1.3 counts them. What this earns its place on is the date picker or the autocomplete a
 * consumer already depends on and is not going to rewrite.
 *
 * It must be wrapped *inside* a `KvadrantTheme`, because everything it provides is read from one.
 *
 * **What it cannot reach.** Theming squares every component that reads `MaterialTheme.shapes` —
 * `CardDefaults.shape` under this adapter is a zero corner, measured — and it reaches nothing that
 * hard-codes its own. `ButtonDefaults.shape` is `RoundedCornerShape(50%)` from a token and ignores
 * the theme entirely, so a Material `Button` stays a pill here no matter how many `Shapes` slots are
 * filled. That is not a gap in this adapter; it is the "shapes forced round" cause behind the ten or
 * so components research §1.3 counts as needing a wrapper rather than theming. Pass
 * `shape = MaterialTheme.shapes.small` at the call site for those, and expect to find more of them
 * the same way this one was found — by measuring a corner.
 */
@Composable
public fun KvadrantMaterialAdapter(content: @Composable () -> Unit) {
    val colors = KvadrantTheme.colors
    val typography = KvadrantTheme.typography

    val scheme = remember(colors) { colors.toColorScheme() }
    val type = remember(typography) { typography.toMaterialTypography() }

    CompositionLocalProvider(
        // The ripple is not turned off by styling it away; Material calls it from inside its own
        // implementations rather than through `LocalIndication`, and `null` here is the documented
        // way to say "no ripple at all". It is also the single most fragile line in this artefact —
        // an alpha Material API, which is why this lives in its own module with its own release
        // cadence and why the core declares no Material dependency at all.
        LocalRippleConfiguration provides null,
        // And this is what replaces it: the tilt, from the theme above.
        LocalIndication provides TiltIndication(),
    ) {
        MaterialTheme(colorScheme = scheme, typography = type, shapes = FLAT, content = content)
    }
}

/**
 * All **eight** slots rectangular.
 *
 * Metro has no rounded corner anywhere — not on a button, not on a dialog, not on a chip. Leaving
 * one slot round is how a Material dialog ends up with a soft edge in the middle of a flat design.
 *
 * `RectangleShape` will not go in: `Shapes` takes `CornerBasedShape`, not `Shape`, so the way to say
 * "no corner" to Material is a rounded corner of zero. Same pixels, and worth knowing before
 * spending time on why the obvious spelling does not compile.
 *
 * Eight, and B-14 says six. This version of `Shapes` carries `largeIncreased`,
 * `extraLargeIncreased` and `extraExtraLarge` beyond the five everyone knows, and its five-argument
 * constructor still exists and still compiles — leaving the other three at their rounded defaults,
 * silently. Filling every slot by name rather than positionally is what makes the next one that
 * appears a compile error instead of a round corner somebody notices in a screenshot.
 */
private val SQUARE = RoundedCornerShape(0.dp)

private val FLAT =
    Shapes(
        extraSmall = SQUARE,
        small = SQUARE,
        medium = SQUARE,
        large = SQUARE,
        largeIncreased = SQUARE,
        extraLarge = SQUARE,
        extraLargeIncreased = SQUARE,
        extraExtraLarge = SQUARE,
    )

/**
 * Thirteen Metro tokens into Material's forty-eight roles.
 *
 * The mapping is lossy in one direction only and that is the honest way round: Metro has one accent
 * where Material has primary/secondary/tertiary with containers for each, so those all become the
 * accent. What matters more than any single row is `surfaceTint`, which is
 * [Color.Transparent] here: it is the one that makes a Material surface tint itself by elevation,
 * and a tinted surface in a design with no depth reads as a mistake nobody can point at.
 */
private fun KvadrantColors.toColorScheme() =
    with(this) {
        val base = if (background.luminance() < 0.5f) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent,
            onPrimaryContainer = onAccent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accent,
            onSecondaryContainer = onAccent,
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = accent,
            onTertiaryContainer = onAccent,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = chrome,
            onSurfaceVariant = subtle,
            surfaceContainer = chrome,
            surfaceContainerLow = chrome,
            surfaceContainerLowest = background,
            surfaceContainerHigh = chrome,
            surfaceContainerHighest = chrome,
            outline = border,
            outlineVariant = inactive,
            // The one that kills tonal elevation. Everything else here is colour; this is depth.
            surfaceTint = Color.Transparent,
        )
    }

/**
 * The Metro ramp into Material's fifteen styles.
 *
 * Material's ramp is display/headline/title/body/label × large/medium/small, and Metro's is not that
 * shape — it is mostly size, where Material is size *and* weight *and* colour (research §1.3). So
 * this is a placement rather than a translation, and the rule it follows is that a Material widget
 * should end up looking like its nearest Kvadrant neighbour: a `label` is what sits inside a button,
 * so it takes the size a Kvadrant button uses.
 */
private fun KvadrantTypography.toMaterialTypography() =
    Typography(
        displayLarge = panoramaTitle,
        displayMedium = pivotHeader,
        displaySmall = pivotHeader,
        headlineLarge = extraLarge,
        headlineMedium = extraLarge,
        headlineSmall = large,
        titleLarge = large,
        titleMedium = mediumLarge,
        titleSmall = title,
        bodyLarge = mediumLarge,
        bodyMedium = normal,
        bodySmall = subtle,
        labelLarge = normal,
        labelMedium = normal,
        labelSmall = subtle,
    )
