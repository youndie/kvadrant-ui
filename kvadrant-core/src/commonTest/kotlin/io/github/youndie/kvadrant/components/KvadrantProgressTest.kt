package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private val animation = TargetBasedAnimation(dotPath(), Float.VectorConverter, 0f, 1f)
private val opacity = TargetBasedAnimation(dotOpacity(), Float.VectorConverter, 1f, 0f)

private fun at(millis: Int): Float = animation.getValueFromNanos(millis * 1_000_000L)

private fun alphaAt(millis: Int): Float = opacity.getValueFromNanos(millis * 1_000_000L)

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
        // Then it waits out the rest of the 4.4 second cycle where it landed — invisibly, which is
        // the other half of the same storyboard and lives in [the_dot_switches_off_when_it_lands].
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

    /**
     * The dots are a sweep, not a queue.
     *
     * `Storyboard.TargetProperty="Opacity"` is two `DiscreteDoubleKeyFrame`s per dot — one at
     * nought, nought at 2.5 s. This library animated the position and not the opacity, so all five
     * dots stayed lit where they landed and stood against the right-hand edge for the last stretch
     * of every cycle. It is the one part of this animation a still frame can show, and no still was
     * taken at the wrong moment.
     */
    @Test
    fun the_dot_switches_off_when_it_lands() {
        assertClose(1f, alphaAt(0))
        assertClose(1f, alphaAt(1250))
        assertClose(1f, alphaAt(2499))
        // Discrete: off within the millisecond, not faded out over the rest of the cycle.
        assertClose(0f, alphaAt(2500))
        assertClose(0f, alphaAt(4400))
    }

    /**
     * The middle leg is linear and the outer two are not, which is the shape of the whole thing.
     *
     * Both exponentials were `ExponentialOut6` here — the wrong exponent, and the wrong *direction*
     * on the last leg, where the template eases in. A dot decelerated into the finish instead of
     * accelerating out of the crawl.
     */
    @Test
    fun the_outer_legs_curve_and_the_middle_one_does_not() {
        val firstHalf = at(250) - at(0)
        val firstRest = at(500) - at(250)
        assertTrue(firstHalf > firstRest, "the first leg does not ease out: $firstHalf then $firstRest")

        val middleHalf = at(1250) - at(500)
        val middleRest = at(2000) - at(1250)
        assertClose(middleHalf, middleRest, tolerance = 0.01f)

        val lastHalf = at(2250) - at(2000)
        val lastRest = at(2500) - at(2250)
        assertTrue(lastHalf < lastRest, "the last leg does not ease in: $lastHalf then $lastRest")
    }
}
