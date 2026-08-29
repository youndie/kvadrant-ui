---
id: B-42
title: "The picker's parts are built and the picker is not"
status: done
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

## Two decisions it needed, and both are about not reaching for a dependency

**`kotlinx-datetime` was the obvious value type and is not used.** Its `LocalDate` is exactly what a
date picker returns and it is the Kotlin multiplatform standard — but putting it on
`kvadrant-core`'s API puts it on every consumer of a Metro *button*, and a component library is not
the right place to decide which date library an application uses. `KvadrantDate(year, month, day)`
and `KvadrantTime(hour, minute)` convert to whatever the caller already has, in one line, at the one
call site that opened a picker. The calendar arithmetic that buys is `daysInMonth`: five lines of
the Gregorian rule, century included, because a picker offering 29 February 2100 is wrong in a way
nobody notices for seventy years.

**The column order is a table and says so.** The original read
`CultureInfo.DateTimeFormat.ShortDatePattern`, which is the operating system's answer for every
culture it knows. Compose gives a `Locale` with a language, a script and a region and nothing that
formats a date. A per-platform `expect`/`actual` could reach a real one — `java.text.DateFormat`,
`Intl.DateTimeFormat().formatToParts()` — and that is the faithful version; it is three source sets
of pattern parsing for a column order, and a wrong actual is worse than a table that admits to being
one. So the two orders that are not the world's default are listed and everything else is
day-month-year, which is a claim about coverage rather than about correctness, and the parameter is
the way out.

The hour cycle is a *parameter* rather than a table, and the asymmetry is deliberate: the date order
has three discrete answers with a published source for each, while whether a locale writes half past
seven as 19:30 or 7:30 PM is a per-user setting rather than a property of the language. Tabulating
that would be inventing an answer instead of recording one.

## Three things the tests caught that the pictures could not

- **The day has to stay inside the month it moves to.** 31 January to February is the 28th, or the
  29th in a leap year, and leaving a leap day for a common year clamps the same way.
- **A twelve-hour column is not a modulo.** Midnight and noon both draw as "12", so the hour the
  column shows is `hour % 12` and the half of the day is carried beside it; tapping "12" from half
  past one in the afternoon has to mean noon and from half past one in the morning has to mean
  midnight.
- **A column that fits needs no window.** The meridiem column has two values in a window of two, and
  the wrap put them in `[PM, AM]` when PM was selected — AM and PM changing places depending on
  which was chosen, from a mechanism that exists to join ends a two-cell column does not have.

**And one the picture did catch.** The first recording of the preview came back month-day-year,
because the default reads `Locale.current` and the machine recording it is American. A golden of a
component whose default reads the environment is a picture of the environment — the same class of
defect as a golden recording its rasteriser ([B-35](B-35-cyrillic-renders-differently-on-linux.md)). The
preview pins the order and says why; what the order does with a locale is the test's claim.

## Acceptance

- ~~AC: `KvadrantDatePicker` and `KvadrantTimePicker`, each a picker page carrying looping selectors,
  returning a value rather than an index.~~ Done, and the columns **wrap**, which is what makes the
  selector a looping one: the cell above the first of the month is the last of it.
- ~~AC: the column order follows the locale rather than being fixed to day-month-year.~~ Done —
  `KvadrantDateOrder.forLocale`, asserted for `en-US`, `en-GB`, `ru-RU`, `ja-JP` and an unlisted
  locale, and asserted **off the screen** rather than off the enum, because a mapping that returns
  the right list and a layout that ignores it look the same from inside the component. See the
  section above for what the table is and is not.
- ~~AC: a preview each.~~ Done, `date-picker` and `time-picker`.
- ~~AC: the `LoopingSelector` KDoc stops describing itself hypothetically.~~ Done. It had said "one
  column of a date or time picker" about a thing that did not exist, and now names both and says
  when to use it directly instead.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantLoopingSelector.kt`
