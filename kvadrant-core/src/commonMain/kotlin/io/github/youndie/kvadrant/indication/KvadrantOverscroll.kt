package io.github.youndie.kvadrant.indication

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
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
 * The end of a list, the way Windows Phone ended one: the content **compresses** towards the edge
 * the finger is pushing against, and springs back when it lets go.
 *
 * **This is the same argument as the tilt, one line further down `KvadrantTheme`.** That theme
 * replaces `LocalIndication` because a Metro surface that ripples is not a Metro surface; it has
 * until now left `LocalOverscrollFactory` alone, so on Android a Metro list ended with Android's
 * stretch. A finger meets this every time a list runs out, which makes it about as frequent as a
 * press and about as recognisable.
 *
 * **Compression is Microsoft's word, not a description chosen here.** Windows Phone 7.1 added
 * `HorizontalCompression` and `VerticalCompression` visual state groups to `ScrollViewer` so that an
 * application could react to the end of a scroll. What it did *not* publish is any of the numbers:
 * how far the content compresses, how the resistance builds, how deep a *fling* into the end goes,
 * or how it returns. Those four are therefore **this project's own**, they are parameters rather
 * than constants, and they are named as ours here — the same rule research §1.10 applies to the
 * panorama's peek.
 *
 * It is a scale rather than a translation, and that distinction is the whole of the effect. A
 * translation is what iOS does — the content slides away from the edge and leaves a gap. Compression
 * keeps the edge where it is and squeezes what is behind it, so the boundary never moves and nothing
 * empty appears.
 *
 * @param maxCompression how much of its own length the content gives up at the limit, as a fraction.
 *   **Ours.** 0.06 is a twentieth, which reads as give rather than as a bounce.
 * @param resistance how much finger travel it takes to reach that limit, as a multiple of the
 *   viewport. **Ours.** Below 1 the effect is reached before the finger has crossed the screen.
 * @param flingReference the speed, in viewports per second, at which a fling arriving at the end
 *   spends about two thirds of [maxCompression]. **Ours**, and the shape around it is a choice too:
 *   see [absorb].
 */
public class KvadrantOverscroll(
    private val scope: CoroutineScope,
    private val maxCompression: Float = DEFAULT_MAX_COMPRESSION,
    private val resistance: Float = DEFAULT_RESISTANCE,
    private val flingReference: Float = DEFAULT_FLING_REFERENCE,
) : OverscrollEffect {
    /**
     * Signed, in fractions of the viewport: negative at the start edge, positive at the end.
     *
     * **A plain field rather than an `Animatable`, and the first version was the latter.** A drag is
     * synchronous — `applyToScroll` is called and the next frame is drawn — while `Animatable.snapTo`
     * is suspending, so updating through `scope.launch` meant the value the draw pass read was the
     * one from before the gesture. Nothing compressed, and the test said so. Animation belongs only
     * to the release, which genuinely is a coroutine.
     */
    private var compression = 0f

    private var horizontal = false

    override val isInProgress: Boolean
        get() = compression != 0f

    override val node: DelegatableNode = CompressionNode()

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
        val unwinding = compression != 0f && sign(axisDelta) != sign(compression)
        val consumedByUnwinding =
            if (unwinding) {
                val room = abs(compression) * viewport * resistance
                val used = minOf(abs(axisDelta), room) * sign(axisDelta)
                setCompression(compression - used / (viewport * resistance))
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
                setCompression(compression + leftoverAxis / (viewport * resistance))
            }
        }
        return consumedByUnwinding + consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val consumed = performFling(velocity)
        // What the scroller could not spend hits the stop, and this is where it becomes compression.
        // **These two lines used to be one comment saying so and no code doing it** ([B-45](
        // https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-45-overscroll-ignores-the-fling.md)):
        // the release ran on a compression of zero and returned at once, so a list flung into its
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
     * than animations. The compression was never a template animation: it is the scroll engine's own
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
        setCompression(compression + maxCompression * (1f - exp(-speed / flingReference)) * sign(axis))
    }

    private suspend fun performRelease() {
        val from = compression
        if (from == 0f) return
        animate(from, 0f, animationSpec = tween(RELEASE_MILLIS, easing = KvadrantEasing.ExponentialOut6)) { value, _ ->
            setCompression(value)
        }
    }

    private fun setCompression(value: Float) {
        val clamped = value.coerceIn(-maxCompression, maxCompression)
        if (clamped == compression) return
        compression = clamped
        (node as CompressionNode).invalidateDraw()
    }

    private var viewport = 0f

    private inner class CompressionNode :
        Modifier.Node(),
        DrawModifierNode {
        override fun ContentDrawScope.draw() {
            viewport = if (horizontal) size.width else size.height
            val amount = compression
            if (amount == 0f) {
                drawContent()
                return
            }
            // The pivot is the edge being pushed against, so that edge stays put and the content
            // behind it gives. A pivot at the centre would move both edges, which is a zoom.
            //
            // **The sign is the finger's, not the edge's**, and the first version had it the other
            // way round. A finger travelling *up* at the end of a list leaves a negative delta and
            // is pushing against the **bottom**; the measurement said so — the bottom edge rose and
            // the top stayed, which is a list compressing away from the boundary it is resting on.
            val pivot =
                when {
                    horizontal && amount < 0f -> Offset(size.width, size.height / 2f)
                    horizontal -> Offset(0f, size.height / 2f)
                    amount < 0f -> Offset(size.width / 2f, size.height)
                    else -> Offset(size.width / 2f, 0f)
                }
            val factor = 1f - abs(amount)
            scale(
                scaleX = if (horizontal) factor else 1f,
                scaleY = if (horizontal) 1f else factor,
                pivot = pivot,
            ) {
                this@draw.drawContent()
            }
        }
    }

    public companion object {
        /** **Ours.** A twentieth of the viewport, which reads as give rather than as a bounce. */
        public const val DEFAULT_MAX_COMPRESSION: Float = 0.06f

        /** **Ours.** A full viewport of travel to reach the limit. */
        public const val DEFAULT_RESISTANCE: Float = 1f

        /**
         * **Ours**, and set from the range flings actually arrive in rather than from taste.
         *
         * A thumb produces roughly 3 000 to 12 000 px/s on a phone, which over a viewport of about
         * 2 200 px is **1.4 to 5.5 viewports per second**. A reference in the middle of that spreads
         * an ordinary flick across the spring — a gentle one spends about a third of it, a hard one
         * about three quarters — where the first value, a viewport and a half, put everything above
         * a lazy flick at the limit. That is what "it starts at maximum" was: not the timing, the
         * saturation.
         *
         * The curve is `1 − e^(−v ⁄ reference)`, so nothing ever spends *all* of the spring and no
         * clamp is ever reached from below.
         */
        public const val DEFAULT_FLING_REFERENCE: Float = 4f

        /** **Ours.** The phone's ordinary settle, as the panorama's is. */
        public const val RELEASE_MILLIS: Int = 300
    }
}

/** Hands `KvadrantTheme` an effect per scrolling surface, the way an indication is handed out. */
public class KvadrantOverscrollFactory(
    private val scope: CoroutineScope,
    private val maxCompression: Float = KvadrantOverscroll.DEFAULT_MAX_COMPRESSION,
    private val resistance: Float = KvadrantOverscroll.DEFAULT_RESISTANCE,
    private val flingReference: Float = KvadrantOverscroll.DEFAULT_FLING_REFERENCE,
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect =
        KvadrantOverscroll(scope, maxCompression, resistance, flingReference)

    override fun equals(other: Any?): Boolean =
        other is KvadrantOverscrollFactory &&
            other.maxCompression == maxCompression &&
            other.resistance == resistance &&
            other.flingReference == flingReference

    override fun hashCode(): Int =
        31 * (31 * maxCompression.hashCode() + resistance.hashCode()) + flingReference.hashCode()
}
