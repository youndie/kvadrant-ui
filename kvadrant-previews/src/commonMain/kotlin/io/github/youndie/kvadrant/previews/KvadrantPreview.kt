package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography

/**
 * One thing a reader can press.
 *
 * A preview is a **bare component on its own**, not a corner of the demo: the page about the toggle
 * switch shows a toggle switch and nothing else. That is what makes a component library readable —
 * somebody arrives knowing which component they want, and a screen containing eleven of them
 * answers a question they did not ask.
 *
 * @property id the address. It is in the URL, in the HTML attribute the site mounts by, and in the
 *   filename of anything recorded from it, so it is lower-case ASCII and stays put once published.
 * @property group the section of the site this belongs under. It is a field rather than the file
 *   the preview happens to be declared in, because the site's index is the first thing a reader
 *   sees and the order things are read in should not depend on how the sources were split up.
 * @property component the public composable this demonstrates, spelled exactly as it is declared —
 *   `scripts/component_catalog.py` matches these against the sources and reports either direction
 *   of mismatch.
 * @property summary one line, present tense, saying what the preview shows rather than what the
 *   component is for. The component's own KDoc is the place for the second thing.
 * @property heightDp how tall the canvas has to be for the thing to be visible. A preview clipped
 *   by its host looks like a broken component.
 */
public class KvadrantPreview(
    public val id: String,
    public val group: String,
    public val component: String,
    public val summary: String,
    public val heightDp: Int,
    public val body: @Composable () -> Unit,
)

/** The registry. Everything that mounts a preview reads it from here and nowhere else. */
public object KvadrantPreviews {
    public val all: List<KvadrantPreview> = buildPreviews()

    public fun byId(id: String): KvadrantPreview? = all.firstOrNull { it.id == id }

    /** Grouped by the component each preview is about, in the order [all] declares them. */
    public val byComponent: Map<String, List<KvadrantPreview>> = all.groupBy { it.component }

    init {
        // A duplicate id silently wins in `byId` and silently loses a page on the site. It is
        // cheaper to refuse to load.
        val duplicates =
            all
                .groupingBy { it.id }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplicates.isEmpty()) { "duplicate preview ids: ${duplicates.sorted()}" }
    }
}

/**
 * Mounts one preview inside a theme, on the theme's own background.
 *
 * The theme is **here** rather than inside each preview, which is what lets one preview serve both
 * palettes: the site's light/dark switch changes this argument and the preview is not consulted.
 * A preview that themed itself would be a preview that could only ever be seen one way round.
 */
@Composable
public fun KvadrantPreviewHost(
    preview: KvadrantPreview,
    dark: Boolean = true,
) {
    val colors = if (dark) KvadrantColors.dark() else KvadrantColors.light()
    KvadrantTheme(colors = colors, typography = KvadrantTypography.default(kvadrantLatin())) {
        Box(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) {
            preview.body()
        }
    }
}
