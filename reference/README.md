# Reference material

Sources this project was derived from, vendored so that a verification address does not stop
resolving. Nothing here is maintained: it is evidence, and evidence does not get amended.

## `metro-compose-brief/`

The primary research pass this library was commissioned from, in **Russian**, exactly as it was
delivered on 26 August 2026. Every inherited fact in
[docs/research/research-architecture.md](../docs/research/research-architecture.md) that is marked as
coming from the brief points here.

**It is a snapshot and it is wrong in places.** That is not a defect to fix in this directory — the
corrections live in the research document, where each one names what the brief said and why it does
not hold. Editing the brief to agree with the corrections would destroy the only record of what was
believed at the start, which is the half that stops a refuted idea being tried again. Known
corrections so far: §1.2 (the Material version matrix), §1.6 (the tilt's 0.975 prediction, and the
per-backend camera claim), §1.6c (the ×0.75 multiplier's stated justification), §1.7 (the font
stack), §1.10 (the list-item type size).

`references/metro-tokens.json` is the machine-readable half — colours, both type ramps, metrics,
tiles, motion curves, tilt formulas — and it is here rather than only in the brief's own folder
because [B-06](../docs/backlog/B-06-token-generator.md) builds Kotlin constants from it, and a build
must not reach outside the repository.

**One trap for whoever writes the generator.** `metro-tokens.json` carries `scale.pxToDp = 0.75`
next to a `scale.justification` of three bullets, and research §1.6c refutes two of them as the same
identity restated: if a Metro pixel were 1/96 inch then px→pt is ×0.75 by definition, so "20 px ×
0.75 = the official 15 pt" and "the Win8 ramp lands on the official pt ramp" say nothing about dp.
The **value** survives — 0.75 is a defensible choice of reference phone, and the measurements are in
§1.6c — but the justification beside it must not travel into KDoc as though it were sound. It is the
ordinary hazard of a machine-readable dump: the numbers are checked and the prose next to them is
whatever somebody wrote.

`references/*.svg` open in any browser. Their dimensions and gaps are to scale and can be measured.
