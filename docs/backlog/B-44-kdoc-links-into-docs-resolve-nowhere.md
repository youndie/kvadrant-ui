---
id: B-44
title: "Every KDoc link into docs/ is a 404 on the site, and three of them disagree about the depth"
status: open
priority: P2
size: S
stage: stage-3-completeness
blocked_by: []
---

# B-44 — Every KDoc link into `docs/` is a 404 on the site, and three of them disagree about the depth

A component's KDoc is the prose on its page of the documentation site — that is the arrangement
`CLAUDE.md` calls out as the reason to get KDoc right, and it is why several of them link back to the
backlog item that explains a number. Those links do not work.

The site's pages are **flat, all at the root of `build/site`**, and `docs/` is not copied into the
site at all. So a relative path out of a page reaches nothing whatever its depth, and the depth is
written for the source tree rather than for the page:

```html
<!-- build/site/kvadrantlistpicker.html, generated from KvadrantListPicker.kt -->
<a href="../../../../../../../docs/backlog/B-30-list-picker-full-mode.md">B-30</a>
```

**The proof that nobody checks it is in the sources rather than in the output.** Four shipped files
carry such a link and they use three different depths — three, seven and eight `../` — which cannot
all be right, and no two of them were written by someone who had followed one.

## What to decide

- Point them at `https://github.com/youndie/kvadrant-ui/blob/main/docs/...` — works from the site,
  from Dokka, and from a checkout, at the price of an absolute URL in the source.
- Or copy `docs/` into `build/site` and rewrite the links to it, which keeps the source relative and
  makes the site self-contained.

Either way the interesting half is the check, and **half of it already exists**:
`scripts/backlog_index.py` refuses a backlog item whose relative link resolves to nothing, and it
caught a wrapped link in [B-40](B-40-keyboard-and-focus-on-desktop-and-wasm.md) the first time it was
run against it. Nothing does the same for KDoc, for `docs/research/` or for `README.md` —
`scripts/doc_images.py` covers images from those files and stops at images.

## Acceptance

- AC: a script fails on a link from KDoc, from `docs/research/` or from `README.md` that resolves to
  nothing, and it is inside `make check` — a checker outside the gate is a checker that runs on one
  machine. The backlog's own check is the model; the point is that it stops at the backlog.
- AC: it is verified by breaking a link on purpose and watching it fail, not by a clean run.
- AC: the four existing links are fixed, and so is the one B-40 added on top of them: it was written
  knowing this, in the depth its neighbours use, so that they can be swept together rather than
  leaving one file spelled differently from the rest of its directory.
- Anchors: `scripts/backlog_index.py`, `scripts/doc_images.py`, `scripts/build_site.py`,
  `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantListPicker.kt`
