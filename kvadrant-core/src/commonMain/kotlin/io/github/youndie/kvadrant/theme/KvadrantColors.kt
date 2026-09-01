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
    /**
     * Black or white, whichever stays legible on [accent] — and a parameter rather than a
     * derivation, because a caller may not be free to move the accent.
     *
     * The default is [contrastOn], which is what Windows Phone did and stays what this library
     * does; white on a cyan tile is the authentic answer at 2.90:1 and it is not changed here.
     * What the parameter adds is the second lever [B-11](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-11-accessibility-policy.md)'s
     * policy needs and did not have: `accessible()` reaches AA by walking the *accent* towards
     * black or white, which is the right move for a caller whose accent is negotiable and the
     * wrong one for an application arriving with a brand's fixed hex. For that caller the accent
     * stays and the ink is what has to move.
     *
     * **Carried by `copy()` rather than recomputed, and that is safe rather than lucky.**
     * `accessible()` walks the accent *away* from the text colour on it — darker when the text is
     * white, lighter when it is black — so the walk cannot carry an accent across the 0.5
     * luminance threshold that would flip the ink. Checked for all twenty: none flips.
     */
    val onAccent: Color = contrastOn(accent),
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
    public companion object {
        public fun dark(
            accent: Color = KvadrantAccents.Cyan,
            onAccent: Color = contrastOn(accent),
        ): KvadrantColors =
            KvadrantColors(
                accent = accent,
                onAccent = onAccent,
                foreground = KvadrantTokens.Dark.foreground,
                background = KvadrantTokens.Dark.background,
                contrastForeground = KvadrantTokens.Dark.contrastForeground,
                contrastBackground = KvadrantTokens.Dark.contrastBackground,
                disabled = KvadrantTokens.Dark.disabled,
                subtle = KvadrantTokens.Dark.subtle,
                chrome = KvadrantTokens.Dark.chrome,
                semitransparent = KvadrantTokens.Dark.semitransparent,
                border = KvadrantTokens.Dark.border,
                inactive = KvadrantTokens.Dark.inactive,
                textBox = KvadrantTokens.Dark.textBox,
                textBoxEditBackground = KvadrantTokens.Dark.textBoxEditBackground,
                textBoxForeground = Color(0xFF000000),
                checkBoxPressed = Color(0xFFFFFFFF),
                checkBoxPressedBorder = Color(0xFFFFFFFF),
                isDark = true,
            )

        public fun light(
            accent: Color = KvadrantAccents.Cyan,
            onAccent: Color = contrastOn(accent),
        ): KvadrantColors =
            KvadrantColors(
                accent = accent,
                onAccent = onAccent,
                foreground = KvadrantTokens.Light.foreground,
                background = KvadrantTokens.Light.background,
                contrastForeground = KvadrantTokens.Light.contrastForeground,
                contrastBackground = KvadrantTokens.Light.contrastBackground,
                disabled = KvadrantTokens.Light.disabled,
                subtle = KvadrantTokens.Light.subtle,
                chrome = KvadrantTokens.Light.chrome,
                semitransparent = KvadrantTokens.Light.semitransparent,
                border = KvadrantTokens.Light.border,
                inactive = KvadrantTokens.Light.inactive,
                textBox = KvadrantTokens.Light.textBox,
                textBoxEditBackground = KvadrantTokens.Light.textBoxEditBackground,
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
    // Every value below is `KvadrantTokens`, which is generated from the vendored
    // `metro-tokens.json`. These are the names and the doc; the numbers are the dump's.
    public val Lime: Color = KvadrantTokens.Accents.Lime
    public val Green: Color = KvadrantTokens.Accents.Green
    public val Emerald: Color = KvadrantTokens.Accents.Emerald
    public val Teal: Color = KvadrantTokens.Accents.Teal
    public val Cyan: Color = KvadrantTokens.Accents.Cyan
    public val Cobalt: Color = KvadrantTokens.Accents.Cobalt
    public val Indigo: Color = KvadrantTokens.Accents.Indigo
    public val Violet: Color = KvadrantTokens.Accents.Violet
    public val Pink: Color = KvadrantTokens.Accents.Pink
    public val Magenta: Color = KvadrantTokens.Accents.Magenta
    public val Crimson: Color = KvadrantTokens.Accents.Crimson
    public val Red: Color = KvadrantTokens.Accents.Red
    public val Orange: Color = KvadrantTokens.Accents.Orange
    public val Amber: Color = KvadrantTokens.Accents.Amber
    public val Yellow: Color = KvadrantTokens.Accents.Yellow
    public val Brown: Color = KvadrantTokens.Accents.Brown
    public val Olive: Color = KvadrantTokens.Accents.Olive
    public val Steel: Color = KvadrantTokens.Accents.Steel
    public val Mauve: Color = KvadrantTokens.Accents.Mauve
    public val Taupe: Color = KvadrantTokens.Accents.Taupe

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
