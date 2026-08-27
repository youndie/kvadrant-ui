---
id: B-30
title: "A picker with six options reports Full mode and nothing happens"
status: open
priority: P2
size: M
stage: stage-1-core
blocked_by: []
---

# B-30 — A picker with six options reports Full mode and nothing happens

`KvadrantListPicker` computes `KvadrantListPickerMode.Full` past `FULL_MODE_THRESHOLD` and hands it
to `onExpandRequest`. Nothing in this repository does anything with it: the sample ignores the mode
entirely, so a picker with six options is a control that cannot be opened. The threshold is not a
detail either — Windows Phone's own Settings pages went to a page far more often than they unfolded
in place, so **Full is the common case and Expanded is the exception**, and we have built only the
exception.

**What the original does**, read out of `ListPicker.cs` rather than inferred:

| Fact | Where |
|---|---|
| `ItemCountThreshold` default is **5** | `DependencyProperty.Register(..., new PropertyMetadata(5, ...))`, line 443 |
| Full mode is **a real frame navigation**, not a popup or a dialog | `OpenPickerPage` walks `Application.Current.RootVisual as PhoneApplicationFrame` and navigates |
| The destination is a template property | `PickerPageUri`, defaulted in `Generic.xaml` to `/Microsoft.Phone.Controls.Toolkit;component/ListPicker/ListPickerPage.xaml` |
| Page transitions are **saved and cleared** around it, then restored | the two comments calling it a `"popup"` navigation, lines 1242 and 1277 |

That last row is the interesting one and it is the reason this is not a small item. The control
suppresses the frame's own navigation animation for the trip to the picker page and puts it back
afterwards, so the picker page arrives without the usual turnstile — it reads as a modal surface
even though it is a navigation. Reproducing that means having an opinion about navigation, and this
library has deliberately had none.

- **The library must not grow a navigator.** A component library that owns a back stack is a
  component library that fights every application's. The mode is already reported; what is missing
  is a `KvadrantListPickerPage` — the *content* of that page, styled and laid out — that a caller
  routes to with whatever it already uses.
- The rejected alternative is showing the long list in a full-screen overlay inside the control. It
  needs no navigation and it is wrong in a way that matters: the phone's back button dismissed the
  picker page, and an overlay that swallows back is the single most reliable way to make an
  application feel foreign on Android.
- **`DatePicker`/`TimePicker` are the same shape** and are also unbuilt: research §1.11 records that
  they are "real frame navigation with zeroed transitions, not a popup". Whatever is decided here
  decides those, so decide it once.
- Not covered: the transition suppression. Whether an application can or should flatten its own
  navigation animation for this is the caller's, and the most this library can do is say that the
  original did.

- AC: `KvadrantListPickerPage` exists, takes the items and the selection, and is a composable a
  caller can route to — no navigation dependency in `kvadrant-core`.
- AC: the sample routes to it, so a six-option picker in the demo opens. Today it does nothing, and
  that is how this was found.
- AC: a behaviour test taps a picker over the threshold and asserts `Full` is reported — the mode is
  currently computed and untested, which is how it stayed inert.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantListPicker.kt`
