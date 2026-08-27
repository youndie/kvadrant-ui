---
id: B-30
title: "A picker with six options reports Full mode and nothing happens"
status: done
priority: P2
size: M
stage: stage-1-core
blocked_by: []
---

# B-30 — A picker with six options reports Full mode and nothing happens

**Done.** `KvadrantListPickerPage` is the *content* of the page a long picker opens; the sample routes
to it and dismisses it with the system back gesture, which is what makes it a destination rather than
an overlay. `kvadrant-core` gained no navigation dependency, and it must not: a component library
with a back stack is one that fights every application's.

`ListPickerFullModeTest` taps a picker at the threshold and one over it and asserts which mode each
reports. That test is the item's real lesson: the mode was computed, handed to the caller and dropped
by every caller here, **and it was untested** — a value nothing reads and nothing checks is
indistinguishable from a value that is never produced, which is exactly how it stayed inert for as
long as it did.

**One number on that page is this project's and says so.** The toolkit's `ListPickerPage.xaml` is a
separate file from the `Generic.xaml` this repository has, so the type size of a row — the one thing
that decides how the page looks — could not be read. It ships as `itemStyle`, a parameter, and is
the first thing to fix if that file turns up. Everything else is transcribed: the navigation shape,
the selection commit and the threshold from `ListPicker.cs`, the page chrome from `KvadrantPage`.

The transition suppression is still not reproduced and still should not be: the original clears the
frame's navigation transitions for the trip and restores them afterwards, and whether an application
flattens its own animation is the application's business. The sample does the equivalent by drawing
the page with no transition at all, and says so where it does it.

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
