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
import io.github.youndie.kvadrant.components.FULL_MODE_THRESHOLD
import io.github.youndie.kvadrant.components.KvadrantListPicker
import io.github.youndie.kvadrant.components.KvadrantListPickerMode
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tapping a picker reports the mode its option count calls for.
 *
 * The mode was computed by the component, handed to the caller and dropped by every caller in the
 * repository, so a picker with six options was a control that could not be opened at all. It was
 * also untested, which is how it stayed inert: a value nothing reads and nothing checks is
 * indistinguishable from a value that is never produced.
 */
@OptIn(ExperimentalTestApi::class)
class ListPickerFullModeTest {
    private fun modeAfterTapping(optionCount: Int): KvadrantListPickerMode? {
        var reported: KvadrantListPickerMode? = null
        runComposeUiTest {
            setContent {
                KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
                    Box(Modifier.size(300.dp)) {
                        KvadrantListPicker(
                            modifier = Modifier.testTag("picker"),
                            items = List(optionCount) { "option $it" },
                            selectedIndex = 0,
                            onSelect = {},
                            expanded = false,
                            onExpandRequest = { reported = it },
                        )
                    }
                }
            }
            // The picker's own node, not the box around it. A click at the centre of a 300 dp box
            // lands below a picker that is one row tall, and reports nothing — which reads exactly
            // like a mode that is never produced.
            onNodeWithTag("picker").performTouchInput { click() }
            waitForIdle()
        }
        return reported
    }

    @Test
    fun the_threshold_decides_which_mode_a_tap_asks_for() {
        assertEquals(
            KvadrantListPickerMode.Expanded,
            modeAfterTapping(FULL_MODE_THRESHOLD),
            "a picker at the threshold should still unfold in place",
        )
        assertEquals(
            KvadrantListPickerMode.Full,
            modeAfterTapping(FULL_MODE_THRESHOLD + 1),
            "a picker over the threshold should ask for a page",
        )
    }
}
