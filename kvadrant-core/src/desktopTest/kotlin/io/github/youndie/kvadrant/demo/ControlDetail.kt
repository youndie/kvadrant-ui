package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantRadioButton
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/** Large enough to judge. A control this size in a gallery is a control nobody has looked at. */
@Composable
private fun Detail(colors: KvadrantColors) {
    KvadrantTheme(colors = colors, typography = portableTypography(kvadrantLatin())) {
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KvadrantText("off / on", style = KvadrantTheme.typography.subtle)
            Row {
                KvadrantToggleSwitch(false, {})
                KvadrantToggleSwitch(true, {})
            }
            KvadrantText("checkbox", style = KvadrantTheme.typography.subtle)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                KvadrantCheckBox(false, {}, boxSize = 60.dp)
                KvadrantCheckBox(true, {}, boxSize = 60.dp)
            }
            KvadrantText("radio", style = KvadrantTheme.typography.subtle)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                KvadrantRadioButton(false, {}, size = 60.dp)
                KvadrantRadioButton(true, {}, size = 60.dp)
            }
        }
    }
}

@ViddikScreenshot(name = "detail dark", group = "detail", width = 320, height = 430)
@Composable
internal fun DetailDark(): Unit = Detail(KvadrantColors.dark())

@ViddikScreenshot(name = "detail light", group = "detail", width = 320, height = 430)
@Composable
internal fun DetailLight(): Unit = Detail(KvadrantColors.light())
