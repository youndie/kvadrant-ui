package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A single surface wider than the screen, panned sideways, with a title too big to fit and a
 * background that drifts behind it.
 *
 * The numbers are the ones in `Microsoft.Phone.dll`'s own template, and two of them correct a
 * long-standing confusion: the **panorama title** is 170 px `PhoneFontFamilyLight` at -35/1000 em
 * of tracking, while the **section header** is 66 px SemiLight. They are different elements two
 * levels apart, and treating the title as 72 px SemiLight — which is what the brief said — makes it
 * less than half the size it should be.
 *
 * **The parallax rates are derived, not chosen.** Each layer has more width than the viewport and
 * has to finish traversing its own overflow exactly when the content finishes traversing its own.
 * So a layer's rate is its overflow divided by the content's, which is what the original's pair of
 * `TranslateTransform`s does and the reason the effect holds at any number of sections.
 *
 * **It wraps, and wrapping is what makes the parallax rates derivable.** Past the last section
 * comes the first, as the original does with its `LeftWraparound` and `RightWraparound` borders:
 * the sections are laid out twice and the scroll position is folded back by one copy's width
 * whenever it crosses into the second.
 *
 * That fold is invisible only for a layer that is **periodic with the same fold**. This used to
 * offset each layer by `scroll * rate` with the layer drawn once, so at every fold the background
 * snapped sideways by `copyWidth * rate` — a distance that is a period of nothing. The paragraph
 * here claimed "never a jump to see" and there was one, on every wrap; the title escaped notice
 * only because a title narrower than the viewport had its rate coerced to zero and did not move at
 * all.
 *
 * The correct model is a set of **cylinders**. Scrolling one content copy has to advance each layer
 * by exactly one of its own circumferences, so a layer's rate is *its own period* over the content's
 * — not a coefficient anybody gets to pick. Each layer is drawn twice and offset modulo its period,
 * and the fold then lands on a seam that is identical to where it started.
 *
 * **The title is the one layer that is not a cylinder**, because the original says so: it "does not
 * repeat itself when you pan past the edges of the content", and instead leaves and re-enters at the
 * selection change that crosses the edge. So it is drawn once, travels its own overflow exactly once
 * across a copy, and the seam it would otherwise have is the transition — `titleTransition`, and
 * [B-33](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-33-panorama-is-a-scroller-not-an-item-model.md).
 */
@Composable
public fun KvadrantPanorama(
    title: String,
    modifier: Modifier = Modifier,
    cyrillic: FontFamily? = null,
    scroll: ScrollState = rememberScrollState(),
    background: @Composable (Modifier) -> Unit = {},
    sections: List<Pair<String, @Composable () -> Unit>>,
) {
    var viewport by remember { mutableIntStateOf(0) }
    var titleWidth by remember { mutableIntStateOf(0) }
    // The title's own displacement during a wrap, on top of the travel below. Zero at every other
    // moment; see [titleTransition].
    var titleShift by remember { mutableFloatStateOf(0f) }
    var copyWidth by remember { mutableIntStateOf(0) }
    var backgroundWidth by remember { mutableIntStateOf(0) }
    // The left edge of every section in the first copy, which is where a release has to land.
    val sectionWidths = remember(sections.size) { mutableStateListOf(*Array(sections.size) { 0 }) }

    // A layer of period `period` advances by exactly one period per content copy, so it is where it
    // started whenever the content is — which is the only arrangement in which the fold cannot be
    // seen. Zero period means the layer does not move, which is what a title narrower than the
    // viewport does.
    fun drift(period: Int): Int =
        if (period <= 0 || copyWidth <= 0) {
            0
        } else {
            -((scroll.value.toFloat() * period / copyWidth) % period).roundToInt()
        }

    // The title is the one layer that is **not** a cylinder, so it does not get [drift]. It travels
    // its own overflow exactly once across one copy of the content — left-aligned at the first
    // section, its right edge against the right of the viewport at the last — and what happens at
    // the seam is [titleTransition] rather than a period.
    fun titleTravelBase(): Float {
        val overflow = (titleWidth - viewport).coerceAtLeast(0)
        if (copyWidth <= 0) return 0f
        val within = (scroll.value % copyWidth).toFloat() / copyWidth
        return -within * overflow
    }

    fun titleTravel(): Int = (titleTravelBase() + titleShift).roundToInt()

    // The fold, and it folds **both ways**, which needs three copies rather than two.
    //
    // With two, the scroll rests in the first and there is nothing to its left: panning backwards
    // from the opening section hit a wall, so "no end to hit" was only true going forwards. Resting
    // in the *middle* copy leaves a whole one spare in each direction, and crossing out of it puts
    // the scroll at the same place in the middle again — invisible, because the copies are
    // identical and every drifting layer is periodic with the same copy.
    //
    // Only while the scroll is at rest. `ScrollState.scrollTo` takes the scroll's mutex and would
    // cancel a settle in flight, leaving the panorama stopped between two sections — and the spare
    // copy is there precisely so that nothing has to be folded mid-gesture.
    LaunchedEffect(copyWidth) {
        if (copyWidth <= 0) return@LaunchedEffect
        // `maxValue` is in the flow, and it has to be. `scrollTo` clamps, so the opening move into
        // the middle copy is silently swallowed while the content is still being measured — and
        // nothing then re-emits, because neither the value nor the gesture changed. The panorama
        // opened part-way through a section and stayed there, one copy from the end it could not
        // wrap past.
        // **The opening move is not a wrap and must not look like one.** Positioning the scroll
        // into the middle copy goes through this same branch, more than once while the content is
        // still being measured, and the title would fly in from the side of the screen every time
        // the panorama was composed. So nothing animates until the scroll has been observed *at*
        // home once; after that, a mismatch is a person having panned past the edge.
        var opened = false
        snapshotFlow { Triple(scroll.isScrollInProgress, scroll.value, scroll.maxValue) }
            .collect { (moving, value, limit) ->
                if (moving || limit < 2 * copyWidth) return@collect
                // Modulo rather than one subtraction: a single step gets it home from wherever it
                // is, including from a position left behind by an earlier, smaller measurement.
                val home = copyWidth + (((value - copyWidth) % copyWidth) + copyWidth) % copyWidth
                if (home == value) {
                    opened = true
                    return@collect
                }
                if (!opened) {
                    scroll.scrollTo(home)
                    return@collect
                }
                // Panned past the edge of the content, which is the one moment the title is not a
                // silent passenger. It leaves in the direction the pan was going and comes back
                // from the other side while the fold happens behind it, unseen.
                val forward = value > home
                titleTransition(
                    // Cleared exactly rather than by a generous constant: the title is somewhere in
                    // its own travel when the wrap happens, and a fixed distance either leaves a
                    // sliver on screen or overshoots so far that the whole return happens out of
                    // sight. One pixel past the edge on each side is what "out of view" means.
                    exitShift =
                        if (forward) {
                            -(titleTravelBase() + titleWidth + 1f)
                        } else {
                            viewport - titleTravelBase() + 1f
                        },
                    entryShift = {
                        if (forward) {
                            viewport - titleTravelBase() + 1f
                        } else {
                            -(titleTravelBase() + titleWidth + 1f)
                        }
                    },
                    set = { titleShift = it },
                    fold = { scroll.scrollTo(home) },
                )
            }
    }

    Box(modifier.fillMaxSize().onSizeChanged { viewport = it.width }) {
        // The background spans both rows in the original and drifts slowest of the three. **No
        // insets**: on the phone the image runs under the status bar and only the content is held
        // clear of it, so insetting the whole panorama — which the sample did — leaves a band of
        // page colour above an image that was supposed to reach the top of the glass.
        // `wrapContentWidth(unbounded = true)` is the whole of why this measured wrong. The same
        // trap the sections row carries a comment about, one layer up: inside a bounded Box a Row is
        // measured against the viewport, so `backgroundWidth` came back as the width of the screen
        // rather than of the background. The period was then the viewport, the two copies were laid
        // out one screen apart while each painted its full width over the other, and the seam that
        // produced is what a wrap looked like.
        Row(
            Modifier
                .fillMaxHeight()
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .offset { IntOffset(drift(backgroundWidth), 0) },
        ) {
            repeat(COPIES) { copy ->
                Box(Modifier.onSizeChanged { if (copy == 0) backgroundWidth = it.width }) {
                    background(Modifier.fillMaxHeight())
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
        ) {
            // Margin 10,-34,0,0: the title deliberately sits above the top of its row.
            //
            // **Drawn once**, unlike every other layer here, and that is the whole of what
            // `PanningTitleLayer` is: it "does not repeat itself when you pan past the edges of the
            // content. Instead, during a selection change between `PanoramaItem` controls, it
            // animates out of view in the direction it was previously moving and animates back into
            // the scene from the other side of the screen" (`ff941126`, quoted in full because the
            // reading matters — see [titleTransition]). This used to be a cylinder like the others,
            // a named deviation waiting on an item model; the snap gave it one
            // ([B-33](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-33-panorama-is-a-scroller-not-an-item-model.md)).
            //
            // `unbounded` for the reason the background layer carries a paragraph about: inside a
            // bounded Column a Row is measured against the viewport, so a title wider than the
            // screen reports the screen's width, its overflow comes out as zero, and the layer that
            // exists to be too big to fit sits perfectly still.
            Row(
                Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .offset { IntOffset(titleTravel(), 0) },
            ) {
                KvadrantText(
                    title,
                    Modifier
                        .onSizeChanged { titleWidth = it.width }
                        .padding(start = 7.5.dp),
                    KvadrantTheme.typography.panoramaTitle,
                    cyrillic,
                )
            }

            // No fillMaxWidth: a row inside a horizontal scroll must be free to exceed the viewport,
            // and constraining it is what makes a panorama scroll a few pixels instead of pages.
            //
            // The fling snaps. `ff941126`: a vertical `PanoramaItem` "will snap only to the left
            // side of the screen during a gesture movement", and the items layer "moves in a 1:1
            // fashion with the pan gesture; so whatever content is beneath the finger at the
            // beginning of the pan remains until the finger is lifted" — so the settling happens on
            // release and never during the drag, which is what a `FlingBehavior` is and what
            // reaching for a pager instead would have got wrong.
            Row(
                Modifier.horizontalScroll(
                    scroll,
                    flingBehavior =
                        remember(scroll) {
                            SectionSnap(scroll) {
                                stops(sectionWidths, viewport, copyWidth, scroll.maxValue)
                            }
                        },
                ),
            ) {
                // Twice: one copy to look at, one to wrap into.
                repeat(COPIES) { copy ->
                    Row(
                        Modifier.onSizeChanged { if (copy == 0) copyWidth = it.width },
                    ) {
                        sections.forEachIndexed { index, (header, body) ->
                            Column(
                                Modifier
                                    // **Before the padding, not after.** A section's stop is where
                                    // its header lands on the margin, so the width that matters
                                    // includes the 9 before it and the 18 after; measured inside
                                    // the padding the stops came out 27 short per section, and a
                                    // release settled a little further left each time.
                                    .onSizeChanged {
                                        if (copy == 0 && index < sectionWidths.size) {
                                            sectionWidths[index] = it.width
                                        }
                                    }.padding(start = 9.dp, end = 18.dp),
                            ) {
                                // Margin 12,-2,0,38 at Metro's pixels.
                                KvadrantText(
                                    header,
                                    Modifier.padding(start = 9.dp, bottom = 28.5.dp),
                                    KvadrantTheme.typography.panoramaSectionHeader,
                                    cyrillic,
                                )
                                body()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Every scroll position a release is allowed to settle on: the left edge of each section, in both
 * copies, plus the far end.
 *
 * Both copies, because a fling launched near the end of the first has to be able to land in the
 * second — the fold then moves it back and the seam is the one the wrap already hides.
 */
private fun stops(
    widths: List<Int>,
    viewport: Int,
    copyWidth: Int,
    maxValue: Int,
): List<Int> {
    if (widths.isEmpty() || widths.any { it <= 0 } || viewport <= 0) return emptyList()
    val starts = widths.runningFold(0) { acc, w -> acc + w }.dropLast(1)
    // A section wider than the screen gets a second stop: its right edge against the right of the
    // viewport. `ff941126` again — a horizontal `PanoramaItem` "will snap to both the left and the
    // right sides of the screen", and "allows for a user to pan around the center contents without
    // snapping to a new `PanoramaItem` control view". With only the left edge as a stop, a section
    // with content off the screen could not be looked at: a release inside it threw the panorama
    // past the whole thing to the next section, which is what the first version of this did. The
    // original made the orientation an explicit property; here it follows from the measurement,
    // because content that overflows the viewport is what that property was for.
    val withinOne =
        starts.flatMapIndexed { index, start ->
            if (widths[index] > viewport) listOf(start, start + widths[index] - viewport) else listOf(start)
        }
    return (0 until COPIES)
        .flatMap { copy -> withinOne.map { it + copy * copyWidth } }
        .plus(maxValue)
        .filter { it in 0..maxValue }
        .distinct()
        .sorted()
}

/**
 * Where a release settles: one of the **two stops the finger is between**, whichever the fling was
 * heading for.
 *
 * This used to take the nearest stop to the predicted end of the decay, on the argument that
 * clamping to one section per gesture "is a pager and not a panorama". The argument was invented and
 * the rule skipped sections: released between two narrow ones, a prediction landing slightly past
 * the second is nearer to the third, and a section goes by unseen. Bracketing is what stops that,
 * and it costs nothing that was ever specified — Microsoft published no fling model at all.
 *
 * The decay still decides *which* of the two, so a gentle release falls back and a firm one carries
 * on, and no velocity threshold has to be invented to tell them apart.
 */
private class SectionSnap(
    private val scroll: ScrollState,
    private val stops: () -> List<Int>,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val landings = stops()
        if (landings.size < 2) return initialVelocity
        val from = scroll.value.toFloat()
        val predicted = from + exponentialDecay<Float>().calculateTargetValue(0f, initialVelocity)
        val below = landings.lastOrNull { it <= from } ?: landings.first()
        val above = landings.firstOrNull { it >= from } ?: landings.last()
        val target = (if (abs(below - predicted) <= abs(above - predicted)) below else above).toFloat()
        var last = from
        animate(
            initialValue = from,
            targetValue = target,
            initialVelocity = initialVelocity,
            animationSpec = tween(SETTLE_MILLIS, easing = KvadrantEasing.ExponentialOut6),
        ) { value, _ ->
            last += scrollBy(value - last)
        }
        return 0f
    }
}

/**
 * How long the settle takes.
 *
 * **This project's number.** Microsoft published neither the duration nor the curve of a panorama's
 * settle, and spec §2.3 already lists the sibling unknowns — the peek of the next section and the
 * parallax coefficient. The curve is the theme's own exponential-out, which every other Metro
 * settle here uses; the duration is the phone's ordinary page-transition length.
 */

private const val SETTLE_MILLIS = 300

/**
 * The title leaving and returning at a wrap, which is the one thing `PanningTitleLayer` does that
 * no other layer here does.
 *
 * ```
 * The layer also does not repeat itself when you pan past the edges of the content. Instead,
 * during a selection change between PanoramaItem controls, it animates out of view in the
 * direction it was previously moving and animates back into the scene from the other side of
 * the screen.                                                    — ff941126, verbatim
 * ```
 *
 * **The reading is an interpretation and is stated as one.** Taken alone, "during a selection
 * change between `PanoramaItem` controls" could mean *every* section change — which would send a
 * title off the screen and back several times in one pan, and is not what a panorama looked like.
 * The sentence before it is what settles it: the clause begins "**instead**", and what it is
 * instead *of* is repeating "when you pan past the edges of the content". So the transition belongs
 * to the selection change that crosses the edge — last to first — which is exactly the fold. If
 * somebody measures a device and finds otherwise, this is the paragraph to correct.
 *
 * **The timings are the settle's, and that is the point rather than a shortcut.** This item stood
 * open on the grounds that the duration and the curve are unpublished and inventing them would put
 * two more of this project's figures into a control that already carries one. Reusing what the
 * control already has costs none: the whole transition takes `SETTLE_MILLIS`, the same as the snap
 * that provoked it, and the two halves are `ExponentialIn6` going out and `ExponentialOut6` coming
 * back — the settle's own curve and its mirror. A movement and its reverse costing the same is the
 * argument [io.github.youndie.kvadrant.indication.TiltIndication] already makes about the press.
 *
 * The split into two halves *is* a choice, and it is the only one here.
 */
private suspend fun titleTransition(
    exitShift: Float,
    entryShift: () -> Float,
    set: (Float) -> Unit,
    fold: suspend () -> Unit,
) {
    animate(
        0f,
        exitShift,
        animationSpec = tween(SETTLE_MILLIS / 2, easing = KvadrantEasing.ExponentialIn6),
    ) { value, _ -> set(value) }
    // Off the screen, so the fold behind it is invisible for the ordinary reason *and* this one.
    // **After the exit rather than before**: the fold moves the title's own travel, and doing it
    // while the title is still visible would show that move rather than hide it.
    fold()
    // Read after the fold, because it is measured from where the title now belongs.
    val entry = entryShift()
    set(entry)
    animate(
        entry,
        0f,
        animationSpec = tween(SETTLE_MILLIS / 2, easing = KvadrantEasing.ExponentialOut6),
    ) { value, _ -> set(value) }
}

/**
 * Three of everything: one to look at and one to wrap into on **each** side.
 *
 * Two was enough only for a panorama that wraps forwards. The scroll rests in the middle one, so
 * there is always a whole copy of content to move into whichever way the finger goes, and the fold
 * never has to happen during a gesture.
 *
 * There used to be a `DEFAULT_BACKGROUND_RATE` here — 0.35, this project's invention, exposed as a
 * parameter precisely because Microsoft never published a coefficient and nobody could check ours.
 * It is gone, and not because a better number turned up: **once the panorama wraps there is no
 * coefficient to choose.** The background's rate is its own width over the content's, and any other
 * value tears the seam. An invented number that had to be a parameter turned out to be a derivation
 * that had no business being one.
 */
private const val COPIES = 3
