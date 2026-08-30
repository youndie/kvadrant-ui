package io.github.youndie.kvadrant.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * The demo in a browser.
 *
 * There is nothing here but an entry point on purpose: the screen is `KvadrantSampleApp`, shared with the
 * desktop and Android ones, so a browser cannot show a version of this library the other two do not
 * have. A wasm demo assembled separately would be a third opinion about what these components look
 * like, and the whole argument for B-34's documentation site is that there should be one.
 *
 * `ComposeViewport` rather than `CanvasBasedWindow`: the latter is gone in Compose Multiplatform
 * 1.12, which is the kind of thing that reads as a missing dependency until somebody unpacks the
 * klib and looks — nineteen mentions of one, none of the other.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun main() {
    ComposeViewport(document.body!!) { KvadrantSampleApp() }
}
