package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.math.roundToInt

/**
 * A one-line control that opens into its options — or, past a threshold, into a whole page.
 *
 * **The threshold is five.** Up to five items the list expands in place over 200 ms; beyond that
 * the phone navigated to a full page instead, because a list unfolding under your thumb stops being
 * readable somewhere around there. Both numbers are Microsoft's, and the three modes are the
 * control's own: `Normal`, `Expanded`, `Full`.
 *
 * This composable covers the first two. [KvadrantListPickerMode.Full] is a navigation decision — it
 * needs somewhere to navigate to — so the control reports it and the caller acts.
 *
 * **Nothing in this repository acts on it yet**, which means a picker with six options is a control
 * that cannot be opened at all. Worth knowing before reaching for one: on the phone, Full was the
 * *common* case — Settings went to a page far more often than anything unfolded in place — so what
 * is built here is the exception.
 * [B-30](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-30-list-picker-full-mode.md).
 */
@Composable
public fun KvadrantListPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandRequest: (KvadrantListPickerMode) -> Unit = {},
    header: String? = null,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val mode =
        if (items.size > FULL_MODE_THRESHOLD) {
            KvadrantListPickerMode.Full
        } else {
            KvadrantListPickerMode.Expanded
        }
    val openness by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(EXPAND_MILLIS, easing = KvadrantEasing.ExponentialInOut4),
        label = "picker",
    )

    Column(modifier.fillMaxWidth()) {
        if (header != null) {
            KvadrantText(
                header,
                Modifier.padding(bottom = 3.dp),
                KvadrantTheme.typography.subtle.copy(color = colors.subtle),
                cyrillic,
            )
        }

        // One box that grows, which is what the template says and what two attempts at this missed.
        // `controls:ListPicker` is a `Border` wrapped around `Canvas x:Name="ItemsPresenterHost"`,
        // and it is the canvas whose `Height` the storyboard animates — so the *bordered box itself*
        // goes from one item tall to the whole list, and the closed state is that same list scrolled
        // to the selected item by the `TranslateTransform`. A separate closed field with a list
        // unfolding underneath reaches the same end state and never shows the control grow, which is
        // exactly what it looked like.
        Box(
            Modifier
                .fillMaxWidth()
                .border(KvadrantTheme.metrics.borderThickness, colors.border, RectangleShape)
                .clickable { onExpandRequest(mode) }
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val itemHeight = placeable.height / items.size.coerceAtLeast(1)
                    // `MinHeight="46"` on the canvas, so a one-line picker is never thinner than
                    // that even if its single row would be.
                    // `NormalModeOffset` is subtracted twice from the height and once from the
                    // translation, so a closed picker is four Metro pixels shorter than its row at
                    // each end and the row sits four higher inside it. It reads as the box gripping
                    // its selection; without it the closed picker is just a row with a frame drawn
                    // round it, which is what this was.
                    val offset = NORMAL_MODE_OFFSET.roundToPx()
                    val closed = maxOf(itemHeight - offset * 2, MIN_CONTENT_HEIGHT.roundToPx())
                    val height = closed + ((placeable.height - closed) * openness).roundToInt()
                    // The item height as an even division. The original read the selected
                    // container's real layout slot and did not need to assume it; this is exact
                    // while the labels are one line each, which is what a five-item picker is for.
                    val slide = ((-(itemHeight * selectedIndex) - offset) * (1f - openness)).roundToInt()
                    layout(placeable.width, height) { placeable.place(0, slide) }
                },
        ) {
            Column(Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, label ->
                    // Highlighted only while open: `SizeForExpandedMode` sets `IsSelected` on the
                    // selected container and `SizeForNormalMode` clears it, so a closed picker shows
                    // its selection as plain text rather than as an accent bar.
                    val highlighted = expanded && index == selectedIndex
                    KvadrantText(
                        label,
                        Modifier
                            .fillMaxWidth()
                            // Only while open. Every row exists at every moment — closed, they are
                            // simply clipped away — so a row that is always clickable puts the
                            // selected one's `onSelect` under the closed box and the picker cannot
                            // be opened at all. That is what happened, and neither test caught it
                            // because both drive `expanded` as a parameter and never tap anything.
                            .then(
                                if (expanded) Modifier.clickable { onSelect(index) } else Modifier,
                            ).background(if (highlighted) colors.accent else Color.Transparent)
                            .padding(horizontal = 9.dp, vertical = 9.dp),
                        // `PhoneFontSizeMediumLarge`, 25.333 px: `Style TargetType="controls:ListPicker"`,
                        // toolkit `Generic.xaml`.
                        KvadrantTheme.typography.mediumLarge.copy(
                            color = if (highlighted) colors.onAccent else colors.foreground,
                        ),
                        cyrillic,
                    )
                }
            }
        }
    }
}

/** What a picker does when tapped, which depends only on how many options it has. */
public enum class KvadrantListPickerMode { Expanded, Full }

/** More than this many options and the phone opened a page instead of unfolding. */
public const val FULL_MODE_THRESHOLD: Int = 5

/** `Duration duration = TimeSpan.FromSeconds(0.2)` in `ListPicker.cs`, read out of the source. */
private const val EXPAND_MILLIS = 200

/** `MinHeight="46"` on `Canvas x:Name="ItemsPresenterHost"` in the toolkit template. */
private val MIN_CONTENT_HEIGHT = 34.5.dp

/** `private const double NormalModeOffset = 4` in `ListPicker.cs`. */
private val NORMAL_MODE_OFFSET = 3.dp
