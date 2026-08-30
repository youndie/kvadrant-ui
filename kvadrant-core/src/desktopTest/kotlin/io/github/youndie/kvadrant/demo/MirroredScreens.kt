package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.youndie.kvadrant.components.KvadrantPageHeader
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * The three layouts that compute an x by hand, mirrored.
 *
 * [B-41](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-41-rtl-is-canon-and-untested.md).
 * Windows Phone took `FlowDirection` from the phone's culture and an application did nothing to get
 * it, so Arabic and Hebrew are something the original did and this does not — canon rather than an
 * enhancement. Most of the library gets it free: `padding(start`/`end` and `horizontal =`
 * throughout, and no `left`, `right` or `absolute` variant anywhere. What is not free is a custom
 * layout that places children itself, and there are three.
 *
 * **The direction is mirrored and the script is not, deliberately.** A golden in Arabic would be a
 * golden of tofu: this library bundles Selawik and Source Sans 3 and neither has an Arabic or Hebrew
 * face, so such an image would be testing the font stack — B-07's job — rather than the layout,
 * which is this one's. What is being asked here is which way the boxes go.
 */
@Composable
private fun Mirrored(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
}

@ViddikScreenshot(name = "pivot", group = "rtl", width = 400, height = 640)
@Composable
internal fun MirroredPivot(): Unit = Mirrored { PivotStart() }

@ViddikScreenshot(name = "panorama", group = "rtl", width = 400, height = 500)
@Composable
internal fun MirroredPanorama(): Unit = Mirrored { Panorama() }

@ViddikScreenshot(name = "page header", group = "rtl", width = 400, height = 200)
@Composable
internal fun MirroredPageHeader(): Unit =
    Mirrored {
        KvadrantTheme(colors = KvadrantColors.dark(), typography = portableTypography(kvadrantLatin())) {
            Box(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) {
                KvadrantPageHeader("KVADRANT UI", "настройки", cyrillic = kvadrantCyrillic())
            }
        }
    }
