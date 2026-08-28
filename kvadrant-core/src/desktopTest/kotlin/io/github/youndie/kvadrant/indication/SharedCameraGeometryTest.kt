package io.github.youndie.kvadrant.indication

import io.github.youndie.kvadrant.foundation.KvadrantCamera
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * How much a screen-wide camera would actually change, as arithmetic rather than as a picture.
 *
 * [B-26](../../../../../../../../docs/backlog/B-26-per-layer-camera-versus-a-global-one.md) has
 * asked twice for a comparison and got a picture both times: nine tiles rotated together, which a
 * press never does, and then a `transformOrigin` change that swung elements instead of leaning them
 * and reached a device. Its second acceptance criterion is the one that closes it — *the answer
 * written into research as a measurement, including "the difference is not visible"* — and nobody
 * has produced the number.
 *
 * It has a closed form. `graphicsLayer` projects about the element's own centre: a local point
 * `(x, z)` lands at `centre + x·s`, where `s = d / (d − z)`. One camera over the display projects
 * about the screen's centre instead, so the same point lands at `screenCentre + (ox + x)·s` for an
 * element centre `ox` from the middle. Subtract:
 *
 *     displacement = ox · (s − 1)
 *
 * The element's own geometry drops out entirely. **What a shared camera adds is proportional to how
 * far off-centre the element is**, which is exactly the property research §1.6 says the original
 * had and this does not — and it is nothing to do with the element's size, which is what two
 * separate reports from the demo had guessed at.
 */
class SharedCameraGeometryTest {
    /**
     * The displacement in dp of a point at depth [z] on an element whose centre is [offset] dp from
     * the middle of the screen.
     */
    private fun displacement(
        offset: Float,
        z: Float,
    ): Float {
        val d = KvadrantCamera.Distance.value
        return offset * (d / (d - z) - 1f)
    }

    /** The deepest a corner goes: half the diagonal, turned by the tilt's maximum angle. */
    private fun maxDepth(sizeDp: Float): Float = sizeDp / 2f * sqrt(2f) * sin(Tilt.MAX_ANGLE_RADIANS)

    /**
     * The control, and it is what ties this arithmetic to what the renderer actually does.
     *
     * At the centre of the screen the two cameras are the same camera, so the formula must return
     * nothing there. `SharedCameraTest` says the same thing from the other end — two surfaces
     * pressed alike in different places currently render alike — and if this returned a non-zero
     * number for a centred element the formula would be describing something else.
     */
    @Test
    fun at_the_middle_of_the_screen_the_two_cameras_agree() {
        val z = maxDepth(TILE_DP)
        assertTrue(
            abs(displacement(offset = 0f, z = z)) < 0.001f,
            "a centred element is displaced by ${displacement(0f, z)} dp, so this is not measuring " +
                "the difference between the two cameras",
        )
    }

    /**
     * **Measured: 19.5 dp at the edge of a Metro screen, and that settles "is it visible".**
     *
     * The tile is the Start screen's medium one and sits at the far corner of a 480 × 800 canvas,
     * which is the geometry every WVGA phone ran. A shared camera would move its far corner by
     * 19.5 dp — and, because the depth flips sign across the rotation axis, the near corner comes
     * back by 17.4 dp, so the tile is *stretched* by 36.9 dp across its diagonal rather than merely
     * shifted. That is a quarter of the tile's own width.
     *
     * So the outcome this item allowed for — "the difference is not visible", which would have
     * closed it — **is not available**. Whatever a screen-wide camera is worth, it is not nothing,
     * and the remaining question is whether it looks *right*, which needs the implementation
     * `NestedCameraTest` showed cannot be had from `graphicsLayer`.
     */
    @Test
    fun at_the_edge_of_the_screen_it_is_a_visible_difference() {
        val z = maxDepth(TILE_DP)
        val offset = SCREEN_HEIGHT_DP / 2f - TILE_DP / 2f
        val shift = displacement(offset, z)
        assertTrue(
            shift > VISIBLE_DP,
            "a shared camera would move the far corner of an edge tile by only $shift dp, which is " +
                "below the threshold this project calls visible — B-26's 'the difference is not " +
                "visible' outcome is now available and the item should close on this number",
        )
    }

    private companion object {
        /** The Start screen's medium tile. */
        const val TILE_DP = 158f

        /** WVGA in Metro's own canvas units, which is the geometry the whole ramp is scaled to. */
        const val SCREEN_HEIGHT_DP = 800f

        /**
         * A tenth of the medium tile. Nothing published fixes this; it is here so the assertion
         * states its threshold instead of implying one, and it is deliberately generous — the
         * measured figure is 19.5 dp, so the claim survives an argument about where "visible"
         * begins.
         */
        const val VISIBLE_DP = 15.8f
    }
}
