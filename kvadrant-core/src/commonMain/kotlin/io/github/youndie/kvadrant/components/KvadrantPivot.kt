package io.github.youndie.kvadrant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The control the rest of this library exists to make possible.
 *
 * A page title in the flow of the content rather than a bar above it, oversized headers that scroll
 * at their own rate while the pages scroll at theirs, and the next header showing at the right edge
 * so you can see there is more. Every number is Microsoft's: the header is
 * `PivotHeaderFontSize` = 72 px SemiLight, its padding is `21,0,1,0` — that 21 px is the gap between
 * headers — an unselected header sits at `PhonePivotUnselectedItemOpacity` = 0.4, and selection is
 * instant, `Duration="0"`, with no crossfade.
 *
 * **The peek is not a number.** The original lays its headers on a `Canvas` at absolute positions,
 * so how far the next one shows falls out of where the strip is scrolled to — which is what
 * [KvadrantPivotHeaders] reproduces. Nothing here is tuned to make it look right.
 *
 * Pages are cyclical, as Microsoft's guidance says: past the last one the next is the first, and the
 * header strip wraps with them. That is why [rememberKvadrantPivotState] exists — the pager runs on
 * a very large virtual page count and the page index is taken modulo the number of titles.
 */
@Composable
public fun KvadrantPivot(
    titles: List<String>,
    modifier: Modifier = Modifier,
    title: String? = null,
    state: PagerState = rememberKvadrantPivotState(titles.size),
    swipeEnabled: Boolean = true,
    cyrillic: FontFamily? = null,
    content: @Composable (page: Int) -> Unit,
) {
    val metrics = KvadrantTheme.metrics
    val typography = KvadrantTheme.typography

    Column(modifier) {
        if (title != null) {
            // ContentControl, margin 24,17,0,-7 at Metro's pixels.
            KvadrantText(
                title,
                Modifier.padding(start = metrics.margin * 2, top = 12.75.dp, bottom = 0.dp),
                typography.title,
                cyrillic,
            )
        }

        KvadrantPivotHeaders(titles, state, cyrillic, Modifier.padding(top = 4.dp))

        // **`LockablePivot` is this parameter and not a component**, which is
        // [B-43](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-43-the-toolkit-was-never-inventoried.md)'s
        // one-sentence answer. The Toolkit shipped a whole subclass because Silverlight had no way
        // to turn a `Pivot`'s manipulation off from outside it; a pager takes a boolean, and a
        // second component whose only difference is a boolean is a second thing to document, test
        // and keep in step. A page that owns a horizontal gesture — a map, a slider that fills the
        // width — sets this to false while it has the finger.
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = swipeEnabled,
        ) { page ->
            // PivotItemMargin 12,28,12,0 — the item's own margin, inside the page. Putting it on
            // the pager instead makes the neighbouring page peek, which the content never does.
            //
            // And it scrolls: a pivot item taller than the screen is the normal case, not an edge
            // one, and without this the bottom of a page is cut off by whatever sits below it.
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = metrics.margin,
                        top = metrics.pivotItemTopMargin,
                        end = metrics.margin,
                    ),
            ) {
                // The caller counts pages from zero. The pager counts from the middle of a very
                // large virtual range, and handing that number out means every `when (page)` falls
                // through to its else branch — which looks like one page repeated three times.
                content(page.mod(titles.size))
            }
        }
    }
}

/**
 * A pager that never reaches an end.
 *
 * Windows Phone's pivot pages are cyclical — past the last one comes the first — and the cheapest
 * honest way to get that from `HorizontalPager` is a very large page count started in the middle,
 * with the index taken modulo the real one. The alternative, a pager that clamps at both ends with
 * the headers pretending otherwise, is the version that feels wrong without anyone being able to
 * say why.
 */
@Composable
public fun rememberKvadrantPivotState(pageCount: Int): PagerState {
    require(pageCount > 0) { "a pivot needs at least one page" }
    // The middle, rounded down to a whole number of cycles. Without the rounding the pivot opens on
    // whichever page `(VIRTUAL_PAGES / 2) mod pageCount` happens to land on — which is not the first
    // one, and looks like the app remembering a position it was never given.
    val start = VIRTUAL_PAGES / 2 - (VIRTUAL_PAGES / 2) % pageCount
    return rememberPagerState(initialPage = start) { VIRTUAL_PAGES }
}

private const val VIRTUAL_PAGES = 1 shl 20

/**
 * The header strip.
 *
 * Children are measured, laid out end to end, and the whole row is shifted so that the selected
 * header sits at the left margin. The shift follows the pager continuously, so during a swipe the
 * headers travel by *their own* widths while the pages travel by a full page width — and that
 * difference in rate is the parallax. It is geometry, not a tuned coefficient.
 */
@Composable
public fun KvadrantPivotHeaders(
    titles: List<String>,
    state: PagerState,
    cyrillic: FontFamily? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Tapping a header goes to its page. The phone did this, and a strip of large words that looks
    // tappable and is not is worse than one that does not look tappable at all.
    val onHeaderClick: (Int, Int) -> Unit = { copy, index ->
        val current = state.currentPage
        val here = current.mod(titles.size)
        // The nearest page carrying that title, so tapping the one peeking on the right goes
        // forwards rather than most of the way round.
        val delta = index - here + (copy - 1) * titles.size
        scope.launch { state.animateScrollToPage(current + delta) }
    }

    val colors = KvadrantTheme.colors
    val style = KvadrantTheme.typography.pivotHeader
    val gap = KvadrantTheme.metrics.pivotHeaderGap

    // Kept apart on purpose. `currentPage` runs into the hundreds of thousands on a cyclic pivot,
    // and a Float cannot hold `page + fraction` at that magnitude: near 2^19 the gap between
    // representable floats is 0.0625, so the fraction quantises to sixteenths and the strip moves in
    // visible steps. The page is an Int and the fraction is a Float, and they never meet.
    val page = state.currentPage
    val fraction = state.currentPageOffsetFraction

    Layout(
        // Clipped at the page margin: the strip extends to the left of the selected header, and
        // without the clip the previous header's tail shows through the margin — which is a thing
        // the phone never did at rest.
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 9.dp)
                .clipToBounds(),
        content = {
            // Three copies, so there is always a header to the left and to the right of the one in
            // view. The strip is scrolled to the middle copy, which is what makes the wrap seamless
            // rather than a jump.
            repeat(COPIES) { copy ->
                titles.forEachIndexed { index, text ->
                    val selected = copy == 1 && index == state.currentPage.mod(titles.size)
                    KvadrantText(
                        text,
                        // PivotHeaderItem padding 21,0,1,0 — the gap is on the item, not between.
                        Modifier
                            .clickable(
                                interactionSource = null,
                                indication = null,
                            ) { onHeaderClick(copy, index) }
                            .padding(
                                start = if (copy == 0 && index == 0) 0.dp else gap,
                                end = 0.75.dp,
                            ),
                        style.copy(color = if (selected) colors.foreground else colors.subtle),
                        cyrillic,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        // Where each header starts, if the strip were not scrolled.
        val starts = placeables.runningFold(0) { acc, p -> acc + p.width }

        val n = titles.size

        // MSDN, verbatim: headers are drawn until they exceed the width of the control, and **if
        // there are too few to fill it they do not loop**. A strip that fits does not travel either
        // — it stands still and only the selected colour moves. Standing still means parking the
        // middle copy at the origin, which is `starts[n]` and emphatically not zero: the middle copy
        // begins one whole copy in, so a zero shift pushes the whole strip off the right edge.
        val oneCopy = starts[n]
        val loops = oneCopy > constraints.maxWidth

        // The selected header, counted from the start of the middle copy, and how far the swipe has
        // carried it towards its neighbour. The fraction is signed: negative going back.
        val whole = n + page.mod(n)
        val from = starts[whole]
        val neighbour = if (fraction >= 0f) starts.getOrElse(whole + 1) { from } else starts[whole - 1]
        val shift =
            if (!loops) {
                oneCopy
            } else {
                (from + (neighbour - from) * kotlin.math.abs(fraction)).roundToInt()
            }

        // The strip begins at the page margin, like everything else on a Metro page.
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, p ->
                val copy = index / n
                // A strip that does not loop places its middle copy only; the other two exist so
                // the measurement above can be taken and are simply not drawn.
                // **`placeRelative` and not `place`**, which is the whole of what this strip needed
                // for right-to-left ([B-41](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-41-rtl-is-canon-and-untested.md)).
                // `place` is absolute: under `LayoutDirection.Rtl` the headers went on running left
                // to right while the page around them mirrored, and the selected one — which belongs
                // at the margin — was pushed off the far edge instead. The arithmetic above is
                // unchanged and needs to be: it is a strip with a start and an end, and which side
                // those are on is the layout's business rather than its own.
                if (loops || copy == 1) p.placeRelative(starts[index] - shift, 0)
            }
        }
    }
}

private const val COPIES = 3
