package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantContextMenuHost
import io.github.youndie.kvadrant.components.KvadrantGroupHeader
import io.github.youndie.kvadrant.components.KvadrantJumpList
import io.github.youndie.kvadrant.components.KvadrantListItem
import io.github.youndie.kvadrant.components.KvadrantListPicker
import io.github.youndie.kvadrant.components.KvadrantListPickerPage
import io.github.youndie.kvadrant.components.KvadrantLongList
import io.github.youndie.kvadrant.components.KvadrantLoopingSelector
import io.github.youndie.kvadrant.components.KvadrantMessageBox
import io.github.youndie.kvadrant.components.KvadrantPickerPage
import io.github.youndie.kvadrant.components.KvadrantToast

private val CITIES = listOf("Amsterdam", "Berlin", "Copenhagen", "Dublin", "Edinburgh", "Frankfurt")

internal fun listPreviews(): List<KvadrantPreview> =
    listOf(
        KvadrantPreview(
            id = "list-item",
            component = "KvadrantListItem",
            summary = "a row with a subtitle, tilting under a press",
            heightDp = 220,
        ) { ListItemPreview() },
        KvadrantPreview(
            id = "long-list",
            component = "KvadrantLongList",
            summary = "grouped rows with the letter headers a jump list lands on",
            heightDp = 380,
        ) { LongListPreview() },
        KvadrantPreview(
            id = "group-header",
            component = "KvadrantGroupHeader",
            summary = "the accented letter square that opens the jump list",
            heightDp = 140,
        ) { GroupHeaderPreview() },
        KvadrantPreview(
            id = "jump-list",
            component = "KvadrantJumpList",
            summary = "the letter grid, with the empty letters dimmed and dead",
            heightDp = 300,
        ) { JumpListPreview() },
        KvadrantPreview(
            id = "list-picker",
            component = "KvadrantListPicker",
            summary = "tap to expand in place; six or more items go to a page instead",
            heightDp = 300,
        ) { ListPickerPreview() },
        KvadrantPreview(
            id = "list-picker-page",
            component = "KvadrantListPickerPage",
            summary = "the full-mode page a long picker opens",
            heightDp = 420,
        ) { ListPickerPagePreview() },
        KvadrantPreview(
            id = "looping-selector",
            component = "KvadrantLoopingSelector",
            summary = "a wheel with no ends — scroll past the last value and the first comes round",
            heightDp = 300,
        ) { LoopingSelectorPreview() },
        KvadrantPreview(
            id = "picker-page",
            component = "KvadrantPickerPage",
            summary = "the surface a looping selector arrives on, tipping in from -50°",
            heightDp = 360,
        ) { PickerPagePreview() },
        KvadrantPreview(
            id = "message-box",
            component = "KvadrantMessageBox",
            summary = "the modal, swivelling in over a dimmed page",
            heightDp = 320,
        ) { MessageBoxPreview() },
        KvadrantPreview(
            id = "toast",
            component = "KvadrantToast",
            summary = "the notification that slides down from the top of the screen",
            heightDp = 260,
        ) { ToastPreview() },
        KvadrantPreview(
            id = "context-menu",
            component = "KvadrantContextMenuHost",
            summary = "press and hold the row",
            heightDp = 320,
        ) { ContextMenuPreview() },
    )

@Composable
private fun ListItemPreview() {
    Column(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        KvadrantListItem("Anna Peterson", subtitle = "meeting on Thursday", onClick = {})
        KvadrantListItem("build server", subtitle = "nightly is green", onClick = {})
        KvadrantListItem("no subtitle", onClick = {})
    }
}

@Composable
private fun LongListPreview() {
    val groups =
        listOf(
            "A" to listOf("Amsterdam", "Antwerp"),
            "B" to listOf("Berlin", "Bruges"),
            "C" to listOf("Copenhagen"),
        )
    KvadrantLongList(groups, Modifier.fillMaxSize()) { city ->
        KvadrantListItem(city, onClick = {})
    }
}

@Composable
private fun GroupHeaderPreview() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        KvadrantGroupHeader("A", onClick = {})
    }
}

@Composable
private fun JumpListPreview() {
    val letters = ('a'..'x').map { it.toString() }
    KvadrantJumpList(letters, enabled = setOf("a", "b", "c", "f"), modifier = Modifier.fillMaxSize())
}

@Composable
private fun ListPickerPreview() {
    var selected by remember { mutableIntStateOf(1) }
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        KvadrantListPicker(
            items = CITIES.take(3),
            selectedIndex = selected,
            onSelect = {
                selected = it
                expanded = false
            },
            expanded = expanded,
            onExpandRequest = { expanded = true },
            header = "city",
        )
    }
}

@Composable
private fun ListPickerPagePreview() {
    var selected by remember { mutableIntStateOf(2) }
    KvadrantListPickerPage(
        items = CITIES,
        selectedIndex = selected,
        onSelect = { selected = it },
        modifier = Modifier.fillMaxSize(),
        header = "city",
        applicationTitle = "SETTINGS",
    )
}

@Composable
private fun LoopingSelectorPreview() {
    var hour by remember { mutableIntStateOf(9) }
    val hours = (1..12).map { it.toString() }
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        KvadrantLoopingSelector(hours, hour - 1, { hour = it + 1 }, label = "hour")
    }
}

@Composable
private fun PickerPagePreview() {
    var minute by remember { mutableIntStateOf(30) }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }
    KvadrantPickerPage(visible = true, modifier = Modifier.fillMaxSize()) {
        KvadrantLoopingSelector(minutes, minute, { minute = it }, label = "minute")
    }
}

@Composable
private fun MessageBoxPreview() {
    var open by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        KvadrantListItem("tap to bring it back", onClick = { open = true })
        KvadrantMessageBox(
            visible = open,
            title = "delete this message?",
            message = "it will be removed from the phone and from the server.",
            onConfirm = { open = false },
            onCancel = { open = false },
        )
    }
}

@Composable
private fun ToastPreview() {
    var open by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        KvadrantListItem("tap to bring it back", onClick = { open = true })
        KvadrantToast(
            visible = open,
            title = "Anna Peterson",
            message = "are you coming on Thursday?",
            onDismiss = { open = false },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ContextMenuPreview() {
    var open by remember { mutableStateOf(false) }
    KvadrantContextMenuHost(
        expanded = open,
        items = listOf("pin to start", "delete"),
        onDismiss = { open = false },
        onItemClick = { open = false },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            KvadrantListItem("press and hold me", subtitle = "then let go", onClick = { open = true })
        }
    }
}
