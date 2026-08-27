package io.github.youndie.kvadrant.foundation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.LocalKvadrantTextStyle

/**
 * Text in the current theme's colour and style.
 *
 * Cyrillic is not a fallback here, it is a second run. Compose's `FontFamily` list picks among
 * weight and style variants; it does **not** step to the next entry for a missing glyph, so a
 * bundled companion font declared that way never renders and the host's own font fills the gap
 * instead — differently on every operating system. [cyrillic] therefore takes the companion
 * explicitly and the string is split by script.
 */
@Composable
public fun KvadrantText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalKvadrantTextStyle.current,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val resolved = if (style.color.isUnspecified) style.copy(color = colors.foreground) else style
    if (cyrillic == null) {
        BasicText(text, modifier, resolved)
    } else {
        BasicText(splitByScript(text, cyrillic), modifier, resolved)
    }
}

/**
 * Latin runs keep the primary family; Cyrillic runs move to [cyrillic].
 *
 * Only the family changes. The companion's weight is baked into the family it is built with — a
 * variable face instanced at the weight that matches Selawik's SemiLight rather than at the nearest
 * static one, which reads a quarter thinner beside it. And no size compensation: the declared
 * x-heights suggest 2.9 % and the render refutes it. See B-03.
 */
internal fun splitByScript(
    text: String,
    cyrillic: FontFamily,
): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val isCyrillic = text[i].isCyrillic()
            var j = i
            while (j < text.length && text[j].isCyrillic() == isCyrillic) j++
            val run = text.substring(i, j)
            if (isCyrillic) {
                withStyle(SpanStyle(fontFamily = cyrillic)) { append(run) }
            } else {
                append(run)
            }
            i = j
        }
    }

/** Calibrated against Selawik SemiLight by ink parity; see B-03. */
public const val CYRILLIC_SEMILIGHT_WEIGHT: Int = 370

/** Source Sans 3's `wght` matching Selawik Light, by ink parity. See [kvadrantCyrillic]. */
public const val CYRILLIC_LIGHT_WEIGHT: Int = 330

/** Source Sans 3's `wght` matching Selawik Regular, by ink parity. */
public const val CYRILLIC_NORMAL_WEIGHT: Int = 420

/** Source Sans 3's `wght` matching Selawik SemiBold, by ink parity. */
public const val CYRILLIC_SEMIBOLD_WEIGHT: Int = 640

/** Source Sans 3's `wght` matching Selawik Bold, by ink parity. */
public const val CYRILLIC_BOLD_WEIGHT: Int = 690

private fun Char.isCyrillic(): Boolean = this in 'Ѐ'..'ӿ'
