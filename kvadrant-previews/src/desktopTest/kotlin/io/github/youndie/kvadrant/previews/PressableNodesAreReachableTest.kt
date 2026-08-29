package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Nothing you can press is a keyboard dead end.
 *
 * The sibling of `InteractiveNodesAreNamedTest`, and the same argument one step on: that one says a
 * press target has to be *nameable*, this one says it has to be *reachable*. Both walk the whole
 * preview registry rather than a list of components, so the next component is covered by having a
 * preview at all — which is already required.
 *
 * [B-40](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-40-keyboard-and-focus-on-desktop-and-wasm.md)
 * opened claiming nothing in the library was focusable. **That was half wrong and worth writing
 * down**: `clickable`, `toggleable` and `selectable` bring `Modifier.focusable` and Enter/Space
 * activation with them, so every control built on one of the three had all of it already and nobody
 * had checked. What was genuinely unreachable was `kvadrantTilt` — the library's own gesture, and
 * therefore the tile, which is the component the library exists for. One dead end, in the one place
 * a library like this cannot afford one.
 */
@OptIn(ExperimentalTestApi::class)
class PressableNodesAreReachableTest {
    @Test
    fun every_pressable_node_in_every_preview_can_be_reached_by_tab() {
        val unreachable = mutableListOf<String>()
        var pressable = 0

        KvadrantPreviews.all.forEach { preview ->
            runComposeUiTest {
                setContent {
                    Box(Modifier.size(WIDTH.dp, preview.heightDp.dp)) {
                        KvadrantPreviewHost(preview)
                    }
                }
                waitForIdle()
                val nodes =
                    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                nodes.forEach { node ->
                    // **A disabled control is not a tab stop anywhere**, and Compose keeps its
                    // `OnClick` action in the tree next to a `Disabled` flag rather than dropping
                    // it — so the button preview's greyed-out third button arrives here looking
                    // exactly like an oversight. It is exempt, and the exemption is not free: the
                    // count below is of *enabled* targets only, so if this filter ever grew to
                    // cover the tree the control would fail instead of the list coming back empty.
                    if (node.config.getOrNull(SemanticsProperties.Disabled) != null) return@forEach
                    pressable++
                    // `Focused` is present on anything focus can land on, and carries whether it
                    // currently has it. Its *absence* is the defect: a node the focus system has
                    // never heard of.
                    if (node.config.getOrNull(SemanticsProperties.Focused) == null) {
                        unreachable += "${preview.id}: ${node.describe()}"
                    }
                }
            }
        }

        // The same control the naming test carries, and for the same reason: an empty failure list
        // is only good news if the tree was walked at all.
        assertTrue(
            pressable > MINIMUM_PRESSABLE,
            "only $pressable pressable nodes were found across ${KvadrantPreviews.all.size} previews, " +
                "which is too few for this library — the matcher is finding the wrong thing and a " +
                "green result here would mean nothing",
        )

        assertTrue(
            unreachable.isEmpty(),
            "these can be pressed with a finger and not reached with a keyboard: ${unreachable.sorted()}",
        )
    }

    private fun SemanticsNode.describe(): String {
        val text = config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        return "node ${size.width}x${size.height}" + if (text.isNullOrBlank()) "" else " ($text)"
    }

    private companion object {
        const val WIDTH = 360
        const val MINIMUM_PRESSABLE = 20
    }
}
