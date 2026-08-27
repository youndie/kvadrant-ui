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
    var copyWidth by remember { mutableIntStateOf(0) }
    var backgroundWidth by remember { mutableIntStateOf(0) }
    // The left edge of every section in the first copy, which is where a release has to land.
    val sectionWidths = remember(sections.size) { mutableStateListOf(*Array(sections.size) { 0 }) }

    // The fold. Crossing into the second copy puts the scroll back at the same place in the first,
    // which is invisible because the two are identical — and is why there is no end to hit.
    LaunchedEffect(copyWidth) {
        snapshotFlow { scroll.value }.collect { value ->
            if (copyWidth > 0 && value >= copyWidth) scroll.scrollTo(value - copyWidth)
        }
    }

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
            // Drawn twice, and **that is a deviation from the original**, named here rather than
            // quietly kept. `PanningTitleLayer` "does not repeat itself when you pan past the edges
            // of the content"; instead, on a selection change, it "animates out of view in the
            // direction it was previously moving and animates back into the scene from the other
            // side of the screen" (`ff941126`). That behaviour needs a selected item, and this
            // panorama has no item model — it free-scrolls, which is the same gap as the missing
            // snap ([B-33](../../../docs/backlog/B-33-panorama-is-a-scroller-not-an-item-model.md)). Until it has one, a second copy is what keeps the wrap from tearing;
            // the alternative is a title that jumps on every fold, which is worse and was the state
            // this replaced. A title narrower than the viewport has no overflow, does not move, and
            // gets no second copy — that one is the original's behaviour by accident rather than by
            // design.
            val titleMoves = titleWidth > viewport
            Row(Modifier.offset { IntOffset(if (titleMoves) drift(titleWidth) else 0, 0) }) {
                repeat(if (titleMoves) COPIES else 1) { copy ->
                    KvadrantText(
                        title,
                        Modifier
                            .onSizeChanged { if (copy == 0) titleWidth = it.width }
                            .padding(start = 7.5.dp),
                        KvadrantTheme.typography.panoramaTitle,
                        cyrillic,
                    )
                }
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
 * Where a fling ends, rounded to the nearest section edge.
 *
 * The prediction is a decay from the release velocity — where the finger *would* have thrown it —
 * and the nearest stop to that is where it goes instead. Rounding the current position rather than
 * the predicted one would make a hard flick travel exactly one section however hard it was thrown,
 * which is a pager and not a panorama.
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
        val target = landings.minBy { abs(it - predicted) }.toFloat()
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
 * Two of everything: one copy to look at, one to wrap into.
 *
 * There used to be a `DEFAULT_BACKGROUND_RATE` here — 0.35, this project's invention, exposed as a
 * parameter precisely because Microsoft never published a coefficient and nobody could check ours.
 * It is gone, and not because a better number turned up: **once the panorama wraps there is no
 * coefficient to choose.** The background's rate is its own width over the content's, and any other
 * value tears the seam. An invented number that had to be a parameter turned out to be a derivation
 * that had no business being one.
 */
private const val COPIES = 2
