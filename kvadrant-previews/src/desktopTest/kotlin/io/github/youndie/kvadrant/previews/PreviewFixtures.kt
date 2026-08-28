package io.github.youndie.kvadrant.previews

import androidx.compose.runtime.Composable
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * One fixture per preview, and every one of them renders the registry rather than a copy of it.
 *
 * **This file is mechanical and it is checked rather than trusted.** viddik finds fixtures by
 * annotation, so a preview needs a function to hang one on, and forty-seven hand-written functions
 * is forty-seven chances to forget the forty-eighth. `PreviewFixtureCoverageTest` compares the ids
 * the registry declares against the fixtures KSP actually emitted, in both directions, so a missing
 * wrapper is a red build rather than a component that quietly stopped being photographed.
 *
 * The size comes from the preview, so a component that needs more room says so once, in the
 * registry, and the golden follows.
 *
 * **A preview that animates is photographed at one frame, and for one of them that frame is nearly
 * empty.** viddik renders a scene rather than driving a clock, which is deterministic — 117 goldens
 * over three recordings, nothing moved — and for most of these it lands on the settled state, which
 * is the interesting one. `progress-dots` is the exception: five dots crossing a bar over 4.4
 * seconds have no resting state, so its golden is the instant they are all still stacked at the
 * left. It guards the dots' size and colour and says nothing whatever about their motion. That is
 * `ProgressDotsDrawTest`'s job and it does it by stopping the clock. The core suite learned the
 * same thing about the turnstile and deleted the fixture; this one is kept because the coverage
 * guard is worth more than one weak image, and because a reader of the *page* sees the animation
 * running.
 */
@Composable
private fun mount(id: String) {
    KvadrantPreviewHost(requireNotNull(KvadrantPreviews.byId(id)) { "no preview registered as $id" })
}

@ViddikScreenshot(name = "tilt", group = "preview", width = 360, height = 260)
@Composable
internal fun PreviewTilt(): Unit = mount("tilt")

@ViddikScreenshot(name = "type-ramp", group = "preview", width = 360, height = 360)
@Composable
internal fun PreviewTypeRamp(): Unit = mount("type-ramp")

@ViddikScreenshot(name = "text", group = "preview", width = 360, height = 140)
@Composable
internal fun PreviewText(): Unit = mount("text")

@ViddikScreenshot(name = "accents", group = "preview", width = 360, height = 420)
@Composable
internal fun PreviewAccents(): Unit = mount("accents")

@ViddikScreenshot(name = "icons", group = "preview", width = 360, height = 400)
@Composable
internal fun PreviewIcons(): Unit = mount("icons")

@ViddikScreenshot(name = "surface", group = "preview", width = 360, height = 180)
@Composable
internal fun PreviewSurface(): Unit = mount("surface")

@ViddikScreenshot(name = "button", group = "preview", width = 360, height = 200)
@Composable
internal fun PreviewButton(): Unit = mount("button")

@ViddikScreenshot(name = "text-box", group = "preview", width = 360, height = 200)
@Composable
internal fun PreviewTextBox(): Unit = mount("text-box")

@ViddikScreenshot(name = "password-box", group = "preview", width = 360, height = 160)
@Composable
internal fun PreviewPasswordBox(): Unit = mount("password-box")

@ViddikScreenshot(name = "toggle-switch", group = "preview", width = 360, height = 200)
@Composable
internal fun PreviewToggleSwitch(): Unit = mount("toggle-switch")

@ViddikScreenshot(name = "check-box", group = "preview", width = 360, height = 200)
@Composable
internal fun PreviewCheckBox(): Unit = mount("check-box")

@ViddikScreenshot(name = "radio-button", group = "preview", width = 360, height = 220)
@Composable
internal fun PreviewRadioButton(): Unit = mount("radio-button")

@ViddikScreenshot(name = "slider", group = "preview", width = 360, height = 160)
@Composable
internal fun PreviewSlider(): Unit = mount("slider")

@ViddikScreenshot(name = "progress-bar", group = "preview", width = 360, height = 160)
@Composable
internal fun PreviewProgressBar(): Unit = mount("progress-bar")

@ViddikScreenshot(name = "progress-dots", group = "preview", width = 360, height = 160)
@Composable
internal fun PreviewProgressDots(): Unit = mount("progress-dots")

@ViddikScreenshot(name = "list-item", group = "preview", width = 360, height = 220)
@Composable
internal fun PreviewListItem(): Unit = mount("list-item")

@ViddikScreenshot(name = "long-list", group = "preview", width = 360, height = 380)
@Composable
internal fun PreviewLongList(): Unit = mount("long-list")

@ViddikScreenshot(name = "group-header", group = "preview", width = 360, height = 140)
@Composable
internal fun PreviewGroupHeader(): Unit = mount("group-header")

@ViddikScreenshot(name = "jump-list", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewJumpList(): Unit = mount("jump-list")

@ViddikScreenshot(name = "list-picker", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewListPicker(): Unit = mount("list-picker")

@ViddikScreenshot(name = "list-picker-page", group = "preview", width = 360, height = 420)
@Composable
internal fun PreviewListPickerPage(): Unit = mount("list-picker-page")

@ViddikScreenshot(name = "looping-selector", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewLoopingSelector(): Unit = mount("looping-selector")

@ViddikScreenshot(name = "picker-page", group = "preview", width = 360, height = 360)
@Composable
internal fun PreviewPickerPage(): Unit = mount("picker-page")

@ViddikScreenshot(name = "message-box", group = "preview", width = 360, height = 320)
@Composable
internal fun PreviewMessageBox(): Unit = mount("message-box")

@ViddikScreenshot(name = "toast", group = "preview", width = 360, height = 260)
@Composable
internal fun PreviewToast(): Unit = mount("toast")

@ViddikScreenshot(name = "context-menu", group = "preview", width = 360, height = 320)
@Composable
internal fun PreviewContextMenu(): Unit = mount("context-menu")

@ViddikScreenshot(name = "tile", group = "preview", width = 360, height = 340)
@Composable
internal fun PreviewTile(): Unit = mount("tile")

@ViddikScreenshot(name = "tile-row", group = "preview", width = 360, height = 200)
@Composable
internal fun PreviewTileRow(): Unit = mount("tile-row")

@ViddikScreenshot(name = "tile-grid", group = "preview", width = 360, height = 420)
@Composable
internal fun PreviewTileGrid(): Unit = mount("tile-grid")

@ViddikScreenshot(name = "tile-badge", group = "preview", width = 360, height = 240)
@Composable
internal fun PreviewTileBadge(): Unit = mount("tile-badge")

@ViddikScreenshot(name = "flip-tile", group = "preview", width = 360, height = 260)
@Composable
internal fun PreviewFlipTile(): Unit = mount("flip-tile")

@ViddikScreenshot(name = "iconic-tile", group = "preview", width = 360, height = 260)
@Composable
internal fun PreviewIconicTile(): Unit = mount("iconic-tile")

@ViddikScreenshot(name = "cycle-tile", group = "preview", width = 360, height = 260)
@Composable
internal fun PreviewCycleTile(): Unit = mount("cycle-tile")

@ViddikScreenshot(name = "page", group = "preview", width = 360, height = 420)
@Composable
internal fun PreviewPage(): Unit = mount("page")

@ViddikScreenshot(name = "page-header", group = "preview", width = 360, height = 200)
@Composable
internal fun PreviewPageHeader(): Unit = mount("page-header")

@ViddikScreenshot(name = "pivot", group = "preview", width = 360, height = 420)
@Composable
internal fun PreviewPivot(): Unit = mount("pivot")

@ViddikScreenshot(name = "pivot-headers", group = "preview", width = 360, height = 220)
@Composable
internal fun PreviewPivotHeaders(): Unit = mount("pivot-headers")

@ViddikScreenshot(name = "panorama", group = "preview", width = 360, height = 460)
@Composable
internal fun PreviewPanorama(): Unit = mount("panorama")

@ViddikScreenshot(name = "app-bar", group = "preview", width = 360, height = 320)
@Composable
internal fun PreviewAppBar(): Unit = mount("app-bar")

@ViddikScreenshot(name = "app-bar-button", group = "preview", width = 360, height = 180)
@Composable
internal fun PreviewAppBarButton(): Unit = mount("app-bar-button")

@ViddikScreenshot(name = "turnstile", group = "preview", width = 360, height = 320)
@Composable
internal fun PreviewTurnstile(): Unit = mount("turnstile")

@ViddikScreenshot(name = "turnstile-feather", group = "preview", width = 360, height = 360)
@Composable
internal fun PreviewTurnstileFeather(): Unit = mount("turnstile-feather")

@ViddikScreenshot(name = "swivel", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewSwivel(): Unit = mount("swivel")

@ViddikScreenshot(name = "slide", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewSlide(): Unit = mount("slide")

@ViddikScreenshot(name = "roll", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewRoll(): Unit = mount("roll")

@ViddikScreenshot(name = "rotate", group = "preview", width = 360, height = 300)
@Composable
internal fun PreviewRotate(): Unit = mount("rotate")

@ViddikScreenshot(name = "scrim", group = "preview", width = 360, height = 240)
@Composable
internal fun PreviewScrim(): Unit = mount("scrim")
