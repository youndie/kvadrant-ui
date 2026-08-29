---
id: B-39
title: "A screen reader gets a clickable box where a toggle switch is"
status: open
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

## What this is not

Not a request to make the components *look* different. Metro's visual fidelity is not in tension with
any of this — `Modifier.semantics` changes nothing on screen. The only judgement call is
`stateDescription` wording, and that is a string a consumer can override.

## Acceptance

- AC: every control with a state announces it — `Role.Switch` and on/off for the toggle,
  `Role.Checkbox` and checked for the check box, `Role.RadioButton` for the radio, a value for the
  slider and the progress bar.
- AC: `KvadrantAppBarButton` takes a label, because a circle with a glyph in it is unreadable to
  anything that is not looking at it.
- AC: a test walks the semantics tree of every preview in `kvadrant-previews` and fails on an
  interactive node with no role — the registry already enumerates them, so this is a loop rather
  than a list to maintain.
- AC: B-11's title is corrected to what it covered, or this item is folded into it and the title is
  earned.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/`
