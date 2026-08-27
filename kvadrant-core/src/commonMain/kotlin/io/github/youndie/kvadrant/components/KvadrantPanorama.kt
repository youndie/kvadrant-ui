package io.github.youndie.kvadrant.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
 * **It wraps.** Past the last section comes the first, as the original does with its
 * `LeftWraparound` and `RightWraparound` borders: the sections are laid out twice and the scroll
 * position is folded back by one copy's width whenever it crosses into the second, so there is
 * never an end to reach and never a jump to see.
 */
@Composable
public fun KvadrantPanorama(
    title: String,
    modifier: Modifier = Modifier,
    cyrillic: FontFamily? = null,
    scroll: ScrollState = rememberScrollState(),
    backgroundRate: Float = DEFAULT_BACKGROUND_RATE,
    background: @Composable (Modifier) -> Unit = {},
    sections: List<Pair<String, @Composable () -> Unit>>,
) {
    var viewport by remember { mutableIntStateOf(0) }
    var titleWidth by remember { mutableIntStateOf(0) }
    var copyWidth by remember { mutableIntStateOf(0) }

    // The fold. Crossing into the second copy puts the scroll back at the same place in the first,
    // which is invisible because the two are identical — and is why there is no end to hit.
    LaunchedEffect(copyWidth) {
        snapshotFlow { scroll.value }.collect { value ->
            if (copyWidth > 0 && value >= copyWidth) scroll.scrollTo(value - copyWidth)
        }
    }

    val contentOverflow = (copyWidth.takeIf { it > 0 } ?: scroll.maxValue).coerceAtLeast(1)

    fun rateFor(width: Int): Float = ((width - viewport).coerceAtLeast(0).toFloat() / contentOverflow).coerceIn(0f, 1f)

    Box(modifier.fillMaxSize().onSizeChanged { viewport = it.width }) {
        // The background spans both rows in the original and drifts slowest of the three.
        background(
            Modifier.offset { IntOffset(-(scroll.value * backgroundRate).roundToInt(), 0) },
        )

        Column(Modifier.fillMaxSize()) {
            // Margin 10,-34,0,0: the title deliberately sits above the top of its row.
            KvadrantText(
                title,
                Modifier
                    .offset { IntOffset(-(scroll.value * rateFor(titleWidth)).roundToInt(), 0) }
                    .onSizeChanged { titleWidth = it.width }
                    .padding(start = 7.5.dp),
                KvadrantTheme.typography.panoramaTitle,
                cyrillic,
            )

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
 * How fast the background drifts, as a fraction of the content's rate.
 *
 * Unlike the title's rate this one is **not** derivable: the original's background is an image whose
 * width the application chooses, and Microsoft never published the coefficient. **This number is
 * this project's invention**, which is why it is a parameter of [KvadrantPanorama] rather than a
 * constant inside it — anyone who measures the real thing can pass their own without a fork.
 */
public const val DEFAULT_BACKGROUND_RATE: Float = 0.35f

private const val COPIES = 2
