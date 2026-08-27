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
