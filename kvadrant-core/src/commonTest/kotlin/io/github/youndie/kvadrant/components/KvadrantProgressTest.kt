package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private val animation = TargetBasedAnimation(dotPath(), Float.VectorConverter, 0f, 1f)

private fun at(millis: Int): Float = animation.getValueFromNanos(millis * 1_000_000L)

private fun assertClose(
    expected: Float,
    actual: Float,
    tolerance: Float = 0.02f,
) = assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")

class KvadrantProgressTest {
    @Test
    fun the_dot_covers_the_published_thirds_at_the_published_times() {
        assertClose(0f, at(0))
        assertClose(0.33f, at(500))
        assertClose(0.66f, at(2000))
        assertClose(1f, at(2500))
        // Then it waits out the rest of the 4.4 second cycle where it landed.
        assertClose(1f, at(4400))
    }

    @Test
    fun the_middle_third_is_the_slow_one() {
        // The point of the three-part path: the dots bunch up in the middle of the run and stretch
        // out at both ends. Each leg covers about a third, but the middle one takes three times as
        // long — which a screenshot of the animation can never show.
        val first = at(500) - at(0)
        val middle = at(2000) - at(500)
        val last = at(2500) - at(2000)
        assertTrue(first > 0.3f, "first leg was $first")
        assertTrue(last > 0.3f, "last leg was $last")
        assertTrue(middle <= first + 0.01f, "middle leg was $middle over three times the duration")
    }

    @Test
    fun the_dot_never_goes_backwards() {
        var previous = -1f
        for (millis in 0..4400 step 50) {
            val value = at(millis)
            assertTrue(value >= previous - 0.001f, "went backwards at $millis ms: $previous -> $value")
            previous = value
        }
    }
}
