package io.github.youndie.kvadrant.theme

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every measurement in the set moves with [KvadrantMetrics.scaled], or is on a list that says why.
 *
 * Three separate numbers have now been found sitting outside this set as constants in a component,
 * growing not at all while everything around them grew: the button's content padding, the progress
 * bar's thickness and its margin. Each was noticed by a person looking at a scaled-up screen and
 * saying it looked wrong. This catches the next one at the point it is added — a metric that reaches
 * the data class but not `scaled` — which is the half of the problem a machine can see.
 *
 * The half it cannot see is a number that never reaches the data class at all. For that there is
 * only the rule: a dimension belongs in [KvadrantMetrics], not beside the composable that draws it.
 */
class MetricsScaleTest {
    /**
     * The two that deliberately do not scale, each with its reason written where it is defined.
     *
     * The list is here rather than as a flag on the field so that adding a metric cannot quietly
     * join it: an exemption has to be typed into a test, in the same commit, by the person taking it.
     */
    private val exempt = setOf("touchTargetMin", "tiltDepression")

    @Test
    fun scaling_the_set_scales_every_measurement_in_it() {
        val base = KvadrantMetrics()
        val doubled = base.scaled(2f)
        // A `Dp` is a value class over `Float`, so its getter compiles to a mangled name returning
        // a primitive. That is what identifies a measurement here; `scale` and `isDark`-style
        // fields are not `Dp` and do not match.
        val measurements =
            KvadrantMetrics::class.java.methods.filter {
                it.parameterCount == 0 &&
                    it.returnType == Float::class.javaPrimitiveType &&
                    '-' in it.name &&
                    // `componentN` is the same measurement under the data class's destructuring
                    // name, and reporting `component6` tells nobody which field is wrong.
                    it.name.startsWith("get")
            }
        assertTrue(measurements.size >= 10, "found only ${measurements.size} measurements; the getter shape changed")

        measurements.forEach { getter ->
            val name =
                getter.name
                    .removePrefix("get")
                    .substringBefore('-')
                    .replaceFirstChar(Char::lowercase)
            val before = getter.invoke(base) as Float
            val after = getter.invoke(doubled) as Float
            if (name in exempt) {
                assertTrue(abs(after - before) < 0.01f, "$name is on the exempt list but scaled: $before -> $after")
            } else {
                assertTrue(
                    abs(after - before * 2f) < 0.01f,
                    "$name does not scale with the set: $before -> $after, expected ${before * 2f}",
                )
            }
        }
    }
}
