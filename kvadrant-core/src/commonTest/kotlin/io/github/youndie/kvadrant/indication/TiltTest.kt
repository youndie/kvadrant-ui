package io.github.youndie.kvadrant.indication

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Surface = Size(200f, 200f)

private fun assertClose(
    expected: Float,
    actual: Float,
    tolerance: Float = 0.01f,
) = assertTrue(kotlin.math.abs(expected - actual) <= tolerance, "expected $expected, was $actual")

class TiltTest {
    @Test
    fun a_press_in_the_centre_is_all_depression_and_no_rotation() {
        val t = tiltFor(Offset(100f, 100f), Surface)
        assertClose(0f, t.rotationX)
        assertClose(0f, t.rotationY)
        assertEquals(Tilt.maxDepression, t.depression)
    }

    @Test
    fun a_press_in_a_corner_is_all_rotation_and_no_depression() {
        val t = tiltFor(Offset(0f, 0f), Surface)
        // The full 0.3 rad, split between the two axes.
        assertClose(17.188f, kotlin.math.abs(t.rotationX) + kotlin.math.abs(t.rotationY))
        assertEquals(0.dp, t.depression)
    }

    @Test
    fun the_plane_leans_towards_the_finger() {
        // These are in Silverlight's convention, because that is what the function transcribes.
        // Compose turns the opposite way on both axes, and TiltIndication negates them on the way
        // into the layer — see the comment there.
        //
        // Right edge: rotation about the y-axis, negative — the right side goes away from the eye.
        val right = tiltFor(Offset(200f, 100f), Surface)
        assertTrue(right.rotationY < 0f, "rotationY was ${right.rotationY}")
        assertClose(0f, right.rotationX)

        val left = tiltFor(Offset(0f, 100f), Surface)
        assertClose(-right.rotationY, left.rotationY)

        // Bottom edge: rotation about the x-axis, positive.
        val bottom = tiltFor(Offset(100f, 200f), Surface)
        assertTrue(bottom.rotationX > 0f, "rotationX was ${bottom.rotationX}")
        assertClose(0f, bottom.rotationY)
    }

    @Test
    fun an_edge_press_is_half_the_angle_of_a_corner_press() {
        val edge = tiltFor(Offset(200f, 100f), Surface)
        val corner = tiltFor(Offset(200f, 200f), Surface)
        assertClose(8.594f, kotlin.math.abs(edge.rotationY))
        assertClose(17.188f, kotlin.math.abs(corner.rotationX) + kotlin.math.abs(corner.rotationY))
        // Half the angle also means half the depression is given up.
        assertEquals(Tilt.maxDepression * 0.5f, edge.depression)
    }

    @Test
    fun a_degenerate_surface_does_not_produce_NaN() {
        assertEquals(TiltTransform.None, tiltFor(Offset(1f, 1f), Size(0f, 0f)))
    }
}

class TiltReturnTest {
    @Test
    fun the_plane_holds_where_it_was_for_two_hundred_milliseconds() {
        // The press is instant; the release is not. Nothing moves until the delay is up, and that
        // pause is the difference between a tap that feels acknowledged and one that twitches.
        assertClose(1f, tiltReturn(0))
        assertClose(1f, tiltReturn(100))
        assertClose(1f, tiltReturn(199))
    }

    @Test
    fun then_it_unwinds_linearly_over_a_hundred() {
        assertClose(1f, tiltReturn(200))
        assertClose(0.75f, tiltReturn(225))
        assertClose(0.5f, tiltReturn(250))
        assertClose(0.25f, tiltReturn(275))
        assertClose(0f, tiltReturn(300))
    }

    @Test
    fun and_stays_down() {
        assertClose(0f, tiltReturn(301))
        assertClose(0f, tiltReturn(5_000))
    }

    @Test
    fun the_whole_thing_takes_three_hundred_milliseconds() {
        assertEquals(300, Tilt.RETURN_DELAY_MILLIS + Tilt.RETURN_DURATION_MILLIS)
    }
}
