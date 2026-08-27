package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The menu a long press opens, and the page pushed away behind it.
 *
 * **It does not blur.** Nearly every reimplementation blurs the background because that is what a
 * modern sheet does; the original scales the page from 1.0 to **0.94 over 420 ms** and leaves it
 * perfectly sharp. There is no blur anywhere in the source. The effect is depth by scale, which is
 * the same idea as the tilt.
 *
 * [content] is the page. [items] is the menu, which appears over it once [expanded].
 */
@Composable
public fun KvadrantContextMenuHost(
    expanded: Boolean,
    items: List<String>,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cyrillic: FontFamily? = null,
    anchorTop: Dp? = null,
    anchorHeight: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val colors = KvadrantTheme.colors
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(DURATION_MILLIS, easing = KvadrantEasing.ExponentialInOut2),
        label = "menu",
    )

    Box(modifier.fillMaxSize().background(colors.background)) {
        // The background is painted here rather than left to the page, because the page is about to
        // be scaled away from the edges and something has to be behind it. On the phone that
        // something was the shell; in a Compose tree it is whatever happens to be underneath, which
        // in a demo turned out to be the previous screen showing in the margins.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val scale = 1f - (1f - PUSHED_BACK) * progress
                scaleX = scale
                scaleY = scale
            },
        ) { content() }

        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.foreground.copy(alpha = 0f))
                    .background(Color.Black.copy(alpha = FADE_TO * progress))
                    .clickable(onClick = onDismiss),
            )
            ContextMenuSurface(anchorTop, anchorHeight) {
                // The open animation, straight out of the `Open` visual state in the toolkit's
                // `Generic.xaml`: `ScaleX` runs 0 to 1 over the first 300 ms and `ScaleY` sits at
                // zero until 300 and then runs 0 to 1 by 420. Both linear, 420 ms in total. So the
                // menu draws itself as a line across the page first and only then unrolls
                // downwards — it is the two phases that make it recognisable, and this had no
                // animation at all.
                val opening = (progress * TOTAL_MILLIS)
                Column(
                    Modifier
                        .graphicsLayer {
                            scaleX = (opening / SCALE_X_MILLIS).coerceIn(0f, 1f)
                            scaleY =
                                ((opening - SCALE_X_MILLIS) / (TOTAL_MILLIS - SCALE_X_MILLIS))
                                    .coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                        // Full width. `UpdateContextMenuPlacement` sets `x = bounds.Left` and
                        // `Width = bounds.Width` in portrait — the menu spans the page, and the
                        // inset it used to have here was ours.
                        .fillMaxWidth()
                        .background(colors.chrome)
                        .padding(vertical = 6.dp),
                ) {
                    items.forEachIndexed { index, label ->
                        KvadrantText(
                            label,
                            Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(index) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            // `PhoneFontSizeLarge`, 32 px: `Style TargetType="controls:MenuItem"`
                            // in the toolkit's `Generic.xaml` sets it explicitly, overriding the
                            // page's Normal. A menu item is nearly twice the body size.
                            KvadrantTheme.typography.large,
                            cyrillic,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Places the menu the way `AdjustContextMenuPositionForPortraitMode` does.
 *
 * Below the pressed item if the menu fits there, above it if it fits there instead, and pinned to
 * the bottom of the page when neither is true — which is also what happens when there is no anchor
 * at all, the case the original reaches by opening the menu from code rather than from a finger.
 *
 * It is a `Layout` rather than an offset computed from a remembered height because the decision
 * needs the menu's measured height, and measuring it into state costs a frame in which the menu is
 * drawn in the wrong place.
 */
@Composable
private fun ContextMenuSurface(
    anchorTop: Dp?,
    anchorHeight: Dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val menu = measurables.first().measure(constraints.copy(minHeight = 0))
        val page = constraints.maxHeight
        val top = anchorTop?.roundToPx()
        val height = anchorHeight.roundToPx()

        val y =
            when {
                top == null || page <= menu.height -> page - menu.height
                top + height <= page - menu.height -> top + height
                top >= menu.height -> top - menu.height
                else -> page - menu.height
            }
        layout(constraints.maxWidth, page) { menu.place(0, y.coerceAtLeast(0)) }
    }
}

/** `Duration="0:0:0.42"` on the `Open` storyboard, with the ScaleX keyframe landing at 0.3. */
private const val TOTAL_MILLIS = 420f
private const val SCALE_X_MILLIS = 300f

private const val PUSHED_BACK = 0.94f

/** `From = 0, To = .3` on the fade layer, same storyboard, same duration. It used to be 0.667. */
private const val FADE_TO = 0.3f
private const val DURATION_MILLIS = 420
