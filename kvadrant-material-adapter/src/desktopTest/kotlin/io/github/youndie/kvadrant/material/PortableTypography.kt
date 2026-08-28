package io.github.youndie.kvadrant.material

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import io.github.youndie.kvadrant.theme.KvadrantTypography
import ru.workinprogress.viddik.core.ViddikPlatformTextStyle

/**
 * The ramp with hinting and smoothing pinned, so a golden means the same thing on two operating
 * systems.
 *
 * **The suite's first run on Linux failed on twenty-odd images and every one of them was text.** The
 * repository's own notes claimed these frames were portable because every glyph comes from a bundled
 * file — true, and not enough: the *file* is the same and the **rasteriser** is not. macOS and
 * FreeType hint and smooth differently, so identical outlines land on different pixels.
 *
 * **A second copy, because a test source set is not visible from another module.** The idea is one
 * line — `copy(platformStyle = ViddikPlatformTextStyle)` — and the alternative is publishing test
 * fixtures from `kvadrant-core` so that one module can borrow another's helpers, which is build
 * machinery bought with a real maintenance cost to avoid twelve lines. If a third module ever wants
 * it, buy the machinery then.
 *
 * `ViddikPlatformTextStyle` is viddik's answer — a `PlatformTextStyle` carrying an explicit
 * `FontHinting` and `FontSmoothing` rather than each platform's default. Applied here and nowhere
 * else: a library that pinned these for its consumers would be overriding the thing an operating
 * system is entitled to decide, to make its own tests convenient.
 */
internal fun portableTypography(family: FontFamily): KvadrantTypography = KvadrantTypography.default(family).portable()

/** Every slot, because a ramp with one unpinned style is a ramp with one unportable golden. */
internal fun KvadrantTypography.portable(): KvadrantTypography =
    copy(
        normal = normal.pinned(),
        subtle = subtle.pinned(),
        title = title.pinned(),
        mediumLarge = mediumLarge.pinned(),
        large = large.pinned(),
        extraLarge = extraLarge.pinned(),
        pageTitle = pageTitle.pinned(),
        pivotHeader = pivotHeader.pinned(),
        panoramaTitle = panoramaTitle.pinned(),
        panoramaSectionHeader = panoramaSectionHeader.pinned(),
    )

private fun TextStyle.pinned(): TextStyle = copy(platformStyle = ViddikPlatformTextStyle)
