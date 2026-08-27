package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import io.github.youndie.kvadrant.foundation.KvadrantCamera
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Metro's press feedback, in the slot Material fills with a ripple.
 *
 * The plane leans towards the finger and sinks away from the eye; see [tiltFor] for the geometry.
 * The press itself is **not** animated — the original sets the properties outright on every
 * manipulation delta, and animates only the return.
 *
 * The original pushed the plane away along Z. Compose has no `translationZ`, and its perspective
 * term is applied only when there is a rotation — so a centre press, which is pure depression,
 * would get no perspective at all. The sinking is therefore emulated with a uniform scale computed
 * from the same camera the rotation uses, which is the one deviation from the original geometry.
 *
 * [cameraDistance] is a **[Dp]**, and that is the whole of what B-25 turned out to be. Compose's own
 * `GraphicsLayerScope.cameraDistance` lands the camera at a fixed number of *pixels* — measured at
 * `value * 72` px on both skiko and hwui, the two backends this library has — while every piece of
 * geometry it looks at is in dp. On a 2.625x screen a 100 dp tile is 262 px and subtends 0.456 of a
 * 576 px camera where the same tile subtends 0.174 on the desktop, so the denser the screen the
 * more perspective a press gets and the deeper it sinks. Measured, before and after: a tile drew at
 * 0.9225 of itself on a Pixel 6a against 0.9685 on the desktop.
 *
 * Holding the camera at a distance in dp makes the whole projection density-independent, which is
 * what "the same design on every screen" has to mean. The default reproduces Compose's own camera
 * exactly at density 1, so nothing about the desktop moves.
 *
 * The distance itself is still a decision rather than a transcription: the original's camera was
 * global to the screen and Microsoft never published how far away it was.
 */
public class TiltIndication(
    private val cameraDistance: Dp = DEFAULT_CAMERA_DISTANCE,
    private val maxDepression: Dp = Tilt.maxDepression,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        TiltNode(interactionSource, cameraDistance, maxDepression)

    override fun equals(other: Any?): Boolean =
        other is TiltIndication &&
            other.cameraDistance == cameraDistance &&
            other.maxDepression == maxDepression

    override fun hashCode(): Int = 31 * cameraDistance.hashCode() + maxDepression.hashCode()

    public companion object {
        /**
         * The library's one camera, [KvadrantCamera.Distance]. At that distance the full 18.75 dp
         * depression is a shrink of **0.9685**, on every screen.
         */
        public val DEFAULT_CAMERA_DISTANCE: Dp = KvadrantCamera.Distance
    }
}

/**
 * The shrink that stands in for sinking away from the eye: a plane at [depressionPx] behind the
 * drawing plane, seen from a camera [depthPx] away.
 *
 * Both arguments are pixels, and that they are the *same* pixels is the point. Taking a depth in one
 * unit and a depression in another is what made a press sink 0.9225 of a tile on a phone and 0.9685
 * on a desktop.
 */
internal fun depressionScale(
    depressionPx: Float,
    depthPx: Float,
): Float = depthPx / (depthPx + depressionPx)

private class TiltNode(
    private val interactionSource: InteractionSource,
    private val cameraDistance: Dp,
    private val maxDepression: Dp,
) : Modifier.Node(),
    LayoutModifierNode {
    /** Where the finger is, or was. Kept after release so the plane has somewhere to return from. */
    private var pressPosition: Offset? by mutableStateOf(null)

    /**
     * How much of the tilt is applied: 1 while pressed, unwinding to 0 on release along
     * [tiltReturn]. The node keeps no timing logic of its own — that lives in the function, where
     * it can be tested without a renderer.
     */
    private var amount by mutableFloatStateOf(0f)

    private var returning: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        returning?.cancel()
                        pressPosition = interaction.pressPosition
                        // Instant. Snapping rather than animating is the whole point.
                        amount = 1f
                        invalidatePlacement()
                    }

                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        returning?.cancel()
                        returning =
                            coroutineScope.launch {
                                val start = withFrameMillis { it }
                                var elapsed = 0L
                                while (elapsed < TOTAL_RETURN_MILLIS) {
                                    elapsed = withFrameMillis { it } - start
                                    amount = tiltReturn(elapsed)
                                    invalidatePlacement()
                                }
                                pressPosition = null
                                invalidatePlacement()
                            }
                    }

                    else -> {
                        Unit
                    }
                }
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Read inside the placement block on purpose: a press then re-runs placement only,
            // never measurement or composition.
            val position = pressPosition
            val amount = amount
            if (position == null || amount == 0f) {
                placeable.place(0, 0)
            } else {
                val tilt =
                    tiltFor(
                        position,
                        Size(placeable.width.toFloat(), placeable.height.toFloat()),
                        maxDepression,
                    )
                val depthPx = this@TiltNode.cameraDistance.toPx()
                placeable.placeWithLayer(0, 0) {
                    this.cameraDistance = kvadrantCameraUnits(this@TiltNode.cameraDistance)
                    // Both negated. `tiltFor` reproduces `TiltEffect.cs` exactly, and Silverlight's
                    // `PlaneProjection` turns the opposite way from `graphicsLayer` on both axes:
                    // applied as written, the corner under the finger comes *towards* the eye, which
                    // is a button popping out instead of being pressed in.
                    //
                    // Verified on the edge-press goldens, not the corner ones. A corner press turns
                    // about both axes at once, and the X rotation changes the height of the side
                    // edges enough to swamp what the Y rotation does to them — measuring a corner
                    // press said the horizontal axis was fine when it was inverted.
                    //
                    // Everything scales by the same amount, so the plane returns along the path it
                    // arrived on rather than unwinding one axis at a time.
                    rotationX = -tilt.rotationX * amount
                    rotationY = -tilt.rotationY * amount
                    val sink = depressionScale(tilt.depression.toPx() * amount, depthPx)
                    scaleX = sink
                    scaleY = sink
                }
            }
        }
    }
}

private const val TOTAL_RETURN_MILLIS =
    Tilt.RETURN_DELAY_MILLIS.toLong() + Tilt.RETURN_DURATION_MILLIS.toLong()
