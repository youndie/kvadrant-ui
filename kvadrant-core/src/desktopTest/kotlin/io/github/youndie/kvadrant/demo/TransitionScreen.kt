package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantIconicTile
import io.github.youndie.kvadrant.components.TileRow
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

@Composable
private fun Dark(content: @Composable () -> Unit) {
    KvadrantTheme(
        colors = KvadrantColors.dark(),
        typography = KvadrantTypography.default(kvadrantLatin()),
    ) {
        Box(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) { content() }
    }
}

/**
 * Swivel's axis is the **top** edge — the page tips away from you rather than turning like a door.
 * Held at four angles, because a still of a transition is otherwise a still of nothing.
 */
@ViddikScreenshot(name = "swivel axis", group = "transition", width = 400, height = 420)
@Composable
internal fun SwivelAxis() {
    Dark {
        Column(
            Modifier.fillMaxSize().padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(-45f, 0f, 60f, 90f).forEach { angle ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationX = angle
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            cameraDistance = 8f
                        }.background(KvadrantTheme.colors.accent)
                        .padding(9.dp),
                ) {
                    KvadrantText(
                        "swivel ${angle.toInt()}°",
                        style =
                            KvadrantTheme.typography.normal.copy(
                                color = KvadrantTheme.colors.onAccent,
                            ),
                    )
                }
            }
        }
    }
}

@ViddikScreenshot(name = "iconic tiles", group = "transition", width = 400, height = 300)
@Composable
internal fun IconicTiles() {
    val cyrillic = kvadrantCyrillic()
    Dark {
        TileRow(Modifier.padding(9.dp)) {
            KvadrantIconicTile(size = TileSize.Medium, count = 12, label = "почта", cyrillic = cyrillic) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .background(KvadrantTheme.colors.onAccent),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                KvadrantIconicTile(size = TileSize.Small, color = KvadrantAccents.Emerald, count = 3) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(KvadrantTheme.colors.onAccent),
                        )
                    }
                }
                KvadrantIconicTile(size = TileSize.Small, color = KvadrantAccents.Crimson)
            }
        }
    }
}
