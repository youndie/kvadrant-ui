package io.github.youndie.kvadrant.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The Windows Phone theme palette: thirteen tokens and one accent, transcribed from the SDK's
 * `ThemeResources.xaml`.
 *
 * The light theme is **not** an inversion of the dark one — foreground is 87 % black rather than
 * 100 % white, subtle is 40 % against 60 %, disabled 30 % against 40 % — so both are written out.
 */
@Immutable
public data class KvadrantColors(
    val accent: Color,
    val foreground: Color,
    val background: Color,
    val contrastForeground: Color,
    val contrastBackground: Color,
    val disabled: Color,
    val subtle: Color,
    val chrome: Color,
    val semitransparent: Color,
    val border: Color,
    val inactive: Color,
    val textBox: Color,
    val textBoxEditBackground: Color,
    /**
     * `PhoneTextBoxForegroundColor` — what is typed into a text box, and **not** [foreground].
     *
     * A Metro text box is a *light* box in both themes: [textBox] is 75 % white on a dark page and
     * 15 % black on a light one, and the text on it is dark either way. Reaching for
     * [contrastForeground] instead looks right in the dark theme and inverts to white-on-white in
     * the light one, which is why this is its own token rather than a reuse.
     */
    val textBoxForeground: Color,
    /**
     * `PhoneRadioCheckBoxPressedColor` — what fills a check box or a radio ring while it is held.
     *
     * **Not the accent, and this used to be.** Research §1.12 recorded the accent's role as "the
     * pressed state, and only that", read off the *name* `PhoneRadioCheckBoxPressedBrush` without
     * resolving it. Resolved, it is `#FFFFFFFF` in the dark theme and `#00000000` — fully
     * transparent — in the light one. Exactly one colour differs between the twenty accent
     * dictionaries, and it is `PhoneAccentColor`; the accent marks selection and progress, never
     * touch.
     */
    val checkBoxPressed: Color,
    /** `PhoneRadioCheckBoxPressedBorderColor`, which parts company with [checkBoxPressed] in light. */
    val checkBoxPressedBorder: Color,
    val isDark: Boolean,
) {
    /** Black or white, whichever stays legible on [accent]. */
    val onAccent: Color get() = contrastOn(accent)

    public companion object {
        public fun dark(accent: Color = KvadrantAccents.Cyan): KvadrantColors =
            KvadrantColors(
                accent = accent,
                foreground = Color(0xFFFFFFFF),
                background = Color(0xFF000000),
                contrastForeground = Color(0xFF000000),
                contrastBackground = Color(0xFFFFFFFF),
                disabled = Color(0x66FFFFFF),
                subtle = Color(0x99FFFFFF),
                chrome = Color(0xFF1F1F1F),
                semitransparent = Color(0xAA000000),
                border = Color(0xBFFFFFFF),
                inactive = Color(0x33FFFFFF),
                textBox = Color(0xBFFFFFFF),
                textBoxEditBackground = Color(0xFFFFFFFF),
                textBoxForeground = Color(0xFF000000),
                checkBoxPressed = Color(0xFFFFFFFF),
                checkBoxPressedBorder = Color(0xFFFFFFFF),
                isDark = true,
            )

        public fun light(accent: Color = KvadrantAccents.Cyan): KvadrantColors =
            KvadrantColors(
                accent = accent,
                foreground = Color(0xDE000000),
                background = Color(0xFFFFFFFF),
                contrastForeground = Color(0xFFFFFFFF),
                contrastBackground = Color(0xDE000000),
                disabled = Color(0x4D000000),
                subtle = Color(0x66000000),
                chrome = Color(0xFFDDDDDD),
                semitransparent = Color(0xAAFFFFFF),
                border = Color(0x99000000),
                inactive = Color(0x33000000),
                textBox = Color(0x26000000),
                textBoxEditBackground = Color(0x00000000),
                textBoxForeground = Color(0xDE000000),
                // Held down, a light-theme box goes *emptier* — its 15 % fill drops to nothing while
                // the border darkens. The dark theme floods to solid white. Neither is the other
                // inverted, which is the light theme's rule and not an exception to it.
                checkBoxPressed = Color(0x00000000),
                checkBoxPressedBorder = Color(0xDE000000),
                isDark = false,
            )
    }
}

/**
 * Which of black or white to put on a colour, by the rule Windows Phone used: light accents take
 * black text, dark ones take white.
 *
 * The threshold is the one Metro-Compose uses. It flips only `Yellow` in practice; `Lime` and
 * `Amber` stay on white text at roughly 2.2:1, which is the authentic result and fails WCAG AA.
 * That trade is deliberate — see D7.
 */
public fun contrastOn(background: Color): Color = if (background.luminance() >= 0.5f) Color.Black else Color.White

/**
 * The twenty accents Windows Phone 8 shipped.
 *
 * Microsoft published two of these as text — `Cyan` and `Red` — and the rest only as an image, so
 * the remaining eighteen are consistent across independent copies rather than quoted from a source.
 */
public object KvadrantAccents {
    public val Lime: Color = Color(0xFFA4C400)
    public val Green: Color = Color(0xFF60A917)
    public val Emerald: Color = Color(0xFF008A00)
    public val Teal: Color = Color(0xFF00ABA9)
    public val Cyan: Color = Color(0xFF1BA1E2)
    public val Cobalt: Color = Color(0xFF0050EF)
    public val Indigo: Color = Color(0xFF6A00FF)
    public val Violet: Color = Color(0xFFAA00FF)
    public val Pink: Color = Color(0xFFF472D0)
    public val Magenta: Color = Color(0xFFD80073)
    public val Crimson: Color = Color(0xFFA20025)
    public val Red: Color = Color(0xFFE51400)
    public val Orange: Color = Color(0xFFFA6800)
    public val Amber: Color = Color(0xFFF0A30A)
    public val Yellow: Color = Color(0xFFE3C800)
    public val Brown: Color = Color(0xFF825A2C)
    public val Olive: Color = Color(0xFF6D8764)
    public val Steel: Color = Color(0xFF647687)
    public val Mauve: Color = Color(0xFF76608A)
    public val Taupe: Color = Color(0xFF87794E)

    /** All twenty, in the order the phone's settings list showed them. */
    public val All: List<Pair<String, Color>> =
        listOf(
            "lime" to Lime,
            "green" to Green,
            "emerald" to Emerald,
            "teal" to Teal,
            "cyan" to Cyan,
            "cobalt" to Cobalt,
            "indigo" to Indigo,
            "violet" to Violet,
            "pink" to Pink,
            "magenta" to Magenta,
            "crimson" to Crimson,
            "red" to Red,
            "orange" to Orange,
            "amber" to Amber,
            "yellow" to Yellow,
            "brown" to Brown,
            "olive" to Olive,
            "steel" to Steel,
            "mauve" to Mauve,
            "taupe" to Taupe,
        )
}
