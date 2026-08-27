package io.github.youndie.kvadrant.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KvadrantAccessibleTest {
    @Test
    fun every_accent_reaches_aa_in_the_opt_in_palette() {
        val failing =
            KvadrantAccents.All
                .map { (name, c) -> name to accessibleAccent(c) }
                .filter { (_, c) -> contrastRatio(c, contrastOn(c)) < WCAG_AA }
        assertEquals(emptyList(), failing, "these did not reach AA")
    }

    @Test
    fun the_nine_that_needed_help_actually_changed() {
        // If the adjustment were a no-op this test and the one above would both pass, and the
        // palette would be a lie that type-checks.
        val moved =
            KvadrantAccents.All
                .filter { (_, c) -> accessibleAccent(c) != c }
                .map { it.first }
        assertEquals(
            listOf("lime", "green", "teal", "cyan", "pink", "orange", "amber", "olive", "taupe"),
            moved,
        )
    }

    @Test
    fun the_eleven_that_already_passed_are_left_alone() {
        val untouched = KvadrantAccents.All.filter { (_, c) -> accessibleAccent(c) == c }
        assertEquals(11, untouched.size)
    }

    @Test
    fun subtle_text_reaches_aa_in_both_themes() {
        listOf(KvadrantColors.dark(), KvadrantColors.light()).forEach { canonical ->
            val fixed = canonical.accessible()
            val flattened =
                canonical.background.let { bg ->
                    Color(
                        red = bg.red + (fixed.subtle.red - bg.red) * fixed.subtle.alpha,
                        green = bg.green + (fixed.subtle.green - bg.green) * fixed.subtle.alpha,
                        blue = bg.blue + (fixed.subtle.blue - bg.blue) * fixed.subtle.alpha,
                    )
                }
            val ratio = contrastRatio(flattened, canonical.background)
            assertTrue(ratio >= WCAG_AA, "subtle was $ratio on ${if (canonical.isDark) "dark" else "light"}")
        }
    }

    @Test
    fun the_authentic_palette_is_left_exactly_as_it_was() {
        // The opt-in must not leak into the default: this is the test that would fail if someone
        // decided to be helpful.
        assertEquals(Color(0xFF1BA1E2), KvadrantColors.dark().accent)
        assertEquals(Color(0x99FFFFFF), KvadrantColors.dark().subtle)
        assertEquals(Color(0x66000000), KvadrantColors.light().subtle)
    }
}
