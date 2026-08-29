package io.github.youndie.kvadrant.indication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The same trapezoid, solved on the renderer this library will mostly ship on.
 *
 * **This is what Android gets instead of goldens, and the choice is B-29's.** viddik's capture
 * engine publishes JVM variants only — `metadataApiElements`, `jvmApiElements-published`,
 * `jvmRuntimeElements-published`, read out of its module metadata at 0.2.0.14 — so there is no path
 * to Android pictures inside `check` without work in another repository. A second screenshot tool
 * was ruled out on ownership rather than on evidence: two golden formats, two ways to record and two
 * answers to "is the suite green" cost every future change, while Android being uncovered costs only
 * where Android differs.
 *
 * So the guard is numeric. A press on the centre of a tile's bottom edge is pure rotation about x,
 * so the tile draws as a symmetric trapezoid and the camera's depth follows from the two horizontal
 * widths: `depth = s(r + 1) / (r - 1)`. That is a smaller claim than a picture and it is a claim
 * that can actually be made here.
 *
 * **B-25 is the defect this exists for.** It was a camera in pixels behind geometry in dp — visible
 * on Android and invisible on the desktop — and it was confirmed by one screenshot somebody took by
 * hand off a phone that happened to be plugged in, which is an anecdote with a timestamp.
 *
 * `./gradlew :kvadrant-core:connectedAndroidDeviceTest`, and it needs a device. It is deliberately
 * **not** in `check`: a gate that cannot run without hardware is a gate that gets skipped, and a
 * skipped gate reads as a green one.
 */
@OptIn(ExperimentalTestApi::class)
class AndroidCameraProbeTest {
    @Test
    fun the_android_camera_sits_where_the_arithmetic_says() =
        runComposeUiTest {
            val tile = 300
            var top = 0
            var bottom = 0
            setContent {
                Box(
                    Modifier.size(360.dp).background(Color.Black).testTag("frame"),
                    contentAlignment = Alignment.Center,
                ) {
                    val source = remember { MutableInteractionSource() }
                    // **In pixels, not in dp.** `PressInteraction.Press` carries a position in the
                    // pressed element's own pixels; the desktop probe writes dp there and gets away
                    // with it because its density is 1. At 2.625 the same numbers land near the
                    // top-left corner instead of the bottom edge, and the tile draws almost flat —
                    // which reads as a camera at minus thirty-eight thousand pixels.
                    val pixels = with(LocalDensity.current) { tile.dp.toPx() }
                    LaunchedEffect(pixels) {
                        source.emit(PressInteraction.Press(Offset(pixels / 2f, pixels - 2f)))
                    }
                    Box(
                        Modifier
                            .size(tile.dp)
                            .indication(source, TiltIndication())
                            .background(Color.White),
                    )
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)
            val widths =
                (0 until image.height).map { y ->
                    (0 until image.width).count { x -> (pixels[y * image.width + x] shr 16 and 0xFF) > 0x80 }
                }
            val rows = widths.withIndex().filter { it.value > image.width / 4 }.map { it.index }
            assertTrue(
                rows.size > 8,
                "no trapezoid was drawn: ${rows.size} rows of tile in a ${image.width}x${image.height} frame",
            )
            top = widths[rows.first() + 2]
            bottom = widths[rows.last() - 2]

            // The tile is measured in dp and the image in pixels, so the solved depth comes out in
            // pixels and the declared one is dp. Comparing them needs the density, which is the
            // whole of what B-25 was: a camera in one unit behind a geometry in another.
            val density = image.width / 360f
            val s = (tile * density / 2f) * sin(0.484f * 0.3f)
            val r = top.toFloat() / bottom
            val solved = s * (r + 1) / (r - 1)
            val declared = TiltIndication.DEFAULT_CAMERA_DISTANCE.value * density

            // **Kept, not just asserted.** B-29's decision was that Android gets a *number* where the
            // desktop gets goldens, and until this line the number was computed, compared against a
            // six per cent window and thrown away — so two runs a year apart on two Android versions
            // were indistinguishable as long as both were inside the window. `scripts/android_guard.py`
            // reads this out of logcat and writes it into the run record, where a drift is visible
            // as a drift rather than as a pass.
            Log.i(
                "KvadrantProbe",
                // `Locale.ROOT`, and the first run without it recorded `density=2,625`. The
                // formatter follows the *device's* locale, so a machine-readable line written for a
                // record file comes out with a decimal comma on a phone set to most of Europe — a
                // number that no longer parses, produced by a passing test.
                "camera solved=%.0f declared=%.0f density=%.3f trapezoid=%d/%d"
                    .format(Locale.ROOT, solved, declared, density, top, bottom),
            )

            assertTrue(
                abs(solved - declared) / declared < 0.06f,
                "the tilt camera solves to $solved px from a $top/$bottom trapezoid on this device, " +
                    "where $declared px was declared — `cameraDistance` is not reaching the layer " +
                    "in the unit this thinks it is",
            )
        }
}
