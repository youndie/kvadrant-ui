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
 * Nothing you can press is anonymous.
 *
 * [B-39](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-39-semantics-beyond-touch-targets.md):
 * the library had six mentions of `semantics` in total, so a screen reader on a toggle switch got a
 * box that could be clicked and nothing about it being a switch or being on. Fixing the six
 * controls that had a state is the easy half. **The hard half is that the seventh arrives next
 * month**, and a control shipped without a role looks exactly like one with a role to everybody
 * except the person who cannot see it.
 *
 * So this walks the semantics tree of **every preview in the registry** — the registry already
 * enumerates them, so a new component is covered by existing to be previewed at all — and requires
 * every node carrying an `OnClick` action to be identifiable: a `Role`, or a label, or text inside
 * it. Any one of the three is enough, because any one of them is something a screen reader can say.
 *
 * It deliberately does **not** demand a role specifically. A list row named "Anna Peterson" needs no
 * role to be useful, and requiring one would be a rule written for the checker's convenience rather
 * than for the person it is supposed to serve.
 */
@OptIn(ExperimentalTestApi::class)
class InteractiveNodesAreNamedTest {
    @Test
    fun every_pressable_node_in_every_preview_can_be_named() {
        val anonymous = mutableListOf<String>()
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
                pressable += nodes.size
                nodes.forEach { node ->
                    if (!node.isNamed()) anonymous += "${preview.id}: ${node.describe()}"
                }
            }
        }

        // The control. If nothing in forty-eight previews is pressable, the matcher is wrong and the
        // empty failure list below means the tree was never walked rather than that it was clean.
        assertTrue(
            pressable > MINIMUM_PRESSABLE,
            "only $pressable pressable nodes were found across ${KvadrantPreviews.all.size} previews, " +
                "which is too few for this library — the matcher is finding the wrong thing and a " +
                "green result here would mean nothing",
        )

        assertTrue(
            anonymous.isEmpty(),
            "these can be pressed and cannot be named — a screen reader announces them as nothing " +
                "at all: ${anonymous.sorted()}",
        )
    }

    /** A role, a label, or text somewhere inside: any one is something that can be read aloud. */
    private fun SemanticsNode.isNamed(): Boolean {
        val config = config
        if (config.getOrNull(SemanticsProperties.Role) != null) return true
        if (config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().isNotEmpty()) return true
        if (config.getOrNull(SemanticsProperties.Text).orEmpty().isNotEmpty()) return true
        // A text field is named by its own content: a reader announces what is typed in it, and an
        // empty one announces its placeholder, which is drawn as text inside.
        if (config.getOrNull(SemanticsProperties.EditableText) != null) return true
        // Merged descendants: the tilt merges its subtree, so a row's label sits below the clickable.
        return children.any { it.isNamed() }
    }

    private fun SemanticsNode.describe(): String {
        val text = config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        return "node ${size.width}x${size.height}" + if (text.isNullOrBlank()) "" else " ($text)"
    }

    private companion object {
        const val WIDTH = 360

        /** Forty-eight previews of a component library contain more than this many press targets. */
        const val MINIMUM_PRESSABLE = 20
    }
}
