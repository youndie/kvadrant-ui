package io.github.youndie.kvadrant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The top of a Windows Phone page: a small application title and an oversized page title beneath
 * it, both in the content and both scrolling away with it.
 *
 * **This is not a top app bar and the difference is the point.** Metro has no bar: the title is text
 * in the flow, so it leaves the screen as you scroll and gives the page its height back. Reaching
 * for `TopAppBar` here produces a Material screen wearing Metro's colours.
 *
 * The margins are the template's: the title panel sits at `12,17,0,28` and the page title is pulled
 * up by 7 px inside it — `9,-7,0,0`. That negative number is not a mistake to tidy away; it is how
 * the two titles end up as close together as they are.
 *
 * **Right-to-left needs nothing here, and that is checked rather than assumed**
 * ([B-41](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-41-rtl-is-canon-and-untested.md)).
 * The asymmetric margins are `padding(start = …)` and a vertical `offset`, both of which Compose
 * mirrors on its own, so `rtl/page header` shows the title against the right margin with no code
 * for it. Said here because the next person asking will otherwise re-check all three of the custom
 * layouts, and this is the one that was free.
 */
@Composable
public fun KvadrantPageHeader(
    applicationTitle: String,
    pageTitle: String,
    modifier: Modifier = Modifier,
    cyrillic: FontFamily? = null,
) {
    Column(
        modifier.padding(start = 9.dp, top = 12.75.dp, bottom = 21.dp),
    ) {
        KvadrantText(
            applicationTitle,
            style = KvadrantTheme.typography.pageTitle,
            cyrillic = cyrillic,
        )
        KvadrantText(
            pageTitle,
            // `9,-7,0,0`: 6.75 dp in, and 5.25 dp *up* — an offset, because a negative padding
            // is not a thing and pretending otherwise would silently drop the number.
            Modifier.padding(start = 6.75.dp).offset(y = (-5.25).dp),
            KvadrantTheme.typography.pivotHeader,
            cyrillic,
        )
    }
}
