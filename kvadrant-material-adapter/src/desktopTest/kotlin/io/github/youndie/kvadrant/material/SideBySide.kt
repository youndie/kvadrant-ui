package io.github.youndie.kvadrant.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.components.KvadrantTextBox
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * Each Kvadrant control above the Material one the adapter is meant to make bearable.
 *
 * This picture is what the adapter's claim rests on, and it is also the honest record of where the
 * claim runs out: the button below is a pill and no amount of theming squares it, because
 * `ButtonDefaults.shape` is a token rather than a theme slot. Anything that still looks foreign here
 * is a component that needs a wrapper — research §1.3 counts about ten of them — and this is where
 * the count stops being a number in a table.
 */
@Composable
private fun Pairs(colors: KvadrantColors) {
    // **With a typography, and without one this fixture drew in the host's system sans-serif.**
    // `KvadrantTheme`'s default is `FontFamily.SansSerif`, which is whatever the machine supplies —
    // so these two goldens recorded the recording machine, which is the exact failure `CLAUDE.md`
    // rules out for fixtures with a missing font. It went unnoticed until Linux drew them.
    KvadrantTheme(colors = colors, typography = portableTypography(kvadrantLatin())) {
        KvadrantMaterialAdapter {
            Column(
                Modifier.fillMaxSize().background(colors.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KvadrantText("kvadrant", style = KvadrantTheme.typography.subtle)
                KvadrantButton(onClick = {}, text = "готово")
                KvadrantTextBox(value = "имя", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                KvadrantToggleSwitch(checked = true, onCheckedChange = {})
                KvadrantSlider(value = 0.4f, onValueChange = {})

                KvadrantText("material, under the adapter", style = KvadrantTheme.typography.subtle)
                Button(onClick = {}) { Text("готово") }
                OutlinedTextField(value = "имя", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                Switch(checked = true, onCheckedChange = {})
                Slider(value = 0.4f, onValueChange = {})
                Card(Modifier.fillMaxWidth()) {
                    Text("card", Modifier.padding(9.dp), style = MaterialTheme.typography.bodyMedium)
                }

                KvadrantText("material, wrapped", style = KvadrantTheme.typography.subtle)
                KvadrantMaterialButton(onClick = {}) { Text("готово") }
                KvadrantMaterialSwitch(checked = true, onCheckedChange = {})
                KvadrantMaterialSlider(value = 0.4f, onValueChange = {})
            }
        }
    }
}

@ViddikScreenshot(name = "pairs dark", group = "adapter", width = 420, height = 880)
@Composable
internal fun PairsDark(): Unit = Pairs(KvadrantColors.dark())

@ViddikScreenshot(name = "pairs light", group = "adapter", width = 420, height = 880)
@Composable
internal fun PairsLight(): Unit = Pairs(KvadrantColors.light())
