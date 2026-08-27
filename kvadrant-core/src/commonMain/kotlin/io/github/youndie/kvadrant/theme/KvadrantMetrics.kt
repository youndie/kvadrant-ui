package io.github.youndie.kvadrant.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Page and control measurements, in dp at the canonical 0.75 of Metro's pixels.
 *
 * Only what is used so far. A token added before something reads it is a number nobody has
 * checked.
 */
@Immutable
public data class KvadrantMetrics(
    /** `PhoneMargin` 12 px. The margin everything on a page hangs off. */
    val margin: Dp = 9.dp,
    /** `PhoneBorderThickness` 3 px — a Metro button's border, and it is thick on purpose. */
    val borderThickness: Dp = 2.25.dp,
    /** `PhoneTouchTargetOverhang` 12 px: the invisible padding around a small target. */
    val touchTargetOverhang: Dp = 9.dp,
    /** The canonical touch target. Authentic; [touchTargetMin] is what is actually enforced. */
    val touchTargetVisual: Dp = 25.5.dp,
    /** Extended to the modern minimum, always — visual stays canonical. See D7. */
    val touchTargetMin: Dp = 48.dp,
    /** `PivotItemMargin` 12,28,12,0. */
    val pivotItemMargin: Dp = 9.dp,
    val pivotItemTopMargin: Dp = 21.dp,
    /** `PivotHeaderItem` padding 21,0,1,0 — the 21 px is the gap between headers. */
    val pivotHeaderGap: Dp = 15.75.dp,
    /**
     * How far a pressed surface sinks — `MaxDepression = 25` Metro pixels.
     *
     * A theme metric rather than a constant beside the tilt formula so that a theme can tune it,
     * and so that [scaled] can say in one place why it is one of the three numbers the scale does
     * not touch.
     */
    val tiltDepression: Dp = 18.75.dp,
    /** Tile sizes: 99 / 210 / 432×210 px, gap 12 px. */
    val tileSmall: Dp = 74.25.dp,
    val tileMedium: Dp = 157.5.dp,
    val tileWide: Dp = 324.dp,
    val tileGap: Dp = 9.dp,
)

/**
 * The same measurements, every one of them multiplied by [factor].
 *
 * Metro's numbers were drawn for a 480 px phone, and at the canonical 0.75 they land on a 360 dp
 * canvas. On a larger window — a desktop, a tablet — the 9 dp page margin that felt right there
 * reads as cramped, and adjusting it alone would break its relationship with the tile gap, which is
 * the same 12 Metro pixels. So the scale is one knob over the whole set: everything moves together
 * or nothing does.
 *
 * **The type ramp is deliberately not scaled with it.** Text has its own reason to be the size it
 * is — the 72 px header is the design, not a consequence of the canvas — and scaling both at once
 * is how a faithful layout turns into a merely large one.
 *
 * **[tiltDepression] is not scaled either, and that one is a measurement rather than a taste.** It
 * reads as the obvious omission — every other screen distance here scales, so surely a press should
 * sink further on a scaled-up tile — but the tilt is already proportional without any help. The
 * depression is a push along z, and what reaches the screen is `depth / (depth + depression)`,
 * a ratio with no term in it for the size of the thing being pressed: at the default numbers every
 * surface, from a 24 dp checkbox to a 210 dp tile, draws at 0.9685 of itself. Scaling the
 * depression does not make a big tile sink more *in proportion* — it makes it sink more than a
 * small one, which is the defect, not the fix. `TiltScaleInvarianceTest` fails if this line
 * acquires a `* factor`.
 */

public fun KvadrantMetrics.scaled(factor: Float): KvadrantMetrics =
    KvadrantMetrics(
        margin = margin * factor,
        borderThickness = borderThickness * factor,
        touchTargetOverhang = touchTargetOverhang * factor,
        touchTargetVisual = touchTargetVisual * factor,
        // Not scaled: the modern minimum is a fixed number of millimetres under a thumb, and a bigger
        // window does not make thumbs bigger.
        touchTargetMin = touchTargetMin,
        pivotItemMargin = pivotItemMargin * factor,
        pivotItemTopMargin = pivotItemTopMargin * factor,
        pivotHeaderGap = pivotHeaderGap * factor,
        // Not scaled either, and this one is worth the paragraph in the doc above: it looks like
        // an oversight and is not.
        tiltDepression = tiltDepression,
        tileSmall = tileSmall * factor,
        tileMedium = tileMedium * factor,
        tileWide = tileWide * factor,
        tileGap = tileGap * factor,
    )

/**
 * The metric set scaled so that one row of the Start screen — margin, wide tile, margin — is exactly
 * [width] across.
 *
 * This is the scale a surface should normally use, and it is the reason a Metro layout has no
 * breakpoints: Windows Phone stretched one 480-pixel canvas to WVGA, WXGA and 720p rather than
 * reflowing anything. Passing a factor to [scaled] by hand means choosing a number instead, and a
 * number chosen by eye cannot be re-derived — the desktop demo ran at a hand-picked 1.6 for a week,
 * and the fitted factor for its 560 dp window turns out to be 1.64.
 *
 * The divisor is read out of the metric set rather than written down, so it stays true if the tile
 * sizes ever move. **It is 342 dp and not the 360 dp the canvas is wide**, and that difference is
 * unexplained: 480 Metro pixels is exactly `24 + 432 + 24`, which says the Start screen's outer
 * margin is 24 px where [margin] is the 12 px of a text page. Until somebody reads that off a real
 * Start screen, fitting the row is the honest thing — it makes both edges equal without asserting
 * a number nobody has verified.
 */
public fun KvadrantMetrics.scaledToWidth(width: Dp): KvadrantMetrics = scaled(width / (margin * 2 + tileWide))
