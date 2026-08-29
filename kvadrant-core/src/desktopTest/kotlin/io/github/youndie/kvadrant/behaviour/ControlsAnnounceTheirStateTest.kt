package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantProgressBar
import io.github.youndie.kvadrant.components.KvadrantProgressDots
import io.github.youndie.kvadrant.components.KvadrantRadioButton
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What a screen reader is told, for the six controls that have something to tell it.
 *
 * [B-39](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-39-semantics-beyond-touch-targets.md).
 * Before this, `KvadrantToggleSwitch` was a `Modifier.clickable` — so it announced as a box that
 * could be tapped, with nothing about being a switch and nothing about being on. The same for the
 * check box and the radio; the slider and both progress indicators reported no value at all, which
 * for a progress indicator is the only thing it has.
 *
 * `InteractiveNodesAreNamedTest` in `kvadrant-previews` guards the general rule — nothing pressable
 * is anonymous — across every preview, so a *new* control cannot arrive unannounced. This one is the
 * specific claim: that these six say the right thing, and keep saying it when their value changes.
 */
@OptIn(ExperimentalTestApi::class)
class ControlsAnnounceTheirStateTest {
    @Test
    fun the_toggle_switch_is_a_switch_and_says_which_way_it_is_set() {
        assertEquals(Role.Switch, roleOf { KvadrantToggleSwitch(true, {}) })
        assertEquals(ToggleableState.On, toggleOf { KvadrantToggleSwitch(true, {}) })
        assertEquals(ToggleableState.Off, toggleOf { KvadrantToggleSwitch(false, {}) })
    }

    @Test
    fun the_check_box_is_a_check_box_and_says_whether_it_is_ticked() {
        assertEquals(Role.Checkbox, roleOf { KvadrantCheckBox(true, {}) })
        assertEquals(ToggleableState.On, toggleOf { KvadrantCheckBox(true, {}) })
        assertEquals(ToggleableState.Off, toggleOf { KvadrantCheckBox(false, {}) })
    }

    @Test
    fun the_radio_button_is_a_radio_button_and_says_whether_it_is_chosen() {
        assertEquals(Role.RadioButton, roleOf { KvadrantRadioButton(true, {}) })
        assertEquals(true, selectedOf { KvadrantRadioButton(true, {}) })
        assertEquals(false, selectedOf { KvadrantRadioButton(false, {}) })
    }

    @Test
    fun the_slider_and_the_determinate_bar_report_where_they_are() {
        val slider = rangeOf { KvadrantSlider(0.25f, {}) }
        val bar = rangeOf { KvadrantProgressBar(0.75f) }
        assertEquals(0.25f, slider?.current)
        assertEquals(0.75f, bar?.current)
    }

    /**
     * And the indeterminate one says it has no position, rather than reporting a false zero.
     *
     * Five dots crossing a bar carry no progress value at all, so `ProgressBarRangeInfo(0f, 0f..1f)`
     * would be a reader announcing "nought per cent" forever about something that is not measuring
     * anything.
     */
    @Test
    fun the_dots_report_that_they_are_indeterminate() {
        assertEquals(ProgressBarRangeInfo.Indeterminate, rangeOf { KvadrantProgressDots() })
    }

    private fun roleOf(content: @Composable () -> Unit): Role? =
        read(content) { it.getOrNull(SemanticsProperties.Role) }

    private fun toggleOf(content: @Composable () -> Unit): ToggleableState? =
        read(content) { it.getOrNull(SemanticsProperties.ToggleableState) }

    private fun selectedOf(content: @Composable () -> Unit): Boolean? =
        read(content) { it.getOrNull(SemanticsProperties.Selected) }

    private fun rangeOf(content: @Composable () -> Unit): ProgressBarRangeInfo? =
        read(content) { it.getOrNull(SemanticsProperties.ProgressBarRangeInfo) }

    /**
     * The property, read off the node or off whichever descendant carries it.
     *
     * The tilt merges its subtree, so which node a property lands on is an implementation detail of
     * the indication rather than something these assertions should depend on.
     */
    private fun <T> read(
        content: @Composable () -> Unit,
        of: (androidx.compose.ui.semantics.SemanticsConfiguration) -> T?,
    ): T? {
        var found: T? = null
        runComposeUiTest {
            setContent {
                KvadrantTheme(colors = KvadrantColors.dark()) {
                    Box(Modifier.size(300.dp, 200.dp).testTag(TAG)) { content() }
                }
            }
            waitForIdle()

            fun search(node: androidx.compose.ui.semantics.SemanticsNode): T? =
                of(node.config) ?: node.children.firstNotNullOfOrNull(::search)
            found = search(onNodeWithTag(TAG).fetchSemanticsNode())
        }
        return found
    }

    /** The control: a bare box announces nothing, so a green run above is about the components. */
    @Test
    fun an_empty_frame_announces_nothing() {
        assertTrue(roleOf { } == null, "an empty frame reported a role, so these reads find something else")
    }

    private companion object {
        const val TAG = "frame"
    }
}
