package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.components.KvadrantProgressDots
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.components.TileRow
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.components.rememberKvadrantPivotState
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

private val Titles = listOf("start", "почта", "settings")

@Composable
private fun PivotPage(
    colors: KvadrantColors,
    page: Int,
    offset: Float,
) {
    val latin = kvadrantLatin()
    val cyrillic = kvadrantCyrillic()
    KvadrantTheme(colors = colors, typography = KvadrantTypography.default(latin)) {
        val state = rememberKvadrantPivotState(Titles.size)
        LaunchedEffect(page, offset) {
            state.scrollToPage(state.currentPage - state.currentPage.mod(Titles.size) + page, offset)
        }

        Column(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) {
            KvadrantPivot(Titles, title = "KVADRANT UI", state = state, cyrillic = cyrillic) { index ->
                when (index) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            TileRow {
                                KvadrantTile(TileSize.Medium) {
                                    KvadrantText(
                                        "почта",
                                        Modifier.align(Alignment.BottomStart).padding(9.dp),
                                        KvadrantTheme.typography.normal.copy(
                                            color = KvadrantTheme.colors.onAccent,
                                        ),
                                        cyrillic,
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    KvadrantTile(TileSize.Small, color = KvadrantAccents.Emerald)
                                    KvadrantTile(TileSize.Small, color = KvadrantAccents.Magenta)
                                }
                            }
                            KvadrantTile(TileSize.Wide, color = KvadrantAccents.Cobalt)
                        }
                    }

                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                "Анна Петрова" to "встреча в четверг",
                                "build server" to "nightly is green",
                                "Дмитрий" to "документы отправил",
                            ).forEach { (who, what) ->
                                Column {
                                    KvadrantText(who, style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
                                    KvadrantText(
                                        what,
                                        style =
                                            KvadrantTheme.typography.subtle.copy(
                                                color = KvadrantTheme.colors.subtle,
                                            ),
                                        cyrillic = cyrillic,
                                    )
                                }
                            }
                            KvadrantProgressDots(Modifier.padding(top = 12.dp))
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            KvadrantText("тёмная тема", style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
                            KvadrantToggleSwitch(checked = colors.isDark, onCheckedChange = {})
                            KvadrantText("акцент", style = KvadrantTheme.typography.normal, cyrillic = cyrillic)
                            TileRow {
                                KvadrantTile(TileSize.Small, color = KvadrantAccents.Cyan)
                                KvadrantTile(TileSize.Small, color = KvadrantAccents.Lime)
                                KvadrantTile(TileSize.Small, color = KvadrantAccents.Crimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

@ViddikScreenshot(name = "pivot start", group = "pivot", width = 400, height = 640)
@Composable
internal fun PivotStart(): Unit = PivotPage(KvadrantColors.dark(), 0, 0f)

/** Mid-swipe. The headers have moved by their own widths while the page moves by a full width. */
@ViddikScreenshot(name = "pivot mid swipe", group = "pivot", width = 400, height = 640)
@Composable
internal fun PivotMidSwipe(): Unit = PivotPage(KvadrantColors.dark(), 0, 0.4f)

@ViddikScreenshot(name = "pivot mail", group = "pivot", width = 400, height = 640)
@Composable
internal fun PivotMail(): Unit = PivotPage(KvadrantColors.dark(), 1, 0f)

@ViddikScreenshot(name = "pivot settings light", group = "pivot", width = 400, height = 640)
@Composable
internal fun PivotSettingsLight(): Unit = PivotPage(KvadrantColors.light(), 2, 0f)

/** Past the last page comes the first: the wrap, caught in the middle of happening. */
@ViddikScreenshot(name = "pivot wrap around", group = "pivot", width = 400, height = 640)
@Composable
internal fun PivotWrapAround(): Unit = PivotPage(KvadrantColors.dark(), 2, 0.5f)
