package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

/**
 * A press that keeps leaning towards the finger as it moves, the way the original did.
 *
 * `TiltEffect.cs` calls `ApplyTiltEffect` from **three** handlers, not one — pressed, moved, and the
 * timer — so a press that starts in the middle of a tile and slides to its corner ends up at the
 * corner's full lean. [TiltIndication] cannot do this and the reason is structural rather than an
 * oversight: it reads an `InteractionSource`, and that source carries `Press`, `Release` and
 * `Cancel` and no motion at all. The position arrives once and there is no later event to update it
 * from.
 *
 * **Measured before it was built, because the item asked for that.** On a 158 px tile the leading
 * column of the drawn quad is 152 px for a centre press and **119** for a corner one — a fifth of
 * the edge — so a finger dragged across a tile leaves a quarter of the effect on the table.
 *
 * **This is canon, not an improvement, so it is not behind `remastered`.**
 *
 * **It cannot be built on `clickable`, and that was tried first.** Wrapping it — an observer that
 * watches the pointer and re-emits `clickable`'s own press at the new position — is tidier in every
 * way except the one that matters: `clickable` gives the gesture up at the touch slop, so the press
 * it is mirroring is cancelled by the very movement this exists to follow. Measured, not reasoned:
 * the wrapped version put a press dragged to the corner **further** from a corner press than from a
 * centre one, because by then there was no press at all.
 *
 * So the gesture is this modifier's own, and the price is paid explicitly. **A change somebody else
 * has consumed ends the press**, which is how a list scrolls out from under a finger that started on
 * a tile — without that line the list scrolls *and* the tile stays leaning, which is a defect the
 * scroll test would not have caught because the list did move. What is still `clickable`'s job and
 * is not reimplemented here: keyboard activation and focus. A surface that needs those keeps
 * `clickable` and gets the ordinary tilt, which is the default everywhere anyway.
 *
 * A press is this modifier's vocabulary because it is the indication's: a bespoke `Interaction`
 * would mean every component in the library remembering to emit it, which is the forgotten-call-site
 * failure the indication design exists to avoid.
 */
public fun Modifier.kvadrantTilt(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier =
    composed {
        val source = interactionSource ?: remember { MutableInteractionSource() }
        // `awaitEachGesture` runs in a pointer scope, which is not a coroutine scope; emitting an
        // interaction needs one, and the composition's is the one that dies with the component.
        val scope = rememberCoroutineScope()
        if (!enabled) {
            Modifier
        } else {
            Modifier
                .indication(source, LocalIndication.current)
                // **Merging, because `clickable` merges.** A clickable surface and the label inside
                // it are one thing to a screen reader and one thing to a test that looks for a node
                // by its text; leaving them separate quietly changes what `onNodeWithText` returns,
                // which is how swapping this in under the tiles broke two sample tests that had
                // nothing to do with tilting.
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                }.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var press = PressInteraction.Press(down.position)
                        scope.launch { source.emit(press) }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                scope.launch { source.emit(PressInteraction.Cancel(press)) }
                                break
                            }
                            // Somebody else took it — a scroll, a pager. The phone's tilt let go at
                            // the same moment, and a tile still leaning under a list that has
                            // started moving is the thing this line exists to prevent.
                            if (change.isConsumed) {
                                scope.launch { source.emit(PressInteraction.Cancel(press)) }
                                break
                            }
                            if (!change.pressed) {
                                val inside =
                                    change.position.x in 0f..size.width.toFloat() &&
                                        change.position.y in 0f..size.height.toFloat()
                                scope.launch {
                                    if (inside) {
                                        source.emit(PressInteraction.Release(press))
                                        onClick()
                                    } else {
                                        source.emit(PressInteraction.Cancel(press))
                                    }
                                }
                                break
                            }
                            if (change.position != press.pressPosition) {
                                press = PressInteraction.Press(change.position)
                                scope.launch { source.emit(press) }
                            }
                        }
                    }
                }
        }
    }
