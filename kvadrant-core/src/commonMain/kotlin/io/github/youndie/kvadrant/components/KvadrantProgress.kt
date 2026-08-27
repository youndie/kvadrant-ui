package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * Five squares crossing the width, over and over. After the tilt this is the most recognisable
 * motion Metro has, and an approximation of it reads as a knock-off at once — so the numbers are
 * the published ones: five 4×4 px dots, a 4.4 second cycle, each starting 200 ms after the one
 * before it.
 *
 * The path is not linear. A dot covers the first third in half a second on an exponential curve,
 * crawls the middle third over a second and a half, then takes the last third in another half
 * second — which is what makes the group bunch up and stretch out as it travels.
 */
@Composable
public fun KvadrantProgressDots(
    modifier: Modifier = Modifier,
    color: Color = KvadrantTheme.colors.accent,
) {
    val transition = rememberInfiniteTransition(label = "dots")
    val positions =
        List(DOT_COUNT) { index ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = CYCLE_MILLIS
                                0f at 0 using KvadrantEasing.ExponentialOut6
                                0.33f at 500 using LinearEasing
                                0.66f at 2000 using KvadrantEasing.ExponentialOut6
                                1f at 2500 using LinearEasing
                                1f at CYCLE_MILLIS
                            },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset =
                            androidx.compose.animation.core
                                .StartOffset(index * STAGGER_MILLIS),
                    ),
                label = "dot$index",
            )
        }

    Canvas(modifier.fillMaxWidth().height(DOT)) {
        val dot = DOT.toPx()
        positions.forEach { p ->
            drawRect(color, Offset((size.width - dot) * p.value, 0f), Size(dot, dot))
        }
    }
}

/**
 * One dot's journey across the width, as the phone drew it: a third in the first half second, the
 * middle third over the next second and a half, the last third in another half second, then a pause
 * until the cycle comes round. A still cannot show any of this, which is why it is a value a test
 * can sample rather than a literal buried in a composable.
 */
public fun dotPath(): KeyframesSpec<Float> =
    keyframes {
        durationMillis = CYCLE_MILLIS
        0f at 0 using KvadrantEasing.ExponentialOut6
        0.33f at 500 using LinearEasing
        0.66f at 2000 using KvadrantEasing.ExponentialOut6
        1f at 2500 using LinearEasing
        1f at CYCLE_MILLIS
    }

private val DOT = 3.dp // 4 px
private const val DOT_COUNT = 5
private const val CYCLE_MILLIS = 4400
private const val STAGGER_MILLIS = 200
