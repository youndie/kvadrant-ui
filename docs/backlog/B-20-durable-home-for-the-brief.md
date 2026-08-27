---
id: B-20
title: "The primary research brief has no durable address"
status: open
priority: infra
size: XS
stage: stage-1-core
---

# B-20 — The primary research brief has no durable address

Every inherited fact in [research](../research/research-architecture.md) points at a document in
`metro-compose-brief/`, and that directory currently lives in a session output folder that will not
survive. A verification address that stops resolving turns a checkable claim into a claim, which is
the one thing this documentation format exists to prevent.

- **The fix is a location, not a copy into the docs tree.** The brief is in Russian, this tree is in
  English, and it is a snapshot of one research pass rather than a living document — merging it here
  would make it look like something that gets amended, which it is not.
- What does get vendored into this repository is the machine-readable part:
  `references/metro-tokens.json`, because [B-06](B-06-token-generator.md) builds from it and a build
  must not reach outside the repository.
- Rejected: leaving the brief where it is and relying on the research document's summary. The
  summary is the argument; the brief is the evidence, and a research document whose evidence is
  unreachable is an essay.

- AC: the brief sits somewhere that will still exist in a year, and
  [research §0](../research/research-architecture.md) names that location instead of the session
  path.
- AC: `metro-tokens.json` is committed in this repository next to the generator.
