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
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import io.github.youndie.kvadrant.foundation.KvadrantCamera
import io.github.youndie.kvadrant.foundation.KvadrantHomography
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.theme.KvadrantMetrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Metro's press feedback, in the slot Material fills with a ripple — and, since B-40, the keyboard
 * focus ring that goes in the same slot.
 *
 * Both live here for the reason the tilt does: `LocalIndication` is one place, and a component that
 * has to remember to apply something is a component that will one day forget. Every
 * `clickable`/`toggleable`/`selectable` in the library got the ring the moment this drew it, and
 * nothing had to be edited to receive it. See [drawKvadrantFocusRing] for what is drawn and
 * [TiltNode.draw] for when.
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
    private val animatePress: Boolean = false,
    private val focusRingThickness: Dp = KvadrantMetrics().focusRingThickness,
    private val sharedCamera: Boolean = false,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        TiltNode(
            interactionSource,
            cameraDistance,
            maxDepression,
            animatePress,
            focusRingThickness,
            sharedCamera,
        )

    override fun equals(other: Any?): Boolean =
        other is TiltIndication &&
            other.cameraDistance == cameraDistance &&
            other.maxDepression == maxDepression &&
            other.animatePress == animatePress &&
            other.focusRingThickness == focusRingThickness &&
            other.sharedCamera == sharedCamera

    override fun hashCode(): Int =
        31 * (
            31 * (
                31 * (31 * cameraDistance.hashCode() + maxDepression.hashCode()) +
                    animatePress.hashCode()
            ) + focusRingThickness.hashCode()
        ) + sharedCamera.hashCode()

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
    private val animatePress: Boolean,
    private val focusRingThickness: Dp,
    private val sharedCamera: Boolean,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    /** Where the finger is, or was. Kept after release so the plane has somewhere to return from. */
    private var pressPosition: Offset? by mutableStateOf(null)

    /**
     * How much of the tilt is applied: 1 while pressed, unwinding to 0 on release along
     * [tiltReturn]. The node keeps no timing logic of its own — that lives in the function, where
     * it can be tested without a renderer.
     */
    private var amount by mutableFloatStateOf(0f)

    private var returning: Job? = null
    private var pressing: Job? = null

    /** Whether the focus ring is owed. Whether it is *drawn* also depends on the input mode. */
    private var focused by mutableStateOf(false)

    /**
     * **Read from the focus system, not from the interaction stream, and that is not a preference.**
     *
     * `FocusInteraction.Focus` travels down the same `InteractionSource` as the presses above, and
     * collecting it here looks like the obvious symmetry. It loses the event: `interactions` is a
     * hot flow with no replay, and `clickable` builds its indication node **lazily, on the first
     * interaction** — so on a mouse click, where focus is the first interaction there is, the node
     * attaches in response to an event it then never receives. Tab happens to arrive early enough
     * to be seen, so the flow version worked for the keyboard and silently did nothing for the
     * mouse. It passed this file's own tests, both of them, for a fortnight of a morning: the
     * keyboard test drew its ring, and the mouse test asserted *nothing was drawn* and got it for
     * free. Removing the input-mode gate below left both green, which is what said the ring was
     * never wired at all.
     *
     * `onFocusEvent` reports the state rather than a change to it, so there is nothing to miss.
     */
    override fun onFocusEvent(focusState: FocusState) {
        focused = focusState.isFocused
        invalidateDraw()
    }

    /**
     * **There is no shared camera here, and B-26 concluded there should be.** The conclusion was
     * wrong and this paragraph replaced it.
     *
     * `Modifier.graphicsLayer` uses `transformOrigin` for **two** things at once: the centre of the
     * perspective projection *and* the pivot the rotation turns about. Moving it to the screen's
     * centre — which is what "one camera over the display" sounds like — therefore moves the pivot
     * as well, and an element away from the middle stops leaning and starts swinging. Measured on a
     * 60 dp bar pressed at the same point in its own coordinates: **65 px tall at the centre of the
     * screen, 84 px at the top**. It reached a device as a push notification pressed far too hard.
     *
     * The evidence that argued for the change did not survive it either. The comparison fixture
     * rotated **nine tiles at once**, and the shared version bent them into a single sheet that
     * looked unmistakably like Metro — but a press does not rotate nine tiles. It rotates one. The
     * sheet was the fixture's own construction.
     *
     * A real shared camera needs the projection centre and the rotation pivot to be different
     * points, which one `graphicsLayer` cannot express. That is the shape of the work, and it is
     * back in B-26.
     */

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        returning?.cancel()
                        pressing?.cancel()
                        pressPosition = interaction.pressPosition
                        if (!animatePress) {
                            // Canon. `TiltEffect.cs` holds one storyboard and it is the *return*
                            // one; the press sets the properties outright on every manipulation
                            // delta. Snapping rather than animating is the whole point.
                            amount = 1f
                            invalidatePlacement()
                        } else {
                            pressing =
                                coroutineScope.launch {
                                    val start = withFrameMillis { it }
                                    var elapsed = 0L
                                    while (elapsed < PRESS_MILLIS) {
                                        elapsed = withFrameMillis { it } - start
                                        amount = (elapsed.toFloat() / PRESS_MILLIS).coerceIn(0f, 1f)
                                        invalidatePlacement()
                                    }
                                    amount = 1f
                                    invalidatePlacement()
                                }
                        }
                    }

                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        pressing?.cancel()
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

                    else -> {}
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
            if (position == null || amount == 0f || sharedCamera) {
                // Under the shared camera the whole transform is a canvas concatenation in `draw`,
                // because the projection centre and the rotation pivot have to be different points
                // and one `graphicsLayer` cannot express that — B-26, measured twice.
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

    /**
     * One camera over the whole display, which is what the phone had and what this is not by
     * default.
     *
     * [B-26](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-26-per-layer-camera-versus-a-global-one.md)
     * asked which of the two the tilt should use and was answered by pressing tiles on a phone with
     * both, through **this** code rather than through a fixture: a copy of the tilt built beside the
     * real one is a different tilt — its press timing, its unwind and `kvadrantTilt`'s gesture would
     * all be the fixture's — and the judgement would have been about the fixture. The two previous
     * attempts at the item were made from stills of things a press never does, and one of them
     * shipped and was reported from a device within a day.
     *
     * The answer was to keep the per-layer camera. So this is off, and it is a parameter rather than
     * a deleted branch because the *default* is the deviation here: research §1.6 records that the
     * original had one camera over the screen, and a library whose default departs from that owes a
     * reader the other option and a way to look at it. `sample`'s settings page has the switch.
     *
     * The eye is the root's centre expressed in this element's coordinates, which is the whole of
     * the difference between the two cameras: at `Offset.Zero` the homography and
     * `Modifier.graphicsLayer` describe the same camera and `KvadrantHomographyTest` requires them
     * to draw the same shape.
     *
     * The sink goes **inside** the concatenation. It is a shrink in the element's own plane, and
     * `graphicsLayer` applies scale before the projection; applying it outside would scale the
     * projected quad instead, which is a different picture and a plausible-looking one.
     */
    private fun ContentDrawScope.drawUnderSharedCamera() {
        val position = pressPosition
        val amount = amount
        if (position == null || amount == 0f) {
            drawContent()
            return
        }
        val tilt = tiltFor(position, size, maxDepression)
        val coordinates = requireLayoutCoordinates()
        val root = coordinates.findRootCoordinates()
        val topLeft = coordinates.positionInRoot()
        val eye =
            Offset(
                x = root.size.width / 2f - (topLeft.x + size.width / 2f),
                y = root.size.height / 2f - (topLeft.y + size.height / 2f),
            )
        val quad =
            KvadrantHomography.quadUnderCamera(
                size = size,
                rotationXDegrees = -tilt.rotationX * amount,
                rotationYDegrees = -tilt.rotationY * amount,
                cameraDistance = this@TiltNode.cameraDistance.toPx(),
                eye = eye,
            )
        val sink = depressionScale(tilt.depression.toPx() * amount, this@TiltNode.cameraDistance.toPx())
        drawContext.canvas.save()
        drawContext.canvas.concat(KvadrantHomography.homographyFromRect(size, quad))
        scale(sink, sink) { this@drawUnderSharedCamera.drawContent() }
        drawContext.canvas.restore()
    }

    /**
     * The ring, and the one condition on it: **the keyboard has to be the thing being used.**
     *
     * Windows 8's template draws the dotted rectangle from a `Focused` visual state and leaves
     * `PointerFocused` empty, so a button that a mouse gave focus to showed nothing. Compose has no
     * such pair of states, but it has the fact behind them: `InputModeManager` moves to
     * [InputMode.Touch] on a pointer event and back to [InputMode.Keyboard] on a key. Reading it
     * here is that template's condition in this framework's vocabulary.
     *
     * **Measured on desktop and nowhere else.** The mode's movement was probed there; Android is
     * covered by the paragraph below rather than by the mode; and on wasm nothing has been run,
     * because `wasmJsBrowserTest` is skipped in this repository (research D14). The browser is where
     * the documentation site runs, so that is a gap worth naming rather than a target worth
     * assuming.
     *
     * **Without it the ring is not a small deviation, it is a permanent one**: on desktop and in the
     * browser `Modifier.clickable` requests focus on click — `isRequestFocusOnClickEnabled` is
     * hard-coded `true` in the desktop source set — so every tile a mouse ever touched would keep a
     * dotted rectangle around it until something else was clicked, on a library whose subject never
     * had one. Android is not exposed to that: its input mode starts at [InputMode.Touch] and
     * `clickable` does not take focus there at all.
     *
     * Drawn after the content and outside the tilt, which is a placement rather than an accident:
     * this node's own drawing is in its own coordinates, while the lean is applied to the child in
     * placement, so the ring stays still while the surface leans under it. That is also how the
     * template has it — `FocusVisualWhite` is a sibling of the animated `Border`, not a child.
     */
    override fun ContentDrawScope.draw() {
        if (sharedCamera) drawUnderSharedCamera() else drawContent()
        if (!focused) return
        if (currentValueOf(LocalInputModeManager).inputMode != InputMode.Keyboard) return
        drawKvadrantFocusRing(focusRingThickness.toPx())
    }
}

private const val TOTAL_RETURN_MILLIS =
    Tilt.RETURN_DELAY_MILLIS.toLong() + Tilt.RETURN_DURATION_MILLIS.toLong()

/**
 * How long a remastered press takes to sink, and **this project's number** in both senses.
 *
 * The phone had no such animation at all — `TiltEffect.cs` carries one storyboard and it is the
 * return — so this is not a duration Microsoft chose too slowly, it is a duration nobody chose. It
 * matches the return's own `TiltReturnAnimationDuration`, on the argument that a movement and its
 * reverse should cost the same; that argument is mine and is the only thing behind the number.
 */
private const val PRESS_MILLIS = 100L
