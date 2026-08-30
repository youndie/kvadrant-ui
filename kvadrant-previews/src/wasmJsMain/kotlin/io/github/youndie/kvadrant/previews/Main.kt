package io.github.youndie.kvadrant.previews

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.Element

/**
 * The documentation site's engine: **one bundle, many mounts**.
 *
 * Every page of the site loads this same binary once and marks the places a component should appear
 * with `data-kvadrant-preview="<id>"`. Nothing here knows what a page looks like, and no page knows
 * what a component looks like — the [KvadrantPreviews] registry is the only thing between them, and
 * it is the same registry the golden fixtures read. That is what stops a documented example and a
 * tested one drifting apart, which is a failure with no symptom: a stale example looks exactly like
 * a current one.
 *
 * A binary per component was the alternative and is why this is worth eight lines: the bundle is
 * several megabytes, and twenty of them on one page is a page nobody waits for.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun main() {
    val nodes = document.querySelectorAll("[data-kvadrant-preview]")
    for (index in 0 until nodes.length) {
        val element = nodes.item(index) as? Element ?: continue
        val id = element.getAttribute("data-kvadrant-preview") ?: continue
        val dark = element.getAttribute("data-kvadrant-theme") != "light"
        val preview = KvadrantPreviews.byId(id)
        if (preview == null) {
            // Louder than an empty rectangle. A mount that silently renders nothing is
            // indistinguishable from a component that draws nothing, and one of those is a bug in
            // the site while the other is a bug in the library.
            element.textContent = "no preview registered under \"$id\""
            continue
        }
        ComposeViewport(element) { KvadrantPreviewHost(preview, dark = dark) }
    }
}
