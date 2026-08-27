package io.github.youndie.kvadrant.sample

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * A tile can be tapped, and tapping it opens its page.
 *
 * The page is an overlay over the whole screen, and an alpha of zero does not stop a composable
 * receiving touches. Once the exit animation was given content to animate — by keeping the page
 * composed instead of dropping it the moment its flag flipped — that invisible page covered the
 * Start screen and swallowed every tap meant for a tile. The screen looked completely normal and
 * nothing responded.
 *
 * Both halves are asserted because either alone passes on the defect: with the page always composed
 * the tile is still *displayed*, it simply cannot be reached.
 */
@OptIn(ExperimentalTestApi::class)
class TilesAreReachableTest {
    @Test
    fun tapping_a_tile_opens_its_page() =
        runComposeUiTest {
            setContent { KvadrantSampleApp() }
            waitForIdle()

            // The calendar tile, whose label appears nowhere else — "почта" is also a Pivot
            // header, and a test that clicks the wrong one of four proves nothing about tiles.
            // Scrolled to first: the Start page is taller than the test window, and a tile below
            // the fold is legitimately not displayed. What is being checked is that a tile which is
            // on screen can be reached, not that every tile happens to fit.
            onNodeWithText("календарь").performScrollTo().assertIsDisplayed()
            onNodeWithText("календарь").performClick()
            waitForIdle()

            // A label that exists only on the page the tile opens.
            onNodeWithText("месяц").assertExists()
        }

    /**
     * The live tiles answer a tap too, which is the half that was actually broken.
     *
     * `KvadrantFlipTile` and `KvadrantCycleTile` shipped with no `onClick` at all, and putting them
     * on the Start screen left two dead rectangles among seven working ones. A Start tile is the
     * thing you tap to launch an application; a live one that cannot be tapped is not a Start tile.
     */
    @Test
    fun tapping_a_live_tile_opens_its_page() =
        runComposeUiTest {
            setContent { KvadrantSampleApp() }
            waitForIdle()

            // The flip tile's front face. It is the only "4" on the Start screen.
            onNodeWithText("4").performScrollTo().performClick()
            waitForIdle()

            onNodeWithText("погода").assertExists()
        }
}
