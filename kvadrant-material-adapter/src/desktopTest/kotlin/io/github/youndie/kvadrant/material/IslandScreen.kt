package io.github.youndie.kvadrant.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantTheme
import ru.workinprogress.viddik.annotations.ViddikScreenshot
import ru.workinprogress.viddik.core.viddikTypography

/**
 * A Material screen with a Kvadrant island in the middle of it — B-19's acceptance criterion.
 *
 * The host's scheme is deliberately a *tinted* near-black rather than `#FF000000`, because the seam
 * is what this frame exists to show: the island's background is absolute black by decision, so its
 * edge is visible against a host that merely tends dark. The accent crosses and nothing else does.
 */
@Composable
private fun Island() {
    // `viddikTypography` on the host's ramp, and not only on ours. `portableTypography` pins the
    // Metro ramp; the Material components on this page carry Material's own, unpinned, and three
    // goldens went on failing on Linux after everything else had stopped — all three of them the
    // ones with a `Button` or a `Text` in them.
    MaterialTheme(colorScheme = HOST, typography = viddikTypography(MaterialTheme.typography)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("material host", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = {}) { Text("material button") }

                KvadrantIsland(portableTypography(kvadrantLatin())) {
                    // The island paints its own background, which is the whole of the seam: Metro's
                    // dark theme is absolute black by decision, so it does not blend into a host
                    // that merely tends dark. A fixture whose island paints nothing would show a
                    // frame that agrees with the KDoc's words and not with its claim.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(KvadrantTheme.colors.background)
                            .padding(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            KvadrantText("kvadrant island", style = KvadrantTheme.typography.title)
                            KvadrantTile(TileSize.Medium, color = KvadrantTheme.colors.accent) {}
                            KvadrantButton("save", {})
                            // The same call renders differently on each side of the boundary.
                            AdaptiveWidget(
                                kvadrant = { KvadrantButton("adaptive", {}) },
                                material = { Button(onClick = {}) { Text("adaptive") } },
                            )
                        }
                    }
                }

                AdaptiveWidget(
                    kvadrant = { KvadrantButton("adaptive", {}) },
                    material = { Button(onClick = {}) { Text("adaptive") } },
                )
            }
        }
    }
}

/** A host that is dark but not Metro-dark, so the island's absolute black shows as an edge. */
private val HOST =
    darkColorScheme(
        primary = Color(0xFFF0A30A),
        background = Color(0xFF161A22),
        surface = Color(0xFF161A22),
    )

@ViddikScreenshot(name = "island in material", group = "reverse", width = 360, height = 620)
@Composable
internal fun IslandInMaterial(): Unit = Island()
