package io.github.youndie.kvadrant.foundation

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Selawik, the only legal metric-compatible stand-in for Segoe UI, in the five weights Metro uses.
 *
 * It has no Cyrillic — none at all, verified in its `cmap` — so anything outside Latin needs
 * [kvadrantCyrillic] and [KvadrantText]'s `cyrillic` parameter.
 */
public fun kvadrantLatin(): FontFamily =
    FontFamily(
        Font("fonts/selawkl.ttf", FontWeight.W200),
        Font("fonts/selawksl.ttf", FontWeight.W300),
        Font("fonts/selawk.ttf", FontWeight.W400),
        Font("fonts/selawksb.ttf", FontWeight.W600),
        Font("fonts/selawkb.ttf", FontWeight.W700),
    )

/**
 * Source Sans 3, instanced on its `wght` axis at [weight].
 *
 * The default is not a round number and not a static weight: Selawik's SemiLight sits between Light
 * and Regular, and 370 is where the two scripts stop reading as two weights when their ink coverage
 * is compared. Only this slot is calibrated so far.
 */
public fun kvadrantCyrillic(weight: Int = CYRILLIC_SEMILIGHT_WEIGHT): FontFamily =
    FontFamily(
        Font(
            "fonts/SourceSans3VF.ttf",
            FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )
