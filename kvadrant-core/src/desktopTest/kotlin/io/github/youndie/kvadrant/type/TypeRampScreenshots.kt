package io.github.youndie.kvadrant.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * Every slot of the ramp, in both scripts, in one image.
 *
 * This is the only thing that turns a missing glyph into a diff instead of a bug report. Selawik has
 * no Cyrillic at all, so every Cyrillic run here is Source Sans 3 arriving through
 * `KvadrantText`'s per-run segmentation — and if that segmentation ever stops happening, the
 * Cyrillic falls back to whatever the host has and this picture changes in a way nobody can miss.
 *
 * Each line carries both scripts rather than the two being separate fixtures, because the failure
 * worth catching is not "Cyrillic renders" but "the two sit on the same optical size".
 */
@Composable
private fun Ramp(colors: KvadrantColors) {
    val latin = kvadrantLatin()
    val cyrillic = kvadrantCyrillic()
    val type = portableTypography(latin)
    val slots =
        listOf(
            "subtle" to type.subtle,
            "normal" to type.normal,
            "title" to type.title,
            "large" to type.large,
            "extra large" to type.extraLarge,
            "page title" to type.pageTitle,
            "panorama section" to type.panoramaSectionHeader,
            "pivot header" to type.pivotHeader,
            "panorama title" to type.panoramaTitle,
        )

    KvadrantTheme(colors = colors, typography = type) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            slots.forEach { (name, style) ->
                KvadrantText(
                    "$name образец",
                    style = style.copy(color = colors.foreground),
                    cyrillic = cyrillic,
                )
            }
        }
    }
}

@ViddikScreenshot(name = "ramp dark", group = "type", width = 400, height = 900)
@Composable
internal fun TypeRampDark(): Unit = Ramp(KvadrantColors.dark())

@ViddikScreenshot(name = "ramp light", group = "type", width = 400, height = 900)
@Composable
internal fun TypeRampLight(): Unit = Ramp(KvadrantColors.light())

// There is deliberately no "and here is what it looks like without the companion" fixture. It was
// written, recorded, and thrown away: without Source Sans 3 the Cyrillic does not vanish, it is
// supplied by whatever the *host* has, so the golden would record a macOS font and fail on a Linux
// runner for a reason that has nothing to do with this library. The portability of these images
// rests on every glyph coming from a bundled file (research §1.9), and a fixture whose point is a
// missing file cannot honour that.
