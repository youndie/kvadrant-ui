package io.github.youndie.kvadrant.foundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import kotlin.math.cos
import kotlin.math.sin

/**
 * The geometry a camera over the whole screen needs, and the transform that can express it.
 *
 * **Why this is not `graphicsLayer`.** That gives every element its own camera at its own centre
 * ([B-26](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-26-per-layer-camera-versus-a-global-one.md)),
 * and neither its `transformOrigin` nor nesting two of them can move the projection centre away
 * from the rotation pivot — measured in `NestedCameraTest`. What `Canvas.concat` *will* carry is a
 * projective transform whose divide is driven by x and y rather than by z: a 3 x 3 homography,
 * measured in `CanvasPerspectiveTest`.
 *
 * That is enough, and the reason is worth stating once. A flat surface rotated in space and
 * projected from any eye lands as a **quadrilateral**, and every plane-to-plane projective map is a
 * homography. So the two halves here are: work out where the four corners go under a camera at an
 * arbitrary point ([quadUnderCamera]), and build the transform that takes the element's rectangle
 * there ([homographyFromRect]).
 */
internal object KvadrantHomography {
    /**
     * Where an element's four corners land, seen from an eye [eye] away from the element's own
     * centre in the plane, at depth [cameraDistance].
     *
     * `eye` is the whole of the difference between the two cameras. At `Offset.Zero` this is what
     * `graphicsLayer` already does — the projection centred on the element — and every other value
     * moves the projection centre without moving the rotation pivot, which is the thing one
     * `graphicsLayer` cannot be made to do.
     *
     * Corners come back in the order a rectangle is read: top-left, top-right, bottom-right,
     * bottom-left, in the element's own coordinates.
     */
    fun quadUnderCamera(
        size: Size,
        rotationXDegrees: Float,
        rotationYDegrees: Float,
        cameraDistance: Float,
        eye: Offset,
    ): List<Offset> {
        val halfWidth = size.width / 2f
        val halfHeight = size.height / 2f
        val corners =
            listOf(
                Offset(-halfWidth, -halfHeight),
                Offset(halfWidth, -halfHeight),
                Offset(halfWidth, halfHeight),
                Offset(-halfWidth, halfHeight),
            )

        val radiansX = rotationXDegrees * PI_OVER_180
        val radiansY = rotationYDegrees * PI_OVER_180
        val cosX = cos(radiansX)
        val sinX = sin(radiansX)
        val cosY = cos(radiansY)
        val sinY = sin(radiansY)

        return corners.map { corner ->
            // Rotate about the element's own centre: Y first, then X. The order is a choice and it
            // matches the one `graphicsLayer` makes, which is what lets the centred case be checked
            // against the renderer rather than only against itself.
            val x1 = corner.x * cosY
            val z1 = -corner.x * sinY
            val y2 = corner.y * cosX - z1 * sinX
            val z2 = corner.y * sinX + z1 * cosX

            // The divide, about the eye rather than about the element. `scale` is the same number
            // `graphicsLayer` applies; what differs is the point it is applied around.
            val scale = cameraDistance / (cameraDistance - z2)
            Offset(
                x = halfWidth + eye.x + (x1 - eye.x) * scale,
                y = halfHeight + eye.y + (y2 - eye.y) * scale,
            )
        }
    }

    /**
     * The transform taking the rectangle `0, 0, size` onto [quad].
     *
     * Heckbert's unit-square solve, pre-scaled by the element's size. The `sx`/`sy` test is not a
     * tidiness check: when a quad is a parallelogram the projective terms have no solution — the
     * denominator is zero — and the map is affine. A press at the exact centre of a surface produces
     * exactly that, so it is the ordinary case rather than a degenerate one.
     *
     * The result is written in the layout Compose's own `Matrix.map` reads, where the divisor is
     * `m[0, 3] · x + m[1, 3] · y + m[3, 3]`. That layout is asserted rather than assumed:
     * `KvadrantHomographyTest` maps the corners back through `Matrix.map` and requires them to land
     * on [quad].
     */
    fun homographyFromRect(
        size: Size,
        quad: List<Offset>,
    ): Matrix {
        require(quad.size == 4) { "a homography needs four corners, got ${quad.size}" }
        val (topLeft, topRight, bottomRight, bottomLeft) = quad

        val dx1 = topRight.x - bottomRight.x
        val dx2 = bottomLeft.x - bottomRight.x
        val dy1 = topRight.y - bottomRight.y
        val dy2 = bottomLeft.y - bottomRight.y
        val sumX = topLeft.x - topRight.x + bottomRight.x - bottomLeft.x
        val sumY = topLeft.y - topRight.y + bottomRight.y - bottomLeft.y

        val determinant = dx1 * dy2 - dx2 * dy1
        val g: Float
        val h: Float
        if (determinant == 0f) {
            g = 0f
            h = 0f
        } else {
            g = (sumX * dy2 - dx2 * sumY) / determinant
            h = (dx1 * sumY - sumX * dy1) / determinant
        }

        val a = topRight.x - topLeft.x + g * topRight.x
        val b = bottomLeft.x - topLeft.x + h * bottomLeft.x
        val c = topLeft.x
        val d = topRight.y - topLeft.y + g * topRight.y
        val e = bottomLeft.y - topLeft.y + h * bottomLeft.y
        val f = topLeft.y

        // The unit square's map, then the element's own size folded in so the transform takes
        // pixels rather than fractions.
        val scaleX = if (size.width == 0f) 0f else 1f / size.width
        val scaleY = if (size.height == 0f) 0f else 1f / size.height

        return Matrix().apply {
            this[0, 0] = a * scaleX
            this[1, 0] = b * scaleY
            this[3, 0] = c
            this[0, 1] = d * scaleX
            this[1, 1] = e * scaleY
            this[3, 1] = f
            this[0, 3] = g * scaleX
            this[1, 3] = h * scaleY
            this[3, 3] = 1f
        }
    }

    private const val PI_OVER_180 = 0.017453292f
}
