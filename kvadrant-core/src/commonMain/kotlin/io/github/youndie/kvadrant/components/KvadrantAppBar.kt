package io.github.youndie.kvadrant.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The bar along the bottom of a Windows Phone page: at most four circular buttons and an overflow
 * menu, 72 px tall.
 *
 * **The bar draws the circle; the caller supplies the glyph.** Forty icons in one style on a 26×26
 * grid is a designer's week and the part of a project like this that is routinely planned as a
 * programmer's afternoon, so it is not on the critical path — see D10. [KvadrantAppBarButton] takes
 * a slot and centres whatever goes in it.
 *
 * The menu labels are lowercase because the phone's were, and `mini = true` is the 30 px form the
 * bar collapses to when a page wants its space back.
 */
@Composable
public fun KvadrantAppBar(
    modifier: Modifier = Modifier,
    mini: Boolean = false,
    menuItems: List<String> = emptyList(),
    onMenuItemClick: (Int) -> Unit = {},
    menuExpanded: Boolean = false,
    onMenuToggle: () -> Unit = {},
    cyrillic: FontFamily? = null,
    buttons: @Composable () -> Unit = {},
) {
    val colors = KvadrantTheme.colors

    // Background to the very bottom, content above the navigation bar. The order of those two
    // modifiers is the whole of edge-to-edge: padding *before* the background insets the paint and
    // leaves the system bar showing the page behind it, padding *after* it fills to the edge and
    // keeps the buttons reachable. `navigationBars` is empty on the desktop, so this costs nothing
    // there and needs no per-platform branch.
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.chrome)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        if (menuExpanded && menuItems.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                menuItems.forEachIndexed { index, label ->
                    KvadrantText(
                        // The phone lowercased these, and so does this.
                        label.lowercase(),
                        Modifier
                            .fillMaxWidth()
                            .clickable { onMenuItemClick(index) }
                            .padding(vertical = 9.dp),
                        // `PhoneFontSizeLarge`, and the reference is the toolkit's `MenuItem`
                        // rather than the native ApplicationBar: the native bar is drawn by
                        // the shell, not by a template, so its metrics are not published.
                        // The nearest thing Microsoft did publish is a menu, and it is Large.
                        KvadrantTheme.typography.large,
                        cyrillic,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(if (mini) MINI_HEIGHT else HEIGHT),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons()
            if (menuItems.isNotEmpty()) {
                KvadrantAppBarButton(onClick = onMenuToggle) {
                    KvadrantText("···", style = KvadrantTheme.typography.normal)
                }
            }
        }
    }
}

/**
 * One circular button: 48×48 px with a 1.5 px ring, and a 26×26 px space in the middle for a glyph.
 * It tilts, like everything else the theme touches.
 */
@Composable
public fun KvadrantAppBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    // **A circle with a glyph in it is unreadable to anything that is not looking at it.** The
    // original had no such problem: an ApplicationBar button carried a `Text` alongside its icon,
    // shown when the bar was opened, and that text was the button's name. This is that name, and it
    // is nullable only because it arrived after the component did — a button without one announces
    // as a button and nothing else, which is what every one of them did until now.
    val described =
        if (label == null) {
            Modifier
        } else {
            Modifier.semantics { contentDescription = label }
        }
    val colors = KvadrantTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val tint = if (enabled) colors.foreground else colors.disabled

    Box(
        modifier
            .then(described)
            .size(BUTTON)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            ).border(RING, tint, CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private val HEIGHT: Dp = 54.dp // 72 px
private val MINI_HEIGHT: Dp = 22.5.dp // 30 px
private val BUTTON: Dp = 36.dp // 48 px
private val RING: Dp = 1.125.dp // 1.5 px

/** The glyph box inside a bar button: 26×26 px. Exposed so a caller can size its own icon to it. */
public val KvadrantAppBarGlyphSize: Dp = 19.5.dp
