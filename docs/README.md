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
| Reference | [`components.md`](components.md) | what the library exposes, and which preview shows it | generated from the sources and the preview registry |

**Why there are still no feature documents.** `main` describes what exists, and what exists is
research, a plan, forty-eight components and a catalogue of them. A feature document describes
behaviour a user gets, and a component library's behaviour is its components — which is what
[`components.md`](components.md) and the [documentation site](https://youndie.github.io/kvadrant-ui/)
cover, one composable at a time, with the transcription notes that say where each number came from.
BDD scenarios wait for a behaviour that spans components rather than sits inside one. There will be
no `api/` layer (this is a library, not a service) and no `screens/` layer (the sample gallery is a
demo, not a product surface).

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

### Reference (1)

- [x] [components](components.md) — every public composable, its preview and the file it lives in.
  The table in it is generated; `make check` fails when it no longer matches the sources.

**Not in the map above, and it cannot be.** `coverage_map.py` walks the layer directories, and this
document is not in one. It is listed here so a reader finds it; nothing guards the fact that it is
listed. What *is* guarded is the document's contents —
[`scripts/component_catalog.py`](../scripts/component_catalog.py), run by both gates.
