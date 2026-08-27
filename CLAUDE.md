# Kvadrant UI — working notes for an agent

A Metro (Windows Phone 8 / Windows 8) component library for Compose Multiplatform, with an optional
`androidx.compose.material3` adapter. The skeleton builds; `kvadrant-core` has the desktop target
only, and the component work has not started.

## How to start a session

1. **[docs/research/research-architecture.md](docs/research/research-architecture.md)** — first,
   every time. It says what was verified and against what, which twelve decisions were taken and
   what each of them rejected, and which risks are open. A task read without it looks like "do the
   obvious thing", and here the obvious thing is usually the one that was rejected for a reason.
2. **[backlog.md](backlog.md)** — the order of work and why it is that order. Find the item; the
   item states the decision, the rejected alternative and the acceptance criteria.
3. The layer document for the area you are touching — once those exist. Today they do not, and that
   is deliberate: see the note in [docs/README.md](docs/README.md).

## Writing code here

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew check                         # tests + ktlint + viddikVerify. One gate, nothing outside it.
./gradlew ktlintFormat                  # on the mac; a formatter's edits do not survive elsewhere
./gradlew :kvadrant-core:viddikRecord   # re-record goldens after an intended visual change
```

- **Toolchain**: Gradle 9.7.1, Java 25 toolchain, Kotlin 2.4.10, ktlint plugin 14.2.0 running ktlint
  CLI 1.8.0. Versions live in `gradle/libs.versions.toml` and nowhere else.
- **Compose Multiplatform stays on the current release.** skiko is never declared here — it comes
  transitively with `compose.ui` at the version CMP pins, and forcing it means running a renderer
  the Compose runtime above it was not built against.
- **Desktop only for now.** Add a target when something runs on it (D14).
- **Screenshots**: a `@ViddikScreenshot` fixture must always exist in the module. With none, KSP
  reports SKIPPED and the verify task passes green with no tests in it. Name goldens in ASCII —
  non-ASCII collapses into colliding filenames. Desktop only, so a green suite says nothing about
  any other renderer.
- **Style is in `.editorconfig`**, not in the build script.

## Rules that are easy to get wrong here

- **`Kvadrant` in every identifier; `Metro` only in prose.** The primary research brief is written
  in `Metro*` names throughout — `MetroTheme`, `metro-core`, `io.metro.theme`. Do not copy them.
  D4 in the research document says why.
- **The brief's Gradle snippets are not safe to copy.** In particular
  `api(compose.material3) { version { strictly("[1.12.0, 1.13.0)") } }` resolves into the Jetpack
  M3 1.5 alpha line it was written to keep out — research §1.2.
- **A number that is not Microsoft's says so**, in KDoc, and ships as a parameter of the public API
  rather than as a constant. Where the specification has gaps they are named in research §1.10.
- **The light theme is not an inversion of the dark one.** Transcribe both; never derive one.
- **No Segoe asset enters this repository**, including test fixtures. Selawik (OFL) is the legal
  stand-in, and it has no Cyrillic.
- **`main` describes what exists.** A document describing something not yet built is `status: draft`
  and lives in an open pull request; on `main` that is an error the checker reports.

## Documentation checks

```bash
pip install pyyaml
make check
```

`make check` guards the documentation tree and is exactly what CI runs; `./gradlew check` guards the
code. `make report` is non-blocking and stays non-green on purpose:
the research anchors point at artefacts outside this repository, so `code_anchors.py` reports them
as absent, and there are no BDD scenarios while there is no behaviour to describe.

## Language

Documentation and code are in **English**. The primary research brief that this tree is derived from
is in Russian and lives outside the repository — research §0 names it.
