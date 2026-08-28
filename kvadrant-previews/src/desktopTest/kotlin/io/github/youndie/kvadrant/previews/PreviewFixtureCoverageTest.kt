package io.github.youndie.kvadrant.previews

import ru.workinprogress.viddik.generated.GeneratedViddikRegistry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The registry, the fixtures and the goldens are the same set, checked in every direction.
 *
 * B-34's claim is that the example on a documentation page and the fixture in the golden suite are
 * the same composable, so a component that changes cannot leave a page showing the old one. That
 * holds only while every preview *has* a fixture — a registry entry with no fixture is a page whose
 * appearance nothing guards, and it looks exactly like one that is guarded.
 *
 * `PreviewFixtures.kt` is written mechanically and could not be trusted to stay complete on its
 * own; this is what makes it safe to add a preview and forget the wrapper.
 */
class PreviewFixtureCoverageTest {
    private val snapshots: File =
        sequenceOf("src/desktopTest/snapshots", "kvadrant-previews/src/desktopTest/snapshots")
            .map(::File)
            .firstOrNull { it.isDirectory }
            // Never a skip. A guard that cannot find what it guards has to say so, or it is the
            // second thing passing green over nothing.
            ?: error("snapshots directory not found from ${File("").absolutePath}")

    private val fixtures = GeneratedViddikRegistry.components.filter { it.group == "preview" }

    @Test
    fun every_preview_has_a_fixture_and_every_fixture_has_a_preview() {
        val declared = KvadrantPreviews.all.map { it.id }.toSet()
        val photographed = fixtures.map { it.name }.toSet()

        assertEquals(
            emptySet(),
            declared - photographed,
            "previews with no fixture: they appear on the site with nothing guarding how they look",
        )
        assertEquals(
            emptySet(),
            photographed - declared,
            "fixtures naming a preview the registry does not have: left behind by a rename, and now " +
                "photographing an id no page can mount",
        )
    }

    @Test
    fun every_fixture_is_the_size_its_preview_asked_for() {
        val heights = KvadrantPreviews.all.associate { it.id to it.heightDp }
        val wrong = fixtures.filter { heights[it.name] != null && it.height != heights[it.name] }
        assertTrue(
            wrong.isEmpty(),
            "a fixture is a different height from the preview it renders, so the golden is a crop " +
                "of what the page shows: ${wrong.map { "${it.name} ${it.height} != ${heights[it.name]}" }}",
        )
    }

    /**
     * viddik's own filename rule, mirrored because it is private to the engine.
     *
     * Group and name joined by an underscore, then every run of characters that is neither
     * alphanumeric nor a hyphen replaced by one. **The hyphen is the part to get right** — the
     * first version of this replaced it too and reported all forty-seven fixtures as having no
     * golden, because every preview id is kebab-case. `kvadrant-core`'s copy of this rule has the
     * same mistake and has never shown it: no golden over there has a hyphen in its name.
     */
    private fun goldenName(
        group: String,
        name: String,
    ): String = "${group}_$name".replace(Regex("[^A-Za-z0-9-]+"), "_") + ".png"

    @Test
    fun every_fixture_has_a_golden() {
        val expected = fixtures.map { goldenName(it.group, it.name) }.toSet()
        val onDisk =
            snapshots
                .listFiles { f -> f.name.endsWith(".png") }
                .orEmpty()
                .map { it.name }
                .toSet()

        assertEquals(
            emptySet(),
            expected - onDisk,
            "fixtures with no golden: they verify green because there is nothing to compare against",
        )
        assertEquals(emptySet(), onDisk - expected, "goldens with no fixture")
    }
}
