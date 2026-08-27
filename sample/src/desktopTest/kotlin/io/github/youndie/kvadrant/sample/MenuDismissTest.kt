package io.github.youndie.kvadrant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

/**
 * The app bar's menu closes on a tap in free space.
 *
 * The dismissal is a full-screen layer in the sample rather than in the component, because the app
 * bar sits in a column and cannot cover the page above it — the context menu has a host for exactly
 * this and the app bar does not. A layer placed one child too early in the Box is below the page and
 * never sees the tap, which is the way this fails and is invisible in the source.
 */
@OptIn(ExperimentalTestApi::class)
class MenuDismissTest {
    @Test
    fun tapping_free_space_closes_the_app_bar_menu() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(520.dp, 1000.dp).testTag("screen")) { KvadrantSampleApp() }
            }
            waitForIdle()

            onNodeWithText("···").performTouchInput { click() }
            waitForIdle()
            onNodeWithText("настройки").assertExists()

            // Well up the page, clear of the bar and of the menu that grew out of it.
            onNodeWithTag("screen").performTouchInput { click(Offset(width / 2f, height * 0.25f)) }
            waitForIdle()

            onAllNodesWithText("настройки").assertCountEquals(0)
        }
}
