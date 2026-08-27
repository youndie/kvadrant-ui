package io.github.youndie.kvadrant.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

class KvadrantColorsTest {
    @Test
    fun the_light_theme_is_not_an_inversion_of_the_dark_one() {
        val dark = KvadrantColors.dark()
        val light = KvadrantColors.light()
        // 100 % white against 87 % black, 60 % against 40 %, 40 % against 30 %. Deriving one
        // palette from the other produces something that reads as almost-Metro.
        assertEquals(
            0xFFFFFFFF,
            dark.foreground.value
                .shr(32)
                .toLong() and 0xFFFFFFFFL,
        )
        assertEquals(
            0xDE000000,
            light.foreground.value
                .shr(32)
                .toLong() and 0xFFFFFFFFL,
        )
        assertTrue(dark.subtle.alpha > light.subtle.alpha, "subtle: dark 60 %, light 40 %")
        assertTrue(dark.disabled.alpha > light.disabled.alpha, "disabled: dark 40 %, light 30 %")
    }

    @Test
    fun the_border_colour_differs_between_the_themes() {
        // The brief's dump showed one value in both and flagged it as suspect. The SDK's own
        // ThemeResources says otherwise: 75 % white against 60 % black.
        assertTrue(
            KvadrantColors.dark().border != KvadrantColors.light().border,
            "PhoneBorderColor is per-theme",
        )
    }

    @Test
    fun every_accent_is_paired_with_the_text_colour_windows_phone_used() {
        // Only Yellow is light enough to take black text under the luminance rule.
        val black = KvadrantAccents.All.filter { (_, c) -> contrastOn(c) == Color.Black }.map { it.first }
        assertEquals(listOf("yellow"), black)
    }

    @Test
    fun nine_accents_fail_wcag_aa_and_that_is_the_authentic_result() {
        // D7: the visual stays canonical and a higher-contrast palette is opt-in. The list is
        // written out so that a tenth joining it is a change somebody made rather than one nobody
        // noticed — and because the count is the argument for the decision. Nearly half the palette
        // is below AA at its authentic text colour, the default accent `cyan` among it at 2.90:1.
        val failing =
            KvadrantAccents.All
                .filter { (_, c) -> contrastRatio(c, contrastOn(c)) < 4.5f }
                .map { it.first }
        assertEquals(
            listOf("lime", "green", "teal", "cyan", "pink", "orange", "amber", "olive", "taupe"),
            failing,
        )
    }

    @Test
    fun yellow_is_the_most_legible_accent_not_the_least() {
        // It is the one the luminance rule flips to black text, which takes it from the worst
        // contrast in the palette to the best by a wide margin.
        val ratios = KvadrantAccents.All.associate { (n, c) -> n to contrastRatio(c, contrastOn(c)) }
        assertEquals("yellow", ratios.maxByOrNull { it.value }!!.key)
        assertTrue(ratios.getValue("yellow") > 12f, "yellow was ${ratios.getValue("yellow")}")
    }
}
