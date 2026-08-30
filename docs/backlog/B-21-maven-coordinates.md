---
id: B-21
title: "Which Maven coordinates and which publishing identity?"
status: done
priority: infra
size: XS
stage: stage-1-core
---

# B-21 — Which Maven coordinates and which publishing identity?

**Answered: `io.github.youndie`, published as snapshots to the WIP Reposilite.** Not Maven Central,
and not yet — the Central Portal is a later conversation, and when it happens this gains a second
repository rather than losing the one it has. The host is the same one `settings.gradle.kts` already
resolves viddik from, so publishing added no infrastructure and no secret to the tree: credentials
come from `REPOSILITE_USER` / `REPOSILITE_SECRET`, a Gradle property on a workstation or an
environment variable anywhere automated, and a publish that finds neither fails **at configuration**, naming
`credentials.username`, before it has built or sent anything — measured by running it without
them, which is the only part of the pipeline that could not be verified here.

Two modules publish — `kvadrant-core` and `kvadrant-material-adapter` — and the two samples do not,
checked rather than assumed: publishing a sample is how a consumer ends up with a demo on their
classpath. Verified through `publishToMavenLocal`, which needs no credentials and proves the part
credentials cannot: that the coordinates, the variants and the POM are what they should be.

The version is `0.1.0-SNAPSHOT` and the publish task refuses a non-snapshot to the snapshots
repository — Reposilite would reject it too, but after the build had spent its time and with a
message about HTTP rather than about versions.

**Amended by [B-46](B-46-the-first-release.md): the version is `0.1.0`, and that guard is gone.** The
destination is now derived from the version rather than checked against it, so there is nothing left
for a guard to refuse. The other half of the amendment is less comfortable: "verified through
`publishToMavenLocal`" was the whole of the verification, and it proved the coordinates, the variants
and the POM while proving nothing about the host — `io/github/youndie/kvadrant-core` was absent from
both trees on it, so this item closed with an install snippet that had never resolved for anybody.
The part named here as the only one that could not be verified was in fact two parts, and the second
one went unnamed.

**Two criteria are amended.** "Verified with the Central Portal" does not apply to a destination that
is not the Portal. And the group is in the root build script rather than the version catalog: a
catalog holds the versions of things this project *depends on*, and putting its own identity there
would be the one entry nobody could resolve.

**One reading to correct if it is wrong.** The answer given was `io.github.youndie:kvadrant-ui`,
which reads as a single coordinate; the artefact names were already settled by
[D4](../research/research-architecture.md) as `kvadrant-core` and the rest, so this took
`io.github.youndie` as the group and `kvadrant-ui` as the project. Nothing has been pushed, so it is
still free.

`io.github.<org>` through the Central Portal, or a group under a domain this project controls. The
decision is cheap now and expensive later: a published coordinate cannot be renamed, only deprecated
and republished, and every consumer's build file carries it.

- Everything else about publishing is already decided — `com.vanniktech.maven.publish` to Maven
  Central through the Central Portal, signed, Apache-2.0 for code with the OFL fonts declared
  separately on `kvadrant-resources` ([D11](../research/research-architecture.md)).
- The artefact **names** are settled and are not part of this question: `kvadrant-core`,
  `kvadrant-resources`, `kvadrant-icons`, `kvadrant-material-adapter` — see
  [D4](../research/research-architecture.md), which also explains why they do not say "metro".
- This is `question` rather than `open` because the answer is not a technical finding — it depends
  on who owns the project and whether a domain is available to verify.

- AC: a group id chosen, verified with the Central Portal, and written into the version catalog.
