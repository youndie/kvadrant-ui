---
id: B-42
title: "The picker's parts are built and the picker is not"
status: open
priority: P2
size: S
stage: stage-3-completeness
blocked_by: []
---

# B-42 — The picker's parts are built and the picker is not

`KvadrantLoopingSelector` is a column of a date or time picker, built, tested and previewed.
`KvadrantPickerPage` is the surface such a picker arrives on, tipping in from -50°, built, tested and
previewed. What does not exist is the thing they are halves of: a component that puts three looping
selectors on a picker page, in the order the locale asks for, and hands back a date.

The brief lists it as C11 at P2 and sizes it **L**, on the assumption that the looping selector was
the work. The looping selector is done. What is left is assembly and a locale-ordered column list,
which is why this is S.

**It is the component a consumer asks for first**, because a form needs a date, and it is the last
control from the brief's catalogue whose metrics are all published — 148 × 148 per cell, the value at
54 px, both already transcribed into the looping selector.

## Acceptance

- AC: `KvadrantDatePicker` and `KvadrantTimePicker`, each a picker page carrying looping selectors,
  returning a value rather than an index.
- AC: the column order follows the locale rather than being fixed to day-month-year, because the
  original took it from the phone's culture.
- AC: a preview each, so the site shows them like everything else.
- AC: the `LoopingSelector` KDoc stops describing itself as "one column of a date or time picker"
  hypothetically and points at the component.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantLoopingSelector.kt`
