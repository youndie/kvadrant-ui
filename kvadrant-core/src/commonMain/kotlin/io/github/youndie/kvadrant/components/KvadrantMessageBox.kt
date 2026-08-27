package io.github.youndie.kvadrant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The phone's message box: a full-width band across the top of the screen, not a floating card.
 *
 * **The positive action is on the left.** That is the opposite of Material, and of Windows 8, and it
 * is not a mistake to be tidied up — it is what the phone did, and a user who reaches for the
 * left-hand button expects it to be the one that proceeds. [win8ButtonOrder] flips it for anyone
 * building the desktop profile.
 *
 * Only OK and OK/Cancel exist. The phone had no others.
 */
@Composable
public fun KvadrantMessageBox(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    confirmText: String = "ok",
    cancelText: String = "cancel",
    win8ButtonOrder: Boolean = false,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors

    // The box carries its own overlay, because the original does: `CustomMessageBox.Show` builds a
    // container, puts a `Rectangle` in it and the box on top, and opens the pair in a `Popup`. A
    // caller handed only the box has no way to know it was supposed to dim the screen, and this one
    // did not — the dialog sat at the top of a perfectly bright page.
    //
    // The fill is `Color.FromArgb(0x99, PhoneBackgroundColor)`: sixty percent of the **theme
    // background**, not a fixed black. In the light theme it is a white veil, which is why
    // `PhoneSemitransparentBrush` — black at 0.667, and a real token used elsewhere — is the wrong
    // one to reach for here.
    Box(modifier.fillMaxSize().background(colors.background.copy(alpha = OVERLAY_ALPHA))) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.chrome)
                // The overlay above covers the whole display, system bars included, because a
                // scrim that stops at the status bar is a scrim with a bright strip across the
                // top. The box insets its own content instead.
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 21.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KvadrantText(title, style = KvadrantTheme.typography.large, cyrillic = cyrillic)
            KvadrantText(message, style = KvadrantTheme.typography.normal, cyrillic = cyrillic)

            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val confirm: @Composable () -> Unit = {
                    KvadrantButton(confirmText, onConfirm, Modifier.weight(1f), cyrillic)
                }
                val cancel: @Composable () -> Unit = {
                    if (onCancel != null) KvadrantButton(cancelText, onCancel, Modifier.weight(1f), cyrillic)
                }
                if (win8ButtonOrder) {
                    cancel()
                    confirm()
                } else {
                    confirm()
                    cancel()
                }
            }
        }
    }
}

/**
 * Dims what is behind something, at `PhoneSemitransparentColor`.
 *
 * **Not what a message box uses**, despite the name it used to carry here: that one dims with 60% of
 * the theme *background*, so it is a white veil in the light theme, and it now lives inside
 * [KvadrantMessageBox] where the original keeps it. This is the token brush, for whatever else wants
 * it.
 */
@Composable
public fun KvadrantScrim(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().background(KvadrantTheme.colors.semitransparent),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

/** `0x99` of 255, from `Color.FromArgb(0x99, ...)` in `CustomMessageBox.Show`. */
private const val OVERLAY_ALPHA = 0.6f
