package io.github.youndie.kvadrant.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What crosses the boundary in the reverse direction, and what deliberately does not. */
@OptIn(ExperimentalTestApi::class)
class ReverseAdapterTest {
    private val hostAccent = Color(0xFFF0A30A)

    @Test
    fun the_accent_crosses_and_the_background_does_not() {
        var accent = Color.Unspecified
        var background = Color.Unspecified
        runComposeUiTest {
            setContent {
                MaterialTheme(
                    colorScheme =
                        darkColorScheme(
                            primary = hostAccent,
                            background = Color(0xFF161A22),
                            surface = Color(0xFF161A22),
                        ),
                ) {
                    KvadrantIsland(portableTypography(kvadrantLatin())) {
                        accent = KvadrantTheme.colors.accent
                        background = KvadrantTheme.colors.background
                    }
                }
            }
        }
        assertEquals(hostAccent, accent, "the host's primary did not become the island's accent")
        // The seam, asserted rather than described. Metro's dark background is absolute black by
        // decision and the host's is a tinted near-black; the island does not meet it half way.
        assertEquals(Color(0xFF000000), background, "the island borrowed the host's background")
    }

    @Test
    fun a_light_host_gets_the_light_theme() {
        var isDark = true
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = hostAccent)) {
                    KvadrantIsland(portableTypography(kvadrantLatin())) {
                        isDark = KvadrantTheme.colors.isDark
                    }
                }
            }
        }
        assertTrue(!isDark, "a light Material host produced a dark island")
    }

    @Test
    fun adaptive_widget_branches_on_whether_a_theme_is_actually_there() {
        var outside = ""
        var inside = ""
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    AdaptiveWidget(kvadrant = { outside = "kvadrant" }, material = { outside = "material" })
                    KvadrantIsland(portableTypography(kvadrantLatin())) {
                        AdaptiveWidget(kvadrant = { inside = "kvadrant" }, material = { inside = "material" })
                    }
                }
            }
        }
        // The point of the marker local: every other value in the theme has a working default, so
        // reading one outside a theme returns something rather than nothing and decides wrongly.
        assertEquals("material", outside, "AdaptiveWidget chose Metro outside a Metro theme")
        assertEquals("kvadrant", inside, "AdaptiveWidget chose Material inside a Metro theme")
    }
}
