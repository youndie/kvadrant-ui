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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme

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
                style = KvadrantTheme.typography.normal,
                cyrillic = cyrillic,
            )
        }

        if (openness > 0f && mode == KvadrantListPickerMode.Expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = openness
                        scaleY = openness
                        transformOrigin = TopEdge
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
                        KvadrantTheme.typography.normal.copy(
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

private const val EXPAND_MILLIS = 200
private val TopEdge =
    androidx.compose.ui.graphics
        .TransformOrigin(0f, 0f)
