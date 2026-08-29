# docs — Kvadrant UI

Kvadrant UI is a Metro (Windows Phone 8 / Windows 8) component library for Compose Multiplatform,
with an optional adapter that lets it coexist with `androidx.compose.material3`. The documentation
is layered; links run top to bottom.

```
[ Research (why the architecture is what it is) ]
                     │
[ Backlog (what to do next, and why in that order) ]
                     │
[ Reference — components.md (what exists, one composable at a time) ]
                     │
[ Feature (behaviour + BDD) ]  ── not yet, and see below for why
```

| Layer | Directory | Answers | Source of truth |
|---|---|---|---|
| Research | `research/` | *why* it is built this way; what is verified, what is a hypothesis | the artefacts each fact names |
| Backlog | `backlog/` | what to do next, in what order, and why that order | this repository |
| Reference | [`components.md`](components.md) | what the library exposes, and which preview shows it | generated from the sources and the preview registry |

**Why there are still no feature documents, now that there is code.** The earlier answer here was
"there is no behaviour to describe", and that stopped being true somewhere around the fortieth
component. The answer now is a different one: a feature document describes behaviour a user gets,
and a component library's behaviour *is* its components. That is covered one composable at a time by
[`components.md`](components.md) and by the [documentation site](https://youndie.github.io/kvadrant-ui/),
each carrying the transcription notes that say where its numbers came from — which is a truer
document than a feature file restating them a second time. BDD scenarios wait for behaviour that
spans components rather than sitting inside one; nothing here does yet.

**And why no module layer either.** There are five modules and their ownership is not in doubt:
`kvadrant-core` is the library, `kvadrant-material-adapter` is the optional interop,
`kvadrant-previews` is what the documentation site mounts, and `sample`/`sample-android` are the
demo. What a `services/` document would carry — build quirks, the things that are easy to get wrong
— is in [`../CLAUDE.md`](../CLAUDE.md), because that is the file anybody starting work here opens
first. A second copy in `services/` would be a second copy.

There will be no `api/` layer (this is a library, not a service) and no `screens/` layer (the sample
gallery is a demo, not a product surface).

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
  about. The research anchors are the exception and stay one: they point at the artefacts each fact
  was verified against — a template dictionary from the Windows Phone SDK, a class inside a decompiled
  assembly, a published maven-metadata — and those live outside this repository. `code_anchors.py`
  reports eleven of them as absent for that reason, and the report is non-blocking on purpose.
- **A number that is not Microsoft's says so.** Where the specification has a gap, this project's
  value ships as a parameter of the public API with a KDoc sentence naming it as ours.

## Templates

`templates/` holds a copy of the document templates, so the format travels with the repository.
Sections marked `<!-- optional -->` can be deleted.

## Checks

Two gates, and neither is a superset of the other.

```bash
pip install pyyaml
make check                                       # the documentation tree and the catalogue
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew check                                  # tests, ktlint, the goldens, the ABI dump
```

Both run on CI, on every push and every pull request. **They do not agree yet:** the screenshot
suite is red on the Linux runner over the Cyrillic companion — deterministically, not as a flake,
and [B-35](backlog/B-35-cyrillic-renders-differently-on-linux.md) is open on it. A green run on a
mac is a claim about a mac.

The two gates also make deliberately different claims about [`components.md`](components.md).
`make check` builds nothing, so it verifies the catalogue against the sources and prints that the
preview column was carried over unverified; `./gradlew :kvadrant-previews:check` builds the registry
first and verifies both. The smaller claim is not allowed to print like the larger one.

## Coverage map

The list below is **checked** against the files on disk: a document missing here, or an entry with
no file behind it, fails `coverage_map.py`. The grouping and the descriptions are written by a
person — the machine only guards the membership.

### Research (2)

- [x] [research-architecture](research/research-architecture.md) — what was verified and against
  what, the twelve decisions and what each rejected, the six risks and their machinery
- [x] [research-component-coverage](research/research-component-coverage.md) — what the set is still
  missing, against the brief's catalogue, the Toolkit's controls and the behaviours a Metro surface
  had; the largest gaps turn out not to be components

### Reference (1)

- [x] [components](components.md) — every public composable, its preview and the file it lives in.
  The table in it is generated; `make check` fails when it no longer matches the sources.

**Not in the map above, and it cannot be.** `coverage_map.py` walks the layer directories, and this
document is not in one. It is listed here so a reader finds it; nothing guards the fact that it is
listed. What *is* guarded is the document's contents —
[`scripts/component_catalog.py`](../scripts/component_catalog.py), run by both gates.
