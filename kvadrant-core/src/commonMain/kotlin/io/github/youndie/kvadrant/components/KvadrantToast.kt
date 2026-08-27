package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.delay

/**
 * The notification that slides down from the top and waits ten seconds.
 *
 * **From the top, not the bottom.** Material's snackbar rises from the bottom edge over the content
 * it is talking about; Metro's toast comes down over the status bar, because on a phone the bottom
 * of the screen belongs to the application bar. Anyone reaching for a `SnackbarHost` here gets the
 * right behaviour in the wrong place.
 *
 * Ten seconds is the phone's timeout, not a preference.
 */
@Composable
public fun KvadrantToast(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    onDismiss: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    val slide by animateFloatAsState(
        targetValue = if (visible) 0f else -1f,
        animationSpec = tween(SLIDE_MILLIS, easing = KvadrantEasing.ExponentialOut6),
        label = "toast",
    )

    LaunchedEffect(visible) {
        if (visible) {
            delay(TIMEOUT_MILLIS)
            onDismiss()
        }
    }

    if (slide > -1f) {
        Column(
            modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = slide * size.height }
                .background(colors.chrome)
                .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            KvadrantText(title, style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
            if (message != null) {
                KvadrantText(
                    message,
                    style = KvadrantTheme.typography.subtle.copy(color = colors.subtle),
                    cyrillic = cyrillic,
                )
            }
        }
    }
}

private const val TIMEOUT_MILLIS = 10_000L
private const val SLIDE_MILLIS = 350
