package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.contrastOn
import kotlinx.coroutines.delay

/**
 * A Metro button: a rectangle, a thick border, no fill — and the fill arrives on press, inverted.
 *
 * There is no elevation, no corner radius and no ripple; the press feedback is the theme's tilt.
 */
@Composable
public fun KvadrantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val metrics = KvadrantTheme.metrics
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier
            .defaultMinSize(minHeight = metrics.touchTargetMin)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .border(metrics.borderThickness, colors.foreground, RectangleShape)
            .background(if (pressed) colors.foreground else Color.Transparent)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        KvadrantText(
            text,
            style =
                KvadrantTheme.typography.normal.copy(
                    color = if (pressed) colors.background else colors.foreground,
                ),
            cyrillic = cyrillic,
        )
    }
}

/**
 * The Windows Phone toggle, from the Toolkit's own `Generic.xaml` rather than from its published
 * dimensions — which is the difference between a switch that looks like Metro and one that is it.
 *
 * The track is **always filled with the accent**. Over it sits a rectangle of the page's background
 * colour, 77×20, centred — a window punched through the fill. Switching on slides that window and
 * the thumb 69 px to the right together, and what "turning on" looks like is the accent being
 * uncovered rather than anything changing colour.
 *
 * The track also carries a double border — 3 px of foreground outside, 4 px of background inside —
 * and the thumb is foreground-filled with 4 px background gutters either side and a −4 margin, so it
 * overhangs the track by four pixels at each end. That overhang is what makes the published travel
 * of 69 work out against an 89 px track and a 28 px thumb.
 *
 * Both translations ease out on an exponential of 15 over 50 ms — the same curve as Swivel's exit,
 * and the reason the switch snaps rather than glides.
 */
@Composable
public fun KvadrantToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KvadrantTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val travel by animateDpAsState(
        if (checked) TRAVEL else 0.dp,
        tween(SNAP_MILLIS, easing = KvadrantEasing.ExponentialOut15),
        label = "switch",
    )

    Box(
        modifier
            .size(ROOT_WIDTH, ROOT_HEIGHT)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
            ) { onCheckedChange(!checked) }
            .alpha(if (enabled) 1f else DISABLED_OPACITY),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.size(TRACK_WIDTH, ROOT_HEIGHT), contentAlignment = Alignment.CenterStart) {
            // SwitchBottom: the accent, all of it, all the time.
            Box(
                Modifier
                    .size(TRACK_WIDTH, TRACK_HEIGHT)
                    .background(if (enabled) colors.accent else colors.foreground),
                contentAlignment = Alignment.Center,
            ) {
                // SwitchBackground: the window over the fill, which is what actually moves.
                Box(
                    Modifier
                        .offset(x = travel)
                        .size(WINDOW_WIDTH, WINDOW_HEIGHT)
                        .background(colors.background),
                )
                // The double border, outside in.
                Box(
                    Modifier
                        .matchParentSize()
                        .border(OUTER_BORDER, colors.foreground, RectangleShape)
                        .padding(OUTER_BORDER)
                        .border(INNER_BORDER, colors.background, RectangleShape),
                )
            }

            // SwitchThumb: foreground, with background gutters, overhanging by four either side.
            Box(
                Modifier
                    .offset(x = travel - THUMB_OVERHANG)
                    .size(THUMB_WIDTH, THUMB_HEIGHT)
                    .background(colors.background)
                    .padding(horizontal = THUMB_GUTTER)
                    .background(if (enabled) colors.foreground else colors.disabled),
            )
        }
    }
}

private val ROOT_WIDTH = 102.dp // 136 px, the hit area
private val ROOT_HEIGHT = 71.25.dp // 95 px
private val TRACK_WIDTH = 66.75.dp // 89 px
private val TRACK_HEIGHT = 25.5.dp // 34 px
private val WINDOW_WIDTH = 57.75.dp // 77 px
private val WINDOW_HEIGHT = 15.dp // 20 px
private val OUTER_BORDER = 2.25.dp // 3 px
private val INNER_BORDER = 3.dp // 4 px
private val THUMB_WIDTH = 21.dp // 28 px
private val THUMB_HEIGHT = 28.5.dp // 38 px
private val THUMB_GUTTER = 3.dp // the 4 px left/right borders
private val THUMB_OVERHANG = 3.dp // Margin="-4,0"
private val TRAVEL = 51.75.dp // 69 px

private const val SNAP_MILLIS = 50
private const val DISABLED_OPACITY = 0.3f

/** Black or white on the accent, exposed so a caller can label a tile the way the phone did. */
@Composable
public fun onAccent(): Color = contrastOn(KvadrantTheme.colors.accent)

/**
 * A Metro text field: a rectangle with a thick border and a placeholder that vanishes.
 *
 * Two behaviours here look like defects and are canon, so they are written down rather than fixed.
 * The placeholder disappears the moment the field has focus, not the moment it has text — so an
 * empty focused field shows nothing at all. And the field does not scroll internally; it grows.
 */
@Composable
public fun KvadrantTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val metrics = KvadrantTheme.metrics
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier
            .defaultMinSize(minHeight = metrics.touchTargetMin)
            .border(metrics.borderThickness, if (focused) colors.accent else colors.textBox, RectangleShape)
            .background(if (focused) colors.textBoxEditBackground else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && !focused && placeholder.isNotEmpty()) {
            KvadrantText(
                placeholder,
                style = KvadrantTheme.typography.normal.copy(color = colors.subtle),
                cyrillic = cyrillic,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            textStyle =
                KvadrantTheme.typography.normal.copy(
                    color = if (focused) colors.contrastForeground else colors.foreground,
                ),
            cursorBrush = SolidColor(if (focused) colors.contrastForeground else colors.foreground),
            singleLine = true,
        )
    }
}

/**
 * Two lines: the title at 20 px and the second line at 18.667 px in the subtle colour. The whole row
 * is the touch target, and it tilts.
 */
@Composable
public fun KvadrantListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                },
            ).padding(vertical = 6.dp),
    ) {
        KvadrantText(title, style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
        if (subtitle != null) {
            KvadrantText(
                subtitle,
                style = KvadrantTheme.typography.subtle.copy(color = colors.subtle),
                cyrillic = cyrillic,
            )
        }
    }
}

/**
 * The determinate bar: a straight line, part of it accent, the rest the inactive token.
 *
 * Four pixels tall and nothing else — no rounded cap, no gap before the head, no stop indicator.
 * Material grew all three; Metro had none of them.
 */
@Composable
public fun KvadrantProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = KvadrantTheme.colors
    Box(modifier.fillMaxWidth().height(3.dp).background(colors.inactive)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(colors.accent),
        )
    }
}

/**
 * A password field that shows the character you just typed, then hides it.
 *
 * Two seconds, or the moment the next key arrives — whichever comes first. That behaviour is the
 * reason the phone's password entry felt usable on a touch keyboard, and it is a timing
 * (`passwordMaskMs` = 2000) rather than a preference.
 */
@Composable
public fun KvadrantPasswordBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    cyrillic: FontFamily? = null,
) {
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value.isEmpty()) {
            revealed = false
        } else {
            revealed = true
            delay(MASK_MILLIS)
            revealed = false
        }
    }

    val shown =
        when {
            value.isEmpty() -> ""
            revealed -> MASK.toString().repeat(value.length - 1) + value.last()
            else -> MASK.toString().repeat(value.length)
        }

    KvadrantTextBox(
        value = shown,
        onValueChange = { typed ->
            // Only the length is meaningful once the text is masked, so edits are applied to the
            // real value rather than to what is on screen.
            when {
                typed.length < value.length -> onValueChange(value.dropLast(value.length - typed.length))
                typed.length > value.length -> onValueChange(value + typed.takeLast(typed.length - value.length))
            }
        },
        modifier = modifier,
        placeholder = placeholder,
        cyrillic = cyrillic,
    )
}

private const val MASK = '\u25CF'
private const val MASK_MILLIS = 2000L
