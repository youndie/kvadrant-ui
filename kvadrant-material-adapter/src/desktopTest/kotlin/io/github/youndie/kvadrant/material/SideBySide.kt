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
    // **Latin only, and with a typography.** Two ways this frame was recording the machine that
    // recorded it, and Linux found both. Without a `typography` argument `KvadrantTheme` falls back
    // to `FontFamily.SansSerif`, which is whatever the host supplies. And the words used to be
    // Russian — put through Selawik, which **has no Cyrillic**, so every one of them fell back to a
    // host font as well. The subject of this picture is shape, not script; the components' own
    // script splitting is `KvadrantText`'s job and a raw Material `Text` has none, which is itself
    // worth seeing here.
    KvadrantTheme(colors = colors, typography = portableTypography(kvadrantLatin())) {
        KvadrantMaterialAdapter {
            Column(
                Modifier.fillMaxSize().background(colors.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KvadrantText("kvadrant", style = KvadrantTheme.typography.subtle)
                KvadrantButton(onClick = {}, text = "done")
                KvadrantTextBox(value = "name", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                KvadrantToggleSwitch(checked = true, onCheckedChange = {})
                KvadrantSlider(value = 0.4f, onValueChange = {})

                KvadrantText("material, under the adapter", style = KvadrantTheme.typography.subtle)
                Button(onClick = {}) { Text("done") }
                OutlinedTextField(value = "name", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                Switch(checked = true, onCheckedChange = {})
                Slider(value = 0.4f, onValueChange = {})
                Card(Modifier.fillMaxWidth()) {
                    Text("card", Modifier.padding(9.dp), style = MaterialTheme.typography.bodyMedium)
                }

                KvadrantText("material, wrapped", style = KvadrantTheme.typography.subtle)
                KvadrantMaterialButton(onClick = {}) { Text("done") }
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
