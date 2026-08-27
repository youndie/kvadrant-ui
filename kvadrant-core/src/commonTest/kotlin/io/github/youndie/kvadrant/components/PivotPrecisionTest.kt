package io.github.youndie.kvadrant.components

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Why `KvadrantPivotHeaders` keeps the page and the fraction apart.
 *
 * A cyclic pivot runs the pager on a virtual page count and starts it in the middle, so
 * `currentPage` is around half a million. Adding the offset fraction to it in a `Float` looks
 * harmless and destroys the fraction: this test is the arithmetic that proves it, kept so that
 * anyone tempted to write `page + fraction` back into one expression finds out here rather than by
 * watching the headers move in steps.
 */
class PivotPrecisionTest {
    private val virtualStart = (1 shl 20) / 2 // what rememberKvadrantPivotState opens on

    @Test
    fun a_float_cannot_hold_the_page_and_the_fraction_together() {
        val combined = virtualStart.toFloat() + 0.30f
        val recovered = combined - virtualStart.toFloat()
        // Anything smaller than the float spacing at this magnitude is simply gone.
        assertTrue(
            abs(recovered - 0.30f) > 0.01f,
            "expected the fraction to be mangled, but it survived as $recovered",
        )
    }

    @Test
    fun the_spacing_between_representable_floats_there_is_a_sixteenth() {
        val a = virtualStart.toFloat()
        val next = a.nextUp()
        assertEquals(0.0625f, next - a, "float spacing near 2^19")
    }

    @Test
    fun the_two_approaches_disagree_where_it_shows() {
        // Four positions a finger passes through during one swipe. Interpolating a 200 px header
        // gap by each of them: kept apart, four distinct offsets; folded into one float, fewer —
        // and repeats are exactly what a jerk looks like.
        val fractions = listOf(0.05f, 0.10f, 0.15f, 0.20f)
        val width = 200f

        val apart = fractions.map { (width * it).toInt() }
        val folded =
            fractions.map { fraction ->
                val combined = virtualStart.toFloat() + fraction
                (width * (combined - virtualStart.toFloat())).toInt()
            }

        assertEquals(4, apart.distinct().size, "kept apart, every step should differ: $apart")
        assertTrue(
            folded.distinct().size < 4,
            "folded into one float the steps should collapse, but got $folded",
        )
    }
}

private fun Float.nextUp(): Float {
    val bits = toRawBits()
    return Float.fromBits(if (this >= 0f) bits + 1 else bits - 1)
}
