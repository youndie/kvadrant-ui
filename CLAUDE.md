# Kvadrant UI — working notes for an agent

A Metro (Windows Phone 8 / Windows 8) component library for Compose Multiplatform, with an optional
`androidx.compose.material3` adapter. The components exist and are looked at: `./gradlew :sample:run`
opens the demo on the desktop, `:sample-android:installDebug` puts the same screen on a device.

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
- **Desktop and Android.** Android was added ahead of the plan because it is the only target that is
  not skiko (B-24); iOS and wasm still wait for something to run on them (D14). Gradle 9.7.1 forces
  AGP 9.x, AGP 9 forbids `com.android.library`/`com.android.application` in a KMP module, and AGP
  must be declared in the **root** build file or the Compose plugin cannot see its classes —
  research §1.13 before touching the build.
- **Screenshots**: `ScreenshotSuiteTest` guards the set — empty registry, fixture without a golden,
  golden without a fixture. Name goldens in ASCII; non-ASCII collapses into colliding filenames.
  A test that reads the golden directory must declare it as a task input, or Gradle leaves the test
  UP-TO-DATE and reports the last run's verdict about a set that has changed.
- **The suite is desktop only, and not for want of wiring**: viddik's capture engine publishes JVM
  variants only. A green suite says the skiko renderer is unchanged and nothing about Android, where
  the one defect that mattered so far (B-25) lived. That is B-29.
- **Every glyph in a golden comes from a bundled file.** That is what makes these images portable
  across operating systems, and it rules out fixtures whose point is a *missing* font — the host
  supplies one and the golden records the recording machine.
- **Style is in `.editorconfig`**, not in the build script.
- **The public ABI is pinned.** `check` runs `checkKotlinAbi` against
  `kvadrant-core/api/desktop/kvadrant-core.api`. A deliberate API change means running
  `./gradlew :kvadrant-core:updateKotlinAbi` and committing the diff, so a reviewer sees it. The
  dump is desktop-only; there is no `androidMain` source yet, and the first one will need this
  revisited.

## Rules that are easy to get wrong here

- **`Kvadrant` in every identifier; `Metro` only in prose.** The primary research brief is written
  in `Metro*` names throughout — `MetroTheme`, `metro-core`, `io.metro.theme`. Do not copy them.
  D4 in the research document says why.
- **The brief's Gradle snippets are not safe to copy.** In particular
  `api(compose.material3) { version { strictly("[1.12.0, 1.13.0)") } }` resolves into the Jetpack
  M3 1.5 alpha line it was written to keep out — research §1.2.
- **Canon first, improvements second and behind `remastered`.** Build what the phone did, from the
  documents; anything better than the phone is a deviation, is named as one, and waits for the flag
  (B-28). Restoring behaviour the original had is not a deviation and is not gated.
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

Documentation and code are in **English**. The primary research brief this tree is derived from is
in Russian and sits in [reference/](reference/), vendored rather than maintained — it is evidence,
and evidence does not get amended. Where it turned out to be wrong, the correction lives in research
and names what the brief said; five such corrections so far, listed at the end of research §0.
