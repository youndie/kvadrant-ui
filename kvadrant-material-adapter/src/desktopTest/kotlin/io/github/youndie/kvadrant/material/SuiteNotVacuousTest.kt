package io.github.youndie.kvadrant.material

import ru.workinprogress.viddik.generated.GeneratedViddikRegistry
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The same guard the core carries, for the same reason: with no fixture, KSP emits an empty registry
 * and `viddikVerify` passes green with no tests in it. A screenshot suite that proves nothing while
 * looking healthy is worse here than in the core, because the side-by-side picture is the only thing
 * the adapter's claim rests on.
 */
class SuiteNotVacuousTest {
    @Test
    fun the_suite_is_not_empty() {
        assertTrue(
            GeneratedViddikRegistry.components.isNotEmpty(),
            "the screenshot registry is empty — the side-by-side is gone and nothing said so",
        )
    }
}
