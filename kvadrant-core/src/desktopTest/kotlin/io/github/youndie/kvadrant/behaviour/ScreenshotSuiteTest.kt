package io.github.youndie.kvadrant.behaviour

import ru.workinprogress.viddik.generated.GeneratedViddikRegistry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What guards the screenshot suite itself.
 *
 * `viddikVerify` compares each fixture against its golden and reports nothing at all about the set:
 * with no fixture, KSP generates an empty registry, the task finds no tests, and `check` goes green
 * over a suite that proves nothing while looking healthy. The set is the thing this checks, and it
 * checks it against the registry the processor actually emitted rather than against a count written
 * down here — a number in an assertion rots the first time somebody adds a fixture.
 */
class ScreenshotSuiteTest {
    private val snapshots: File =
        sequenceOf("src/desktopTest/snapshots", "kvadrant-core/src/desktopTest/snapshots")
            .map(::File)
            .firstOrNull { it.isDirectory }
            // Never a skip. A guard that cannot find what it guards has to say so, or it becomes
            // the second thing passing green over nothing.
            ?: error("snapshots directory not found from ${File("").absolutePath}")

    /**
     * viddik's own rule: group and name joined, then every run of characters that is neither
     * alphanumeric nor a hyphen replaced by an underscore.
     *
     * **The hyphen was missing from this and it never showed**, because no golden in this suite has
     * one in its name. `kvadrant-previews` copied the rule, its ids are all kebab-case, and every
     * one of its forty-seven fixtures came back reported as having no golden. A rule that is only
     * ever exercised on the inputs it happens to be right for is not a checked rule.
     */
    private fun goldenName(
        group: String,
        name: String,
    ): String = "${group}_$name".replace(Regex("[^A-Za-z0-9-]+"), "_") + ".png"

    @Test
    fun the_suite_is_not_empty() {
        assertTrue(
            GeneratedViddikRegistry.components.isNotEmpty(),
            "the screenshot registry is empty — KSP found no @ViddikScreenshot fixture, and " +
                "viddikVerify passes green with no tests in it",
        )
    }

    @Test
    fun every_fixture_has_a_golden_and_every_golden_has_a_fixture() {
        val expected = GeneratedViddikRegistry.components.map { goldenName(it.group, it.name) }.toSet()
        val onDisk =
            snapshots
                .listFiles { f -> f.name.endsWith(".png") }
                .orEmpty()
                .map { it.name }
                .toSet()

        assertEquals(
            emptySet(),
            expected - onDisk,
            "fixtures with no golden: they verify as green because there is nothing to compare to",
        )
        assertEquals(
            emptySet(),
            onDisk - expected,
            "goldens with no fixture: left behind by a deleted or renamed fixture, and now guarding " +
                "nothing while making the suite look larger than it is",
        )
    }
}
