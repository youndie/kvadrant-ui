---
id: B-46
title: "Cut 0.1.0 — the first version of this that exists anywhere"
status: done
priority: infra
size: S
stage: stage-2-release
---

# B-46 — Cut `0.1.0`, the first version of this that exists anywhere

Forty-five items closed and **nothing had ever been published** — not a release and not a snapshot.
Checked rather than assumed: `io/github/youndie/kvadrant-core` answers 404 under both
`/releases` and `/snapshots` on the host, and `io/github/youndie/` under `/releases` does not exist
at all. [B-21](B-21-maven-coordinates.md) verified publishing through `publishToMavenLocal`, which
proves the coordinates, the variants and the POM and cannot prove that anything left the machine —
so the README's install snippet had never resolved for a single reader, and a green build said
nothing about it either way.

- **The decision: spend `0.1.0` now, on an API nobody has used.** The README stated the opposite
  rule — the version stays a snapshot "until the API has been used by somebody other than its
  author" — and that rule cannot be satisfied from where it stands, because a snapshot nobody can
  find is not how an API gets used. The rule is recorded as traded away rather than deleted; what it
  was protecting is real and is now the known cost: a published coordinate cannot be renamed, only
  deprecated, so an API that turns out wrong is a `0.2.0` and not an edit.
- **The version decides the repository, instead of a check comparing them.** Reposilite keeps
  releases and snapshots in separate trees, so the two have to agree. `build.gradle.kts` now derives
  the destination from `kvadrant.version`; the `doFirst` guard that used to refuse a release version
  to the snapshots repository is gone, not because it was wrong but because a destination that
  cannot be typed has nothing left to check. **Rejected:** two repositories declared side by side and
  a guard on each — that is the arrangement where `publish` sends the artefact to both.
- **What derivation cannot catch is a second publish of the same release**, and that is the mistake
  with no undo. The releases publish task asks the host for the POM first and refuses on a 200. A
  GET, not a HEAD: a HEAD carries no body, so its status is one nobody had to produce a document to
  justify. This asks for the POM itself. Snapshots are exempt, which is what a snapshot is.
- **The version moved to `gradle.properties`** because it is written in more places than one: the
  README quotes it twice and names the repository tree that holds it, and a release that moves the
  build and not the README hands a reader a coordinate that 404s. `scripts/version_guard.py` compares
  them and is in `make check`. It bit on the real drift the moment it was written, before the README
  was touched — the three lines it named were the three that were wrong.
- Not covered: **Maven Central**. B-21's answer stands — when the Portal happens this gains a second
  repository rather than losing this one. Also not covered: publishing from CI. The credentials are
  a workstation's, `gh secret list` is empty, and a release that one machine can cut is the honest
  description of where this is.

- AC: `./gradlew publish` sends both modules to `/releases`, and
  `https://reposilite.kotlin.website/releases/io/github/youndie/kvadrant-core/0.1.0/kvadrant-core-0.1.0.pom`
  answers 200 afterwards — **the first time any of this has been checked over the network**.
- AC: a `v0.1.0` tag and a GitHub release naming what is in it and what is knowingly not.
- Anchors: `build.gradle.kts`, `gradle.properties`, `scripts/version_guard.py`, `README.md`,
  `CHANGELOG.md`.
