package io.github.youndie.kvadrant.previews

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The registry's own shape, before anything renders.
 *
 * Ids reach three places that cannot be corrected afterwards — a URL on the site, an HTML attribute,
 * and the filename of anything recorded from the preview — so the rules are enforced here rather
 * than left to whoever adds the next one.
 */
class PreviewRegistryTest {
    private val idPattern = Regex("[a-z][a-z0-9-]*")

    @Test
    fun the_registry_is_not_empty() {
        // Every other test in this file iterates the registry, so an empty one turns the whole file
        // green while proving nothing. That is the failure mode this exists for.
        assertTrue(KvadrantPreviews.all.isNotEmpty(), "no previews are registered")
    }

    @Test
    fun every_id_is_a_usable_address() {
        val bad = KvadrantPreviews.all.map { it.id }.filterNot { idPattern.matches(it) }
        assertTrue(bad.isEmpty(), "ids must be lower-case kebab-case ASCII: $bad")
    }

    @Test
    fun every_preview_names_a_component_and_says_what_it_shows() {
        KvadrantPreviews.all.forEach { preview ->
            if (preview.component.isBlank()) fail("${preview.id} names no component")
            if (preview.summary.isBlank()) fail("${preview.id} has no summary")
            if (preview.heightDp < 40) fail("${preview.id} asks for ${preview.heightDp} dp, which cannot show anything")
        }
    }
}
