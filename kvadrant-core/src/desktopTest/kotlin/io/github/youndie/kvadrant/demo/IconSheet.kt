package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.icons.KvadrantIcons
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * Every glyph in the set, at the size the application bar draws it.
 *
 * A drawn icon is wrong in ways only a picture shows — a stroke that misses a join, a shape that is
 * a pixel off centre — and the generator that produces them can be perfectly correct while the
 * drawing behind it is not. So the set has a sheet, and adding a glyph without one would be adding
 * a shape nobody has looked at.
 */
@Composable
private fun Sheet() {
    KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KvadrantIcons.All.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (name, vector) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .paint(
                                        rememberVectorPainter(vector),
                                        colorFilter = ColorFilter.tint(KvadrantTheme.colors.foreground),
                                    ),
                            )
                            KvadrantText(name, style = KvadrantTheme.typography.subtle)
                        }
                    }
                }
            }
        }
    }
}

@ViddikScreenshot(name = "sheet", group = "icons", width = 320, height = 880)
@Composable
internal fun IconSheet(): Unit = Sheet()
