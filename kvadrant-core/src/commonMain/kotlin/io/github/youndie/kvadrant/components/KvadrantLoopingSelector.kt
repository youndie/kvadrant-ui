package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantWeights

/**
 * One column of a date or time picker: a 148×148 px square with a very large number in it.
 *
 * The phone did not put its pickers in a dialog — it navigated to a **page** of these, filling the
 * screen, which is why the squares are as big as they are and the numbers are 54 px. A picker that
 * fits in a popover is a different control wearing the same name.
 */
@Composable
public fun KvadrantLoopingSelector(
    values: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (label != null) {
            KvadrantText(
                label,
                Modifier.padding(bottom = 4.5.dp),
                KvadrantTheme.typography.normal.copy(
                    fontSize = 15.sp, // 20 px
                    color = colors.subtle,
                ),
                cyrillic,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ITEM_MARGIN)) {
            values.forEachIndexed { index, value ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .size(CELL)
                        .clickable { onSelect(index) }
                        .background(if (selected) colors.accent else colors.chrome),
                    contentAlignment = Alignment.Center,
                ) {
                    KvadrantText(
                        value,
                        style =
                            KvadrantTheme.typography.normal.copy(
                                fontSize = NUMBER, // 54 px
                                fontWeight = KvadrantWeights.SemiLight,
                                color = if (selected) colors.onAccent else colors.foreground,
                            ),
                        cyrillic = cyrillic,
                    )
                }
            }
        }
    }
}

/**
 * The page a picker opens as, and the rotation it opens with: the columns tip in from **-50°** over
 * 200 ms and leave by rotating to **+90°** — face-on to edge-on, so the page does not slide away, it
 * turns away.
 */
@Composable
public fun KvadrantPickerPage(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Starts face-down at -50° and tips up; leaving, it turns to edge-on at +90° rather than sliding
    // away. The two start from different places, which is why this is an Animatable and not an
    // animateFloatAsState.
    val angle = remember { Animatable(OPEN_FROM) }
    LaunchedEffect(visible) {
        angle.animateTo(
            targetValue = if (visible) 0f else CLOSE_TO,
            animationSpec = tween(DURATION_MILLIS, easing = KvadrantEasing.ExponentialOut6),
        )
    }
    Row(
        modifier.graphicsLayer {
            rotationX = angle.value
            transformOrigin = TransformOrigin(0.5f, 0f)
            cameraDistance = kvadrantCameraUnits()
        },
        horizontalArrangement = Arrangement.spacedBy(ITEM_MARGIN),
    ) { content() }
}

private val CELL = 111.dp // 148 px
private val ITEM_MARGIN = 4.5.dp // 6 px
private val NUMBER = 40.5.sp // 54 px
private const val OPEN_FROM = -50f
private const val CLOSE_TO = 90f
private const val DURATION_MILLIS = 200
