package io.github.youndie.kvadrant.material

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography

/**
 * A Metro palette derived from a Material one, for dropping a Kvadrant island into a Material
 * application.
 *
 * **Only the accent crosses, and the seam that leaves is the point rather than a defect.** Metro's
 * background is `#FF000000` or `#FFFFFFFF` and nothing else — the dark theme's absolute black is a
 * decision with a reason (research D6), not a shade that happens to be dark — so a
 * host whose `surface` is a tinted near-black gets a visible edge where the island starts. Softening
 * it would mean giving up the one colour Metro is least willing to negotiate, which is a worse trade
 * than an edge; and an island that blends perfectly is an island nobody can see is there.
 *
 * What is read: [ColorScheme.primary] becomes the accent, and the luminance of
 * [ColorScheme.background] decides dark against light. Everything else in the scheme is discarded,
 * because Metro's other twelve brushes are fixed by the theme rather than derived from anything.
 */
public fun ColorScheme.toKvadrantColors(): KvadrantColors =
    if (background.luminance() < LIGHT_THRESHOLD) {
        KvadrantColors.dark(accent = primary)
    } else {
        KvadrantColors.light(accent = primary)
    }

/**
 * Wraps [content] in a [KvadrantTheme] derived from the surrounding [MaterialTheme] — the reverse of
 * [KvadrantMaterialAdapter], and inherently the more partial of the two.
 *
 * Forward, a Material widget inside Metro can be told about thirteen brushes and a type ramp. Back,
 * a Metro island inside Material takes the host's accent and asserts everything else, because that
 * is what the design language *is*. Read [toKvadrantColors] for what the seam costs.
 *
 * [typography] is a parameter because a font is not derivable from a `ColorScheme` and this module
 * must not decide one for a consumer: pass the family the host already loads, or
 * `KvadrantTypography.default(kvadrantLatin())` for Metro's own.
 */
@Composable
public fun KvadrantIsland(
    typography: KvadrantTypography,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = remember(scheme) { scheme.toKvadrantColors() }
    KvadrantTheme(colors = colors, typography = typography, content = content)
}

/**
 * Renders [kvadrant] on a Metro surface and [material] anywhere else.
 *
 * On the model `compose-cupertino` proved — the only working precedent for a second design language
 * beside Material in a Kotlin Multiplatform project (research §1.1). The decision is
 * [KvadrantTheme.present], which is false unless a theme actually provided it, rather than any of
 * the theme's values: every other local here has a working default, so reading one says nothing
 * about whether anybody is above you.
 *
 * It is a demonstration more than a tool. A component library cannot know which of two design
 * languages a screen wants, and a caller who does know can simply write the `if` — what this earns
 * its place on is making "drop Metro into your existing application" a thing somebody can try in one
 * line before committing to it.
 */
@Composable
public fun AdaptiveWidget(
    kvadrant: @Composable () -> Unit,
    material: @Composable () -> Unit,
) {
    if (KvadrantTheme.present) kvadrant() else material()
}

/** Above this, a background is a light theme's. Half, because there is nothing subtler to say. */
private const val LIGHT_THRESHOLD = 0.5f
