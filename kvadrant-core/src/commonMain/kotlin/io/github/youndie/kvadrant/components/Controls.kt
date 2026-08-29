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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantWeights
import io.github.youndie.kvadrant.theme.contrastOn
import kotlinx.coroutines.delay

/**
 * A Metro button, from `PhoneButtonBase` in the SDK's own `System.Windows.xaml`.
 *
 * A rectangle with a thick border and no fill; pressing inverts it — the border colour floods the
 * background and the text turns the colour of the page. There is no elevation, no corner radius
 * (`CornerRadius="0"` is written out in the template) and no ripple; the rest of the press feedback
 * is the theme's tilt.
 *
 * **The visible rectangle is smaller than the button.** In the template the `Border` carries
 * `Margin="{StaticResource PhoneTouchTargetOverhang}"` — 12 px on every side — inside a `Grid` whose
 * `Background="Transparent"` makes it hit-testable. So twelve pixels of invisible button surround
 * the frame in every direction, and two buttons side by side sit 24 px apart while looking 24 px
 * apart plus a gap. Reproducing the frame without the overhang, which is what this used to do,
 * gives a control that measures right and is harder to hit.
 *
 * Every number here is Microsoft's, at this library's canonical 0.75 of a Metro pixel:
 * `BorderThickness` 3, the overhang 12, `Padding="10,3,10,5"` — asymmetric top to bottom, 3 above
 * and 5 below — and `PhoneFontSizeMediumLarge` 25.333 in `PhoneFontFamilySemiBold`. The type slot
 * matters: this was set in [KvadrantTypography.normal] and so came out two steps of the ramp too
 * small and a weight too light.
 *
 * [interactionSource] is hoisted for the same reason Material hoists it — a caller that wants to
 * observe or drive the press — and here it is also the only way to photograph the pressed state,
 * which a golden otherwise cannot reach.
 *
 * [enabled] is the template's `Disabled` state and nothing more: text and border go to
 * [KvadrantColors.disabled], the background is forced back to transparent — so a button disabled
 * mid-press does not stay filled — and the tilt is not raised, because a disabled `ButtonBase`
 * never entered the pressed state to begin with.
 */
@Composable
public fun KvadrantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = KvadrantButtonDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val metrics = KvadrantTheme.metrics
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && pressed

    // The line of the frame, and what it does to the text inside it. Disabled wins over pressed:
    // the template's Disabled storyboard sets Background back to Transparent explicitly.
    val line = if (enabled) colors.foreground else colors.disabled
    val fill = if (active) colors.foreground else Color.Transparent
    val ink =
        when {
            !enabled -> colors.disabled
            pressed -> colors.background
            else -> colors.foreground
        }

    Box(
        modifier
            // D7: the canonical target is the frame plus its overhang, which already clears the
            // modern minimum at the default type — this holds it when a caller shrinks the text.
            .defaultMinSize(minHeight = metrics.touchTargetMin)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
            // The Grid's transparent, hit-testable margin. Everything below is the Border.
            .padding(metrics.touchTargetOverhang),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .border(metrics.borderThickness, line, RectangleShape)
                .background(fill)
                // XAML reserves the border thickness and then applies Padding inside it; Compose
                // draws the border over the bounds and insets nothing, so the thickness is added.
                .padding(metrics.borderThickness)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            KvadrantText(
                text,
                style =
                    KvadrantTheme.typography.mediumLarge.copy(
                        color = ink,
                        fontWeight = KvadrantWeights.SemiBold,
                    ),
                cyrillic = cyrillic,
            )
        }
    }
}

/** The button's defaults, so the one number a caller is likely to want is reachable. */
public object KvadrantButtonDefaults {
    /**
     * `Padding="10,3,10,5"` at 0.75, **as the current theme has scaled it**.
     *
     * Composable rather than a constant, and that is the whole point: it is read out of
     * [KvadrantMetrics] so a caller that scales the metric set scales this with it. As a constant
     * it was the one measurement on a button that did not move, and a scaled-up button came out
     * with a grown line of text inside an ungrown frame.
     */
    public val contentPadding: PaddingValues
        @Composable @ReadOnlyComposable
        get() =
            with(KvadrantTheme.metrics) {
                PaddingValues(
                    start = buttonPaddingHorizontal,
                    top = buttonPaddingTop,
                    end = buttonPaddingHorizontal,
                    bottom = buttonPaddingBottom,
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
            // `toggleable` carries `Role.Switch` and the on/off state into the semantics tree;
            // `clickable`, which this was, reported a tappable box and nothing about what it was or
            // which way it was set. The interaction source and indication are unchanged, so the
            // tilt is untouched.
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ).alpha(if (enabled) 1f else DISABLED_OPACITY),
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
 * A Metro text box, from `PhoneTextBox` in the SDK's own `System.Windows.xaml`.
 *
 * **It is a light box in both themes, and this drew a transparent one.** `Background` and
 * `BorderBrush` are both `PhoneTextBoxBrush` — 75 % white on a dark page, 15 % black on a light one
 * — and `Foreground` is `PhoneTextBoxForegroundBrush`, dark on that fill either way. What this had
 * was a transparent field with the page's own text colour in it, which reads as a Material outlined
 * field and is the single most recognisable thing about a Windows Phone form got wrong.
 *
 * Focus does **not** bring in the accent. `PhoneTextBoxEditBorderBrush` is white on dark and 87 %
 * black on light — the page's foreground — and `PhoneTextBoxEditBackgroundBrush` goes to solid
 * white on dark and to *transparent* on light. So focusing brightens a dark theme's box and empties
 * a light theme's, which is the light theme not being an inversion again.
 *
 * Geometry is the template's: the border sits inside `PhoneTouchTargetOverhang`, so twelve pixels of
 * invisible field surround it exactly as they surround a button, and the content carries
 * `Padding="2"` plus `PhoneTextBoxInnerMargin` of `1,2`. [contentPadding] is a parameter only
 * because `PhonePasswordBox` differs from this in one number — its inner margin is `3,2`.
 *
 * **[actionIcon] is the Toolkit's half of this control**, and it was missing for as long as the
 * library had the plain half ([B-43](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-43-the-toolkit-was-never-inventoried.md)).
 * The Toolkit's `PhoneTextBox` puts an `ActionIconBorder` of `Width="84" Height="72"` at the bottom
 * right of the field, transparent, holding a `26 × 26` image — so the icon is small and its target
 * is not, which is the same relationship the app bar's glyphs have to their rings. Twenty-six is
 * also the app bar's own icon size, which is corroboration rather than coincidence.
 *
 * The icon is a slot rather than a drawable because [D10](https://github.com/youndie/kvadrant-ui/blob/main/docs/research/research-architecture.md)
 * says so: no Segoe asset enters this repository, and a caller's glyph is the only kind there is.
 */
@Composable
public fun KvadrantTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    contentPadding: PaddingValues = KvadrantTextBoxDefaults.ContentPadding,
    actionIcon: (@Composable () -> Unit)? = null,
    onActionIconClick: (() -> Unit)? = null,
    actionIconLabel: String? = null,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val metrics = KvadrantTheme.metrics
    var focused by remember { mutableStateOf(false) }
    val active = focused && enabled

    // The Disabled storyboard swaps the whole border for one that is transparent with a disabled
    // outline, so it wins over focus rather than blending with it.
    val line =
        when {
            !enabled -> colors.disabled
            active -> colors.foreground
            else -> colors.textBox
        }
    val fill =
        when {
            !enabled -> Color.Transparent
            active -> colors.textBoxEditBackground
            else -> colors.textBox
        }
    val ink = if (enabled) colors.textBoxForeground else colors.disabled

    Box(
        modifier
            .defaultMinSize(minHeight = metrics.touchTargetMin)
            .padding(metrics.touchTargetOverhang)
            .border(metrics.borderThickness, line, RectangleShape)
            .background(fill)
            .padding(metrics.borderThickness)
            .padding(contentPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && !active && placeholder.isNotEmpty()) {
            KvadrantText(
                placeholder,
                style = KvadrantTheme.typography.mediumLarge.copy(color = ink.copy(alpha = PLACEHOLDER_ALPHA)),
                cyrillic = cyrillic,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            // `PhoneFontSizeMediumLarge`, 25.333 px: `Style TargetType="controls:PhoneTextBox"`
            // in the toolkit's `Generic.xaml`. Typing into a field happens at a size above the
            // page's body text, which is a thing every screenshot of the phone shows and no
            // amount of looking at our own output would have suggested.
            textStyle = KvadrantTheme.typography.mediumLarge.copy(color = ink),
            cursorBrush = SolidColor(ink),
            singleLine = true,
        )
        if (actionIcon != null) {
            // Bottom right and inside the border, as `ActionIconBorder` is: an 84 × 72 px target
            // around a 26 × 26 px glyph, so what a thumb hits is nearly three times what the eye
            // sees. Aligned to the *end* rather than the right, because a right-to-left layout
            // wants it on the other side and B-41 is where that gets checked.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(KvadrantTextBoxDefaults.ActionIconTarget)
                    .then(if (onActionIconClick == null) Modifier else Modifier.clickable(onClick = onActionIconClick))
                    // The same arrangement `KvadrantAppBarButton` has, for the same reason and
                    // found the same way: a glyph is a picture, so the only name it can have is the
                    // caller's. `InteractiveNodesAreNamedTest` failed on this target the first time
                    // it existed, which is what that guard is for.
                    .then(
                        if (actionIconLabel == null) {
                            Modifier
                        } else {
                            Modifier.semantics { contentDescription = actionIconLabel }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(KvadrantTextBoxDefaults.ActionIconSize)) { actionIcon() }
            }
        }
    }
}

/** The two inner margins the platform gives a field, which are the only thing that differs. */
public object KvadrantTextBoxDefaults {
    /** `Padding="2"` plus `PhoneTextBoxInnerMargin="1,2"`, at 0.75. */
    public val ContentPadding: PaddingValues = PaddingValues(horizontal = 2.25.dp, vertical = 3.dp)

    /** The same, with `PhonePasswordBoxInnerMargin="3,2"` instead. */
    public val PasswordContentPadding: PaddingValues = PaddingValues(horizontal = 3.75.dp, vertical = 3.dp)

    /** `ActionIconBorder`'s `Width="84" Height="72"`, at 0.75. */
    public val ActionIconTarget: DpSize = DpSize(63.dp, 54.dp)

    /** The `Image` inside it: `26 × 26`, the app bar's icon size as well. */
    public val ActionIconSize: Dp = 19.5.dp
}

/**
 * How much of the field's own ink a placeholder gets.
 *
 * **This project's number.** The template has no placeholder at all — `PhoneTextBox`'s hint text is
 * the Toolkit's `PhoneTextBox`, a different control, and it sets the hint with a brush this
 * dictionary does not contain.
 */
private const val PLACEHOLDER_ALPHA = 0.5f

/**
 * Two lines: the title at 20 px and the second line at 18.667 px in the subtle colour. The whole row
 * is the touch target, and it tilts.
 *
 * **[titleStyle] is a parameter because Microsoft published no number for it.** The page default is
 * `PhoneFontSizeNormal` and a list item that overrides nothing gets it — that is the whole of what
 * the SDK's dictionary and the toolkit's six `FontSize` overrides say, and none of the six is a
 * list. The phone's own Mail, People and Messaging lists were visibly larger than 20 px, and they
 * were larger because each of those *applications* said so, in a data template that shipped inside
 * the application and not in any dictionary. So the default here is the only defensible one and it
 * is also, on a modern screen, the one that reads as small — which is why the way out is an
 * argument rather than a different constant. Research §1.10.
 */
@Composable
public fun KvadrantListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    cyrillic: FontFamily? = null,
    titleStyle: TextStyle = KvadrantTheme.typography.normal,
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
        KvadrantText(title, style = titleStyle, cyrillic = cyrillic)
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
 * The determinate half of the same `ProgressBar` template: two 4 px rectangles, one over the other.
 *
 * **The track is the accent at a tenth, not a grey.** `ProgressBarTrack` is
 * `Fill="{TemplateBinding Background}"` with `Opacity="0.1"`, and the style sets *both* `Background`
 * and `Foreground` to `PhoneAccentBrush` — so an amber theme gets an amber ghost behind an amber
 * bar, and this used [KvadrantColors.inactive], a neutral, which reads as a Material track wearing
 * Metro's colours.
 *
 * `Padding="{StaticResource PhoneHorizontalMargin}"` — 12,0 — is the template's, and applies to the
 * determinate root as much as to the dots.
 */
@Composable
public fun KvadrantProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    // Determinate, so it announces where it is. Without this a screen reader finds a coloured
    // rectangle and can say nothing about how far along it is, which is the only thing it is for.
    val announced =
        Modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
        }
    val colors = KvadrantTheme.colors
    val metrics = KvadrantTheme.metrics
    Box(
        modifier
            .then(announced)
            .fillMaxWidth()
            .padding(horizontal = metrics.margin)
            .height(metrics.progressThickness)
            .background(colors.accent.copy(alpha = TRACK_ALPHA)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(metrics.progressThickness)
                .background(colors.accent),
        )
    }
}

/** `Opacity="0.1"` on `ProgressBarTrack`. */
private const val TRACK_ALPHA = 0.1f

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
    mask: Char = KVADRANT_PASSWORD_MASK,
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
            revealed -> mask.toString().repeat(value.length - 1) + value.last()
            else -> mask.toString().repeat(value.length)
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
        // `PhonePasswordBoxInnerMargin="3,2"` where a text box has `1,2`. Two Metro pixels, and the
        // only structural difference between the two templates in the whole dictionary.
        contentPadding = KvadrantTextBoxDefaults.PasswordContentPadding,
        cyrillic = cyrillic,
    )
}

/**
 * What a masked character is drawn as, and **it is not Silverlight's**.
 *
 * `PasswordChar` defaults to `U+25CF BLACK CIRCLE`, and Selawik's `cmap` does not carry it — read
 * out of the font, not assumed. A text field draws through `BasicTextField`, which does no script
 * splitting, so the circle fell through to whatever the operating system had: the dots were a
 * different glyph on every platform, and the way it surfaced was a screenshot that would not match
 * between two of them.
 *
 * `U+2022 BULLET` **is** in Selawik. It is smaller than the circle and it is this project's choice
 * rather than Microsoft's, which is why it is a parameter of [KvadrantPasswordBox] — a caller with a
 * font that has the circle can ask for it back.
 */
public const val KVADRANT_PASSWORD_MASK: Char = '\u2022'

private const val MASK = KVADRANT_PASSWORD_MASK
private const val MASK_MILLIS = 2000L
