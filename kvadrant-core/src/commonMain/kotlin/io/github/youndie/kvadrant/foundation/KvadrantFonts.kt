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
 * Source Sans 3, instanced on its `wght` axis once per Metro weight.
 *
 * **Five entries, not one, and that was a defect rather than a refinement.** The family used to hold
 * a single font at 370, so Compose matched it for every requested weight and the axis stayed where
 * it was: a `SemiBold` heading rendered Latin bold and its Cyrillic at 370 beside it, visibly
 * lighter. Cyrillic could not be made bold at all.
 *
 * The axis values are **measured, not derived**. Ink coverage — lit pixels over the area of the
 * drawn line — is compared between Selawik at the Metro weight and Source Sans at each candidate,
 * and the closest wins. The method's own control is that it rediscovers **370** for SemiLight, which
 * B-03 had found by eye; it does, exactly, which is why the other four are trusted.
 *
 * | Metro | Selawik | Source Sans wght |
 * |---|---|---|
 * | Light | W200 | 330 |
 * | SemiLight | W300 | **370** |
 * | Normal | W400 | 420 |
 * | SemiBold | W600 | 640 |
 * | Bold | W700 | 690 |
 *
 * They do not sit on a straight line and there is no offset that produces them — Source Sans runs
 * relatively heavier than Selawik at the thin end and lighter at the thick end, so +130 at Light
 * becomes −10 at Bold. Any rule fitted to one weight would have been wrong at the other, which is
 * the argument for measuring five times rather than once. `InkParityTest` holds them.
 */
@Composable
public fun kvadrantCyrillic(): FontFamily =
    FontFamily(
        cyrillicAt(FontWeight.W200, CYRILLIC_LIGHT_WEIGHT),
        cyrillicAt(FontWeight.W300, CYRILLIC_SEMILIGHT_WEIGHT),
        cyrillicAt(FontWeight.W400, CYRILLIC_NORMAL_WEIGHT),
        cyrillicAt(FontWeight.W600, CYRILLIC_SEMIBOLD_WEIGHT),
        cyrillicAt(FontWeight.W700, CYRILLIC_BOLD_WEIGHT),
    )

/**
 * The companion at one weight, for a caller that wants a single instance — the fitting fixtures do,
 * and so does anything comparing two axis values side by side.
 */
@Composable
public fun kvadrantCyrillic(weight: Int): FontFamily = FontFamily(cyrillicAt(FontWeight(weight), weight))

@Composable
private fun cyrillicAt(
    slot: FontWeight,
    axis: Int,
) = Font(
    Res.font.source_sans_3_variable,
    slot,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis)),
)
