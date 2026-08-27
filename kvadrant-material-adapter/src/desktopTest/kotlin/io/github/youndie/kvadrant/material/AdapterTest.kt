package io.github.youndie.kvadrant.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the adapter is for, checked on the pixels a Material widget puts on screen.
 *
 * Every claim here is one that "it compiles" is silent about. A `ColorScheme` can be built out of
 * the right colours and still hand Material a tinted surface; `Shapes` can hold five entries and
 * still round a button, because Material components pick their slot themselves.
 */
@OptIn(ExperimentalTestApi::class)
class AdapterTest {
    private fun ComposeUiTest.pixelsOf(
        tag: String,
        content: @Composable () -> Unit,
    ): Triple<IntArray, Int, Int> {
        setContent {
            KvadrantTheme(colors = KvadrantColors.dark(KvadrantAccents.Magenta)) {
                KvadrantMaterialAdapter {
                    Box(Modifier.size(300.dp).background(Color.Black).testTag(tag)) { content() }
                }
            }
        }
        val image = onNodeWithTag(tag).captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        return Triple(pixels, image.width, image.height)
    }

    @Test
    fun a_material_button_takes_the_kvadrant_accent() =
        runComposeUiTest {
            val (pixels, _, _) = pixelsOf("frame") { Button(onClick = {}) { Text("готово") } }
            val accent = KvadrantAccents.Magenta
            val want = (accent.red * 255).toInt() to (accent.blue * 255).toInt()
            val hits =
                pixels.count { p ->
                    val r = p shr 16 and 0xFF
                    val g = p shr 8 and 0xFF
                    val b = p and 0xFF
                    kotlin.math.abs(r - want.first) < 6 && kotlin.math.abs(b - want.second) < 6 && g < 60
                }
            assertTrue(hits > 300, "only $hits pixels of the accent — the button is not wearing it")
        }

    @Test
    fun a_material_card_is_square_under_the_adapter() =
        runComposeUiTest {
            val (pixels, width, _) =
                pixelsOf("frame") {
                    Card(Modifier.size(200.dp, 100.dp)) { Text("готово") }
                }

            fun widthAt(y: Int) = (0 until width).count { x -> (pixels[y * width + x] and 0xFFFFFF) != 0 }
            val rows = (0 until width).filter { widthAt(it) > 0 }
            val first = widthAt(rows.first() + 1)
            val widest = rows.maxOf { widthAt(it) }
            assertEquals(
                widest,
                first,
                "the card is $first px wide at its top row and $widest at its widest — a rounded " +
                    "corner survived, so `Shapes` is not reaching the components that read it",
            )
        }

    /**
     * A Material button stays round, and that is a fact about Material rather than a defect here.
     *
     * `ButtonDefaults.shape` is `RoundedCornerShape(50%)`, a token, and it does not read
     * `MaterialTheme.shapes` at all — filling all eight slots with a zero corner changes nothing
     * about it. `CardDefaults.shape` in the same composition *is* the theme's zero corner, so the
     * adapter works and the button is simply outside its reach. This is the "shapes forced round"
     * cause research §1.3 counts against the ~10 components that need a wrapper rather than
     * theming, demonstrated on the most ordinary component there is.
     *
     * Pinned so that a Material version which starts honouring the theme here is noticed, rather
     * than being found out by somebody wondering why a button went square.
     */
    @Test
    fun a_material_button_is_a_pill_and_the_theme_cannot_say_otherwise() =
        runComposeUiTest {
            var button = ""
            var themed = ""
            setContent {
                KvadrantTheme {
                    KvadrantMaterialAdapter {
                        button = ButtonDefaults.shape.toString()
                        themed = MaterialTheme.shapes.small.toString()
                    }
                }
            }
            assertTrue("0.0.dp" in themed, "the theme's own slot is not square: $themed")
            assertTrue(
                "50.0%" in button,
                "ButtonDefaults.shape is no longer a 50% pill but $button — Material may have " +
                    "started reading the theme, and the adapter's documented limitation is stale",
            )
        }

    @Test
    fun the_scheme_carries_no_surface_tint() =
        runComposeUiTest {
            var tint: Color? = null
            setContent {
                KvadrantTheme {
                    KvadrantMaterialAdapter { tint = MaterialTheme.colorScheme.surfaceTint }
                }
            }
            // The one that makes a Material surface tint itself by elevation. In a design with no
            // depth, a tinted surface reads as a mistake nobody can point at.
            assertEquals(Color.Transparent, tint)
        }

    @Test
    fun the_shape_slots_are_all_square() =
        runComposeUiTest {
            val slots = mutableListOf<androidx.compose.ui.graphics.Shape>()
            setContent {
                KvadrantTheme {
                    KvadrantMaterialAdapter {
                        with(MaterialTheme.shapes) {
                            slots += listOf(extraSmall, small, medium, large, extraLarge)
                        }
                    }
                }
            }
            // A rounded corner of zero, not `RectangleShape`: `Shapes` takes `CornerBasedShape`.
            val square =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(0.dp)
            assertTrue(slots.size == 5, "expected five slots, got ${slots.size}")
            assertTrue(slots.all { it == square }, "a slot is not square: $slots")
            assertTrue(square != RectangleShape, "sanity: these are different types, same pixels")
        }
}
