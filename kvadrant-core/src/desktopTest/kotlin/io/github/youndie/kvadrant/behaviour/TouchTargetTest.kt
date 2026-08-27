package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantRadioButton
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.components.KvadrantTextBox
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test

/**
 * D7's other half, and the half a screenshot cannot check.
 *
 * The policy is that Metro's visual stays canonical — a 25.5 dp touch target, which is what the
 * phone drew — while the *hit area* is always extended to 48 dp. That split is only worth anything
 * if it holds, and nothing about a rendered image says whether it does. So: every interactive
 * control, measured.
 */
@OptIn(ExperimentalTestApi::class)
class TouchTargetTest {
    @Test
    fun every_interactive_control_is_at_least_48dp_tall() =
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    Column {
                        KvadrantButton("ok", {}, Modifier.testTag("button"))
                        KvadrantToggleSwitch(false, {}, Modifier.testTag("toggle"))
                        KvadrantCheckBox(false, {}, Modifier.testTag("checkbox"))
                        KvadrantRadioButton(false, {}, Modifier.testTag("radio"))
                        KvadrantSlider(0.5f, {}, Modifier.testTag("slider"))
                        KvadrantTextBox("", {}, Modifier.testTag("textbox"))
                    }
                }
            }

            listOf("button", "toggle", "checkbox", "radio", "slider", "textbox").forEach { tag ->
                onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
            }
        }
}
