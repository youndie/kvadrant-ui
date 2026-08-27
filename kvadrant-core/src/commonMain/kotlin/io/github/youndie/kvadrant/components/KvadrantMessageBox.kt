package io.github.youndie.kvadrant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.delay

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
    visible: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    confirmText: String = "ok",
    cancelText: String = "cancel",
    win8ButtonOrder: Boolean = false,
    cyrillic: FontFamily? = null,
    /**
     * Tap the dimmed area to dismiss. **Not the phone's behaviour** — `CustomMessageBox` hands its
     * overlay no handler at all and is closed by a button or by the back key — so it is a parameter,
     * off by default, and a caller that wants it says so.
     */
    dismissOnOutsideClick: Boolean = false,
) {
    val colors = KvadrantTheme.colors

    // `onScreen` outlives `visible` by the length of the exit, because a surface removed on the
    // frame its flag moves has nothing to animate out. There is deliberately no second "compose it
    // closed first" flag: `KvadrantSwivel` holds an `Animatable(SWIVEL_IN_FROM)` and always starts
    // at -45 degrees, so handing it `false` first makes it run its *exit* to +60 and come back —
    // which is what a dialog that would not animate properly was actually doing.
    var onScreen by remember { mutableStateOf(false) }
    if (visible) onScreen = true
    LaunchedEffect(visible) {
        if (!visible) {
            delay(KVADRANT_SWIVEL_OUT_MILLIS.toLong())
            onScreen = false
        }
    }
    if (!onScreen) return

    // The box carries its own overlay, because the original does: `CustomMessageBox.Show` builds a
    // container, puts a `Rectangle` in it and the box beside it, and opens the pair in a `Popup`. A
    // caller handed only the box has no way to know it was supposed to dim the screen.
    //
    // The fill is `Color.FromArgb(0x99, PhoneBackgroundColor)`: sixty percent of the **theme
    // background**, not a fixed black, so in the light theme it is a white veil — which is why
    // `PhoneSemitransparentBrush`, black at 0.667, is the wrong one to reach for here.
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = OVERLAY_ALPHA))
            .then(
                if (dismissOnOutsideClick && onCancel != null) {
                    Modifier.clickable(indication = null, interactionSource = null, onClick = onCancel)
                } else {
                    Modifier
                },
            ),
    ) {
        // The swivel is on the **box**, not on the pair. `GetTransition(this)` is called on the
        // message box itself and the overlay is its sibling, so the dim stays put while the dialog
        // tips in — a scrim that tips away with its dialog is a thing nothing does anywhere.
        KvadrantSwivel(visible = visible, backward = true) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.chrome)
                    // The overlay covers the whole display, system bars included, because a scrim
                    // that stops at the status bar is a scrim with a bright strip across the top.
                    // The box insets its own content instead.
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
                        if (onCancel != null) {
                            KvadrantButton(cancelText, onCancel, Modifier.weight(1f), cyrillic)
                        }
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
