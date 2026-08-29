package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
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
 * scroll test would not have caught because the list did move.
 *
 * **This used to end by saying keyboard activation and focus were still `clickable`'s job and that a
 * surface needing them should keep `clickable`. That was a sentence, not a plan**: the only surface
 * in the library that uses this modifier is the tile, the tile is the component the library is
 * *for*, and telling it to use something else means telling it to give up the finger-tracking this
 * exists for.
 * [B-40](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-40-keyboard-and-focus-on-desktop-and-wasm.md).
 * So the two are reimplemented below — [Modifier.focusable] over the same interaction source, and a
 * key handler that presses on the way down and clicks on the way up, which is what
 * `AbstractClickableNode` does. Reusing the source is the whole trick: the focus ring lives in the
 * indication, so it arrives with no further wiring.
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
        // Where a keyboard press lands. `PressInteraction.Press` carries a position because the
        // tilt leans towards it, and a key has none — so the centre, which is the only point on a
        // surface a keyboard can be said to have chosen, and what `clickable` uses for the same
        // reason. Zero would work too and would lean every keyboard press into the top-left corner.
        var size by remember { mutableStateOf(IntSize.Zero) }
        var keyPress by remember { mutableStateOf<PressInteraction.Press?>(null) }
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
                }.onSizeChanged { size = it }
                .onKeyEvent { event ->
                    if (event.key !in ACTIVATION_KEYS) {
                        false
                    } else {
                        when (event.type) {
                            KeyEventType.KeyDown -> {
                                // Repeats arrive while a key is held. Emitting a fresh press for
                                // each would re-run the lean from the start thirty times a second.
                                if (keyPress == null) {
                                    val press =
                                        PressInteraction.Press(
                                            Offset(size.width / 2f, size.height / 2f),
                                        )
                                    keyPress = press
                                    scope.launch { source.emit(press) }
                                }
                                true
                            }

                            KeyEventType.KeyUp -> {
                                val press = keyPress
                                keyPress = null
                                if (press != null) {
                                    scope.launch {
                                        source.emit(PressInteraction.Release(press))
                                        onClick()
                                    }
                                }
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    }
                }.focusable(interactionSource = source)
                .pointerInput(Unit) {
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
                            // **The `Final` pass, and reading it on `Main` was the defect.** A
                            // scroller above this one consumes *after* its children have seen the
                            // event, so a child asking `isConsumed` on `Main` is asking before the
                            // answer exists and always hears no. Reported from a phone: drag a tile
                            // sideways to page the pivot, let go, and the tile's page opens —
                            // because the gesture had been taken and nothing here had noticed, so
                            // the lift still counted as a tap.
                            val settled = awaitPointerEvent(PointerEventPass.Final)
                            val after = settled.changes.firstOrNull { it.id == down.id }
                            if (after != null && after.isConsumed) {
                                scope.launch { source.emit(PressInteraction.Cancel(press)) }
                                break
                            }
                        }
                    }
                }
        }
    }

/**
 * What activates a focused surface, matching `AbstractClickableNode`'s own set.
 *
 * [Key.DirectionCenter] is the d-pad's middle button, which is how a television remote and an
 * Android accessibility switch press things; it costs one line and is invisible on the two targets
 * that have a keyboard instead.
 */
private val ACTIVATION_KEYS =
    setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter)
