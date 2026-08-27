package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import io.github.youndie.kvadrant.theme.WCAG_AA
import io.github.youndie.kvadrant.theme.accessibleAccent
import io.github.youndie.kvadrant.theme.contrastOn
import io.github.youndie.kvadrant.theme.contrastRatio
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * Every accent twice: the authentic value on the left, the opt-in accessible one on the right, each
 * carrying the text colour the luminance rule gives it and its contrast ratio.
 *
 * Nine rows differ. Eleven are identical, and being able to see which is the point — a palette that
 * changed everything would be a different palette rather than an adjusted one.
 */
@Composable
private fun Swatches(colors: KvadrantColors) {
    KvadrantTheme(colors = colors, typography = KvadrantTypography.default(kvadrantLatin())) {
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KvadrantText("authentic", Modifier.width(170.dp), KvadrantTheme.typography.subtle)
                KvadrantText("accessible", Modifier.width(170.dp), KvadrantTheme.typography.subtle)
            }
            KvadrantAccents.All.forEach { (name, accent) ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Swatch(name, accent)
                    Swatch(name, accessibleAccent(accent))
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    name: String,
    color: Color,
) {
    val on = contrastOn(color)
    val ratio = contrastRatio(color, on)
    Box(
        Modifier.width(170.dp).background(color).padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KvadrantText(name, style = KvadrantTheme.typography.subtle.copy(color = on))
            KvadrantText(
                "${(ratio * 100).toInt() / 100f}${if (ratio < WCAG_AA) " !" else ""}",
                style = KvadrantTheme.typography.subtle.copy(color = on),
            )
        }
    }
}

@ViddikScreenshot(name = "accents dark", group = "palette", width = 370, height = 560)
@Composable
internal fun AccentsDark(): Unit = Swatches(KvadrantColors.dark())

@ViddikScreenshot(name = "accents light", group = "palette", width = 370, height = 560)
@Composable
internal fun AccentsLight(): Unit = Swatches(KvadrantColors.light())
