package io.github.youndie.kvadrant.foundation

import androidx.compose.ui.text.font.FontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every character this library draws that Selawik cannot is routed to the companion.
 *
 * The password mask was not, and it took a screenshot differing between two operating systems to
 * notice: `U+25CF` is Silverlight's own `PasswordChar`, it is not Cyrillic, and Selawik's `cmap`
 * does not carry it — so the host drew it, differently everywhere. The splitter approximates
 * coverage with a script test, and this holds the exceptions that approximation needs.
 */
class CompanionCoverageTest {
    private val companion = FontFamily.Monospace

    @Test
    fun the_password_mask_goes_to_the_companion() {
        val annotated = splitByScript("●●●", companion)
        assertEquals(1, annotated.spanStyles.size, "the mask was left in the primary family")
        assertEquals(
            companion,
            annotated.spanStyles
                .single()
                .item.fontFamily,
        )
        assertEquals(0, annotated.spanStyles.single().start)
        assertEquals(3, annotated.spanStyles.single().end)
    }

    @Test
    fun latin_stays_where_it_is_and_cyrillic_still_moves() {
        val mixed = splitByScript("inbox почта", companion)
        assertEquals(1, mixed.spanStyles.size, "expected one moved run")
        val span = mixed.spanStyles.single()
        assertEquals("почта", mixed.text.substring(span.start, span.end))

        val latin = splitByScript("settings", companion)
        assertTrue(latin.spanStyles.isEmpty(), "Latin was moved off the primary family")
    }

    @Test
    fun a_mask_beside_cyrillic_is_one_run() {
        val both = splitByScript("a●б", companion)
        // Adjacent characters that both need the companion share a run, which is what the loop
        // does and is worth pinning: a per-character split would be equally correct and would also
        // be one span per character.
        assertEquals(1, both.spanStyles.size, "expected the circle and the letter to share a run")
        val span = both.spanStyles.single()
        assertEquals("●б", both.text.substring(span.start, span.end))
    }
}
