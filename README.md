# Kvadrant UI

A component library for **Compose Multiplatform** in the Metro design language (Windows Phone 8 /
Windows 8), with an optional adapter that lets it coexist with `androidx.compose.material3`.

**Status: skeleton.** `kvadrant-core` builds on the desktop target, with tests, ktlint and
screenshot verification in one `./gradlew check`. The components have not been written.

- [docs/](docs/) — the documentation tree, starting with
  [the architecture research](docs/research/research-architecture.md)
- [backlog.md](backlog.md) — what to do next and why in that order

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew check
```

## Licence

Apache-2.0 for the code — [LICENSE](LICENSE).

**The bundled fonts are not under it.** Selawik and Source Sans 3 are SIL OFL 1.1, and the full text
of each ships in the artefact as `Selawik-OFL.txt` and `SourceSans3-OFL.txt`; the published POM
declares both licences with a note saying which covers what, because licence tooling reads a POM and
not a jar. **No Segoe asset is in this repository**, including test fixtures — Selawik is Microsoft's
own metric-compatible stand-in and is the reason a typeface can ship here at all.

