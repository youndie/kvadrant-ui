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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
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
    content: @Composable () -> Unit,
) {
    val colors = KvadrantTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (expanded) PUSHED_BACK else 1f,
        animationSpec = tween(DURATION_MILLIS, easing = KvadrantEasing.ExponentialInOut4),
        label = "page",
    )

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) { content() }

        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.semitransparent)
                    .clickable(onClick = onDismiss),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
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
                        KvadrantTheme.typography.normal,
                        cyrillic,
                    )
                }
            }
        }
    }
}

private const val PUSHED_BACK = 0.94f
private const val DURATION_MILLIS = 420
