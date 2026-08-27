package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantMessageBox
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tapping the dimmed area dismisses — when asked for, and only then.
 *
 * The box sits at the top and the dim fills the rest, so "outside" is the bottom of the screen. Both
 * halves are here because the default is the phone's behaviour, which is to ignore the tap: a test
 * that only checks dismissal passes just as well on a component that always dismisses.
 */
@OptIn(ExperimentalTestApi::class)
class MessageBoxDismissTest {
    private fun cancelsOnOutsideTap(enabled: Boolean): Int {
        var cancels = 0
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    Box(Modifier.size(400.dp, 700.dp).testTag("screen")) {
                        KvadrantMessageBox(
                            visible = true,
                            title = "удалить письмо?",
                            message = "его нельзя будет вернуть",
                            onConfirm = {},
                            onCancel = { cancels++ },
                            dismissOnOutsideClick = enabled,
                        )
                    }
                }
            }
            waitForIdle()
            // Low on the screen, well below a box that is a few lines tall at the top.
            onNodeWithTag("screen").performTouchInput {
                click(
                    androidx.compose.ui.geometry
                        .Offset(width / 2f, height * 0.85f),
                )
            }
            waitForIdle()
        }
        return cancels
    }

    @Test
    fun a_tap_outside_dismisses_when_the_caller_asked_for_it() =
        assertEquals(1, cancelsOnOutsideTap(enabled = true), "the tap outside did not dismiss")

    @Test
    fun a_tap_outside_does_nothing_by_default() =
        assertEquals(
            0,
            cancelsOnOutsideTap(enabled = false),
            "the box dismissed on an outside tap without being asked — that is not the phone's " +
                "behaviour, and a caller who did not opt in has a modal that is not modal",
        )
}
