package io.github.youndie.kvadrant.indication

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * The dotted rectangle Windows 8 drew around whatever the keyboard was pointing at.
 *
 * Transcribed from the WinRT XAML `Button` template, which puts **two** rectangles over the control
 * and turns both of them on together:
 *
 * ```xml
 * <Rectangle x:Name="FocusVisualWhite" Stroke="{ThemeResource FocusVisualWhiteStrokeThemeBrush}"
 *            StrokeEndLineCap="Square" StrokeDashArray="1,1" Opacity="0" StrokeDashOffset="1.5" />
 * <Rectangle x:Name="FocusVisualBlack" Stroke="{ThemeResource FocusVisualBlackStrokeThemeBrush}"
 *            StrokeEndLineCap="Square" StrokeDashArray="1,1" Opacity="0" StrokeDashOffset="0.5" />
 * ```
 *
 * The two offsets are a dash apart, so the white dashes sit in the black one's gaps and the ring
 * comes out as an unbroken alternating line. That is why there are two of them and why neither
 * brush is a theme colour: `FocusVisualWhiteStrokeThemeBrush` is `White` and
 * `FocusVisualBlackStrokeThemeBrush` is `Black` in **every** theme block of the dictionary, light,
 * dark and high contrast alike. A ring that is half black and half white is legible on any
 * background there is, which is a cheaper answer than picking a colour per theme and hoping.
 *
 * **`StrokeDashArray` is in units of the stroke's own thickness**, not pixels — `1,1` at thickness
 * 1 is a one-unit dash and a one-unit gap. So is `StrokeDashOffset`, which is why the phases below
 * are multiplied by [thicknessPx] rather than used as they are written.
 *
 * **`StrokeEndLineCap="Square"` is not the dashes' cap and reading it as one draws a solid line.**
 * The dashes take `StrokeDashCap`, which the template leaves at its `Flat` default;
 * `StrokeEndLineCap` caps the ends of the whole figure, and a closed rectangle has none. A square
 * cap extends every dash by half a thickness at each end, which at a 1:1 pattern closes the gaps
 * exactly — the ring would be a plain hairline rectangle and nothing would say it was wrong.
 */
internal fun DrawScope.drawKvadrantFocusRing(thicknessPx: Float) {
    // Inset by half the stroke: Compose centres a stroke on its path, so a rectangle on the bounds
    // loses its outer half to the clip and draws at half the thickness it was asked for.
    val inset = thicknessPx / 2f
    val corner = Offset(inset, inset)
    val box = Size(size.width - thicknessPx, size.height - thicknessPx)
    if (box.width <= 0f || box.height <= 0f) return
    val intervals = floatArrayOf(thicknessPx, thicknessPx)
    drawRect(
        color = Color.Black,
        topLeft = corner,
        size = box,
        style =
            Stroke(
                width = thicknessPx,
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(intervals, 0.5f * thicknessPx),
            ),
    )
    drawRect(
        color = Color.White,
        topLeft = corner,
        size = box,
        style =
            Stroke(
                width = thicknessPx,
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(intervals, 1.5f * thicknessPx),
            ),
    )
}
