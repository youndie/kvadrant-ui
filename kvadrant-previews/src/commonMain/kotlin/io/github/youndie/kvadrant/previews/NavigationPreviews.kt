package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantAppBar
import io.github.youndie.kvadrant.components.KvadrantAppBarButton
import io.github.youndie.kvadrant.components.KvadrantListItem
import io.github.youndie.kvadrant.components.KvadrantPage
import io.github.youndie.kvadrant.components.KvadrantPageHeader
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.components.KvadrantPivotHeaders
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.components.rememberKvadrantPivotState
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.icons.KvadrantIcons
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme

internal fun navigationPreviews(): List<KvadrantPreview> =
    listOf(
        KvadrantPreview(
            id = "page",
            component = "KvadrantPage",
            summary = "the page frame: application title, page title, and the margins under them",
            heightDp = 420,
        ) { PagePreview() },
        KvadrantPreview(
            id = "page-header",
            component = "KvadrantPageHeader",
            summary = "the two lines every Windows Phone page opened with",
            heightDp = 200,
        ) { PageHeaderPreview() },
        KvadrantPreview(
            id = "pivot",
            component = "KvadrantPivot",
            summary = "swipe sideways: the headers travel at their own rate, and the ends wrap round",
            heightDp = 420,
        ) { PivotPreview() },
        KvadrantPreview(
            id = "pivot-headers",
            component = "KvadrantPivotHeaders",
            summary = "the header strip on its own, driven by a pager",
            heightDp = 220,
        ) { PivotHeadersPreview() },
        KvadrantPreview(
            id = "panorama",
            component = "KvadrantPanorama",
            summary = "the long horizontal canvas, with its title parallaxing behind the sections",
            heightDp = 460,
        ) { PanoramaPreview() },
        KvadrantPreview(
            id = "app-bar",
            component = "KvadrantAppBar",
            summary = "four round buttons and a menu — tap the ellipsis",
            heightDp = 320,
        ) { AppBarPreview() },
        KvadrantPreview(
            id = "app-bar-button",
            component = "KvadrantAppBarButton",
            summary = "one circle, drawn rather than filled, at the glyph size the guidelines gave",
            heightDp = 180,
        ) { AppBarButtonPreview() },
    )

@Composable
private fun Glyph(name: String) {
    val vector = KvadrantIcons.All.first { it.first == name }.second
    Box(
        Modifier
            .size(19.5.dp)
            .paint(
                rememberVectorPainter(vector),
                colorFilter = ColorFilter.tint(KvadrantTheme.colors.foreground),
            ),
    )
}

@Composable
private fun PagePreview() {
    KvadrantPage(
        Modifier.fillMaxSize(),
        applicationTitle = "KVADRANT",
        pageTitle = "inbox",
    ) {
        KvadrantListItem("Anna Peterson", subtitle = "meeting on Thursday", onClick = {})
        KvadrantListItem("build server", subtitle = "nightly is green", onClick = {})
        KvadrantListItem("Kim Larsen", subtitle = "re: the invoice", onClick = {})
    }
}

@Composable
private fun PageHeaderPreview() {
    Box(Modifier.fillMaxSize()) {
        KvadrantPageHeader("KVADRANT", "settings")
    }
}

@Composable
private fun PivotPreview() {
    KvadrantPivot(
        titles = listOf("inbox", "sent", "drafts"),
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        Column {
            repeat(3) { row ->
                KvadrantListItem("page $page, row $row", onClick = {})
            }
        }
    }
}

@Composable
private fun PivotHeadersPreview() {
    val titles = listOf("inbox", "sent", "drafts", "archive")
    val state = rememberKvadrantPivotState(titles.size)
    Box(Modifier.fillMaxSize()) {
        KvadrantPivotHeaders(titles, state)
    }
}

@Composable
private fun PanoramaPreview() {
    KvadrantPanorama(
        title = "kvadrant",
        modifier = Modifier.fillMaxSize(),
        sections =
            listOf(
                "recent" to {
                    Column {
                        KvadrantTile(TileSize.Medium, color = KvadrantAccents.Cobalt)
                    }
                },
                "all" to {
                    Column {
                        KvadrantListItem("Amsterdam", onClick = {})
                        KvadrantListItem("Berlin", onClick = {})
                    }
                },
                "what's new" to {
                    Column {
                        KvadrantText("nothing yet", style = KvadrantTheme.typography.normal)
                    }
                },
            ),
    )
}

@Composable
private fun AppBarPreview() {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        KvadrantAppBar(
            Modifier.align(Alignment.BottomCenter),
            menuItems = listOf("mark as read", "move to folder"),
            menuExpanded = expanded,
            onMenuToggle = { expanded = !expanded },
        ) {
            KvadrantAppBarButton({}) { Glyph("add") }
            KvadrantAppBarButton({}) { Glyph("check") }
            KvadrantAppBarButton({}) { Glyph("delete") }
        }
    }
}

@Composable
private fun AppBarButtonPreview() {
    Box(
        Modifier.fillMaxSize().background(KvadrantTheme.colors.chrome),
        contentAlignment = Alignment.Center,
    ) {
        KvadrantAppBarButton({}) { Glyph("check") }
    }
}
