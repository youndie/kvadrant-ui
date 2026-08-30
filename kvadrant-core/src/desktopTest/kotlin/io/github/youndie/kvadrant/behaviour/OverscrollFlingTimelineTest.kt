package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * What a fling into the end actually looks like, frame by frame, as a picture.
 *
 * **This exists because three attempts at the feel were argued rather than looked at.** The
 * compression's depth, its shape and its timing were each changed on a report from a device and each
 * change was defended with arithmetic; what nobody had was the motion itself. `OverscrollFlingTest`
 * next door measures one number per frame and asserts on it, which is the right shape for a gate and
 * the wrong one for a question like "why does a small throw jump".
 *
 * Each test writes a **space-time diagram**: the middle column of every frame, laid left to right.
 * Time runs across, position runs down, and the bands' boundaries draw themselves as lines — so the
 * whole gesture is one image, and a squeeze that arrives in a frame looks different from one that
 * builds, at a glance and without a stopwatch.
 *
 *     ./gradlew :kvadrant-core:desktopTest --tests '*OverscrollFlingTimelineTest*'
 *     open kvadrant-core/build/reports/overscroll/
 *
 * It asserts only that the list reached its stop and that something moved, because the picture is
 * the point and a diagnostic that fails for a subtle reason is one nobody runs.
 */
@OptIn(ExperimentalTestApi::class)
class OverscrollFlingTimelineTest {
    @Test
    fun a_hard_throw() = timeline("hard", HARD)

    @Test
    fun a_gentle_throw() = timeline("gentle", GENTLE)

    private fun timeline(
        name: String,
        velocity: Float,
    ) {
        val columns = mutableListOf<IntArray>()
        var released = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                KvadrantTheme(colors = KvadrantColors.dark()) {
                    val scroll = rememberScrollState()
                    Box(
                        Modifier
                            .size(WIDTH.dp, VIEWPORT.dp)
                            .background(Color.Black)
                            .testTag(TAG)
                            .verticalScroll(scroll),
                    ) {
                        Column {
                            repeat(BANDS) { index ->
                                Box(
                                    Modifier
                                        .size(WIDTH.dp, BAND.dp)
                                        .background(if (index % 2 == 0) Color.White else Color.Blue),
                                )
                            }
                        }
                    }
                }
            }
            repeat(SETTLE_FRAMES) { mainClock.advanceTimeByFrame() }

            // **The gesture is injected by hand, one step per frame, so that it is *in* the
            // picture.** `swipeWithVelocity` advances the clock inside itself, so a capture loop
            // that starts after it begins at the release — and the reports being chased are about
            // what happens at the release, which cannot be read from a picture that starts there.
            //
            // The speed is the step divided by the frame, which is also how the injector's own
            // velocity tracker will read it.
            val step = velocity * FRAME_MILLIS / 1000f
            var y = VIEWPORT - 1f
            onNodeWithTag(TAG).performTouchInput { down(Offset(WIDTH / 2f, y)) }
            columns += middleColumn()
            repeat(DRAG_FRAMES) {
                y -= step
                onNodeWithTag(TAG).performTouchInput {
                    advanceEventTime(FRAME_MILLIS.toLong())
                    moveTo(Offset(WIDTH / 2f, y))
                }
                mainClock.advanceTimeByFrame()
                columns += middleColumn()
            }
            released = columns.size
            onNodeWithTag(TAG).performTouchInput { up() }

            repeat(FRAMES) {
                mainClock.advanceTimeByFrame()
                columns += middleColumn()
            }
        }

        assertTrue(columns.size > FRAMES, "only ${columns.size} frames were captured")
        assertTrue(
            columns.map { it.toList() }.distinct().size > 1,
            "every frame is identical, so the gesture did nothing and the picture would show it",
        )
        write(name, columns, released)
    }

    /** The middle column of the frame: one pixel wide, the whole viewport tall. */
    private fun ComposeUiTest.middleColumn(): IntArray {
        val image = onNodeWithTag(TAG).captureToImage()
        val pixels = IntArray(image.width * image.height).also(image::readPixels)
        return IntArray(image.height) { y -> pixels[y * image.width + image.width / 2] }
    }

    /** One column per frame, widened so a frame is visible, with a ruler every ten. */
    private fun write(
        name: String,
        columns: List<IntArray>,
        released: Int,
    ) {
        val height = columns.first().size
        val image = BufferedImage(columns.size * SCALE, height, BufferedImage.TYPE_INT_ARGB)
        columns.forEachIndexed { frame, column ->
            val onRuler = frame % RULER_EVERY == 0
            // The frame the finger left the glass, drawn solid, because every question asked about
            // this effect has been about which side of that line something happens on.
            val onRelease = frame == released
            for (y in 0 until height) {
                // A tick every ten frames, so a distance across the picture can be read as
                // milliseconds without counting pixels: ten frames is a sixth of a second.
                val colour =
                    when {
                        onRelease -> RELEASE
                        onRuler && y % 12 < 2 -> TICK
                        else -> column[y]
                    }
                for (x in 0 until SCALE) image.setRGB(frame * SCALE + x, y, colour)
            }
        }
        val out = File(DIRECTORY).apply { mkdirs() }.resolve("fling-$name.png")
        ImageIO.write(image, "png", out)
        println("wrote $out")
    }

    private companion object {
        const val TAG = "list"
        const val WIDTH = 200
        const val VIEWPORT = 300
        const val BANDS = 6
        const val BAND = 100
        const val THROW = 280f
        const val SETTLE_FRAMES = 8
        const val FRAMES = 45

        /**
         * Long enough for the drag to reach the stop **at the slower speed too**.
         *
         * At twenty-five the gentle throw was still scrolling when the finger left, so its picture
         * showed a list stopping dead rather than a list at its end being thrown into it — and the
         * two diagrams were of different gestures. Thirty-five frames is four hundred and twenty
         * pixels at the slower step against three hundred of travel.
         */
        const val DRAG_FRAMES = 35
        const val FRAME_MILLIS = 16
        const val SCALE = 4
        const val RULER_EVERY = 10
        const val TICK = 0xFF808080.toInt()
        const val RELEASE = 0xFFFF0000.toInt()
        const val GENTLE = 750f
        const val HARD = 1500f
        const val DIRECTORY = "build/reports/overscroll"
    }
}
