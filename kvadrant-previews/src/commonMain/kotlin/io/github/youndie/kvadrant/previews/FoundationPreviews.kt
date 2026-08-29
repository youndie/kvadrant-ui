package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantSurface
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.icons.KvadrantIcons
import io.github.youndie.kvadrant.indication.kvadrantTilt
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.contrastOn

internal fun foundationPreviews(): List<KvadrantPreview> =
    listOf(
        KvadrantPreview(
            id = "tilt",
            group = "foundation",
            component = "kvadrantTilt",
            summary = "press and hold: the plane leans towards the finger and follows it",
            heightDp = 260,
        ) { TiltPreview() },
        KvadrantPreview(
            id = "overscroll",
            group = "foundation",
            component = "KvadrantOverscroll",
            summary =
                "drag the list past either end: the content compresses towards the edge rather " +
                    "than sliding off it",
            heightDp = 300,
        ) { OverscrollPreview() },
        KvadrantPreview(
            id = "type-ramp",
            group = "foundation",
            component = "KvadrantTypography",
            summary = "the named styles of a Windows Phone page, at the weights it set them in",
            heightDp = 360,
        ) { TypeRampPreview() },
        KvadrantPreview(
            id = "text",
            group = "foundation",
            component = "KvadrantText",
            summary = "one string set in the bundled Selawik faces",
            heightDp = 140,
        ) { TextPreview() },
        KvadrantPreview(
            id = "accents",
            group = "foundation",
            component = "KvadrantAccents",
            summary = "the twenty accents Windows Phone 8 shipped, each with its readable ink",
            heightDp = 420,
        ) { AccentsPreview() },
        KvadrantPreview(
            id = "icons",
            group = "foundation",
            component = "KvadrantIcons",
            summary = "the drawn icon set — no Segoe glyph is in this library",
            heightDp = 400,
        ) { IconsPreview() },
        KvadrantPreview(
            id = "surface",
            group = "foundation",
            component = "KvadrantSurface",
            summary = "the chrome colour a panel sits on",
            heightDp = 180,
        ) { SurfacePreview() },
    )

@Composable
private fun TiltPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(200.dp)
                .kvadrantTilt {}
                .background(KvadrantTheme.colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            KvadrantText("press me", style = KvadrantTheme.typography.large)
        }
    }
}

/**
 * A list with an end to reach.
 *
 * Short enough that the end arrives in one drag, and banded so the compression is visible: a plain
 * column of text compresses too, but there is nothing in it whose position the eye can hold on to.
 */
@Composable
private fun OverscrollPreview() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        repeat(8) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (index % 2 == 0) KvadrantTheme.colors.chrome else KvadrantTheme.colors.background),
                contentAlignment = Alignment.CenterStart,
            ) {
                KvadrantText("  row $index", style = KvadrantTheme.typography.normal)
            }
        }
    }
}

@Composable
private fun TypeRampPreview() {
    val ramp = KvadrantTheme.typography
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        KvadrantText("pivot header", style = ramp.pivotHeader)
        KvadrantText("panorama title", style = ramp.panoramaTitle)
        KvadrantText("page title", style = ramp.pageTitle)
        KvadrantText("extra large", style = ramp.extraLarge)
        KvadrantText("large", style = ramp.large)
        KvadrantText("medium large", style = ramp.mediumLarge)
        KvadrantText("normal", style = ramp.normal)
        KvadrantText("subtle", style = ramp.subtle)
    }
}

@Composable
private fun TextPreview() {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KvadrantText("the quick brown fox", style = KvadrantTheme.typography.large)
        KvadrantText("jumps over the lazy dog", style = KvadrantTheme.typography.normal)
        KvadrantText("0123456789", style = KvadrantTheme.typography.subtle)
    }
}

@Composable
private fun AccentsPreview() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KvadrantAccents.All.forEach { (name, color) ->
            Box(
                Modifier.fillMaxWidth().background(color).padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                KvadrantText(
                    name.lowercase(),
                    style = KvadrantTheme.typography.normal.copy(color = contrastOn(color)),
                )
            }
        }
    }
}

@Composable
private fun IconsPreview() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KvadrantIcons.All.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (_, vector) ->
                    Box(
                        Modifier
                            .size(32.dp)
                            .paint(
                                rememberVectorPainter(vector),
                                colorFilter = ColorFilter.tint(KvadrantTheme.colors.foreground),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfacePreview() {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        KvadrantSurface(Modifier.fillMaxWidth()) {
            Box(Modifier.padding(16.dp)) {
                KvadrantText("chrome", style = KvadrantTheme.typography.normal)
            }
        }
    }
}
