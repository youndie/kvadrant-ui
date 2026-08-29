package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * Five squares crossing the width, over and over. After the tilt this is the most recognisable
 * motion Metro has, and an approximation of it reads as a knock-off at once — so the numbers are
 * the published ones: five 4×4 px dots, a 4.4 second cycle, each starting 200 ms after the one
 * before it.
 *
 * The path is not linear. A dot covers the first third in half a second easing **out**, crawls the
 * middle third **linearly** over a second and a half, then takes the last third in another half
 * second easing **in** — which is what makes the group bunch up and stretch out as it travels. Both
 * exponentials are `Exponent="1"`, the gentlest curve in the theme.
 *
 * **And then it disappears.** `Storyboard.TargetProperty="Opacity"` is a pair of
 * `DiscreteDoubleKeyFrame`s per dot — one at nought, nought at 2.5 s — so a dot switches off the
 * instant it lands and the bar is empty for the last stretch of the cycle before the next sweep
 * begins. Leaving it out, which this did, parks five dots against the right-hand edge for over a
 * second of every 4.4 and turns a sweep into a queue.
 */
@Composable
public fun KvadrantProgressDots(
    modifier: Modifier = Modifier,
    color: Color = KvadrantTheme.colors.accent,
) {
    // Indeterminate, and saying so is the point: a screen reader announces "busy" rather than
    // reading a position that does not exist. Five dots crossing a bar carry no progress value.
    val announced = Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
    val transition = rememberInfiniteTransition(label = "dots")
    val dots =
        List(DOT_COUNT) { index ->
            val offset = StartOffset(index * STAGGER_MILLIS)
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(dotPath(), RepeatMode.Restart, offset),
                label = "dot$index",
            ) to
                transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec =
                        infiniteRepeatable(dotOpacity(), RepeatMode.Restart, offset),
                    label = "alpha$index",
                )
        }

    // `Padding="{StaticResource PhoneHorizontalMargin}"` — 12,0 — on the template, carried by the
    // indeterminate root's own margin. A dot's travel is the width inside it, not the whole bar.
    // Both numbers come out of the metric set so that a theme scaled up scales them too.
    val metrics = KvadrantTheme.metrics
    Canvas(
        modifier
            .then(announced)
            .fillMaxWidth()
            .height(metrics.progressThickness)
            .padding(horizontal = metrics.margin),
    ) {
        val dot = metrics.progressThickness.toPx()
        dots.forEach { (position, alpha) ->
            drawRect(
                color.copy(alpha = color.alpha * alpha.value),
                Offset((size.width - dot) * position.value, 0f),
                Size(dot, dot),
            )
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
        // The thirds are exact: the template drives a `Slider` from 0 to 3000 with keyframes at
        // 1000 and 2000, so they are not 0.33 and 0.66 but a third and two thirds.
        0f at 0 using KvadrantEasing.Exponential1
        1f / 3f at 500 using LinearEasing
        2f / 3f at 2000 using KvadrantEasing.ExponentialIn1
        1f at 2500 using LinearEasing
        1f at CYCLE_MILLIS
    }

/**
 * The same dot's visibility: on while it travels, off the moment it lands.
 *
 * Two `DiscreteDoubleKeyFrame`s in the template — one at nought and nought at 2.5 s — so this is a
 * switch rather than a fade, and the millisecond before the switch is what makes it discrete here.
 * Exported beside [dotPath] for the same reason: a still frame cannot show a thing that is only
 * wrong for part of a cycle.
 */
public fun dotOpacity(): KeyframesSpec<Float> =
    keyframes {
        durationMillis = CYCLE_MILLIS
        1f at 0
        1f at VISIBLE_MILLIS - 1
        0f at VISIBLE_MILLIS
        0f at CYCLE_MILLIS
    }

private const val DOT_COUNT = 5
private const val CYCLE_MILLIS = 4400

/** A dot's opacity goes to nought at 2.5 s, which is the moment it finishes its run. */
private const val VISIBLE_MILLIS = 2500
private const val STAGGER_MILLIS = 200
