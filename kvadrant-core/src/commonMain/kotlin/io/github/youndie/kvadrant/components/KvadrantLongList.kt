package io.github.youndie.kvadrant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantWeights

/**
 * A list broken into groups, each under a letter — the contacts list, the app list, every long list
 * the phone had.
 *
 * The group header is **in the accent colour** at 22 sp SemiLight; that is in the theme dictionary,
 * not a decoration added here. Tapping one opens the jump list.
 */
@Composable
public fun <T> KvadrantLongList(
    groups: List<Pair<String, List<T>>>,
    modifier: Modifier = Modifier,
    onHeaderClick: (() -> Unit)? = null,
    cyrillic: FontFamily? = null,
    item: @Composable (T) -> Unit,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        groups.forEach { (letter, entries) ->
            item(key = "header-$letter") {
                KvadrantGroupHeader(letter, onClick = onHeaderClick, cyrillic = cyrillic)
            }
            items(entries.size) { index -> item(entries[index]) }
        }
    }
}

/** The letter above a group: accent-coloured, 22 sp SemiLight. */
@Composable
public fun KvadrantGroupHeader(
    letter: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cyrillic: FontFamily? = null,
) {
    KvadrantText(
        letter,
        modifier
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 9.dp),
        KvadrantTheme.typography.normal.copy(
            fontSize = 22.sp, // 29.333 px
            fontWeight = KvadrantWeights.SemiLight,
            color = KvadrantTheme.colors.accent,
        ),
        cyrillic,
    )
}

/**
 * The grid of letters a long list jumps from: squares with a 2 px border, the letter at 36 sp
 * SemiBold, and a letter with nothing behind it drawn in the inactive colour rather than hidden —
 * so the alphabet keeps its shape and your thumb lands where it expects to.
 *
 * The phone only offered this once a list was long enough to need it; [MIN_GROUPS_FOR_JUMP_LIST] is
 * that threshold.
 */
@Composable
public fun KvadrantJumpList(
    letters: List<String>,
    enabled: Set<String>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    onLetterClick: (String) -> Unit = {},
    cyrillic: FontFamily? = null,
) {
    val colors = KvadrantTheme.colors
    Column(modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(GAP)) {
        letters.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                row.forEach { letter ->
                    val live = letter in enabled
                    Box(
                        Modifier
                            .size(CELL)
                            .then(
                                if (live) {
                                    Modifier
                                        .background(colors.accent)
                                        .clickable { onLetterClick(letter) }
                                } else {
                                    Modifier.border(BORDER, colors.inactive, RectangleShape)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        KvadrantText(
                            letter,
                            style =
                                KvadrantTheme.typography.normal.copy(
                                    fontSize = 36.sp, // 48 px
                                    fontWeight = KvadrantWeights.SemiBold,
                                    color = if (live) colors.onAccent else colors.inactive,
                                ),
                            cyrillic = cyrillic,
                        )
                    }
                }
            }
        }
    }
}

/** Below this many groups the phone did not offer a jump list, because scrolling was quicker. */
public const val MIN_GROUPS_FOR_JUMP_LIST: Int = 8

private val CELL = 84.75.dp // 113 px
private val GAP = 4.5.dp // 6 px
private val BORDER = 1.5.dp // 2 px
