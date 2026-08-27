package io.github.youndie.kvadrant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * is the tick, in the **foreground** colour; the accent shows only while the box is held down. And
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
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
            ) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
    ) {
        Box(
            Modifier
                .size(boxSize)
                .background(if (pressed) colors.accent else Color.Transparent)
                .border(BORDER, outline, RectangleShape),
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
 * the foreground colour, on a transparent fill. The accent appears only while it is held.
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
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
    ) {
        Box(
            Modifier
                .size(size)
                .background(if (pressed) colors.accent else Color.Transparent, CircleShape)
                .border(BORDER, outline, CircleShape),
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
 * A thin straight line with a rectangular thumb.
 *
 * Material's slider is a round thumb on a rounded track with a halo and tick marks; Metro's is a
 * line and a block. The track's thickness follows the specification — the same 4 px the progress bar
 * draws. **The thumb's size is this project's**, and stays a parameter because of it: the phone's
 * `Slider` template is not among those recovered.
 */
@Composable
public fun KvadrantSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    thumbWidth: Dp = DEFAULT_THUMB_WIDTH,
    thumbHeight: Dp = DEFAULT_THUMB_HEIGHT,
) {
    val colors = KvadrantTheme.colors
    val fraction = value.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = KvadrantTheme.metrics.touchTargetMin)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onValueChange((offset.x / size.width).coerceIn(0f, 1f)) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val travel = maxWidth - thumbWidth
        Box(Modifier.fillMaxWidth().height(TRACK).background(colors.inactive))
        Box(Modifier.fillMaxWidth(fraction).height(TRACK).background(colors.accent))
        Box(
            Modifier
                .offset(x = travel * fraction)
                .size(thumbWidth, thumbHeight)
                .background(colors.foreground),
        )
    }
}

private val BOX = 24.dp // 32 px
private val BORDER = 2.25.dp // PhoneBorderThickness / PhoneStrokeThickness, both 3 px
private val TICK_WIDTH = 17.25.dp // 23 px
private val TICK_HEIGHT = 15.75.dp // 21 px
private val CONTENT_GAP = 9.dp // the content's 12,0,0,0 margin
private val DEFAULT_THUMB_WIDTH = 9.dp
private val DEFAULT_THUMB_HEIGHT = 24.dp
private val TRACK = 3.dp // 4 px, the same line the progress bar draws
