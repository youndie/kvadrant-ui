package io.github.youndie.kvadrant.indication

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Velocity
import io.github.youndie.kvadrant.theme.KvadrantEasing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

/**
 * The end of a list, the way Windows Phone ended one: the content **follows the finger past the
 * boundary**, showing the page behind it, and rubber-bands back when it lets go.
 *
 * **This was a scale until it was read properly, and the correction is the most useful thing in this
 * file.** The visual states Microsoft added to `ScrollViewer` in Windows Phone 7.1 are named
 * `HorizontalCompression` and `VerticalCompression`, and the whole of the first implementation was
 * built on that word: the content was squeezed towards the edge being pushed, the boundary held
 * still, and nothing empty ever appeared. The design guidelines say the opposite, in a sentence
 * nobody had gone looking for:
 *
 * ```
 * When the end of the list is reached, it will then scroll up to display the empty section
 * and "rubber band" back to rest in place. Flicking at the end of the list causes it to
 * rubber band back; the list won't wrap to the beginning.
 *                                    — LongListSelector design guidelines, jj735577
 * ```
 *
 * "Display the empty section" settles it: a squeeze cannot produce one, because a squeeze is defined
 * by the boundary not moving. The word "compression" describes the **manipulation** — the scroll
 * being damped past its end — and not the pixels, which is also why those visual states carry no
 * storyboard: they are there so an application can notice the damping, not so it can draw it.
 *
 * **A name is not a description, and this cost three rounds of tuning something that was the wrong
 * effect.** The class keeps its name because research, [B-38](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-38-the-theme-leaves-the-platform-overscroll.md)
 * and [B-45](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-45-overscroll-ignores-the-fling.md)
 * all cite it; the parameters do not, because a parameter called `maxCompression` that sets a
 * translation distance is a claim in a signature.
 *
 * **This is the same argument as the tilt, one line further down `KvadrantTheme`.** That theme
 * replaces `LocalIndication` because a Metro surface that ripples is not a Metro surface; it had
 * until B-38 left `LocalOverscrollFactory` alone, so on Android a Metro list ended with Android's
 * stretch. A finger meets this every time a list runs out, which makes it about as frequent as a
 * press and about as recognisable.
 *
 * **What Microsoft published is that it happens, not how far or how fast.** The states are documented
 * and none of their numbers are, so how far the content travels, how the resistance builds and how
 * it returns are **this project's own**: parameters rather than constants, named as ours here, the
 * same rule research §1.10 applies to the panorama's peek.
 *
 * @param maxOffset how far the content travels past the boundary at the limit, as a fraction of the
 *   viewport. **Ours.**
 * @param resistance how much finger travel it takes to reach that limit, as a multiple of the
 *   viewport. **Ours**, and the strongest lever on how the effect feels — see
 *   [KvadrantOverscroll.DEFAULT_RESISTANCE], which was raised fourfold once a diagram showed a flick
 *   spending the whole of it in two frames.
 * @param flingReference the speed, in viewports per second, at which a fling arriving at the end
 *   spends about two thirds of what is available. **Ours**; see [DEFAULT_FLING_REFERENCE].
 */
public class KvadrantOverscroll(
    private val scope: CoroutineScope,
    private val maxOffset: Float = DEFAULT_MAX_OFFSET,
    private val resistance: Float = DEFAULT_RESISTANCE,
    private val flingReference: Float = DEFAULT_FLING_REFERENCE,
) : OverscrollEffect {
    /**
     * How far the content has travelled past the boundary, in fractions of the viewport, signed:
     * negative when the finger is pushing against the end, positive against the start.
     *
     * **A plain field rather than an `Animatable`, and the first version was the latter.** A drag is
     * synchronous — `applyToScroll` is called and the next frame is drawn — while `Animatable.snapTo`
     * is suspending, so updating through `scope.launch` meant the value the draw pass read was the
     * one from before the gesture. Nothing compressed, and the test said so. Animation belongs only
     * to the release, which genuinely is a coroutine.
     */
    private var displacement = 0f

    private var horizontal = false

    override val isInProgress: Boolean
        get() = displacement != 0f

    override val node: DelegatableNode = DisplacementNode()

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        val axisDelta = if (abs(delta.x) > abs(delta.y)) delta.x else delta.y
        horizontal = abs(delta.x) > abs(delta.y)

        // A finger moving back towards the content unwinds what it compressed before any of it
        // reaches the scroller. Without this the list starts moving while still visibly squashed,
        // which is the one thing a spring must not do.
        val unwinding = displacement != 0f && sign(axisDelta) != sign(displacement)
        val consumedByUnwinding =
            if (unwinding) {
                val room = abs(displacement) * viewport * resistance
                val used = minOf(abs(axisDelta), room) * sign(axisDelta)
                setDisplacement(displacement - used / (viewport * resistance))
                if (horizontal) Offset(used, 0f) else Offset(0f, used)
            } else {
                Offset.Zero
            }

        val left = delta - consumedByUnwinding
        val consumedByScroll = performScroll(left)
        val leftover = left - consumedByScroll

        // **`UserInput` only, and that condition survived B-45 rather than causing it.** A fling's
        // deltas arrive as a `SideEffect`, so this drops them — which is right: absorbing a
        // decelerating fling frame by frame would make the depth a function of the frame rate, and
        // `SideEffect` also carries a nested scroll's propagation. What the fling leaves at the wall
        // is a *velocity*, and it is absorbed once, in [applyToFling].
        if (source == NestedScrollSource.UserInput && viewport > 0f) {
            val leftoverAxis = if (horizontal) leftover.x else leftover.y
            if (leftoverAxis != 0f) {
                setDisplacement(displacement + leftoverAxis / (viewport * resistance))
            }
        }
        return consumedByUnwinding + consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val consumed = performFling(velocity)
        // What the scroller could not spend hits the stop, and this is where it becomes displacement.
        // **These two lines used to be one comment saying so and no code doing it** ([B-45](
        // https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-45-overscroll-ignores-the-fling.md)):
        // the release ran on a displacement of zero and returned at once, so a list flung into its
        // end did nothing while a list dragged into it compressed — and a list is flung into its end
        // far more often than it is dragged into one.
        absorb(velocity - consumed)
        // The release curve is the theme's own exponential-out, which every other Metro settle uses.
        performRelease()
    }

    /**
     * The velocity left at the stop, turned into a depth — **at once**.
     *
     * **The visual states are empty, and that is what settles the shape.** A Windows Phone
     * `ScrollViewer` template declares `NoVerticalCompression`, `CompressionTop` and
     * `CompressionBottom` and gives all three *no storyboard at all* — the same shape `PivotItem`'s
     * `Left`/`Center`/`Right` have, which research §1.11 records as markers the code reads rather
     * than animations. The displacement was never a template animation: it is the scroll engine's own
     * displacement, and the states exist so an application can **notice** it, which is what the
     * pull-to-refresh recipes of the day used them for.
     *
     * So there is nothing to play after the list stops. The squeeze belongs to the arrival, and the
     * arrival is over: the spring's whole range is a few per cent of a viewport, and a fling carries
     * enough momentum to cross it in a frame or two whatever model runs it. Two versions tried
     * otherwise and both were reported: one animated the depth in over the release's own 300 ms,
     * which put the growth *after* the stop and read as a bounce; the other ran the leftover
     * velocity through a decay, which is the honest physics and lands at the limit in one frame at
     * any real speed, because the distance a throw would carry a list is hundreds of pixels and the
     * spring is worth tens.
     *
     * **What is left to get right is therefore the depth, not the timing** — and the depth was
     * pinned. [flingReference] decides how much force spends the whole spring, and at a viewport and
     * a half per second every ordinary flick saturated it, which is what "it starts at maximum"
     * means. Its value is now set from the range flings actually arrive in; see
     * [DEFAULT_FLING_REFERENCE].
     */
    private fun absorb(leftover: Velocity) {
        if (viewport <= 0f) return
        // The axis is the one the drag that threw this list was on, not the velocity's own. A fling
        // is the end of a gesture, so the two agree; deriving it here instead would let a stray
        // sideways component flip the axis after `viewport` had been measured along the other one.
        val axis = if (horizontal) leftover.x else leftover.y
        if (axis == 0f) return
        val speed = abs(axis) / viewport
        // **The square, and the flatness it fixes was reported.** `1 − e^(−v ⁄ r)` is steepest at
        // zero, so a barely-thrown list got most of what a hard one did: at a reference of eight,
        // one viewport a second spent 12 % of the spring and three spent 31 %, a factor of 2.6 for a
        // factor of three in speed. Squaring the argument suppresses the low end and leaves the top
        // where it was — 4 % against 31 %, a factor of seven and a half — which is the separation a
        // hand expects between a nudge and a throw.
        val thrown = maxOffset * (1f - exp(-(speed / flingReference) * (speed / flingReference)))
        // **The deeper of the two, not the sum, and this was a jump.** A gesture that drags past the
        // end and lets go arrives here with the finger's own displacement already in `displacement`
        // and the finger's own velocity as the leftover — so adding them squeezed further at the
        // instant of release, which is the moment a spring should start coming back. Reported as a
        // small throw jumping.
        //
        // The quantity is how far past the boundary the content is. A throw carries it as far as its
        // momentum would; if the finger already carried it further, the momentum adds nothing.
        setDisplacement(maxOf(abs(displacement), thrown) * sign(axis))
    }

    private suspend fun performRelease() {
        val from = displacement
        if (from == 0f) return
        animate(from, 0f, animationSpec = tween(RELEASE_MILLIS, easing = KvadrantEasing.ExponentialOut6)) { value, _ ->
            setDisplacement(value)
        }
    }

    private fun setDisplacement(value: Float) {
        val clamped = value.coerceIn(-maxOffset, maxOffset)
        if (clamped == displacement) return
        displacement = clamped
        (node as DisplacementNode).invalidateDraw()
    }

    private var viewport = 0f

    private inner class DisplacementNode :
        Modifier.Node(),
        DrawModifierNode {
        override fun ContentDrawScope.draw() {
            viewport = if (horizontal) size.width else size.height
            val amount = displacement
            if (amount == 0f) {
                drawContent()
                return
            }
            // **A translation, and this used to be a scale.** The content follows the finger past
            // the boundary and leaves the page showing behind it; the guidelines say so in as many
            // words — "it will then scroll up to display the empty section and rubber band back to
            // rest in place". A scale keeps the edge where it is and squeezes what is behind it,
            // which is the one thing that cannot produce an empty section, and it was built on the
            // name of a visual state rather than on that sentence. [KvadrantOverscroll]'s own doc
            // carries the correction.
            //
            // The sign is the finger's: a finger travelling *up* at the end of a list leaves a
            // negative delta and drags the content up with it, so the gap opens at the bottom.
            val offset = amount * viewport
            translate(
                left = if (horizontal) offset else 0f,
                top = if (horizontal) 0f else offset,
            ) {
                this@draw.drawContent()
            }
        }
    }

    public companion object {
        /**
         * **Ours.** A twentieth of the viewport of travel past the boundary, which is give rather
         * than a bounce.
         *
         * It was the depth of a squeeze until the effect turned out to be a translation, and the
         * number came through unchanged: six per cent of a viewport is 132 px on a phone, which is
         * a plausible pull either way. That it survived a change of meaning is a reason to distrust
         * it rather than to trust it — nobody has looked at this one on a device since it became a
         * distance.
         */
        public const val DEFAULT_MAX_OFFSET: Float = 0.06f

        /**
         * **Ours**, and the number that decides how strong the whole effect feels — which took a
         * space-time diagram to find, after two other knobs had been turned for it.
         *
         * It is how much finger travel past the end spends the spring, as a multiple of the
         * viewport. At **1** the limit is `maxOffset` of a viewport of over-travel — 132 px on
         * a phone — and a finger moving at six thousand pixels a second covers 96 of those **in one
         * frame**. So any flick that ended past the boundary was at the limit within two frames,
         * and every report that the effect was too strong was about this, not about the throw: the
         * fling's own curve is only consulted for whatever the drag has not already spent, and the
         * drag had spent all of it.
         *
         * `OverscrollFlingTimelineTest` is what showed it. The boundary sat at 18 px of a possible
         * 18 for the whole time the finger was past the stop, at both of the speeds being compared.
         *
         * At 4 the same 132 px of over-travel spends a quarter of the spring and the limit needs
         * about 530, which is a deliberate push rather than the tail of a flick.
         */
        public const val DEFAULT_RESISTANCE: Float = 4f

        /**
         * **Ours**, and the one number in this class that was set by somebody looking at a phone.
         *
         * What it does is decide how much of the spring a given arrival speed spends, through
         * `1 − e^(−(v ⁄ reference)²)`. The *range* it has to cover is measured: a thumb produces
         * roughly 3 000 to 12 000 px/s, which over a viewport of about 2 200 px is **1.4 to 5.5
         * viewports per second**. What the range cannot decide is the amplitude, and two reports
         * from a device did: at 4 with no square a throw was too deep at every speed; at 8 the hard
         * end was right and a small throw still jumped, which is a curve too flat to tell a nudge
         * from a throw. The square is that separation and 5 keeps the hard end where it was.
         *
         * **It is deliberately the fling's own number rather than [maxOffset].** That one is
         * shared with the drag, which nobody has complained about; lowering it to fix a throw would
         * change a gesture that was already right. When a report says a throw is too strong at every
         * speed, this is the knob — and if a *drag* held past the end is ever too deep, that is the
         * other one.
         *
         * The curve is `1 − e^(−v ⁄ reference)`, so nothing ever spends *all* of the spring and no
         * clamp is ever reached from below.
         */
        public const val DEFAULT_FLING_REFERENCE: Float = 5f

        /** **Ours.** The phone's ordinary settle, as the panorama's is. */
        public const val RELEASE_MILLIS: Int = 300
    }
}

/** Hands `KvadrantTheme` an effect per scrolling surface, the way an indication is handed out. */
public class KvadrantOverscrollFactory(
    private val scope: CoroutineScope,
    private val maxOffset: Float = KvadrantOverscroll.DEFAULT_MAX_OFFSET,
    private val resistance: Float = KvadrantOverscroll.DEFAULT_RESISTANCE,
    private val flingReference: Float = KvadrantOverscroll.DEFAULT_FLING_REFERENCE,
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect =
        KvadrantOverscroll(scope, maxOffset, resistance, flingReference)

    override fun equals(other: Any?): Boolean =
        other is KvadrantOverscrollFactory &&
            other.maxOffset == maxOffset &&
            other.resistance == resistance &&
            other.flingReference == flingReference

    override fun hashCode(): Int = 31 * (31 * maxOffset.hashCode() + resistance.hashCode()) + flingReference.hashCode()
}
