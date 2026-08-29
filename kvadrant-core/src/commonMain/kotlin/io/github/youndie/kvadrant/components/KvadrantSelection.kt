package io.github.youndie.kvadrant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The phone's check box, from its own `System.Windows` theme rather than from the shape language.
 *
 * Three things it does that a reconstruction gets wrong. The box is **32 px square** with the same
 * 3 px border everything else uses. Its background stays **transparent** when checked — what appears
 * is the tick, in the **foreground** colour; held down it fills with
 * [KvadrantColors.checkBoxPressed], which is white in the dark theme and transparent in the light
 * one — not the accent, whatever the token's name suggested. And
 * the tick is a **filled path**, not two strokes: `M0,123 L39,93 L124,164 L256,18 L295,49 L124,240`,
 * stretched to 23×21, which is why its two arms have different weights and why a pair of straight
 * lines never looks quite like it.
 */
@Composable
public fun KvadrantCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    boxSize: Dp = BOX,
    enabled: Boolean = true,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val outline = if (enabled) colors.foreground else colors.disabled

    Row(
        modifier
            .defaultMinSize(minHeight = KvadrantTheme.metrics.touchTargetMin)
            // `toggleable` rather than `clickable`, and that is the whole of the accessibility fix
            // for this control: it carries the role and the on/off state into the semantics tree,
            // where `clickable` reported a box that could be tapped and nothing about what it was.
            // The interaction source and indication are unchanged, so the tilt is untouched.
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
    ) {
        Box(
            Modifier
                .size(boxSize)
                .background(if (pressed) colors.checkBoxPressed else Color.Transparent)
                .border(BORDER, if (pressed) colors.checkBoxPressedBorder else outline, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Tick(
                    color = if (pressed) colors.contrastForeground else outline,
                    width = boxSize * (TICK_WIDTH / BOX),
                    height = boxSize * (TICK_HEIGHT / BOX),
                )
            }
        }
        if (label != null) {
            KvadrantText(label, style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
        }
    }
}

/** The template's `CheckMark` path, stretched to fill its box exactly as `Stretch="Fill"` does. */
@Composable
private fun Tick(
    color: Color,
    width: Dp,
    height: Dp,
) {
    Canvas(Modifier.size(width, height)) {
        // Bounding box of the data below, which is what Stretch=Fill normalises against.
        val sx = size.width / 295f
        val sy = size.height / 222f

        fun p(
            x: Float,
            y: Float,
        ) = androidx.compose.ui.geometry
            .Offset(x * sx, (y - 18f) * sy)

        val path =
            Path().apply {
                val start = p(0f, 123f)
                moveTo(start.x, start.y)
                listOf(39f to 93f, 124f to 164f, 256f to 18f, 295f to 49f, 124f to 240f).forEach { (x, y) ->
                    val o = p(x, y)
                    lineTo(o.x, o.y)
                }
                close()
            }
        drawPath(path, color)
    }
}

/**
 * The phone's radio button: a 32 px ring with a 3 px stroke and a 16 px dot — exactly half — both in
 * the foreground colour, on a transparent fill. Held down it takes
 * [KvadrantColors.checkBoxPressed] and [KvadrantColors.checkBoxPressedBorder] — not the accent.
 */
@Composable
public fun KvadrantRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    size: Dp = BOX,
    enabled: Boolean = true,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val outline = if (enabled) colors.foreground else colors.disabled

    Row(
        modifier
            .defaultMinSize(minHeight = KvadrantTheme.metrics.touchTargetMin)
            // `selectable`, so a screen reader is told this is one of a set and which one is
            // chosen. A radio button that only reports "clickable" is indistinguishable from a row.
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
    ) {
        Box(
            Modifier
                .size(size)
                .background(if (pressed) colors.checkBoxPressed else Color.Transparent, CircleShape)
                .border(BORDER, if (pressed) colors.checkBoxPressedBorder else outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(size / 2)
                        .background(if (pressed) colors.contrastForeground else outline, CircleShape),
                )
            }
        }
        if (label != null) {
            KvadrantText(label, style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
        }
    }
}

/**
 * Two bars meeting at the value, from `PhoneSlider` in the SDK's own `System.Windows.xaml`.
 *
 * **There is no thumb, and this drew one.** `HorizontalThumb` is `Width="1"` with a
 * `ScaleTransform ScaleX="32"` and the template `PhoneSimpleThumb`, which is
 * `<Rectangle Fill="Transparent"/>` — an invisible thirty-two pixel handle for the finger to find,
 * and nothing to look at. What a Metro slider shows is the accent up to the value and the track
 * after it, meeting at a hard edge. The block this used to draw was named in its own KDoc as this
 * project's own, on the grounds that the template "is not among those recovered"; it is now, and it
 * says the block should not exist.
 *
 * **And the track is three times thicker than it was.** `Height="12"`, not the 4 the progress bar
 * uses — they are not the same line. It sits high in its row, `Margin="0,22,0,50"`, with the whole
 * horizontal template inside `PhoneHorizontalMargin`.
 *
 * The track's colour is `PhoneContrastBackgroundBrush` at `Opacity="0.2"` — white at a fifth on a
 * dark page, 87 % black at a fifth on a light one. [KvadrantColors.inactive] is within a hair of
 * that in both themes and was what this used; the derivation is written out instead so the next
 * reader can check it against the template rather than against a coincidence.
 */
@Composable
public fun KvadrantSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // A slider that reports no value is a bar a screen reader can find and not read. The range is
    // the control's own 0..1, so a caller scaling it to something else does not have to restate it.
    val announced =
        Modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
        }
    val colors = KvadrantTheme.colors
    val metrics = KvadrantTheme.metrics
    val fraction = value.coerceIn(0f, 1f)

    // `To="0.1"` on the track and `PhoneDisabledBrush` on the fill, both from the Disabled state.
    val track = colors.contrastBackground.copy(alpha = if (enabled) TRACK_ALPHA else TRACK_ALPHA * DISABLED_TRACK)
    val fill = if (enabled) colors.accent else colors.disabled

    Box(
        modifier
            .then(announced)
            .fillMaxWidth()
            .defaultMinSize(minHeight = metrics.touchTargetMin)
            // The gestures sit **outside** the horizontal margin, because the template's root is a
            // `Grid Background="Transparent"` that spans the control and only `HorizontalTemplate`
            // carries `PhoneHorizontalMargin`. Putting them inside, which this did for a moment,
            // means a swipe starting at the edge of the control begins outside the handler and
            // nothing moves — which reads as a slider that has stopped responding.
            .then(
                if (!enabled) {
                    Modifier
                } else {
                    Modifier
                        .pointerInput(metrics.margin) {
                            // With no thumb the value is simply where the finger is: a tap and the
                            // drag that follows it cannot disagree, and the arithmetic that used to
                            // keep a thumb's centre under the finger goes away with the thumb.
                            val inset = metrics.margin.toPx()
                            detectTapGestures { offset -> onValueChange(fractionAt(offset.x, size.width, inset)) }
                        }.pointerInput(metrics.margin) {
                            val inset = metrics.margin.toPx()
                            var x = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { start ->
                                    x = start.x
                                    onValueChange(fractionAt(x, size.width, inset))
                                },
                                onHorizontalDrag = { change, delta ->
                                    // Not what wins the gesture from an enclosing pager, though it
                                    // reads like it: `detectHorizontalDragGestures` already consumes
                                    // at the slop threshold, and `SliderDragTest` still passes with
                                    // this line commented out. It is here so that nothing further up
                                    // treats the same movement as its own.
                                    change.consume()
                                    x += delta
                                    onValueChange(fractionAt(x, size.width, inset))
                                },
                            )
                        }
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // `Margin="{StaticResource PhoneHorizontalMargin}"` on `HorizontalTemplate`, inside the
        // hit-testable root rather than around it.
        Box(Modifier.fillMaxWidth().padding(horizontal = metrics.margin)) {
            // `Margin="0,22,0,50"`: the bar is not centred in its row, it rides high in it.
            Box(Modifier.padding(top = TRACK_TOP, bottom = TRACK_BOTTOM)) {
                Box(Modifier.fillMaxWidth().height(metrics.sliderThickness).background(track))
                Box(Modifier.fillMaxWidth(fraction).height(metrics.sliderThickness).background(fill))
            }
        }
    }
}

/** The finger's place along the bar, which starts one horizontal margin in from the control. */
private fun fractionAt(
    x: Float,
    width: Int,
    inset: Float,
): Float {
    val travel = width - inset * 2f
    return if (travel <= 0f) 0f else ((x - inset) / travel).coerceIn(0f, 1f)
}

private val BOX = 24.dp // 32 px
private val BORDER = 2.25.dp // PhoneBorderThickness / PhoneStrokeThickness, both 3 px
private val TICK_WIDTH = 17.25.dp // 23 px
private val TICK_HEIGHT = 15.75.dp // 21 px
private val CONTENT_GAP = 9.dp // the content's 12,0,0,0 margin

/** `Opacity="0.2"` on `HorizontalTrack`. */
private const val TRACK_ALPHA = 0.2f

/** `To="0.1"` on the same rectangle in the Disabled state, which is a fifth of the fifth. */
private const val DISABLED_TRACK = 0.5f

/** `Margin="0,22,0,50"` at 0.75: the bar rides high in its row rather than sitting centred. */
private val TRACK_TOP = 16.5.dp
private val TRACK_BOTTOM = 37.5.dp
