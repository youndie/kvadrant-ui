---
id: B-28
title: "One flag for every deviation from canon: remastered"
status: done
priority: P1
size: M
stage: stage-1-core
blocked_by: []
---

# B-28 — One flag for every deviation from canon: `remastered`

**Done, and the survey was the larger half exactly as the item predicted — it just did not predict
the answer.** `KvadrantTheme(remastered = false)` exists, reaches components through the same
mechanism the metrics do, and has **one** citizen: a press that sinks over 100 ms where canon applies
it outright. `TiltEffect.cs` carries a single storyboard and it is the *return* one, so animating the
press in is a thing the phone did not do — and it is the thing that was asked about out loud, which
is how it got found.

**One row, because everything else the code admits to is a number rather than a behaviour.** The
panorama's settle, the live tile's interval, the placeholder's opacity: a flag cannot help with
those, since turning a number off leaves no number. They stay marked in KDoc and shipped as
parameters, which is a different rule for a different problem.

**And the item's own headline example was wrong.** The 48 dp minimum touch target is not "present
with no way to turn it off" — it is `KvadrantMetrics.touchTargetMin`, a field with a documented
default that a caller can set to `touchTargetVisual` today. D7 stands: an extended hit area changes
no pixel, and a library whose default is hard to tap is a worse outcome than a fidelity asterisk.

**The screenshot-pair criterion is amended rather than met**, and the reason is B-31's. The
difference between a press that snaps and one that sinks exists only *during* the tenth of a second
it takes; a still of either setting taken afterwards is the same image, and a fixture that captures
mid-animation is a golden whose pixels are decided by when the camera fired. `RemasteredTest` holds
the clock and counts pixels one frame in instead — verified by making the indication ignore the flag
and watching it report both settings at 24273 lit pixels.

**The order is fixed: canon first, from the documents; improvements afterwards, behind the flag.**
A component is not finished when it looks good — it is finished when it matches what the phone did,
and only then may it be given something the phone did not have.

Today the two are not separable. A press does not follow the finger ([B-27](B-27-tilt-does-not-follow-the-finger.md)),
the minimum touch target is 48 dp where Metro's is smaller, the tilt could animate on the way in as
well as out — each of these is a judgement about modern hardware and modern hands, and each is
currently either absent, or present with no way to turn it off, or a per-component parameter that a
caller has to find. Someone building a faithful Windows Phone reproduction and someone building a
modern application in a Metro skin want opposite defaults, and both are legitimate.

- **A theme flag, not a build flag and not a per-component parameter.** `KvadrantTheme(remastered =
  true)` reaches every surface at once, can differ per subtree, and — unlike a Gradle property — is
  visible in a screenshot test, so both settings can be rendered side by side in the same suite.
  A per-component parameter has the opposite property: it is a decision each caller makes silently,
  which is how a design system ends up with no design.
- **Off by default.** The library's claim is fidelity; anything else has to be asked for. A default
  that quietly improves things makes the fidelity unfalsifiable.
- **Every deviation names itself in KDoc and in the research document**, the same way an unverified
  number does. "Behind `remastered`" must be as legible as "not Microsoft's".
- The rejected alternative is a second artefact — `kvadrant-remastered` — with its own components.
  It doubles the surface to keep in step and gives a caller an all-or-nothing choice, when the
  realistic want is canon everywhere and one modern behaviour in the one place it matters.
- **Known deviations to gate, so far.** Each was found by measuring rather than by review, and each
  is currently on with no way to turn it off: the 48 dp minimum touch target
  ([B-11](B-11-accessibility-policy.md)); the accessible palette; and the turnstile's alpha, which
  runs on `tween`'s default fast-out-slow-in while its angle runs on Metro's exponential-out(6)
  ([B-15](B-15-motion-easing-and-turnstile.md)) — that one may turn out to be canon once somebody
  reads what the original faded on, which is the point of listing it rather than assuming.
- **Deciding which existing behaviour is already a deviation is part of this item**, and it is the
  larger part. [B-11](B-11-accessibility-policy.md)'s 48 dp touch target is one; the accessible
  palette is arguably another; anything KDoc currently marks "this project's" is a candidate.

- AC: `remastered` exists on `KvadrantTheme`, defaults to `false`, and reaches components through
  the same local the metrics do.
- AC: a checker, or a documented convention with teeth, that fails a deviation which is not gated —
  the flag is worth nothing if the next improvement lands beside it rather than behind it.
- AC: research gains a table of what the flag changes, one row per deviation, each row naming the
  canon behaviour it replaces.
- ~~AC: at least one screenshot pair — the same fixture at both settings.~~ **Amended.** A still
  cannot hold a difference that is purely temporal, and a fixture that captures mid-animation is the
  defect B-31 exists for. The difference is held by a clock-driven test.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantTheme.kt`
