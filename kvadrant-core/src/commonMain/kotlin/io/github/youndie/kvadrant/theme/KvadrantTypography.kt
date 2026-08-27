package io.github.youndie.kvadrant.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Windows Phone sized text in points. Metro's own resource dictionary annotates every size with its
 * point value, and the ratio is 0.75 in all seventeen of them, so this is Microsoft's conversion
 * rather than a convention chosen here.
 */
public object KvadrantFontSizes {
    public val Small: TextUnit = 14.sp // 18.667 px
    public val Normal: TextUnit = 15.sp // 20 px
    public val Medium: TextUnit = 17.sp // 22.667 px
    public val MediumLarge: TextUnit = 19.sp // 25.333 px
    public val Large: TextUnit = 24.sp // 32 px
    public val ExtraLarge: TextUnit = 32.sp // 42.667 px
    public val ExtraExtraLarge: TextUnit = 54.sp // 72 px
    public val Huge: TextUnit = 140.sp // 186.667 px
}

/**
 * Metro chooses weight by picking a family, not by setting a number. Selawik ships exactly the five
 * Segoe WP had, and these are the four the phone's own styles use.
 */
public object KvadrantWeights {
    public val Light: FontWeight = FontWeight.W200
    public val SemiLight: FontWeight = FontWeight.W300
    public val Normal: FontWeight = FontWeight.W400
    public val SemiBold: FontWeight = FontWeight.W600
}

/**
 * The named text styles of a Windows Phone page.
 *
 * [pivotHeader] is the one to look at: 72 px SemiLight, the largest step of the ramp, read out of
 * `PivotHeaderFontSize` in the SDK's theme dictionary rather than guessed.
 */

@Immutable
public data class KvadrantTypography(
    val normal: TextStyle,
    val subtle: TextStyle,
    val title: TextStyle,
    /**
     * `PhoneFontSizeMediumLarge`, 25.333 px.
     *
     * The slot the ramp had but the theme did not, which is why four controls that Microsoft sized
     * *up* from the page default were all set in [normal]: a text box, a list picker and an
     * autocomplete box are `PhoneFontSizeMediumLarge` in the toolkit's `Generic.xaml`, and a
     * control with nowhere to point at ends up pointing at the default.
     */
    val mediumLarge: TextStyle,
    val large: TextStyle,
    val extraLarge: TextStyle,
    val pageTitle: TextStyle,
    val pivotHeader: TextStyle,
    val panoramaTitle: TextStyle,
    val panoramaSectionHeader: TextStyle,
) {
    public companion object {
        /**
         * [family] is a parameter rather than a bundled default: the library does not yet ship its
         * fonts through multiplatform resources, and a theme that loads files is a theme that only
         * works on the platform whose loader it hard-codes. See B-07.
         */
        public fun default(family: FontFamily): KvadrantTypography =
            KvadrantTypography(
                normal =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.Normal,
                        fontWeight = KvadrantWeights.Normal,
                    ),
                subtle =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.Small,
                        fontWeight = KvadrantWeights.Normal,
                    ),
                title =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.Medium,
                        fontWeight = KvadrantWeights.SemiBold,
                    ),
                mediumLarge =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.MediumLarge,
                        fontWeight = KvadrantWeights.Normal,
                    ),
                large =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.Large,
                        fontWeight = KvadrantWeights.SemiLight,
                    ),
                extraLarge =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.ExtraLarge,
                        fontWeight = KvadrantWeights.SemiLight,
                    ),
                // ApplicationTitle: small, upper-cased by the page rather than by the style.
                pageTitle =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.Small,
                        fontWeight = KvadrantWeights.Normal,
                    ),
                pivotHeader =
                    TextStyle(
                        fontFamily = family,
                        fontSize = KvadrantFontSizes.ExtraExtraLarge,
                        fontWeight = KvadrantWeights.SemiLight,
                    ),
                // 170 px Light, tracking -35/1000 em — the panorama title, not the section header.
                panoramaTitle =
                    TextStyle(
                        fontFamily = family,
                        fontSize = 127.5.sp,
                        fontWeight = KvadrantWeights.Light,
                        letterSpacing = (-0.035).em,
                    ),
                // 66 px SemiLight, same tracking.
                panoramaSectionHeader =
                    TextStyle(
                        fontFamily = family,
                        fontSize = 49.5.sp,
                        fontWeight = KvadrantWeights.SemiLight,
                        letterSpacing = (-0.035).em,
                    ),
            )
    }
}

/**
 * The same ramp, every size multiplied by [factor].
 *
 * **Windows Phone scaled its text with its layout and this restores that.** The canvas was 480 units
 * wide whatever the screen was, and the device stretched the whole of it — ×1.5 on 720p, ×1.6 on
 * WXGA, ×2.25 on 1080p — so a 20-unit body line was physically 2.18 mm on a 4-inch Lumia 520 and
 * 3.11 mm on a 6-inch Lumia 1520. Text was measured in the same units as margins and tiles and had
 * no way *not* to scale with them.
 *
 * Leaving the ramp fixed while the layout grows is the Android and iOS convention, and it was this
 * project's until it was checked: research §1.6c has the measurements. Nothing is rounded to whole
 * sp on the way — Metro's own ramp is not whole numbers either (18.667, 22.667, 25.333 px), and
 * rounding a scaled ramp would break the relationships the unrounded one holds.
 */
public fun KvadrantTypography.scaled(factor: Float): KvadrantTypography =
    if (factor == 1f) {
        this
    } else {
        KvadrantTypography(
            normal = normal.scaledBy(factor),
            subtle = subtle.scaledBy(factor),
            title = title.scaledBy(factor),
            mediumLarge = mediumLarge.scaledBy(factor),
            large = large.scaledBy(factor),
            extraLarge = extraLarge.scaledBy(factor),
            pageTitle = pageTitle.scaledBy(factor),
            pivotHeader = pivotHeader.scaledBy(factor),
            panoramaTitle = panoramaTitle.scaledBy(factor),
            panoramaSectionHeader = panoramaSectionHeader.scaledBy(factor),
        )
    }

private fun TextStyle.scaledBy(factor: Float): TextStyle =
    copy(
        fontSize = fontSize * factor,
        // Only if the style set one. `TextUnit.Unspecified * f` is not Unspecified, and a line
        // height that quietly becomes a number changes the layout of every style that had none.
        lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight,
    )
