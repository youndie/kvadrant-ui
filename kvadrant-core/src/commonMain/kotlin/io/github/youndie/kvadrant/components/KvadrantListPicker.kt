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
        Box(
            Modifier
                .fillMaxWidth()
                .border(KvadrantTheme.metrics.borderThickness, colors.border, RectangleShape)
                .clickable { onExpandRequest(mode) }
                .padding(horizontal = 9.dp, vertical = 9.dp),
        ) {
            KvadrantText(
                items.getOrElse(selectedIndex) { "" },
                // `PhoneFontSizeMediumLarge`, 25.333 px: `Style TargetType="controls:ListPicker"`,
                // toolkit `Generic.xaml`.
                style = KvadrantTheme.typography.mediumLarge,
                cyrillic = cyrillic,
            )
        }

        if (openness > 0f && mode == KvadrantListPickerMode.Expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    // `Height`, the layout property, the way `ListPicker.cs` animates it — not a
                    // `scaleY` on a graphics layer, which is what this used to be. The difference is
                    // the whole of what the control does to the page: a visual scale leaves the list
                    // at full height in layout from the first frame, so everything below it jumps
                    // aside at once and then waits; animating the measured height pushes them apart
                    // over the same 200 ms. It also stops the text being squashed on the way, which
                    // a scale cannot help doing.
                    //
                    // There is no fade. The storyboard in `ListPicker.cs` holds two animations,
                    // Height and TranslateTransform.Y, and no opacity — the alpha that used to be
                    // here was ours.
                    //
                    // The second animation is the one that makes this read as a list unfolding
                    // rather than a curtain lifting, and leaving it out was visible immediately.
                    // In the original the control is one window: closed, its height is a single item
                    // and the presenter is translated so the *selected* item is the one showing;
                    // open, the height is the whole list and `_translateAnimation.To = 0` puts the
                    // first item at the top. So the content slides while the window grows. Pinning
                    // the content at the top instead leaves every item at a fixed position and only
                    // moves the clip, which is the same end state reached without the movement.
                    //
                    // The item height is taken as an even division, which the original did not need
                    // to do — it read the selected container's real layout slot. It is exact while
                    // the labels are one line each, which is what a five-item picker is for, and it
                    // is wrong only mid-animation if one ever wraps.
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val height = (placeable.height * openness).roundToInt()
                        val itemHeight = placeable.height / items.size.coerceAtLeast(1)
                        val slide = -(itemHeight * selectedIndex * (1f - openness)).roundToInt()
                        layout(placeable.width, height) { placeable.place(0, slide) }
                    }.background(colors.chrome),
            ) {
                items.forEachIndexed { index, label ->
                    KvadrantText(
                        label,
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .background(if (index == selectedIndex) colors.accent else Color.Transparent)
                            .padding(horizontal = 9.dp, vertical = 9.dp),
                        KvadrantTheme.typography.mediumLarge.copy(
                            color = if (index == selectedIndex) colors.onAccent else colors.foreground,
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
