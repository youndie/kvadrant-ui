package io.github.youndie.kvadrant.sample

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test

/**
 * Opening the context menu on a contact settles instead of spinning forever.
 *
 * The rows used to report their position into shared *state* from `onGloballyPositioned`. Three rows
 * writing one state is a hang: each write differs from the last, each differing write schedules
 * another layout pass, and the three chase each other with no fixed point. Tapping a contact froze
 * the application.
 *
 * `waitForIdle` is the assertion. It returns when composition and layout have settled and never
 * returns when they cannot, so this test hangs on the defect rather than failing — which is a worse
 * failure mode than a red assertion and still far better than shipping it. The timeout that catches
 * it is the test task's own.
 */
@OptIn(ExperimentalTestApi::class)
class ShowcaseSettlesTest {
    @Test
    fun tapping_a_contact_settles() =
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    Showcase("почта", kvadrantCyrillic(), onBack = {})
                }
            }
            waitForIdle()
            onNodeWithText("Анна Петрова").performClick()
            waitForIdle()
            // Reached only if layout found a fixed point; the menu is over the page by now.
            onNodeWithText("закрепить на экране").assertExists()
        }
}
