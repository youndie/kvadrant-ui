# docs — Kvadrant UI

Kvadrant UI is a Metro (Windows Phone 8 / Windows 8) component library for Compose Multiplatform,
with an optional adapter that lets it coexist with `androidx.compose.material3`. The documentation
is layered; links run top to bottom.

```
[ Research (why the architecture is what it is) ]
                     │
[ Feature (behaviour + BDD) ]  ── not yet: there is no code to describe
                     │
[ Service / module (ownership, build, quirks) ]  ── not yet, same reason
```

| Layer | Directory | Answers | Source of truth |
|---|---|---|---|
| Research | `research/` | *why* it is built this way; what is verified, what is a hypothesis | the artefacts each fact names |
| Backlog | `backlog/` | what to do next, in what order, and why that order | this repository |

**Why only two layers today.** `main` describes what exists, and what exists is research, a plan and
a build skeleton with no components in it. Feature documents, module documents and BDD scenarios describe behaviour, and writing them
before there is behaviour would mean documenting intent as fact — the one thing this format refuses
to do. They arrive with the code, in the branch that adds it. There will be no `api/` layer (this is
a library, not a service) and no `screens/` layer (the sample gallery is a demo, not a product
surface).

**Backlog** — [backlog.md](../backlog.md): the index and the decisions; the items themselves are one
file each in [`backlog/`](backlog/), cited as `[B-12](backlog/B-12-pivot.md)`.

## Conventions

- **`id`** in the frontmatter is unique and equals the filename.
- Cross-layer links are ids in the frontmatter and ordinary markdown links in the body.
- **Language: English**, including code identifiers and KDoc. The primary research brief this tree
  is derived from is in Russian and lives outside the repository — see
  [research §0](research/research-architecture.md).
- **`Kvadrant` in every identifier; `Metro` only in prose**, where it names the design language.
  See D4 in the research document for why.
- **The primary consumer is a coding agent.** Every document carries anchors — paths to what it is
  about. While there is no code, research anchors point at the artefacts each fact was verified
  against, which is why `code_anchors.py` reports them as absent: they are outside this repository
  by design.
- **A number that is not Microsoft's says so.** Where the specification has a gap, this project's
  value ships as a parameter of the public API with a KDoc sentence naming it as ours.

## Templates

`templates/` holds a copy of the document templates, so the format travels with the repository.
Sections marked `<!-- optional -->` can be deleted.

## Checks

```bash
pip install pyyaml
make check
```

## Coverage map

The list below is **checked** against the files on disk: a document missing here, or an entry with
no file behind it, fails `coverage_map.py`. The grouping and the descriptions are written by a
person — the machine only guards the membership.

### Research (1)

- [x] [research-architecture](research/research-architecture.md) — what was verified and against
  what, the twelve decisions and what each rejected, the six risks and their machinery
