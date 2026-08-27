package io.github.youndie.kvadrant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

    Column(
        modifier
            .fillMaxWidth()
            .background(colors.chrome)
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

/** Dims what is behind a message box, at `PhoneSemitransparentColor`. */
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
