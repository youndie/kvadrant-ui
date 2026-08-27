package io.github.youndie.kvadrant.components

import androidx.compose.foundation.ScrollState
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
import io.github.youndie.kvadrant.theme.KvadrantTheme
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
            Row(Modifier.horizontalScroll(scroll)) {
                // Twice: one copy to look at, one to wrap into.
                repeat(COPIES) { copy ->
                    Row(
                        Modifier.onSizeChanged { if (copy == 0) copyWidth = it.width },
                    ) {
                        sections.forEach { (header, body) ->
                            Column(Modifier.padding(start = 9.dp, end = 18.dp)) {
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
