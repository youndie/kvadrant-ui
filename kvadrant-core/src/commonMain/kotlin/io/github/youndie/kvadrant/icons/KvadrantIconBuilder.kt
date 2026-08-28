package io.github.youndie.kvadrant.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * What the generated glyphs are built with, written by hand so that they need not be.
 *
 * **The generated file contains no method chains, and that is the point of this one.** ktlint
 * rewrites a chain onto its own line breaks, so a generator that emits `Builder(…).path(…).build()`
 * produces a file the formatter immediately edits — and then the `--check` that keeps the drawings
 * and the Kotlin in step fails on a clean tree. Excluding the output from the formatter was tried
 * first and turned out not to hold; giving the generator a vocabulary with no chains in it does.
 *
 * The grid and the stroke live here too, so forty glyphs cannot disagree about them.
 */
internal fun buildIcon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(name, GRID.dp, GRID.dp, GRID, GRID).apply(block).build()

/** One stroked path on the shared grid: 1.5 wide, butt caps, mitred joins, no fill. */
internal fun ImageVector.Builder.strokePath(block: PathBuilder.() -> Unit) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        pathBuilder = block,
    )
}

/** 26 x 26, the grid the guidelines put an application bar glyph on. */
private const val GRID = 26f

/** 1.5, the stroke they are all drawn at. */
private const val STROKE = 1.5f
