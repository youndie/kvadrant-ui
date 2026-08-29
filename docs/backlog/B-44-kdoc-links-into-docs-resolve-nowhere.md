---
id: B-44
title: "Every KDoc link into docs/ is a 404 on the site, and four of them disagree about the depth"
status: done
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

**Counted properly it is worse than that.** Sixteen links, in fifteen files, at four depths counting
the tests, and **not one of them resolved**: the right count from a source directory is nine, and
nothing used nine. A guess of "four" came from grepping the shipped sources only.

## What was decided

**The canonical URL**, and the second option turns out not to be an option. Copying `docs/` into
`build/site` would let a relative link work on the site — and the same KDoc is *also* published as
the body of its Dokka entry, which is nested several directories under `api/`. Two outputs at
different depths from one string: there is no count of `../` that satisfies both, so correcting the
depth repairs the source tree and leaves both of the places a reader actually meets the text broken.

`https://github.com/youndie/kvadrant-ui/blob/main/…` works in the source tree, in an IDE, on the
site and in Dokka. It names a branch, which is the price; `scripts/doc_links.py` buys it back by
checking the URL's **path** against the working copy, with no network involved. A link written any
other way — another branch, a `tree/` URL, a commit permalink — is reported rather than skipped,
because a rule one spelling escapes is not a rule.

Either way the interesting half is the check, and **half of it already exists**:
`scripts/backlog_index.py` refuses a backlog item whose relative link resolves to nothing, and it
caught a wrapped link in [B-40](B-40-keyboard-and-focus-on-desktop-and-wasm.md) the first time it was
run against it. Nothing does the same for KDoc, for `docs/research/` or for `README.md` —
`scripts/doc_images.py` covers images from those files and stops at images.

## Acceptance

- ~~AC: a script fails on a link that resolves to nothing, and it is inside `make check`.~~ Done —
  `scripts/doc_links.py`, covering `README.md`, `CLAUDE.md`, `backlog.md`, everything under `docs/`
  and every `.kt` in every module: 330 links. `docs/templates/` is excluded, because a template's
  links are relative to a tree that does not exist here, and the count of documents skipped is
  printed so an exclusion that quietly grew would show.
- ~~AC: verified by breaking a link on purpose.~~ Done, four ways — a markdown link to a missing
  file, a KDoc link written relatively, a canonical URL whose document had been renamed, and a
  target that wrapped onto the next line. Each fails; the KDoc one names the URL to write instead,
  because an error that only says "broken" invites somebody to fix the dots and reintroduce the
  defect in a form the checker accepts.
- **The fourth was not planned, and the first version of the script did not catch it.** With every
  link rewritten and the checker green, the built site still carried one relative `docs/` href — a
  target [B-40](B-40-keyboard-and-focus-on-desktop-and-wasm.md) had wrapped across two comment
  lines the day before. The pattern demanded a target with no whitespace in it, so the one link
  written in a shape the author had not thought of was not merely unresolved but **invisible**, and
  what found it was building the site and grepping the output rather than trusting the green run.
  `backlog_index.py` had already tripped over the same wrap in the same item, which should have
  been the hint.
- ~~AC: the existing links are fixed.~~ Done — sixteen, in fifteen files. The paragraphs around them
  were re-wrapped at the width the rest of the file uses; a line holding nothing but the URL is over
  a hundred and twenty characters and cannot be shortened, and ktlint does not object to it.
- Anchors: `scripts/doc_links.py`, `scripts/backlog_index.py`, `scripts/doc_images.py`
