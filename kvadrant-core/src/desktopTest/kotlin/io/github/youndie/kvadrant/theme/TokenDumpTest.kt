package io.github.youndie.kvadrant.theme

import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The palette, field by field, against the dump it was transcribed from.
 *
 * This is B-05's standing criterion, and it could not be written until B-20 put `metro-tokens.json`
 * in the repository — a test, like a build, must not reach outside it.
 *
 * **The dump is the oracle only where it says it is.** Each entry carries a `confidence`, and one is
 * `unverified`: `light.border`, with the dump's own note that it matches the dark value and should
 * be re-checked. It was re-checked, in the SDK's `ThemeResources` — 75% white against 60% black —
 * so our value deliberately differs there. A test asserting the whole dump would fail on that row;
 * a test skipping low-confidence rows would let a real transcription error hide behind the flag. So
 * an unverified row is asserted to **differ**, and the correction is pinned as firmly as the
 * transcription.
 */
class TokenDumpTest {
    private val dump =
        sequenceOf(
            "../reference/metro-compose-brief/references/metro-tokens.json",
            "reference/metro-compose-brief/references/metro-tokens.json",
        ).map(::File)
            .firstOrNull { it.isFile }
            // Never a skip: a check that cannot find its oracle has to say so, or it becomes the
            // second thing passing green over nothing.
            ?: error("metro-tokens.json not found from ${File("").absolutePath}")

    private val root = Json.parseToJsonElement(dump.readText()).jsonObject

    private fun argb(
        theme: String,
        token: String,
    ): Pair<Int, String> {
        val entry = root["color"]!!.jsonObject[theme]!!.jsonObject[token]!!.jsonObject
        val hex = entry["argb"]!!.jsonPrimitive.content.removePrefix("#")
        return hex.toLong(16).toInt() to entry["confidence"]!!.jsonPrimitive.content
    }

    private fun slots(colors: KvadrantColors) =
        mapOf(
            "foreground" to colors.foreground,
            "background" to colors.background,
            "contrastForeground" to colors.contrastForeground,
            "contrastBackground" to colors.contrastBackground,
            "disabled" to colors.disabled,
            "subtle" to colors.subtle,
            "chrome" to colors.chrome,
            "semitransparent" to colors.semitransparent,
            "border" to colors.border,
            "inactive" to colors.inactive,
            "textBox" to colors.textBox,
            "textBoxEditBackground" to colors.textBoxEditBackground,
        )

    private fun check(
        theme: String,
        colors: KvadrantColors,
    ) {
        var verified = 0
        var corrected = 0
        slots(colors).forEach { (token, colour) ->
            val (expected, confidence) = argb(theme, token)
            if (confidence == "verified") {
                verified++
                assertEquals(
                    expected.toUInt().toString(16),
                    colour.toArgb().toUInt().toString(16),
                    "$theme.$token does not match the dump",
                )
            } else {
                corrected++
                assertTrue(
                    expected != colour.toArgb(),
                    "$theme.$token is marked '$confidence' in the dump and we copied it anyway — " +
                        "the flag says re-check, and the re-check is the value, not the flag",
                )
            }
        }
        // Guards the loop itself: a typo in a token name would make `slots` and the dump disagree
        // about what exists, and the loop would quietly assert less than it looks like it does.
        assertEquals(12, verified + corrected, "$theme: wrong number of tokens compared")
        assertTrue(verified >= 10, "$theme: only $verified tokens were actually asserted")
    }

    @Test
    fun the_dark_palette_matches_the_dump() = check("dark", KvadrantColors.dark())

    @Test
    fun the_light_palette_matches_the_dump_except_where_the_dump_admits_doubt() = check("light", KvadrantColors.light())

    @Test
    fun all_twenty_accents_match_the_dump_by_name() {
        val fromDump =
            root["accents"]!!
                .jsonObject["items"]!!
                .jsonArray
                .associate { item ->
                    val o = item.jsonObject
                    o["name"]!!.jsonPrimitive.content to
                        ("FF" + o["hex"]!!.jsonPrimitive.content.removePrefix("#")).toLong(16).toInt()
                }
        assertEquals(20, fromDump.size, "the dump no longer holds twenty accents")
        assertEquals(
            fromDump.keys,
            KvadrantAccents.All.map { it.first }.toSet(),
            "the accent names have drifted from the dump",
        )
        KvadrantAccents.All.forEach { (name, colour) ->
            assertEquals(
                fromDump.getValue(name).toUInt().toString(16),
                colour.toArgb().toUInt().toString(16),
                "accent '$name' does not match the dump",
            )
        }
    }
}
