package io.github.youndie.kvadrant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * A text box that offers what it thinks you are typing, from the Toolkit's `AutoCompleteBox`.
 *
 * **The library documented this control before it had it.** `KvadrantTypography.mediumLarge`'s KDoc
 * names "a text box, a list picker and an autocomplete box" as the three controls Microsoft sized up
 * from the page default — a sentence about a component that was not here, which is what
 * [B-43](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-43-the-toolkit-was-never-inventoried.md)
 * was about.
 *
 * **The suggestion list is white in both themes, and that is transcribed rather than chosen.** The
 * template sets the `ListBox` inside its `Popup` to a literal `Background="White"` — not a theme
 * brush, not `PhoneBackgroundBrush` — with `BorderBrush="{StaticResource PhoneTextBoxEditBorderBrush}"`
 * around it. It is the same decision the focused field makes, where a dark theme's box goes solid
 * white: what you are typing into is a sheet of paper whatever the page behind it is.
 *
 * | Transcribed | Value |
 * |---|---|
 * | The list's fill | literal `White` |
 * | Its border | `PhoneTextBoxEditBorderBrush`, the same the focused field takes |
 * | `Padding` on the list | `0,8` |
 * | `Margin` on a row | `8,7` |
 * | The field's `Padding` | `6,0,6,4` — asymmetric, and deeper at the bottom than the top |
 * | `FilterMode` | `StartsWith`, case-insensitive, from the property's own metadata |
 * | `MinimumPrefixLength` | **1** — nothing is offered until a character is typed |
 *
 * **One deviation, and it is a layout one.** The Toolkit puts the list in a `Popup`, so it floats
 * over whatever follows the field. This draws it in the control's own layout, below the field, which
 * pushes the rest of the form down instead. A `Popup` renders outside the layout it belongs to,
 * where neither a screenshot of this component nor a page that wants to place it can reach it — and
 * a component whose whole point is only visible in a window nothing here can photograph is a
 * component nobody can check. A caller who wants the float back puts this in a `Box` above the rest.
 *
 * @param suggestions everything that could be offered; [filter] decides what is. The Toolkit's
 *   `ItemsSource` is the same list, and it did its own filtering for the same reason: the caller
 *   knows what a match means and a control does not.
 * @param filter defaults to the Toolkit's own `StartsWith`, ignoring case. It takes the typed text
 *   first and the candidate second.
 */
@Composable
public fun KvadrantAutoCompleteBox(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    minimumPrefixLength: Int = MINIMUM_PREFIX_LENGTH,
    maximumSuggestions: Int = Int.MAX_VALUE,
    filter: (String, String) -> Boolean = ::startsWithIgnoringCase,
    cyrillic: FontFamily? = null,
) {
    val offered =
        if (value.length < minimumPrefixLength) {
            emptyList()
        } else {
            suggestions.filter { filter(value, it) }.take(maximumSuggestions)
        }

    Column(modifier) {
        KvadrantTextBox(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder,
            enabled = enabled,
            cyrillic = cyrillic,
        )
        if (offered.isNotEmpty()) {
            Column(
                Modifier
                    // The overhang the field is drawn inside, so the list's edges line up with the
                    // field's border rather than with its invisible touch target.
                    .padding(horizontal = KvadrantTheme.metrics.touchTargetOverhang)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = LIST_PADDING),
            ) {
                offered.forEach { suggestion ->
                    KvadrantText(
                        suggestion,
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onSuggestionSelect(suggestion) }
                            .padding(horizontal = ROW_MARGIN_HORIZONTAL, vertical = ROW_MARGIN_VERTICAL),
                        // The list takes the field's `Foreground`, which is
                        // `PhoneTextBoxForegroundBrush` — dark ink, because the sheet is white in
                        // both themes and the page's own foreground would vanish into it on dark.
                        KvadrantTheme.typography.mediumLarge.copy(color = KvadrantTheme.colors.textBoxForeground),
                        cyrillic,
                    )
                }
            }
        }
    }
}

/** `AutoCompleteFilterMode.StartsWith`, which the property's metadata makes the default. */
private fun startsWithIgnoringCase(
    typed: String,
    candidate: String,
): Boolean = candidate.startsWith(typed, ignoreCase = true)

/** `MinimumPrefixLength`'s `new PropertyMetadata(1, …)`: one character before anything is offered. */
private const val MINIMUM_PREFIX_LENGTH = 1

/** `Padding="0,8"` on the list, at 0.75. */
private val LIST_PADDING = 6.dp

/** `Margin="8,7"` on a row, at 0.75. */
private val ROW_MARGIN_HORIZONTAL = 6.dp
private val ROW_MARGIN_VERTICAL = 5.25.dp
