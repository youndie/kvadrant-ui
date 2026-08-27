package io.github.youndie.kvadrant.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The three Material components the adapter cannot reach by theming, given the shape they need.
 *
 * The side-by-side picture is what put them on this list: under the adapter a Material text field
 * and card come out square and unremarkable, while the button stays a pill and the switch and slider
 * stay recognisably Material. Each is here for a measured reason rather than a suspicion —
 * `ButtonDefaults.shape` is `RoundedCornerShape(50%)` from a token and never reads
 * `MaterialTheme.shapes`, and a held Material `Button` does not change a pixel, so neither the
 * theme's shape nor its indication arrives.
 *
 * **These are not Metro components.** [io.github.youndie.kvadrant.components.KvadrantButton] and its
 * neighbours are, and a new screen should use those. What these are for is the screen that already
 * has a Material `Slider` in it and is not going to be rewritten this quarter.
 */
@Composable
public fun KvadrantMaterialButtonShape(): Shape = MaterialTheme.shapes.small

/** A Material [Button] squared and stripped of its container's own colour opinions. */
@Composable
public fun kvadrantButtonColors(): ButtonColors {
    val colors = KvadrantTheme.colors
    return ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = colors.foreground,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = colors.disabled,
    )
}

/**
 * A Material [Switch] in Metro's colours.
 *
 * The shape is not fixable from here — a `Switch` draws a stadium track and a circular thumb from
 * its own internals, with no shape parameter and no theme slot — so what this can do is stop it
 * being the wrong *colour* as well as the wrong shape. A screen that wants the real thing wants
 * `KvadrantToggleSwitch`, which is 89×34 px of track with a rectangular thumb and no curve anywhere.
 */
@Composable
public fun kvadrantSwitchColors(): SwitchColors {
    val colors = KvadrantTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = colors.background,
        checkedTrackColor = colors.accent,
        checkedBorderColor = colors.accent,
        uncheckedThumbColor = colors.foreground,
        uncheckedTrackColor = Color.Transparent,
        uncheckedBorderColor = colors.foreground,
    )
}

/**
 * A Material [Slider] in Metro's colours.
 *
 * Same limitation as the switch and the same reason to prefer
 * [io.github.youndie.kvadrant.components.KvadrantSlider]: Metro's slider is a 4 px line and a
 * rectangular block, and Material's is a rounded track with a gap and stop indicators that are drawn
 * rather than themed.
 */
@Composable
public fun kvadrantSliderColors(): SliderColors {
    val colors = KvadrantTheme.colors
    return SliderDefaults.colors(
        thumbColor = colors.foreground,
        activeTrackColor = colors.accent,
        inactiveTrackColor = colors.inactive,
        disabledThumbColor = colors.disabled,
        disabledActiveTrackColor = colors.disabled,
        disabledInactiveTrackColor = colors.inactive,
    )
}

/** Shared by the wrappers so a caller's press reaches the same tilt every other surface uses. */
@Composable
internal fun rememberInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * A Material [Slider] with Metro's colours applied, since the colours are the half that can be.
 *
 * Provided as a function rather than left to the caller because `SliderDefaults.colors()` takes
 * eleven arguments and getting one of them wrong is how a slider ends up with a Material-purple
 * disabled track that nobody looks at twice.
 */
@Composable
public fun KvadrantMaterialSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        colors = kvadrantSliderColors(),
        interactionSource = rememberInteraction(),
    )
}

/** A Material [Switch] with Metro's colours, for the same reason. */
@Composable
public fun KvadrantMaterialSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = kvadrantSwitchColors(),
    )
}

/** A Material [Button], squared, in Metro's colours, with the border a Metro button has. */
@Composable
public fun KvadrantMaterialButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = KvadrantTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = KvadrantMaterialButtonShape(),
        colors = kvadrantButtonColors(),
        border =
            androidx.compose.foundation.BorderStroke(
                KvadrantTheme.metrics.borderThickness,
                if (enabled) colors.foreground else colors.disabled,
            ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        interactionSource = rememberInteraction(),
    ) { content() }
}
