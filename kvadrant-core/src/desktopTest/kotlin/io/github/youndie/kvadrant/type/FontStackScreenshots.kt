package io.github.youndie.kvadrant.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.kvadrant.resources.Res
import io.github.youndie.kvadrant.resources.selawik_regular
import io.github.youndie.kvadrant.resources.selawik_semilight
import io.github.youndie.kvadrant.resources.source_sans_3_variable
import org.jetbrains.compose.resources.Font
import ru.workinprogress.viddik.annotations.ViddikScreenshot

// The Metro header weight. Selawik ships a real SemiLight; the fallbacks are asked for W300 and
// give their Light, which is the closest any of them has.
private val SemiLight = FontWeight.W300

// The candidates that lost the spike are still read off the desktop classpath: they are comparison
// material, they exist only in `desktopTest/resources`, and nothing ships them.
private fun classpath(vararg files: Pair<String, FontWeight>) =
    files.map { (path, weight) -> Font(resource = path, weight = weight) }

private val Inter = arrayOf("fonts/Inter-Light.ttf" to SemiLight, "fonts/Inter-Regular.ttf" to FontWeight.W400)
private val Fira = arrayOf("fonts/FiraSans-Light.ttf" to SemiLight, "fonts/FiraSans-Regular.ttf" to FontWeight.W400)

// The two that won are read the way the library reads them, through compose-resources. Reading them
// off the classpath here as well would be a second copy of the binaries and a second code path, and
// the second path is the one that would keep passing after the first one broke.
@Composable
private fun selawik() =
    listOf(
        Font(Res.font.selawik_semilight, SemiLight),
        Font(Res.font.selawik_regular, FontWeight.W400),
    )

@Composable
private fun family(vararg files: Pair<String, FontWeight>) = FontFamily(classpath(*files))

/**
 * The spike's evidence. Each stack renders the same three lines:
 *  - the Pivot header at its real size — 72 Metro px, which is 54 sp;
 *  - one line mixing both scripts, where a rhythm mismatch shows worst;
 *  - body text at 20 Metro px = 15 sp.
 */
@Composable
private fun Specimen(family: FontFamily) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText("settings", style = TextStyle(Color.White, 54.sp, SemiLight, fontFamily = family).portable())
        BasicText("настройки", style = TextStyle(Color.White, 54.sp, SemiLight, fontFamily = family).portable())
        BasicText("inbox почта", style = TextStyle(Color.White, 54.sp, SemiLight, fontFamily = family).portable())
        BasicText(
            "choose a theme · выберите тему",
            style = TextStyle(Color.White, 15.sp, FontWeight.W400, fontFamily = family).portable(),
        )
    }
}

// **Three fixtures used to stand here and they are now `FontFallbackTest`.** `selawik only`,
// `selawik then inter` and `selawik then fira` had one MD5 between them, which was the finding: a
// `FontFamily` list is not a glyph-fallback chain, so the Cyrillic in all three came from the
// *host's* font manager. A golden of that is a golden of whichever font the machine happened to
// have — they differed by 5.4 % on a Linux runner, correctly, with nothing to fix. The claim is a
// comparison within one run and is asserted as one now, which holds on any machine and says the
// same thing. Research §1.7 points at the test.

@ViddikScreenshot(name = "inter only", group = "font stack", width = 480, height = 300)
@Composable
internal fun StackInterOnly(): Unit = Specimen(family(*Inter))

@ViddikScreenshot(name = "fira only", group = "font stack", width = 480, height = 300)
@Composable
internal fun StackFiraOnly(): Unit = Specimen(family(*Fira))

/**
 * What the FontFamily list will not do, done by hand: Latin runs stay in Selawik, Cyrillic runs go
 * to the companion, and the companion is rendered at the ratio of the two x-heights so the two
 * scripts sit on the same optical size.
 */
private fun mixed(
    text: String,
    latin: FontFamily,
    cyrillic: FontFamily,
    size: Float,
    compensation: Float,
): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val cyr = text[i] in '\u0400'..'\u04FF'
            var j = i
            while (j < text.length && ((text[j] in '\u0400'..'\u04FF') == cyr)) j++
            val run = text.substring(i, j)
            if (cyr) {
                withStyle(SpanStyle(fontFamily = cyrillic, fontSize = (size * compensation).sp)) { append(run) }
            } else {
                withStyle(SpanStyle(fontFamily = latin, fontSize = size.sp)) { append(run) }
            }
            i = j
        }
    }

@Composable
private fun MixedSpecimen(
    cyrillic: FontFamily,
    compensation: Float,
) {
    val latin = FontFamily(selawik())
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            mixed("settings настройки", latin, cyrillic, 54f, compensation),
            style = TextStyle(Color.White, fontWeight = SemiLight).portable(),
        )
        BasicText(
            mixed("inbox почта", latin, cyrillic, 54f, compensation),
            style = TextStyle(Color.White, fontWeight = SemiLight).portable(),
        )
        BasicText(
            mixed("choose a theme · выберите тему", latin, cyrillic, 15f, compensation),
            style = TextStyle(Color.White, fontWeight = FontWeight.W400).portable(),
        )
    }
}

// x-height ratios measured from the font files: Selawik 0.5000 em, Inter 0.5459, Fira 0.5270.
@ViddikScreenshot(name = "per run inter raw", group = "font stack", width = 480, height = 300)
@Composable
internal fun MixedInterRaw(): Unit = MixedSpecimen(family(*Inter), 1f)

@ViddikScreenshot(name = "per run inter compensated", group = "font stack", width = 480, height = 300)
@Composable
internal fun MixedInterCompensated(): Unit = MixedSpecimen(family(*Inter), 0.5000f / 0.5459f)

@ViddikScreenshot(name = "per run fira raw", group = "font stack", width = 480, height = 300)
@Composable
internal fun MixedFiraRaw(): Unit = MixedSpecimen(family(*Fira), 1f)

@ViddikScreenshot(name = "per run fira compensated", group = "font stack", width = 480, height = 300)
@Composable
internal fun MixedFiraCompensated(): Unit = MixedSpecimen(family(*Fira), 0.5000f / 0.5270f)

@ViddikScreenshot(name = "per run source sans compensated", group = "font stack", width = 480, height = 300)
@Composable
internal fun MixedSourceSansCompensated(): Unit = MixedSpecimen(sourceSansAt(300), 0.5000f / 0.4860f)

// "A touch bolder": Selawik's Semilight sits between Light and Regular, and Source Sans 3's Light
// at 300 reads a shade thin beside it. The variable face has a wght axis from 200 to 900, so the
// companion can be instanced at the weight that actually matches instead of the nearest static one.
@Composable
private fun sourceSansAt(weight: Int) =
    FontFamily(
        Font(
            Res.font.source_sans_3_variable,
            FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )

@ViddikScreenshot(name = "per run source sans 300", group = "font weight", width = 480, height = 220)
@Composable
internal fun MixedSourceSans300(): Unit = MixedSpecimen(sourceSansAt(300), 0.5000f / 0.4860f)

@ViddikScreenshot(name = "per run source sans 350", group = "font weight", width = 480, height = 220)
@Composable
internal fun MixedSourceSans350(): Unit = MixedSpecimen(sourceSansAt(350), 0.5000f / 0.4860f)

@ViddikScreenshot(name = "per run source sans 400", group = "font weight", width = 480, height = 220)
@Composable
internal fun MixedSourceSans400(): Unit = MixedSpecimen(sourceSansAt(400), 0.5000f / 0.4860f)

@ViddikScreenshot(name = "per run source sans 365", group = "font weight", width = 480, height = 220)
@Composable
internal fun MixedSourceSans365(): Unit = MixedSpecimen(sourceSansAt(365), 0.5000f / 0.4860f)

// Compensation candidates. 1.0288 is the ratio of the declared x-heights; the others trade a
// little of the flat-letter match for the round-letter overshoot, which is what the eye reads.
@ViddikScreenshot(name = "compensation 1000", group = "font fit", width = 480, height = 220)
@Composable
internal fun FitNone(): Unit = MixedSpecimen(sourceSansAt(365), 1.000f)

@ViddikScreenshot(name = "compensation 1010", group = "font fit", width = 480, height = 220)
@Composable
internal fun FitTenth(): Unit = MixedSpecimen(sourceSansAt(365), 1.010f)

@ViddikScreenshot(name = "compensation 1029", group = "font fit", width = 480, height = 220)
@Composable
internal fun FitXHeight(): Unit = MixedSpecimen(sourceSansAt(365), 1.0288f)

@ViddikScreenshot(name = "final 370 no compensation", group = "font fit", width = 480, height = 220)
@Composable
internal fun FitFinal(): Unit = MixedSpecimen(sourceSansAt(370), 1.000f)
