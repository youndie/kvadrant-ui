---
id: B-39
title: "A screen reader gets a clickable box where a toggle switch is"
status: done
priority: P1
size: M
stage: stage-2-release
blocked_by: []
---

# B-39 — A screen reader gets a clickable box where a toggle switch is

The whole library contains **six** mentions of `semantics`, `contentDescription` or `Role`, and one
of them is `Modifier.kvadrantTilt` merging descendants so that a click is reported at all. So
`KvadrantToggleSwitch` announces as a clickable box: not as a switch, and not as on or off.
`KvadrantCheckBox` and `KvadrantRadioButton` are the same. `KvadrantAppBarButton` carries whatever
glyph the caller passed and no label.

**[B-11](B-11-accessibility-policy.md) is titled "accessibility policy" and did not cover this**, and
that is the part worth stating. Its criteria are a 48 dp touch area, an opt-in contrast palette and a
contrast test — all real, all done. A title that promises more than the criteria deliver is how a gap
stops being visible: the item reads as closed, the subject reads as handled.

## Done

The four controls that had a state now carry it, and none of it changed a pixel — `viddikVerify`
passes on the existing goldens, which is the check that this was a semantics change and not a visual
one.

- The toggle switch and the check box are `Modifier.toggleable` rather than `clickable`, so the role
  and the on/off state reach the tree together. The interaction source and indication are the same
  objects, so the tilt is untouched.
- The radio button is `Modifier.selectable`, so a reader is told it is one of a set and which one is
  chosen.
- The slider and the determinate bar report a `ProgressBarRangeInfo` with their value.
- The dots report `Indeterminate` rather than a false zero — five dots crossing a bar carry no
  position, and reporting `0f` would have a reader announcing "nought per cent" forever about
  something that is not measuring anything.
- `KvadrantAppBarButton` takes a `label`. The original had no such problem: an application bar button
  carried a `Text` alongside its icon, shown when the bar opened, and that text was its name.

## Two tests, and they are not the same test

`ControlsAnnounceTheirStateTest` is the specific claim — these six say the right thing, and keep
saying it when their value changes. Its control is an empty frame, which must announce nothing.

`InteractiveNodesAreNamedTest` is the general rule, and it is the one with a future: it walks the
semantics tree of **every preview in the registry** and fails on anything with an `OnClick` that
cannot be named — by a role, a label, its own text, or an editable value. A new component is covered
by existing to be previewed at all, which is the only way this stays true after the seventh control
arrives.

It found seven anonymous press targets on its first run, and **four of them were mine**: the app bar
previews had not been given labels, which is exactly the failure the new parameter exists to prevent
and a fair demonstration that a parameter nobody passes is not a fix. The other three were the text
fields, named by their editable content once the guard learned to look for it.

It deliberately does not demand a `Role` specifically. A list row named "Anna Peterson" needs none to
be useful, and requiring one would be a rule written for the checker rather than for the person it
serves.

## What this is not

Not a request to make the components *look* different. Metro's visual fidelity is not in tension with
any of this — `Modifier.semantics` changes nothing on screen. The only judgement call is
`stateDescription` wording, and that is a string a consumer can override.

## Acceptance

- ~~AC: every control with a state announces it.~~ Done — `ControlsAnnounceTheirStateTest`.
- ~~AC: `KvadrantAppBarButton` takes a label.~~ Done, and the ABI dump moved for it.
- ~~AC: a test walks the semantics tree of every preview.~~ Done —
  `InteractiveNodesAreNamedTest`. It asks for a *name* rather than a role specifically; the reasoning
  is above.
- ~~AC: B-11's title is corrected.~~ Its `title:` field was already honest — *"Authentic visuals with
  extended hit areas, and an opt-in contrast palette"*. What overstated it was the **filename**,
  `B-11-accessibility-policy.md`, which is what a reader sees in a link and in the directory listing.
  Renaming it would break every citation, so B-11 now carries a line saying what it did not cover and
  pointing here. **An id is a name, and this one was making a claim.**
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/`
