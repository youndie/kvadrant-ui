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
- **One repository, `/snapshots`, whatever the version says** — *and the first answer here was
  wrong in an instructive way.* It derived the destination from the version, on the argument that
  Reposilite keeps two trees so the two must agree and a destination that cannot be typed has
  nothing left to check. The argument is fine and it rested on an assumption nobody had tested: that
  this identity may write to both trees. **It may not** — see the AC below. The destination was
  never a function of the version; it is a function of what the token permits, and that is one path.
  The name of that path is the host's and not a claim about the artefacts: `0.1.0` is fixed and
  immutable and lives under `/snapshots`, which is how the neighbours use the same host —
  `io.github.youndie:form-core` has ninety fixed versions in that tree and no `-SNAPSHOT` at all.
- **The host refuses a republish, and the build's check only says so in better words.** This is
  the reverse of what the item claimed for a day. The claim was that a tree called `snapshots`
  cannot be protecting a fixed version, so the build's check was the only thing standing between
  `0.1.0` and an overwrite. Publishing `0.1.0` a second time settled it: Reposilite answered
  **409 Conflict**, in that tree, on that version. The tree's name does not decide immutability;
  the version's shape does, and the host reads it the same way we do.

  The check stays, demoted honestly: it turns a 409 into a sentence naming the artefact, the
  version and what to do. A GET, not a HEAD — a HEAD carries no body, so its status is one nobody
  had to produce a document to justify. A `-SNAPSHOT` version is exempt, which is what a snapshot
  is.
- **The version moved to `gradle.properties`** because it is written in more places than one: the
  README quotes it twice and names the repository tree that holds it, and a release that moves the
  build and not the README hands a reader a coordinate that 404s. `scripts/version_guard.py` compares
  them and is in `make check`. It bit on the real drift the moment it was written, before the README
  was touched — the three lines it named were the three that were wrong.
- Not covered: **Maven Central**. B-21's answer stands — when the Portal happens this gains a second
  repository rather than losing this one. Also not covered: publishing from CI — the credentials were
  a workstation's and `gh secret list` was empty, so a release that one machine could cut was the
  honest description of where this stood. **Closed straight after, by
  [B-47](B-47-publishing-from-ci.md).**

- AC: `./gradlew publish` sends both modules to `/releases`, and
  `https://reposilite.kotlin.website/releases/io/github/youndie/kvadrant-core/0.1.0/kvadrant-core-0.1.0.pom`
  answers 200 afterwards — **the first time any of this has been checked over the network**.
  **NOT MET, and the reason is the point of the item.** The first publish — run from CI through
  [B-47](B-47-publishing-from-ci.md) with the credentials in the repository's secrets — was refused:
  `Could not PUT …/kvadrant-core-android/0.1.0/kvadrant-core-android-0.1.0.aar. Received status code
  403`. The token is **not wrong, it is not permitted**, and that is measured rather than inferred:
  Reposilite answers `401 Missing authorization credentials` with no credentials and `401 Invalid
  authorization credentials` with a made-up pair, so a 403 is a token that authenticated and was
  turned away on the route. The `releases` repository itself takes deployments — another group's
  artefacts are in it. So the token's write route covers `/snapshots/…` and nothing under
  `/releases/…`, which is exactly the shape you get when every previous publish from this identity
  was a snapshot. Settled by `GET /api/auth/me` with the token, which lists its routes.

  Nothing landed: all seven coordinates of `0.1.0` answer 404, so there is no half-published version
  to clean up. That is luck rather than design — `publish` sends one publication at a time, and the
  refusal happened to come on the first PUT.

  **Resolved by moving, not by asking for the permission.** The token keeps the one route it has and
  the build publishes to `/snapshots/io/github/youndie`, where the rest of this host's `io.github.youndie`
  artefacts already live. What that costs is stated above: the host's own refusal to redeploy a
  release is not protecting these coordinates, and the build's guard is the whole of what does.

  **What this proves is the item's own argument.** Publishing had been "verified" through
  `publishToMavenLocal` for months; the first thing that actually talked to the host failed on the
  first request. A local publish cannot fail this way, which is why it could never have found it.
- AC: a `v0.1.0` tag and a GitHub release naming what is in it and what is knowingly not.
- Anchors: `build.gradle.kts`, `gradle.properties`, `scripts/version_guard.py`, `README.md`,
  `CHANGELOG.md`.
