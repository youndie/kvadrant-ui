package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantPasswordBox
import io.github.youndie.kvadrant.components.KvadrantProgressBar
import io.github.youndie.kvadrant.components.KvadrantRadioButton
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

@Composable
private fun Selection(colors: KvadrantColors) {
    val cyrillic = kvadrantCyrillic()
    KvadrantTheme(colors = colors, typography = KvadrantTypography.default(kvadrantLatin())) {
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KvadrantText("selection", style = KvadrantTheme.typography.extraLarge, cyrillic = cyrillic)

            KvadrantCheckBox(true, {}, label = "показывать превью", cyrillic = cyrillic)
            KvadrantCheckBox(false, {}, label = "звук уведомлений", cyrillic = cyrillic)

            KvadrantRadioButton(true, {}, label = "каждые 15 минут", cyrillic = cyrillic)
            KvadrantRadioButton(false, {}, label = "вручную", cyrillic = cyrillic)

            KvadrantText("яркость", Modifier.padding(top = 9.dp), KvadrantTheme.typography.normal, cyrillic)
            KvadrantSlider(0.65f, {}, Modifier.fillMaxWidth())

            KvadrantText("пароль", Modifier.padding(top = 9.dp), KvadrantTheme.typography.normal, cyrillic)
            KvadrantPasswordBox("secret", {}, Modifier.fillMaxWidth(), cyrillic = cyrillic)

            KvadrantProgressBar(0.72f, Modifier.padding(top = 12.dp))
        }
    }
}

@ViddikScreenshot(name = "selection dark", group = "selection", width = 400, height = 480)
@Composable
internal fun SelectionDark(): Unit = Selection(KvadrantColors.dark())

@ViddikScreenshot(name = "selection light", group = "selection", width = 400, height = 480)
@Composable
internal fun SelectionLight(): Unit = Selection(KvadrantColors.light())
