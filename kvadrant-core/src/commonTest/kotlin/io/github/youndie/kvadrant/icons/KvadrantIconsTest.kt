package io.github.youndie.kvadrant.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The set is not empty and every glyph in it has something in it.
 *
 * A generator that reads an empty directory and writes an empty object is a build that goes green
 * having produced nothing, and `All` is exactly the value a gallery or a completeness check would
 * trust. This is the vacuity guard for both.
 */
class KvadrantIconsTest {
    @Test
    fun every_glyph_has_a_name_and_a_path() {
        // Forty is the set as drawn. `>=` because adding one is ordinary and removing one is the
        // thing worth stopping — the generator's own `--check` covers the rest.
        assertTrue(KvadrantIcons.All.size >= 40, "the set has ${KvadrantIcons.All.size} glyphs")
        KvadrantIcons.All.forEach { (name, vector) ->
            assertTrue(name.isNotBlank(), "a glyph has no name")
            assertTrue(vector.root.iterator().hasNext(), "$name has no paths in it")
            assertEquals(26f, vector.viewportWidth, "$name is not drawn on the 26 x 26 grid")
            assertEquals(26f, vector.viewportHeight, "$name is not drawn on the 26 x 26 grid")
        }
        assertEquals(
            KvadrantIcons.All
                .map { it.first }
                .distinct()
                .size,
            KvadrantIcons.All.size,
            "two glyphs share a name",
        )
    }
}
