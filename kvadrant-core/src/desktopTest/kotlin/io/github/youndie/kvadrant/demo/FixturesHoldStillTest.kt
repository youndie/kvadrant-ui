package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import ru.workinprogress.viddik.generated.GeneratedViddikRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * No fixture moves while nobody is touching it.
 *
 * [B-31](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-31-screenshot-suite-is-not-deterministic.md)
 * was opened on six goldens that came back different from two recordings of unchanged source, in
 * shades a few units off the accent — the signature of something being captured at an arbitrary
 * phase. It has never reproduced, including from a worktree at the commit it was found on, and the
 * guard it was given asks the wrong question: recording the suite twice compares two arbitrary
 * phases and passes whenever they happen to be the same one.
 *
 * This asks the question directly. Every fixture in the registry is composed, captured, left alone
 * for [QUIET_MILLIS] of a stopped clock, and captured again. A fixture holding anything that
 * animates on its own — an indeterminate progress, a live tile on a jittered timer, a toast that
 * slides in — cannot pass, whatever phase either capture lands on. That is what makes it a
 * statement about the fixtures rather than about a run of them.
 *
 * It is inside `check` because it costs one extra frame's work per fixture and because a fixture
 * that cannot hold still is a defect in the fixture, not a flake in the machine.
 */
@OptIn(ExperimentalTestApi::class)
class FixturesHoldStillTest {
    @Test
    fun every_screenshot_fixture_draws_the_same_thing_a_second_later() {
        val moving = mutableListOf<String>()
        var checked = 0

        GeneratedViddikRegistry.components.forEach { component ->
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    Box(Modifier.testTag(TAG).size(component.width.dp, component.height.dp)) {
                        component.content()
                    }
                }
                // Enough for the first composition and its layout to land. Not a settle: anything
                // that is *still going* after this is the point.
                val frames =
                    SAMPLES.map { millis ->
                        mainClock.advanceTimeBy(millis)
                        mainClock.advanceTimeByFrame()
                        pixels()
                    }
                checked++
                val differing =
                    frames[0].indices.count { i -> frames.any { it[i] != frames[0][i] } }
                if (differing > 0) moving += "${component.group}/${component.name}"
            }
        }

        // The control. An empty failure list is only good news if the registry was walked.
        assertTrue(
            checked > MINIMUM_FIXTURES,
            "only $checked fixtures were rendered, which is fewer than this suite has — the registry " +
                "is not being read and a green result here would mean nothing",
        )

        // **An exact set, not a list of exemptions.** Four fixtures contain the indeterminate
        // progress dots, which by definition never stop, and a library with an indeterminate
        // indicator has to be able to photograph a page that shows one. What must not happen is a
        // *fifth* arriving unnoticed — so the assertion is equality: a new one fails here, and so
        // does one of these being fixed without the list being updated.
        assertEquals(
            KNOWN_MOVING,
            moving.sorted(),
            "the set of fixtures that never settle has changed. Their goldens hold whichever phase " +
                "the capture happened to land on, which is reproducible only for as long as " +
                "viddik's capture time is — see research §1.9",
        )
    }

    private fun ComposeUiTest.pixels(): IntArray {
        val image = onNodeWithTag(TAG).captureToImage()
        return IntArray(image.width * image.height).also(image::readPixels)
    }

    private companion object {
        const val TAG = "fixture"

        /**
         * Three moments, and **every one of them is compared against the first**, which is the part
         * that does the work.
         *
         * The first version of this compared the last two samples with each other and found
         * nothing. Both sat four seconds from their predecessor, so an animation that repeats lands
         * on the same phase in both and reads as still — the progress dots run a 4 400 ms cycle and
         * came back identical at four seconds and at eight. Measured afterwards rather than
         * reasoned about: against the first sample they differ by 21 px at gaps of 4 000 ms, 24 px
         * at 4 400 and 36 px at **100**, so the gap length is not what was hiding them. Equidistant
         * samples were.
         *
         * The gaps are still seconds rather than frames, and unequal, because that is cheap and
         * because the animations this has to catch are not all fast — a live tile turns over on a
         * jittered interval measured in seconds.
         *
         * The first sample is late for a different reason. Fixtures with an entrance — the message
         * box swivels in, the date picker settles — are *supposed* to be captured after it, and
         * their goldens are: `gallery/message box` holds a square-on dialog, not a mid-swivel one.
         * Sampling at composition would fail eight fixtures for doing the right thing.
         */
        val SAMPLES = listOf(3_000L, 2_300L, 3_700L)
        const val MINIMUM_FIXTURES = 40

        /**
         * The fixtures whose content never stops, every one of them because it shows
         * `KvadrantProgressDots` — five dots on a 4 400 ms infinite cycle.
         *
         * They are recorded rather than excused. Their goldens are stable across a hundred
         * recordings, and they are stable because viddik captures at the same virtual time every
         * run, not because anything about the fixture makes them so: **the phase is nobody's
         * choice.** If a viddik upgrade moves that capture point, these four change with no source
         * change, and this line is what stops that being read as a regression.
         */
        val KNOWN_MOVING =
            listOf(
                "gallery/controls dark",
                "gallery/controls light",
                "pivot/pivot mail",
                "pivot/pivot mid swipe",
            )
    }
}
