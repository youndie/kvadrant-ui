package io.github.youndie.kvadrant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * A Windows Phone page: titles at the top in the content, an application bar at the bottom, and the
 * page margin applied once instead of by every caller.
 *
 * This exists because five demo screens assembled it by hand first, identically each time — the
 * page margin, the title block, the scroll, the bar pinned below. That is the signal a thing should
 * be a component rather than a pattern people copy.
 *
 * **The title scrolls away and the bar does not**, which is the arrangement Metro chose: the top of
 * the screen belongs to the content, the bottom belongs to the application.
 */
@Composable
public fun KvadrantPage(
    modifier: Modifier = Modifier,
    applicationTitle: String? = null,
    pageTitle: String? = null,
    scrollable: Boolean = true,
    cyrillic: FontFamily? = null,
    appBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val margin = KvadrantTheme.metrics.margin
    val scroll = rememberScrollState()

    Column(modifier.fillMaxSize().background(KvadrantTheme.colors.background)) {
        Column(
            Modifier
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier),
        ) {
            if (applicationTitle != null || pageTitle != null) {
                KvadrantPageHeader(
                    applicationTitle.orEmpty(),
                    pageTitle.orEmpty(),
                    cyrillic = cyrillic,
                )
            }
            Column(Modifier.padding(horizontal = margin), content = content)
        }
        appBar?.invoke()
    }
}

/**
 * A flat, rectangular surface in the theme's chrome colour.
 *
 * There is nothing to configure — no elevation, no corner radius, no tonal tint — and the absence
 * is the component. Passing a colour is for the cases where a caller means a specific token.
 */
@Composable
public fun KvadrantSurface(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = KvadrantTheme.colors.chrome,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.background(color), content = content)
}
