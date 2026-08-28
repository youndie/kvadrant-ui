package io.github.youndie.kvadrant.previews

import java.io.File

/**
 * Writes the registry out as JSON for the documentation site's generator.
 *
 * The generator is a Python script and the registry is a Kotlin object, so something has to cross
 * between them. It is **this** rather than a regular expression over the sources, and the difference
 * is what is being read: a regular expression reads what the file says, this reads what the compiler
 * built. A preview added inside an `if`, a summary assembled from two strings, a file the module
 * does not actually include — all three are invisible to a text scan and all three are ordinary
 * Kotlin.
 *
 * The bodies cannot cross, and do not need to: the site mounts them out of the wasm bundle, which
 * is built from this same registry.
 */
fun main(args: Array<String>) {
    val destination = File(args.firstOrNull() ?: error("usage: PreviewIndexKt <output.json>"))
    destination.parentFile?.mkdirs()

    val entries =
        KvadrantPreviews.all.joinToString(",\n") { preview ->
            """
            |    {
            |      "id": ${preview.id.quoted()},
            |      "component": ${preview.component.quoted()},
            |      "summary": ${preview.summary.quoted()},
            |      "heightDp": ${preview.heightDp}
            |    }
            """.trimMargin()
        }

    destination.writeText("[\n$entries\n]\n")
    println("${KvadrantPreviews.all.size} previews -> ${destination.absolutePath}")
}

/**
 * JSON string escaping, hand-rolled because this module has no serialisation dependency and adding
 * one to publish four fields would be the larger change. It escapes what JSON requires and refuses
 * what it cannot escape, rather than emitting something that parses into the wrong text.
 */
private fun String.quoted(): String {
    val body =
        buildString {
            this@quoted.forEach { character ->
                when {
                    character == '"' -> append("\\\"")
                    character == '\\' -> append("\\\\")
                    character == '\n' -> append("\\n")
                    character.code < 0x20 -> error("control character U+%04X in a preview field".format(character.code))
                    else -> append(character)
                }
            }
        }
    return "\"$body\""
}
