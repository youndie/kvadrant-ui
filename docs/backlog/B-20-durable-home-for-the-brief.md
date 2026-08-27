---
id: B-20
title: "The primary research brief has no durable address"
status: done
priority: infra
size: XS
stage: stage-1-core
---

# B-20 — The primary research brief has no durable address

**Done.** The brief is vendored at [`reference/metro-compose-brief/`](../../reference/metro-compose-brief/)
— 316 KB of Markdown, SVG and JSON, versioned with the code and depending on no external service.
It sits outside `docs/` with a `reference/README.md` saying what it is: evidence, in Russian, not
maintained, and wrong in five named places whose corrections live in research rather than in the
brief. Editing it to agree with the corrections would destroy the only record of what was believed
at the start, which is the half that stops a refuted idea being tried again.

Research §0 now points at that path and carries the list of corrections. Two things came of writing
that list down: it is five items long against a document whose numbers are mostly right, which is an
argument for the inherited-fact rule rather than against the brief — and `metro-tokens.json` turns
out to carry `scale.pxToDp = 0.75` **beside a justification research §1.6c refutes**. The value
stands and the prose next to it does not, which is a hazard the generator has to be told about
rather than discover; `reference/README.md` tells it.

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
- AC met in substance, differently from the wording: `metro-tokens.json` is committed at
  `reference/metro-compose-brief/references/metro-tokens.json` rather than beside a generator that
  does not exist yet. Copying it a second time when [B-06](B-06-token-generator.md) lands would give
  the repository two of them and no way to tell which is the source.
- **Not delivered, and worth saying:** nothing reads the file. It is bytes in the tree until B-06 or
  a palette test opens it, so "the dump is in the repository" is not yet the same as "the dump is
  usable". It parses and carries 13 colour tokens per theme, 20 accents, both ramps, 22 + 27
  metrics, tiles and motion — checked, because a vendored file nobody has opened is exactly the kind
  of thing that turns out to be truncated.
