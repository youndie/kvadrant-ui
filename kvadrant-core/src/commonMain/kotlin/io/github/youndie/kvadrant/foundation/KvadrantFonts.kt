package io.github.youndie.kvadrant.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.youndie.kvadrant.resources.Res
import io.github.youndie.kvadrant.resources.selawik_bold
import io.github.youndie.kvadrant.resources.selawik_light
import io.github.youndie.kvadrant.resources.selawik_regular
import io.github.youndie.kvadrant.resources.selawik_semibold
import io.github.youndie.kvadrant.resources.selawik_semilight
import io.github.youndie.kvadrant.resources.source_sans_3_variable
import org.jetbrains.compose.resources.Font

/**
 * Selawik, the only legal metric-compatible stand-in for Segoe UI, in the five weights Metro uses.
 *
 * It has no Cyrillic — none at all, verified in its `cmap` — so anything outside Latin needs
 * [kvadrantCyrillic] and [KvadrantText]'s `cyrillic` parameter.
 *
 * `@Composable` because compose-resources reads a font asynchronously and needs a composition to
 * hang the load on. That is the cost of the declaration serving every target at once; the
 * desktop-only predecessor was a plain function reading a classpath resource, which is a JVM idea.
 */
@Composable
public fun kvadrantLatin(): FontFamily =
    FontFamily(
        Font(Res.font.selawik_light, FontWeight.W200),
        Font(Res.font.selawik_semilight, FontWeight.W300),
        Font(Res.font.selawik_regular, FontWeight.W400),
        Font(Res.font.selawik_semibold, FontWeight.W600),
        Font(Res.font.selawik_bold, FontWeight.W700),
    )

/**
 * Source Sans 3, instanced on its `wght` axis at [weight].
 *
 * The default is not a round number and not a static weight: Selawik's SemiLight sits between Light
 * and Regular, and 370 is where the two scripts stop reading as two weights when their ink coverage
 * is compared. Only this slot is calibrated so far.
 */
@Composable
public fun kvadrantCyrillic(weight: Int = CYRILLIC_SEMILIGHT_WEIGHT): FontFamily =
    FontFamily(
        Font(
            Res.font.source_sans_3_variable,
            FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )
