---
id: B-21
title: "Which Maven coordinates and which publishing identity?"
status: question
priority: infra
size: XS
stage: stage-1-core
---

# B-21 — Which Maven coordinates and which publishing identity?

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
