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
 * the edge — so a finger dragged across a tile leaves a quarter of the effect on the table. It is
 * not subtle and it is not worth leaving out.
 *
 * **This is canon, not an improvement, so it is not behind `remastered`.** Restoring behaviour the
 * original had and adding behaviour it lacked are different things, and D17 keeps them apart.
 *
 * The whole implementation is the gesture: a new `PressInteraction.Press` is emitted on every move,
 * and [TiltIndication] already treats a second press as "the finger is now here" rather than as a
 * new gesture. No geometry is duplicated, which is the point — a second copy of the tilt maths would
 * be a second place for it to drift.
 *
 * It replaces `clickable` rather than joining it: two sources of `Press` on one element fight over
 * the same indication. A surface that wants the ordinary behaviour keeps `clickable` and gets the
 * tilt anyway, because the tilt is the theme's indication.
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
        val indication = LocalIndication.current
        if (!enabled) {
            Modifier
        } else {
            Modifier
                .indication(source, indication)
                .semantics {
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                }.pointerInput(source) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var press = PressInteraction.Press(down.position)
                        scope.launch { source.emit(press) }
                        var up = false
                        var inside = true
                        while (!up) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) {
                                if (change.position != press.pressPosition) {
                                    // A press rather than a bespoke interaction: every component in
                                    // the library would have to remember to emit a custom one, which
                                    // is the forgotten-call-site failure the indication design was
                                    // chosen to avoid.
                                    press = PressInteraction.Press(change.position)
                                    scope.launch { source.emit(press) }
                                }
                                inside =
                                    change.position.x in 0f..size.width.toFloat() &&
                                    change.position.y in 0f..size.height.toFloat()
                            } else {
                                up = true
                            }
                        }
                        scope.launch {
                            if (inside) {
                                source.emit(PressInteraction.Release(press))
                                onClick()
                            } else {
                                source.emit(PressInteraction.Cancel(press))
                            }
                        }
                    }
                }
        }
    }
