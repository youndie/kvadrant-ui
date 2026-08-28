package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantPasswordBox
import io.github.youndie.kvadrant.components.KvadrantProgressBar
import io.github.youndie.kvadrant.components.KvadrantProgressDots
import io.github.youndie.kvadrant.components.KvadrantRadioButton
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.components.KvadrantTextBox
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

internal fun controlPreviews(): List<KvadrantPreview> =
    listOf(
        KvadrantPreview(
            id = "button",
            component = "KvadrantButton",
            summary = "the flat outlined button, and the same one disabled",
            heightDp = 200,
        ) { ButtonPreview() },
        KvadrantPreview(
            id = "text-box",
            component = "KvadrantTextBox",
            summary = "type in it: a light field in both themes, and focus does not bring in the accent",
            heightDp = 200,
        ) { TextBoxPreview() },
        KvadrantPreview(
            id = "password-box",
            component = "KvadrantPasswordBox",
            summary = "the masked field, set in a bullet the bundled font actually has",
            heightDp = 160,
        ) { PasswordBoxPreview() },
        KvadrantPreview(
            id = "toggle-switch",
            component = "KvadrantToggleSwitch",
            summary = "off and on, and one you can move",
            heightDp = 200,
        ) { TogglePreview() },
        KvadrantPreview(
            id = "check-box",
            component = "KvadrantCheckBox",
            summary = "a square, a tick, and a label that is part of the target",
            heightDp = 200,
        ) { CheckBoxPreview() },
        KvadrantPreview(
            id = "radio-button",
            component = "KvadrantRadioButton",
            summary = "one of three, exclusive",
            heightDp = 220,
        ) { RadioPreview() },
        KvadrantPreview(
            id = "slider",
            component = "KvadrantSlider",
            summary = "drag it",
            heightDp = 160,
        ) { SliderPreview() },
        KvadrantPreview(
            id = "progress-bar",
            component = "KvadrantProgressBar",
            summary = "a determinate bar over a track at a tenth of the accent",
            heightDp = 160,
        ) { ProgressBarPreview() },
        KvadrantPreview(
            id = "progress-dots",
            component = "KvadrantProgressDots",
            summary = "five dots on a 4.4 s cycle — each one switches off the moment it lands",
            heightDp = 160,
        ) { ProgressDotsPreview() },
    )

@Composable
private fun ButtonPreview() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KvadrantButton("save", {})
        KvadrantButton("disabled", {}, enabled = false)
    }
}

@Composable
private fun TextBoxPreview() {
    var name by remember { mutableStateOf("") }
    var done by remember { mutableStateOf("filled in") }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KvadrantTextBox(name, { name = it }, Modifier.fillMaxWidth(), placeholder = "your name")
        KvadrantTextBox(done, { done = it }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PasswordBoxPreview() {
    var secret by remember { mutableStateOf("hunter2") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        KvadrantPasswordBox(secret, { secret = it }, Modifier.fillMaxWidth(), placeholder = "password")
    }
}

@Composable
private fun TogglePreview() {
    var live by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KvadrantToggleSwitch(false, {})
        KvadrantToggleSwitch(true, {})
        KvadrantToggleSwitch(live, { live = it })
    }
}

@Composable
private fun CheckBoxPreview() {
    var agreed by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KvadrantCheckBox(agreed, { agreed = it }, label = "keep me signed in")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KvadrantCheckBox(false, {})
            KvadrantCheckBox(true, {})
        }
    }
}

@Composable
private fun RadioPreview() {
    var chosen by remember { mutableStateOf(0) }
    val options = listOf("every day", "every week", "never")
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            KvadrantRadioButton(chosen == index, { chosen = index }, label = label)
        }
    }
}

@Composable
private fun SliderPreview() {
    var value by remember { mutableFloatStateOf(0.4f) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KvadrantSlider(value, { value = it }, Modifier.fillMaxWidth())
        KvadrantText("${(value * 100).toInt()} %", style = KvadrantTheme.typography.subtle)
    }
}

@Composable
private fun ProgressBarPreview() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        KvadrantProgressBar(0.25f, Modifier.fillMaxWidth())
        KvadrantProgressBar(0.7f, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProgressDotsPreview() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        KvadrantProgressDots(Modifier.fillMaxWidth())
    }
}
