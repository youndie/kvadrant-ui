---
id: research-architecture
title: Kvadrant UI — architecture research
type: research
status: active
date: 2026-08-26
---

# Research: the architecture of Kvadrant UI

Kvadrant UI is a component library for Compose Multiplatform in the **Metro** design language
(Windows Phone 8 / Windows 8), plus a separate adapter artefact that lets it coexist with
`androidx.compose.material3`. What makes it a library rather than a theme is that Metro and
Material 3 disagree *structurally* — flat versus elevation, rectangle versus rounded, tilt versus
ripple, absolute black versus tonal surface — so roughly half of the Material 3 control set cannot
be reshaped into Metro by any amount of theming. The core therefore depends on **no** Material
artefact at all, and interop is an opt-in second artefact rather than a property of the theme.

This document records **verified facts**, the **decisions** taken and the **risks** that are still
open. Anything unverified is called a hypothesis and says where it will be settled. The layer
documents (once there is code to document) say what the library does; this one says why it is built
this way.

---

## 0. Where these facts come from, and how much each is worth

There is no code yet, so "verified" cannot mean "read in this repository". It means one of two
things, and the difference decides how much weight a claim can carry:

| Tier | What it means | How it is marked below |
|---|---|---|
| **Re-verified here** | fetched and read on 2026-08-26 while writing this document; the address is in the fact table | no marker — the table names the artefact |
| **Inherited** | read in the primary research brief, which carries its own confidence marks | *(inherited)* + the brief's mark |

An inherited fact is **a hypothesis with a good address**, not a fact of this document. That rule is
not bureaucracy: applying it to the one thing everything else stands on — the dependency versions —
is what produced the correction in §1.2, on a row the brief marked as verified.

The primary brief lives in [`reference/metro-compose-brief/`](../../reference/metro-compose-brief/),
vendored so that the addresses below keep resolving. It is **not** part of this documentation tree
and is not maintained: it is in Russian, this tree is in English, and it is a snapshot of one pass
rather than a living document. It is also wrong in the places listed at the end of §0, and it stays
wrong — the corrections belong here, and editing the brief to agree with them would destroy the only
record of what was believed at the start.

| Brief document | What it holds |
|---|---|
| `metro-compose-brief/01-tech-brief.md` | summary, why this is not a Material re-skin, risks, phases |
| `metro-compose-brief/02-metro-spec.md` | the numeric specification, every number marked ✅ primary / 🟡 derived / ❌ no public source |
| `metro-compose-brief/03-existing-solutions.md` | audit of what exists, what is licence-clean to reuse, the font question |
| `metro-compose-brief/04-architecture.md` | theme model, `TiltIndication`, Material mapping tables, Gradle layout, full Kotlin listings |
| `metro-compose-brief/05-component-catalog.md` | 55 components with size and priority |
| `metro-compose-brief/06-roadmap.md` | phases, three spikes, the open decisions |
| `metro-compose-brief/references/metro-tokens.json` | machine-readable dump of every token |

**Corrections to the brief so far**, each one a place where an inherited fact did not survive being
re-checked: §1.2 the Material version matrix, §1.6 the tilt's 0.975 prediction and the per-backend
camera claim, §1.6c the ×0.75 multiplier's stated justification, §1.7 the font stack, §1.10 the
list-item type size. Five, out of a document whose numbers are mostly right — which is the argument
for the rule above rather than against the brief.

---

## 1. Verified facts

### 1.1 The niche is empty, and the one precedent is licence-clean

| Fact | Where verified |
|---|---|
| The only Metro library for Compose is `louis993546/Metro-Compose`: **Android only**, `com.android.library` + `kotlin.android`, no Kotlin Multiplatform | `metro/build.gradle.kts` in that repository |
| It is **not published** — the `maven-publish` plugin and the whole `publishing` block are commented out | same file |
| MIT, ~9 stars, ~730 commits; its public API is one flat dump | `metro/api/metro.api` *(inherited, ✅)* |
| `compose-fluent-ui` (the Fluent equivalent, 727★, Apache-2.0, full CMP) declares **no** Material dependency: `api(compose.foundation)`, `api(project(":fluent-icons-core"))`, `implementation(compose.uiUtil)`, `implementation(libs.kotlinx.datetime)`, `implementation(libs.haze)` | `fluent/build.gradle.kts` in that repository |
| No Windows-flavoured Compose library anywhere has a bridge to Material | *(inherited, ✅ — read from the same build files)* |

**Consequence 1.** There is no prior art for the adapter, in either the Metro or the Fluent world.
The only working precedent for a second design language beside Material in a KMP project is
`compose-cupertino` (`AdaptiveTheme` / `AdaptiveWidget`), so the adapter's *shape* is borrowed from
there rather than invented — see [D2](#d2-interop-is-a-separate-artefact-strategy-b-plus-c).

**Consequence 2.** `compose-fluent-ui` having shipped a full CMP design system with **zero** Material
dependencies is the strongest available evidence that [D1](#d1-the-core-depends-on-no-material-artefact)
is affordable, not merely tidy. It also supplies the theme shape to copy: an accessor object with
`@Composable @ReadOnlyComposable` getters, `internal` composition locals, and a separate scoped
`ThemeConfiguration` composable.

**Terminology trap worth carrying forward.** `io.github.composefluent.background` contains classes
named `Material`, `MaterialContainer`, `Modifier.materialOverlay()`. Those are *Fluent Materials*
(Mica/Acrylic), not Material Design. Do not reuse that noun anywhere in this project.

### 1.2 There is no stable Material 3 in the Compose Multiplatform 1.12 line

**Correction to the brief.** Its version matrix lists two rows —
`org.jetbrains.compose.material3:material3 1.12.0 stable → androidx M3 1.4.x` and
`1.12.0-alpha03 alpha → androidx M3 1.5.0-alpha22` — and builds the whole pinning strategy on the
first one. **The first row does not exist.**

| Fact | Where verified |
|---|---|
| Compose Multiplatform itself is at **1.12.0 stable** | `org/jetbrains/compose/compose-gradle-plugin/maven-metadata.xml` on Maven Central — `<release>1.12.0</release>`; same for `org/jetbrains/compose/runtime/runtime` |
| `org.jetbrains.compose.material3:material3` has **no** 1.10.0, 1.11.0 or 1.12.0 — all three POMs are HTTP 404 | direct probes of `.../material3/<v>/material3-<v>.pom` |
| Its newest **stable** is **1.9.0**; the newest published anything is **1.12.0-alpha03** | `.../material3/material3/maven-metadata.xml`, `lastUpdated 20260630114516` |
| `material3:1.9.0` resolves to Jetpack **`androidx.compose.material3:material3:1.4.0`** | `material3-1.9.0.pom`, runtime scope |
| `material3:1.12.0-alpha03` resolves to Jetpack **1.5.0-alpha22** | `material3-1.12.0-alpha03.pom` (`material3-lint:1.5.0-alpha22`) |
| The CMP 1.12.0 release pairs itself with `material3*:1.12.0-alpha03` on Jetpack M3 1.5.0-alpha22 | the release page for `v1.12.0` in `JetBrains/compose-multiplatform` |
| Jetpack M3 stable is **1.4.0** and nothing later; the 1.5.0 line is still alpha, up to **1.5.0-alpha26** | `androidx/compose/material3/material3/maven-metadata.xml` on Google's Maven |

**Consequence 1 — the brief's pinning snippet does the opposite of what it says.** It proposes
`api(compose.material3) { version { strictly("[1.12.0, 1.13.0)") } }` in order to *avoid* the 1.5
alphas. On CMP 1.12.0 the `compose.material3` accessor resolves into that very alpha line, ripple
API change included — and the ripple switch is the single trick the adapter is built on (§1.8). The
snippet must not be copied into a build file.

**Consequence 2 — the choice is between two whole CMP lines, not two artefacts inside one.**
`material3:1.9.0` was built against the CMP 1.9 runtime (its POM pulls `animation-core` and
`annotation` at `1.9.1`). Pinning it while the rest of the project is on CMP 1.12.0 is a mixed
graph, and there is a directly relevant observation about what a mixed graph does here: **on another
project on this machine, CMP 1.12.0 with `foundation`/`runtime` at 1.12.0 and `material3` pulled
from a different line resolved cleanly, compiled cleanly, and then died at render time on the first
screen with a text field** — `AbstractMethodError: OutlinedTextFieldDefaults$$Lambda does not define
CustomStyle.applyStyle`. That is not a public source and it is not this project, so it is a strong
prior rather than a fact about Kvadrant; what it settles is the shape of the check, because "it
compiles" was true in that case too. So the real options are:

* **(a)** the adapter targets CMP **1.9.x** + `material3:1.9.0` → Jetpack M3 1.4.0 (the stable pair
  the brief wanted), with the core still free to move ahead, because the core shares no Material
  dependency with it;
* **(b)** the adapter targets CMP **1.12.0** + `material3:1.12.0-alpha03` → Jetpack M3 1.5.0-alpha22,
  and accepts an alpha API — which means accepting that the ripple switch may be renamed under it.

This is settled by **rendering, not by resolving and not by reading POMs**, and it is the first
task of the skeleton — [B-04](../backlog/B-04-repository-skeleton.md). The acceptance is a Material
`OutlinedTextField` drawn on screen under the adapter, on each candidate graph.

**Run, and it settled nothing.** Both graphs draw the field, and both compile
`LocalRippleConfiguration provides null` — the one API the whole adapter is built on.
`GraphRendersTest` captures the pixels rather than trusting the absence of an exception.

| | resolves | compiles | **draws** | ripple switch |
|---|---|---|---|---|
| (a) `material3:1.9.0` → Jetpack M3 1.4.0 | yes | yes | **yes** | yes |
| (b) `material3:1.12.0-alpha03` → Jetpack M3 1.5.0-alpha22 | yes | yes | **yes** | yes |

So the failure this check was designed around — `AbstractMethodError` on the first screen with a
text field — **does not reproduce here**, and §1.2 was right to call it a strong prior rather than a
fact about this project. Worth keeping the check anyway: it cost an afternoon and it retires the
question for one component, which is one more than reading POMs retires.

**The decision therefore turns on which risk is cheaper to be wrong about, and that inverts the
brief.** Option (a) is stable Material on a **mixed graph**: `material3:1.9.0` brings
`runtime:1.9.0`, conflict resolution lifts it to 1.12.0, and its classes run against a runtime they
were not compiled against. That fails at *runtime*, in whichever component nobody happened to
render, in a consumer's application. Option (b) is a coherent graph on an **alpha API**: it fails at
*compile time*, in this build, on the day the alpha renames something, and it is fixed by editing
one line of a version catalog. A loud failure in our build beats a quiet one in someone else's, and
(b) is also the pair CMP 1.12.0 ships itself with — the graph most likely to have been exercised by
somebody other than us.

**Decision: the adapter takes (b).** Consequence 3 below reads the other way round and is
superseded: if the alpha becomes untenable, `-next` is where the stable-Material variant goes, not
the other way about.

**Consequence 3.** The brief's "two adapter branches" idea stops being speculative overhead and
becomes the cheap way out: `kvadrant-material-adapter` takes option (a) and
`kvadrant-material-adapter-next` takes option (b), which is one build-file difference rather than a
fork of the code.

**Consequence 4 — this is a rate of change, not a snapshot.** Jetpack M3 1.5 has been in alpha
long enough to reach alpha26, and CMP's material3 line has been alpha-only for three minor versions.
Anything in this project that pins a Material version is a thing that will need re-checking, and
the version catalog is the one place it is allowed to be written down.

### 1.3 Metro and Material 3 disagree structurally, not stylistically

*(inherited, ✅ — derived by reading the M3 component defaults; the table below is the brief's §7 of
`metro-compose-brief/04-architecture.md`)*

| | Material 3 | Metro |
|---|---|---|
| Shape | rounded, `extraSmall` … `extraLarge` | **everything rectangular** |
| Depth | elevation + tonal surface | **flat; there is no depth** |
| Press feedback | ripple from the touch point | **tilt** — the plane rotates towards the finger |
| Dark background | `surface` ≈ `#1C1B1F` | **absolute `#000000`** |
| Colour model | 48 roles, tonal palettes | **13 tokens + one accent** |
| Type | 15 styles, weight is a number | 12 styles, **weight is a choice of family** |
| Hierarchy | size + weight + colour | **mostly size**, 18.667 → 186.667 px |
| Dialogs | safe action on the right | **positive action on the left** (and the other way round in Win8) |

Counted against the ~22 Material 3 components: **~4** survive on theming alone (`Text`, `Surface`
with `surfaceTint = Transparent`, `Divider`, `Icon`), **~10** need a wrapper, **~11** need replacing
outright. The replacements cluster around four causes: elevation/tonal surface baked in, shapes
forced round, ripple called from inside the implementation rather than through `LocalIndication`,
and hard-coded Material Motion.

**Consequence.** The adapter is worth having for *foreign* Material widgets a consumer already
depends on — date pickers, autocomplete — and is worthless as a way of getting a Metro control set.
The kit itself is written from scratch in the core.

### 1.4 The numeric specification is complete enough to build on, and it is machine-readable

*(inherited, ✅ except where noted)*

| Block | Completeness |
|---|---|
| WP8 colour tokens, dark + light | **complete**, 13 × 2, from `ThemeResources.xaml` |
| 20 WP8 accents | values 🟡 — Microsoft published the palette **only as a picture**; two hexes (`#1BA1E2`, `#E51400`) confirmed from primary sources, the rest are consistent across independent copies |
| WP type ramp (12 styles), Win8 ramp (6 styles + 20 px baseline grid) | **complete** |
| Win8 grid (unit 20, sub-unit 5, silhouette 120/100) | **complete**, verbatim MSDN |
| WP page metrics, tile sizes both systems | **complete**; the 12/10 px tile gaps are 🟡 derived arithmetically |
| Motion curves and durations | **complete**, from WinJS `Animations.js` and the Windows Phone Toolkit |
| Tilt formulas | **complete**, from `TiltEffect.cs` |
| ToggleSwitch, ListPicker, ContextMenu, DatePicker, indeterminate ProgressBar | **complete** |
| **Pivot** | ❌ **nothing** — Microsoft never published it |
| Panorama parallax and peek | ❌ partial |

Every one of these is dumped into `references/metro-tokens.json` beside the brief — colours for both
themes, 20 accents with computed luminance and contrast, both type ramps, metrics, tiles, motion
curves and timings, tilt formulas.

**Consequence.** The token layer is a **transcription job, not a design job**: the JSON is the input
to a generator that emits Kotlin constants, and the generator is worth writing because hand-typing
several hundred numbers is where the errors would come from. Anything the JSON does not contain is
either a gap named in §1.10 or a number this project invents, and an invented number says so in its
KDoc.

**The px → dp multiplier is 0.75**, and it is not a convention but a result: WP `PhoneFontSizeNormal`
20 px × 0.75 = 15 sp = the official 15 pt; the WP status bar 32 px × 0.75 = 24 dp = the standard
Android status bar height; and the entire Win8 ramp under the same multiplier lands exactly on
Microsoft's published point sizes. Three independent axes agreeing is what makes it a canon.

### 1.5 The light theme is not an inversion of the dark one

*(inherited, ✅ — `ThemeResources.xaml`)*

`PhoneForegroundColor` is `#FFFFFFFF` at 100 % in dark and **`#DE000000` at 87 %** in light;
`PhoneSubtleColor` 60 % against 40 %; `PhoneDisabledColor` 40 % against 30 %.

**Consequence.** Generating the light theme by inverting the dark one produces something that reads
as "almost Metro" — and it is the first thing anyone who used a Lumia notices. Both palettes are
transcribed literally, and a test compares them against the JSON rather than against each other.

One number in the dump was suspect: `PhoneBorderColor` came out identical (`#BFFFFFFF`) in both
themes. **Settled, and it was a dump artefact** — the SDK's own `ThemeResources.xaml` gives
`#BFFFFFFF` for dark and **`#99000000`** for light. The tokens carry the correct pair and a test
asserts they differ.

### 1.6 Tilt is fully specified, and three details are usually got wrong

*(inherited, ✅ — `TiltEffect.cs`, Windows Phone Toolkit)*

```
MaxAngle = 0.3 rad = 17.188°      MaxDepression = 25 px
ReturnDelay = 200 ms              ReturnDuration = 100 ms

angleMagnitude = |nx − 0.5| + |ny − 0.5|
angle          = angleMagnitude * 0.3 * 180/π
depression     = (1 − angleMagnitude) * 25
RotationY      = angle * xContrib       * (−sign(nx − 0.5))
RotationX      = angle * (1 − xContrib) * (+sign(ny − 0.5))
GlobalOffsetZ  = −depression
```

Three things nearly every reimplementation gets wrong:

1. **The press is not animated.** Properties are set instantly on each manipulation delta; only the
   return is animated — three animations towards zero.
2. **Scale is not used at all.** The original uses `PlaneProjection` only; the "pushed in" feeling
   comes from perspective, not from shrinking.
3. **A touch in the exact centre gives 0° of rotation and the full 25 px of depression**; a touch in
   a corner gives 17.2° and none.

**Correction found in the tilt spike (B-01), three parts.**

*The formula as transcribed was not computable.* `xContrib` appears in the brief's §5.2 and in
`metro-tokens.json` without a definition, on a section marked ✅ "`TiltEffect.cs` in full". The RTM
algorithm was recovered from a vendored copy of that file and defines it outright:

```csharp
double xMagnitude = Math.Abs(normalizedPoint.X - 0.5);
double yMagnitude = Math.Abs(normalizedPoint.Y - 0.5);
double xDirection = -Math.Sign(normalizedPoint.X - 0.5);
double yDirection =  Math.Sign(normalizedPoint.Y - 0.5);
double angleMagnitude = xMagnitude + yMagnitude;
double xAngleContribution = xMagnitude + yMagnitude > 0 ? xMagnitude / (xMagnitude + yMagnitude) : 0;
```

*The falsifiable prediction was refuted.* This section used to say that at
`cameraDistance = 8 × density` the perspective gives ≈ 0.975, exactly the `pointerDown` scale WinJS
uses — offered as a sign that the geometry had landed on the original by construction. It does not:
Compose's `cameraDistance` is not a density multiplier at all. Its Skia implementation reads
`val depth = cameraDistance * 72f`, so the default 8f puts the camera at **576 px**, and the full
18.75 dp depression is a shrink of **0.9685**, not 0.975. The coincidence was arithmetic, and it was
out by a factor of 72. Nothing rests on it: 0.975 came from Windows 8's WinJS, a different system
from the phone's tilt, so matching it was never evidence of anything.

*The emulation is not a choice.* Compose has no `translationZ`, and its perspective term is applied
only when there is a rotation — `if (!rotationX.isZero() || !rotationY.isZero())` in the same file.
A centre press is pure depression with no rotation, so it would get no perspective even if a z
existed. Sinking is therefore a uniform scale, computed from the same camera the rotation uses.

| Fact | Where verified |
|---|---|
| `xAngleContribution = xMagnitude / (xMagnitude + yMagnitude)`, 0 when the denominator is 0 | `src/Callisto/Effects/TiltEffect.cs` — the RTM shell algorithm, `ApplyTiltEffect` |
| `cameraDistance` is turned into `cameraDistance * 72` pixels of depth | `Matrices.skiko.kt` in `ui-graphics` |
| Perspective is applied only when a rotation is non-zero | same file, same function |
| A centre press renders as a uniform shrink of 116/120 = **0.967**, against 0.9685 computed | `kvadrant-core/src/desktopTest/snapshots/tilt_centre.png`, measured |
| A `LayoutModifierNode` from an `IndicationNodeFactory` **does** survive being delegated inside `clickable` | `tilt_centre_through_clickable.png` is pixel-identical to `tilt_centre.png`; same for the corner pair |

**Correction found by pressing a tile in the running sample.** The formula is transcribed correctly
and was applied wrongly: **Silverlight's `PlaneProjection` turns the opposite way from Compose's
`graphicsLayer` on both axes.** Applied as written, the corner under the finger came *towards* the
eye — a button popping out instead of being pushed in. Both rotations are negated on the way into
the layer.

**And the way that was verified matters more than the fix.** The first attempt negated both, and a
measurement of the **corner** goldens said the horizontal axis had broken — so one negation was
taken back, and the horizontal stayed inverted through another round of "verified". The measurement
was the problem: a corner press turns about both axes at once, and the X rotation changes the height
of the side edges enough to swamp what the Y rotation does to them. Measuring a corner told the
truth about the vertical and nonsense about the horizontal.

The goldens that settle it are **single-axis** presses — the middle of each edge, where exactly one
rotation is non-zero:

| press | measured | |
|---|---|---|
| left edge | left 116 against right 120 | left recedes |
| right edge | left 120 against right 116 | right recedes |
| top edge | top 116 against bottom 120 | top recedes |
| bottom edge | top 120 against bottom 116 | bottom recedes |

A composite case is a bad instrument for a per-axis question, and this is the second time in this
project that a green check turned out to be measuring something adjacent to the thing in doubt.

**Consequence — tilt can be the default indication, and the public API does not grow.** The fallback
in Risk 1, an explicit `Modifier.kvadrantTilt()` on every surface, is not needed. This was the one
finding that could have changed the shape of every component signature in the library.

**And it is not expensive.** Measured on a Pixel 6a at 60 Hz, 25 full press-and-release cycles per
run: the GPU is idle at 1 ms for 518 of 549 frames and the CPU median is 5–6 ms against a 16.7 ms
budget. Jank ran between 0.00% and 1.10% — and *which* surface produced which figure is not a
finding: the wide tile gave 1.10% and then 0.18% on an identical repeat, while the smallest tile in
the set gave 1.10%. The spread between two runs of the same thing is wider than the spread between
the things, so the tempting reading — that jank scales with the area being transformed — is
unsupported rather than confirmed. Full table in
[B-01](../backlog/B-01-spike-tilt-indication.md).

**Consequence 2 — the camera is ours.** The original had a global camera over the whole screen whose
distance Microsoft never published, so there is nothing to transcribe; `TiltIndication` takes
`cameraDistance` as a parameter, defaults to Compose's own, and says so in KDoc.

**Consequence 3 — `cameraDistance` is in inches, and the backends disagree about how many pixels
that is.** Found while scoping what remains of the spike, and it is a defect in the code as written:

| Fact | Where verified |
|---|---|
| skiko converts at a fixed 72 px per inch — `val depth = cameraDistance * 72f`, under the comment "The camera location is passed in inches, set in pt" | `Matrices.skiko.kt` in `ui-graphics` |
| Android passes the value straight to `RenderNode` with no scaling at all | `GraphicsLayerV29.android.kt` — `renderNode.cameraDistance = value`; nothing between the modifier and the layer touches it |
| Android's own default camera is `1280 × density` px, and `density = dpi / 160`, so that is `8 × dpi` px — **8 inches**, the same 8 as `DefaultCameraDistance` | Android `View` documentation, arithmetic |

**The reading was right and the conclusion drawn from it was wrong**, which is worth more than
either. From those three rows it followed that `cameraDistance = 8f` is 576 px of depth under skiko
and some multiple of the display density on Android — "about six times flatter on a modern phone".
The bytecode still says what it said: `GraphicsLayerV29.setCameraDistance` and `RenderNodeApi29` and
`RenderNodeApi23` all hand the float to `RenderNode.setCameraDistance` untouched, whose own
documentation calls the unit pixels. Taken literally that would put the default camera **8 pixels**
away, which is not a flatter tilt — it is a tile folded through the lens — and it is plainly not what
a device shows. A documented unit that cannot be true is not evidence for a value six times larger
either; it is evidence that the documentation is not the place to find out.

**So it was measured, on both, with the same geometry.** A press on the centre of the bottom edge is
pure rotation about the x-axis, so the tile draws as a symmetric trapezoid and the camera falls out
of the two horizontal edge widths: `depth = s(r + 1) / (r - 1)`, `s = (H / 2) sin θ`.

| | top edge | bottom edge | ratio | solved depth |
|---|---|---|---|---|
| desktop, 497 px tile | 521 px | 461 px | 1.1301 | 588 px |
| Pixel 6a, 497 px tile | 507 px | 449 px | 1.1292 | 593 px |

**The two backends agree to within a tenth of a percent.** Both land on Compose's nominal 576 px
(the ~2% excess is the bias of reading antialiased edges two rows in from the ends, and it is the
same bias on both). There is no per-backend divergence in `cameraDistance` at all.

**The defect was ours, and it was one line.** On a Pixel 6a at 420 dpi the same tile pressed dead
centre draws 413 px at rest and 381 px pressed — **0.9225**, against the desktop's 0.9685.
`576 / (576 + 18.75 × 2.625)` predicts 0.9213. Nothing about the backends is involved: the camera
sits at a fixed number of *pixels* on both, and every piece of geometry handed to it is in *dp*, so
the denser the screen the closer the camera effectively is. Both symptoms are that one mismatch —
a press sinks deeper on a denser screen, and a same-sized tile gets more perspective there too
(a 100 dp tile subtends 0.174 of a 576 px camera on the desktop and 0.456 on the phone).

**The fix is to hold the camera at a distance in dp** and convert to the platform's unit at the
edge, which is what B-25 prescribed even though the reason it gave was wrong. `TiltIndication`'s
`cameraDistance` is a `Dp`, defaulting to the 576 dp that reproduces Compose's own camera at
density 1, so no desktop golden moves. `depressionScale` no longer contains the number 72;
`CameraProbeTest` solves the camera out of a rendered trapezoid and fails if the unit stops
reaching the layer.

**And the conversion factor is measured rather than read.** 72 px per unit is in skiko's source;
for Android it is the number the trapezoid gives, because the documented unit there is wrong.

**Confirmed on the device after the change.** The same 497 px tile — which on that phone *is* 189 dp
— now draws at 0.9678 of itself under a centre press against the desktop's 0.9685, and a bottom-edge
press gives a trapezoid ratio of 1.0461 against the 1.0487 the desktop draws for 189 dp. Before, the
same tile gave 0.9225. The comparison that means something is the one in dp: a pixel-for-pixel
comparison across two densities compares two different designs.

This is [B-25](../backlog/B-25-tilt-camera-is-in-inches.md), and it was not what
[B-01](../backlog/B-01-spike-tilt-indication.md) expected to leave behind — it expected a
frame-budget measurement. It is also the argument for the D14 amendment above: the defect was found
by reading, which is cheap, and the next one of its kind will be found by running, which is not.

**Consequence 4 — the metric scale must *not* reach the tilt, and the reason is the opposite of the
obvious one.** Raised from the running demo: *"как будто сила, насколько элементы вдавливаются,
зависит от их размера — это норм?"* Everything else in `KvadrantMetrics` scales together under
`scaled(factor)`, the depression sat outside it as a constant, and that looked exactly like the
omission it was not.

| Fact | Where verified |
|---|---|
| The same centre press draws 38/40, 96/100 and 232/240 of the box — one constant ratio plus whole-pixel rounding, not three strengths | `tilt_scale_ladder.png`, measured |
| `depth / (depth + depression)` contains no term for the size of the pressed surface | `depressionScale` in `TiltIndication.kt` |
| Scaling the depression alone makes a 1.6x tile draw at 0.951 of itself against a 1x tile's 0.9685 | `TiltScaleInvarianceTest`, run against a deliberately broken `scaled()` |
| Scaling the camera with it cancels that exactly, and moves the *rotation* by 0.2% of drawn area — 0.9755 against 0.9781 at 1.6x | same test, area measured off the rendered frame |

So the tilt was already proportional at every size, because the depression is a push along z and what
survives the projection is a ratio. The absolute sink does grow with the surface — 8 px on a tile
against 2 px on a checkbox — and that is the same 3.15% of each; it is what a fixed push-back under
one camera does, and it is what the user was seeing.

The whole correction is therefore a line of `scaled()` that reads `tiltDepression = tiltDepression`
with a paragraph over it, plus `TiltScaleInvarianceTest` to fail the next person who deletes the
paragraph and adds the `* factor`. The scaled-camera half was written, measured, found to be worth
0.2% of area, and reverted — a knob nobody can see is API surface with no case behind it, and the
unit question it would have entrenched belongs to B-25.

**Method note.** The first version of that test asserted the invariant *"sinks by the same fraction
of itself"* and went green. It also went green against `scaled()` with every tilt term removed —
the state the change was supposed to be repairing. A test written from the same reasoning as the fix
inherits the reasoning's blind spot, and only breaking the code in *both* directions showed which of
the two the assertion could actually see. This is [the recurring one](#0-provenance) in this
project: a green check usually measures something adjacent to the thing in doubt.

### 1.6b The ramp was right and four controls pointed at the wrong rung

Raised from the running demo: *"текстовки капец мелкие, а раньше на Windows Phone они казались
крупней — кривой перевод dp→px или так было?"* Neither, as it turned out.

**The conversion is right, to within a percent.** WVGA is 480×800 physical pixels on a ~4.3 in
phone, so a Metro pixel *is* a physical pixel there: 217 ppi. The canonical ×0.75 turns
`PhoneFontSizeNormal` — 20 px — into 15 sp, which on a Pixel 6a at 431 ppi is 2.32 mm against the
phone's 2.34 mm. Every rung of the ramp lands at 99% of the original's physical size.

**Four controls were set at the page default where Microsoft set them above it.** The toolkit's
`Generic.xaml` overrides `FontSize` in exactly six styles, and reading them off is the whole finding:

| Style | Microsoft | Metro px | ours before |
|---|---|---|---|
| `controls:MenuItem` | `PhoneFontSizeLarge` | 32 → 24 sp | 15 sp |
| `controls:ToggleSwitch` | `PhoneFontSizeLarge` | 32 → 24 sp | — (no label in our API) |
| `controls:ListPicker` | `PhoneFontSizeMediumLarge` | 25.333 → 19 sp | 15 sp |
| `controls:PhoneTextBox` | `PhoneFontSizeMediumLarge` | 25.333 → 19 sp | 15 sp |
| `controls:AutoCompleteBox` | `PhoneFontSizeMediumLarge` | 25.333 → 19 sp | not built |
| `controls:CustomMessageBox` | `PhoneFontSizeNormal` | 20 → 15 sp | 15 sp ✓ |

And `local:PhoneApplicationFrame` in the SDK's own dictionary sets `PhoneFontSizeNormal` for the
page, which is why everything that does *not* override is correctly 15 sp — a list item, a button,
a page body. The defect was never the default; it was the four overrides nobody transcribed.

**Consequence — a missing rung hides a class of defect.** `KvadrantTypography` had no
`mediumLarge` slot at all: the ramp constant existed, the style did not, and a control with nowhere
to point at points at the default. Two of the four were mis-sized for that reason rather than by
choice, which is the kind of thing a code review cannot see and a side-by-side with the phone can.

**The other half of the question is not a defect and is still open.** `scaledToWidth` grows every
length in the metric set with the surface and deliberately leaves the type ramp alone (D5), so on a
411 dp phone the tiles are 1.2× their Metro size and the text is 1.0×. Relative to the tiles, the
text really is smaller than on the phone — by exactly the metric scale. Whether that trade is right
is a judgement, not an error, and it is where a `remastered` setting would live
([B-28](../backlog/B-28-remastered-flag.md)).

### 1.6c "px" is not one unit, and ×0.75 is a choice of reference phone

Asked directly: *"а 20 px у Microsoft это не 20 dp? и может px в разных источниках разное dp?"*
Both halves are worth an answer and the second one is the important one.

**"px" means two different things in the two sources this project reads.**

| source | what a "px" is | px → pt | px → dp |
|---|---|---|---|
| WPF / Windows 8 (WinRT) | **1/96 inch**, a physical unit | ×0.75, *by definition* | ×1.667 |
| Silverlight for Windows Phone | one unit of the 480×800 canvas, stretched to the device | — | depends on the device |

**On the phone a Metro pixel was never a fixed physical size.** The canvas is always 480 units
across, so its size on glass is whatever the screen is. Measured across the range Windows Phone 8
actually shipped on:

| device | canvas scale | one Metro px | ×N that would reproduce it in dp |
|---|---|---|---|
| Lumia 620, 3.8″ | ×1.0 | 1/246 in | ×0.652 |
| Lumia 520, 4.0″ | ×1.0 | 1/233 in | ×0.686 |
| Lumia 920, 4.5″ | ×1.6 | 1/207 in | ×0.772 |
| Lumia 1520, 6.0″ | ×2.25 | 1/163 in | ×0.981 |

A 43% spread. **So ×0.75 is not a conversion, it is a choice of reference device** — it puts us
between the 520 and the 920, at the small-phone end of the range, and it is 24% below the 1520,
which is the WP8 device closest in physical size to a modern phone. That is the whole of why the
text reads small: it is faithfully reproducing a 4-inch phone on a 6-inch screen.

**The brief's justification for ×0.75 does not survive being read closely, though its number nearly
does.** It states the premise "Metro metrics are WPF/Silverlight pixels (1/96 inch)" and then offers
three checks. Two of them — the WP ramp landing on 15 pt, the Win8 ramp landing on the official
42/20/11/9 pt — are *the same identity restated*: if a px is 1/96 inch then px→pt is ×0.75 by
definition, and neither says anything about dp. Only the status bar (32 px → 24 dp, Android's own
height) is independent, and it is one number. And the premise itself is false for the phone: at
1/96 inch a 480-unit canvas would be **5.00 inches wide**, and no Windows Phone was wider than 2.94.
Taken at its word the premise demands ×1.667, which would make body text 33 dp.

**Consequence 1 — a live trap in [B-22](../backlog/B-22-win8-branch.md).** On Windows 8 the premise
*is* true: a px there really is 1/96 inch. So the correct multiplier for a Win8 profile is **×1.667,
not ×0.75**, and applying this project's constant to that ramp would render it at 45% of its
intended physical size — 42 dp where 93 is meant. The brief's Win8 "validation" produced points, and
points are not dp.

**Consequence 2 — [D5](#d5-metrics-scale-as-one-set) had it backwards, and is amended.** `scaled()`
used to leave the type ramp alone, on the reasoning that scaling type with layout turns a faithful
design into a merely large one. That is the Android and iOS convention and it is not Metro's:
Windows Phone scaled the **whole canvas** by the resolution ratio — ×1.5, ×1.6, ×2.25 — and text
scaled with it, because text was measured in the same canvas units as everything else. The exception
was a deviation recorded as a preservation, and the flag it would have needed
([B-28](../backlog/B-28-remastered-flag.md)) would have had to gate the *old* behaviour.

**The amendment, and how it is wired.** `KvadrantMetrics.scale` now carries the cumulative factor and
`KvadrantTheme` applies it to `KvadrantTypography`. The two live in different objects here where the
phone had them in one, so leaving it to the caller would mean a caller who scales the layout and
forgets the text — `TypeScaleTest` measures drawn ink at scale 1 against scale 2 and fails if either
end of that wiring comes apart, verified by breaking each. Nothing is rounded to whole sp: Metro's
own ramp is 18.667 / 22.667 / 25.333, and rounding a scaled ramp breaks relationships the unrounded
one holds.

**Consequence 5 — the camera belongs to the screen, and moving it there was B-26's answer.**
`Modifier.graphicsLayer` gives every element its own camera at its own centre; Metro had one over the
whole display. Rendered side by side — `tilt_camera_per_layer` and `tilt_camera_shared` — a grid of
nine tiles under per-element cameras is nine identically deformed copies of one shape, while the same
grid under one camera bends as a **single sheet**: the tiles near the axis barely skewed, the outer
ones leaning progressively. The second is what anyone who used the phone remembers, and the
difference is not marginal — displacing the axis by one tile changes 3 505 pixels of a 24 279-pixel
tile.

`TiltNode` implements `LayoutAwareModifierNode` and sets `transformOrigin` to the root's centre in
its own fractional coordinates. **No new number**: the distance is still `KvadrantCamera.Distance`,
the same one a per-element camera used. Only the axis moved, from a place nobody chose to the place
the original had it — so this is not a deviation and is not behind `remastered`.

*Consequence — nothing caught it, and the reason generalises.* Not one existing test or golden moved
when the camera changed, because **every fixture that presses something centres it in the frame**,
where a shared axis and an element's own axis are the same point. A property that only shows off
centre needs a fixture that is off centre; `SharedCameraTest` puts two tiles either side of the axis,
presses both in their own top-left corner, and asserts they do *not* render alike. Three goldens
moved once it existed to be caught by — the two button-state frames and the pressed start tile, all
three of which press something away from the middle.

*And the first version of that test asserted the wrong thing.* It required the two tiles to be mirror
images, which they are not: both are pressed in *their own* top-left corner, so both lean the same
way and only the camera's axis differs. A symmetry that looks obvious in a sentence is worth checking
against the picture.

### 1.7 Selawik contains no Cyrillic — re-verified

| Fact | Where verified |
|---|---|
| `Script Tags: dlng:'Latn' slng:'Latn'` | Microsoft Typography font list page for Selawik |
| Code pages: **1252 Latin 1, 1250 Latin 2 Eastern Europe, 1254 Turkish, 1257 Windows Baltic** — 1251 Cyrillic is **absent** | same page, verbatim |
| SIL OFL 1.1, © 2015 Microsoft, five weights including Light and Semibold | same page |

Selawik is the only legal, metrically compatible stand-in for Segoe UI — Segoe itself cannot be
bundled under any circumstances, on any platform, and neither can Segoe MDL2 Assets or Segoe Fluent
Icons *(inherited, ⚠️ — several independent readings of the EULA, no legal opinion was obtained)*.

**Verified here, in the files rather than on the page.** The font binaries were parsed directly:
Selawik's `cmap` contains no U+0410, U+044F or U+0401, and the OS/2 code-page bit for 1251 is clear.
Greek is absent too. The Microsoft page and the font agree.

**Correction found in the font spike (B-03) — the stack the brief recommends cannot be built.**
This section used to end with "the fallback font will not be metrically compatible, so the two
scripts will have different rhythm", and it recommended `Selawik → Inter → Noto Sans` as a
`FontFamily` fallback chain. Measured, that chain does nothing at all:

| Fact | Where verified |
|---|---|
| `FontFamily(Selawik…)`, `FontFamily(Selawik…, Inter…)` and `FontFamily(Selawik…, Fira…)` render **byte-identically** | `kvadrant-core/src/desktopTest/snapshots/font_stack_selawik_*.png` — one MD5 across all three |
| The primary font *is* applied — `inter only` and `fira only` differ from each other and from the above | same directory |

A `FontFamily` list in Compose selects among **weight and style variants of one family**; it is not
a glyph-fallback chain. The Cyrillic in those three renders came from the host's own font manager,
not from anything this project bundled. So the brief's stack would have shipped a UI whose Cyrillic
is a different typeface on every operating system, and tofu wherever the host has nothing suitable —
while looking, on the developer's machine, as though the bundled fallback worked.

**Measured metrics of the candidates**, normalised to em (Selawik is the reference because the whole
ramp is calibrated to Segoe, and Selawik is metric-compatible with Segoe):

| Font | x-height/em | cap/em | x vs Selawik | Cyrillic | Light weight |
|---|---|---|---|---|---|
| Selawik | 0.5000 | 0.7002 | — | **no** | yes (the exact Metro set) |
| **Source Sans 3** | 0.4860 | 0.6600 | **0.972×** | yes | yes |
| Fira Sans | 0.5270 | 0.6890 | 1.054× | yes | yes |
| Noto Sans | 0.5360 | 0.7140 | 1.072× | yes | yes |
| Open Sans | 0.5352 | 0.7139 | 1.070× | yes | yes |
| Inter | 0.5459 | 0.7275 | 1.092× | yes | yes |
| PT Sans | **0.5000** | **0.7000** | **1.000×** | yes | **no — Regular and Bold only** |

**Consequence 1 — PT Sans is the trap.** It matches Selawik to four decimal places on both
measures, and it is disqualified anyway: it ships Regular and Bold and nothing lighter. Metro's
typography is weight-by-family, and its signature is a 72 px **SemiLight** header. A companion
without a Light would render every Cyrillic heading visibly heavier than its Latin neighbour, which
is a worse failure than a few per cent of x-height.

**Consequence 2 — the mismatch is compensable, once the fallback is explicit.** If the runs are
split by script deliberately, the companion can be rendered at the ratio of the two x-heights and
the scripts sit at the same optical size. It is not cosmetic: uncompensated, `settings настройки`
wraps to two lines at the header size; compensated, it fits on one
(`font_stack_per_run_fira_raw.png` against `font_stack_per_run_fira_compensated.png`).

**Consequence 3 — the declared x-height is not the rendered one, and compensating on it is wrong.**
The table above says Source Sans 3's x-height is 0.972 of Selawik's, which implies scaling the
Cyrillic run up by ×1.0288. Measured in the render, per glyph, that over-corrects:

| size compensation | Latin x-letters | Cyrillic x-letters |
|---|---|---|
| **×1.000** | 27–28 px | **27–28 px** |
| ×1.010 | 27–28 px | 27–28 px |
| ×1.0288 (declared ratio) | 27–28 px | 27–**29** px |

The flat-topped letters (`n` against `н`, `т`, `и`) match at 27 px with no compensation at all; the
round ones (`s`, `e` against `о`, `а`, `с`) are where the extra pixel appears, because Source Sans
overshoots more than Selawik and Cyrillic lowercase is round-heavy. OS/2's `sxHeight` is a declared
number measured on Latin `x`, and this font draws its Cyrillic taller than that — so the compensation
was correcting a deficit the design had already made up.

**No size compensation is applied.** The correction was found by someone looking at the render and
saying the Cyrillic sat high; the declared metrics had said the opposite, confidently, in a table.

**Consequence 4 — the companion's *weight* needs calibrating, and the number is not 300.**
Selawik's Semilight sits between Light and Regular; asking Source Sans 3 for its nearest static
weight gives a Cyrillic run visibly thinner than the Latin beside it. Compose Desktop honours
`FontVariation`, verified by three weights producing three different renders, so the companion is
instanced on the `wght` axis instead. Ink coverage of the two runs, at equal optical size, brackets
the answer:

| `wght` | Cyrillic ink ÷ Latin ink |
|---|---|
| 300 | 0.731 — a quarter thinner |
| 350 | 0.936 |
| 365 | 0.976 |
| **370** | **0.989** |
| 400 | 1.131 |

`font_weight_per_run_source_sans_*.png` and `font_fit_*.png`. Ink coverage is a proxy rather than a
stem measurement — Cyrillic lowercase carries more straight verticals than Latin — but it brackets
the answer, and **370** is where the two runs stop reading as two weights. (The first pass landed on
365; that was measured against the over-compensated size above, and removing the compensation moved
it by five.)

**Consequence 2.** Selawik is frozen — six commits, one binary release, December 2015 — with known
missing kerning. Nobody upstream will fix it, so any fix is a fork, and the fork is a font project.

### 1.8 Ripple can be switched off globally, and the switch is version-fragile

*(inherited, ✅)*

`LocalRippleConfiguration` is a `ProvidableCompositionLocal<RippleConfiguration?>`, and providing
`null` disables ripple for the subtree; `rememberRipple` is deprecated at `DeprecationLevel.ERROR`.
`IndicationNodeFactory` is a stable interface with no opt-in, `Modifier.indication` wraps the node in
a private `DelegatingNode`, and `AbstractClickableNode` implements no layout delegate of its own —
so the layout slot a tilt node needs is free. The stated limitation is that delegating to multiple
`LayoutModifierNode`s without the delegating node implementing it is not allowed.

**Consequence.** Both halves of the press-feedback plan hang off APIs that the Jetpack M3 1.5 alphas
are actively reshaping (§1.2 lists `RippleThemeConfiguration` among the changes). The core's tilt
does not depend on Material at all and is safe; the **adapter's ripple suppression is the fragile
part**, and it is the reason the adapter is a separate artefact with its own release cadence.

**This is theory until it runs.** "The slot is free" was established by reading, not by executing.
[B-01](../backlog/B-01-spike-tilt-indication.md) exists because a plan that has only been read is a
hypothesis, and this one is load-bearing enough that its fallback (an explicit `Modifier.kvadrantTilt()`
the consumer applies by hand) changes the library's ergonomics.

### 1.9 There is no first-party screenshot testing for Compose Multiplatform

*(inherited, ✅)* Roborazzi covers Android and JVM desktop and marks iOS and Compose Desktop support
experimental. Paparazzi is Android-only. Google's Compose Preview Screenshot Testing states outright
that it does not support non-Android targets in KMP projects.

**Correction found while building the skeleton (B-04), in two steps.** This section used to conclude
"therefore Roborazzi". The toolkit actually reached for was **viddik**, which renders Compose in a
real Skiko window and writes portable PNG goldens — it is already the screenshot tool in use here,
and a golden recorded on a laptop verifies on CI, which Roborazzi's experimental desktop support
does not promise. It was wired in, and it worked: two fixtures recorded, `viddikVerify` inside
`check`, and the gate shown to go red on a changed golden rather than assumed to.

Then the second step, which is the one worth keeping. viddik 0.1.2.13 did not run against the
current Compose Multiplatform, and the project was briefly pinned back to CMP 1.11.1 to accommodate
it. **That was the wrong trade and it was reverted**: a test-only tool does not get to choose the
Compose line a published library is compiled against. The suite came out, and came back a day later
with viddik **0.2.0.14**, which runs on CMP 1.12.0 — skiko resolves to 0.150.1 everywhere with no
force anywhere ([B-23](../backlog/B-23-viddik-pins-the-compose-line.md)). The table below is kept
as the record of what the two-version gap actually cost to diagnose.

| Fact | Where verified |
|---|---|
| viddik's engine calls `Matrix44(float[])`, private in skiko 0.150.1 which CMP 1.12.0 ships | `IllegalAccessError` at the first rendered frame |
| It also calls `ComposeScene.render(Canvas, long)`, which CMP 1.12.0 removed in favour of `measureAndLayout()` + `draw(Canvas)` | `NoSuchMethodError`; `javap` on `ui-desktop-1.12.0.jar` against `ui-desktop-1.11.1.jar` |
| Holding skiko at 0.144.6 on the test classpath alone does **not** work — it gets past the first error onto the second | measured, not reasoned |
| skiko is never declared by this project: it arrives transitively with `compose.ui`, at the version the CMP release pins | `./gradlew :kvadrant-core:dependencies --configuration desktopRuntimeClasspath` |

**Consequence.** The acceptance surface for a design-system library is visual, and it is covered on
**one** target: desktop. Android, iOS and wasm have no screenshot coverage, and a green suite here
says the desktop renderer is unchanged and nothing more.

**Consequence 2 — skiko is not a dependency this project chooses.** It is an implementation detail
of the Compose version, coupled to it in both directions, and forcing it means running a renderer
the Compose runtime above it was not built against. The only version this project pins is CMP's.

**Consequence 3 — Android has no goldens and will not get any here, which is B-29's decision.**
viddik's capture engine publishes JVM variants only, so there is no path to Android pictures inside
`check` without work in another repository. A second screenshot tool was rejected on **ownership**
rather than on evidence: two golden formats, two ways to record and two answers to "is the suite
green" cost every future change, while Android being uncovered costs only where Android differs.

So Android's guard is a **number**. `AndroidCameraProbeTest` presses the centre of a tile's bottom
edge — pure rotation about x — and solves the camera's depth out of the resulting trapezoid, on the
renderer this library will mostly ship on. `./gradlew :kvadrant-core:connectedAndroidDeviceTest`, and
it needs a device; it is deliberately **not** in `check`, because a gate that cannot run without
hardware is a gate that gets skipped and a skipped gate reads as a green one.

| Fact | Where verified |
|---|---|
| The probe runs and passes on a Pixel 6a at API 17 | `connectedAndroidDeviceTest`, this device |
| compose-resources leaves `copyAndroidDeviceTestComposeResourcesToAndroidAssets` without an `outputDirectory`, failing configuration before anything reaches the device | the task's own validation message; worked around in `kvadrant-core/build.gradle.kts` |
| `compose.uiTest` drags in espresso-core 3.5.0, which calls `InputManager.getInstance` — removed in Android 17 | `NoSuchMethodException` from `Espresso.onIdle`; pinned forward to 3.7.0 |
| `runComposeUiTest` launches an activity that nothing declares without `ui-test-manifest` | "Unable to resolve activity for … androidx.activity.ComponentActivity" |

**Consequence 4 — the probe found a defect in itself the moment it met a second density.**
`PressInteraction.Press` carries a position in the pressed element's own **pixels**, and both probes
wrote the dp number straight in. On the desktop, at density 1, that is correct by coincidence; at
2.625 the same numbers land near the top-left corner instead of the bottom edge, the tile draws
almost flat, and the camera solves to **minus thirty-eight thousand pixels**. A second renderer paid
for itself on its first run, which is the argument B-24 made and this is the second time it has held.

**Consequence 5 — determinism is a property to be measured, not assumed.**
[B-31](../backlog/B-31-screenshot-suite-is-not-deterministic.md) recorded six of the sixty-eight
goldens changing between two recordings of unchanged source, by about 147 pixels of 280 000 — 0.052 %
against viddik's 0.05 % tolerance, so those fixtures passed and failed at random. A suite that fails
at random teaches its readers that a red run means nothing, and after that a real regression is
invisible.

`scripts/screenshot_determinism.py` (`make screenshots`, `ROUNDS=n`) records the suite n times and
names every image that moved. It is **not** in `make check`: it records everything twice, and a gate
that takes a minute to say nothing is a gate people stop running. Run it after adding a fixture, and
before believing a claim that no golden moved.

| Fact | Where verified |
|---|---|
| Ten consecutive recordings of the current suite produce byte-identical images | `make screenshots ROUNDS=4`, twice, plus six by hand |
| The script detects a fixture that genuinely varies | a temporary fixture keyed on `System.nanoTime()` was added, flagged as the only non-deterministic image, and removed |
| The mechanism the item suspected is not present: `KvadrantToast(visible = true)` has nothing to animate on its first composition, and `StartScreen`'s `LaunchedEffect` does nothing when no tile is pressed | the fixture sources |

**So the flake is real in the record and not reproducible now, and those are different claims.**
Nothing was fixed; what exists is a way to tell. Anything that would explain it — the tilt camera
becoming a `Dp`, the font family gaining instanced weights — landed between the observation and the
attempt to reproduce it, and none of them is a mechanism anybody has demonstrated. The honest
position is a guard, a lowered priority, and the trigger written into the item: if
`make screenshots` ever names an image, that image's fixture holds something moving.

### 1.10 Confirmed absences — things that do not exist, though the internet says otherwise

*(inherited, ✅)*

* **Pivot metrics.** Header height, active and inactive title sizes, paddings, how far the next title
  peeks — Microsoft published **none** of it for the WP8 Silverlight control, and the spike
  ([B-02](../backlog/B-02-spike-pivot-metrics.md)) did not close that. What it did close is
  everything around it; see §1.11.
* **Live tile flip timing.** Not merely undocumented — documented as *not controllable by
  applications*. Any interval this library offers is its own invention and must be named as one.
* **`ContextMenu` does not blur its background.** It scales it 1.0 → 0.94 over 420 ms. There is no
  blur in the sources.
* **Win8 `Title` is SemiBold, not Bold**, and Caption 12 px is **9 pt**, not 12 pt.
* **There is no Metro library for SwiftUI at all.**
* **The phone's Continuum transition is not in any reachable artefact.** Three were searched and all
  three came back empty: the whole WP8 SDK (every file of both cabs, 1,503 of them), the Windows
  Phone Toolkit (591 tree entries, 27 transition storyboards), and the Windows 10 SDK's
  `generic.xaml`. Continuum on the phone was a **shell** transition — the app-list-to-app effect —
  never exposed to applications, which is why an SDK for writing applications carries no trace of
  it. It joins the live-tile flip timing rather than the list of things still to look for.
* **The Toolkit has no TurnstileFeather storyboard either**, though it has twenty-seven others. The
  feather is generated in code, which is consistent with what it does: a shared axis cannot be
  authored per element.

Eight numeric gaps remain in total; they are listed in `metro-compose-brief/02-metro-spec.md` §9 and
each names how it could be closed.

**The text size of a list item.** Raised by eye — *"но список в почте мелкий, это норм?"* — and
searched for properly: the SDK's own dictionary sets `FontSize` in exactly one place
(`local:PhoneApplicationFrame`, `PhoneFontSizeNormal`), and the toolkit's `Generic.xaml` overrides it
in six styles, none of them a list. The UX guidelines document the LongListSelector's jump-list tiles
and group headers to the pixel and say nothing about the item's type. So a list item that overrides
nothing is 20 px, and that is all Microsoft published.

The memory of the phone's lists being larger is almost certainly right and it is not contradicted by
any of this: Mail, People and Messaging each shipped their own data templates, inside the
application, and those never appeared in a theme dictionary. **What is absent is the number, not the
practice.** `KvadrantListItem.titleStyle` is therefore a parameter with the published default, and
the sample — which is imitating Mail — passes `mediumLarge` and says in a comment that it is the
demo's call.

### 1.11 The Pivot is documented everywhere except in the numbers that matter

The spike went after the WP8 metrics along every route that does not require the SDK. Three things
came back, and they are worth separating carefully, because two of them are about a **different
implementation of the same design** and pretending otherwise is exactly the failure this document
exists to prevent.

**Structure and behaviour — WP8, primary, verified.**

| Fact | Where verified |
|---|---|
| `Pivot` is a `Grid` holding a `PivotHeadersControl` and an `ItemsPresenter`; `PivotItem` is a `Grid` holding a `ContentPresenter` | MSDN `ff941097` — Pivot control architecture for Windows Phone 8 |
| `PivotHeadersControl` names two template parts: `HeaderItemsPresenter` and `Canvas` | MSDN `ff941080` |
| Headers are drawn until they exceed the control's width; **if there are too few to fill it, they do not loop** | MSDN `ff941097` |
| Pages are cyclical — past the last one, the next is the first | MSDN `hh202919` — design guidelines |
| Four pages or fewer | same |
| **The header height is fixed and cannot be changed** | same, verbatim |
| Header text should be one or two words — stated as being *so that the next pane shows*, which is the design intent behind the peek | same |
| Never a Pivot inside a Pivot or inside a Panorama; no panning or scrolling controls inside one | same |

**Metrics and motion — WinJS, primary, and a different implementation.** The WinJS `Pivot` is
Microsoft's own, under MIT, with every number in the open. It is the HTML control of the Windows 8.1
/ WP8.1 generation, **not** the WP8 Silverlight one, so these are a reference point rather than a
transcription of what this library reproduces:

| Fact | Where verified |
|---|---|
| Header strip 48 px; each header 30 px tall with `margin: 12px 12px 0 12px` | `src/less/styles-pivot.less` — `@headersHeight`, `.win-pivot-header` |
| Header type is the ramp's `title` step; unselected headers are `baseMid`, selected `baseHigh`, crossfading over **167 ms** linear | same file, `colors-pivot.less` |
| Pivot title: Segoe UI **bold 15 px**, `margin: 14px 0 13px 24px` | `styles-pivot.less` — `.win-pivot-title` |
| Content padding `0 24px`; the surface is **300 %** of the control's width with the item centred at 100 % | same file |
| Switching a page: outgoing opacity → 0 over **67 ms** linear; incoming opacity over **333 ms** and `translateX(±20px)` → 0 over **767 ms**, both `cubic-bezier(0.1, 0.9, 0.2, 1)` | `src/js/WinJS/Controls/Pivot/_Pivot.ts` |
| The header strip slides over **250 ms**; the newly revealed last header fades in over **167 ms** | same file — `_headerSlideAnimationDuration`, `lastHeaderFadeInDuration` |

**Continuum, in full, for the desktop branch.** The phone's version is unrecoverable (§1.10), and
WinJS's is entirely open. Four directions, each a composition of three independent curves rather than
one movement — which is why it reads as an object continuing rather than a page changing:

| | |
|---|---|
| forward in | page `scale(0.5)` → `1.0` over **350 ms** on `cubic-bezier(0.33, 0.18, 0.11, 1)`; item `translate(0, 225px)` → 0 over 350 on `(0.24, 1.15, 0.11, 1.1575)`; content `rotateX(80°) scale(1.5)` → 0°/1.0 over 350 on `(0, 0.62, 0.8225, 0.9625)` |
| forward out | page `scale(1.0)` → `1.1` over **120 ms**; item `rotateX(0°) scale(1) translate(0,0)` → `rotateX(80°) scale(1.5) translate(0, 150px)` over **152 ms**, both on the exit curve `(0.3825, 0.0025, 0.8775, -0.1075)` |
| backward in | page `scale(1.25)` → `1.0` over **200 ms**; item `rotateX(80°) translate(0, -100px)` → 0 over **250 ms** on `(0.2975, 0.7325, 0.4725, 0.99)` |
| backward out | page `scale(1.0)` → `0.5` over **167 ms** on the exit curve |

`src/js/WinJS/Animations.js`. Two of those overshoot deliberately — the `1.15` and `1.1575` control
points in the forward-in translate, and the negative one in the exit curve — so the item passes its
resting place and comes back. A monotonic approximation of this is the version that looks
mechanical.

**Consequence — the content lags the header, and by how much is now known.** Opacity lands in 333 ms
while the 20 px slide takes 767 ms, so the incoming page is fully opaque long before it stops
moving. That two-rate split is the Pivot's characteristic feel, and it is the sort of thing that
looks like a bug when it is guessed and like the original when it is not.

**Metrics — the UWP XAML Pivot, primary, and the closest relative that still exists.** The
Windows 10 SDK ships the default control templates as plain XAML, and the Pivot is in there whole.
This is the XAML lineage the WP8 control belongs to — WP8 Silverlight → WP8.1 XAML → UWP — so it is
a nearer relative than WinJS, and it has visibly drifted, which is the useful part: knowing *where*
it drifted says which of its numbers are safe to lean on.

| Fact | Where verified |
|---|---|
| `PivotHeaderItem` `Height` = **48**, `Padding` = `PivotHeaderItemMargin` = **12,0,12,0** | `Windows Kits\10\DesignTime\CommonConfiguration\Neutral\UAP\10.0.26100.0\Generic\generic.xaml` |
| Header type: **24** px, **SemiLight**, `CharacterSpacing` **-25** (thousandths of an em) | same — `PivotHeaderItemFontSize`, `PivotHeaderItemThemeFontWeight`, `PivotHeaderItemCharacterSpacing` |
| Pivot title: **14** px, **Bold** | same — `PivotTitleFontSize`, `PivotTitleThemeFontWeight` |
| `PivotItemMargin` **12,0,12,0**; portrait and landscape theme padding both **12,14,0,13** | same |
| Locked state translates the header by **40** and fades it out, both with `Duration="0"` — instantly | same — `PivotHeaderItemLockedTranslation` |
| Nav buttons: **20 × 36**, margin **0,6,0,0**, glyph **12** px, `Opacity` 0 until pointer input | same |
| Unselected header foreground is `BaseMedium`, selected is `AltBaseHigh`; the legacy brushes spell them **`#66FFFFFF`** and **`#FFFFFFFF`** | same |
| Structure: `PivotPanel` › two `PivotHeaderPanel`s (`StaticHeader` and `Header`), each with its own `RenderTransform` | same |

**Correction — these two do not corroborate each other in the way it first appeared.** This section
originally read "one number is now corroborated across two independent implementations: WinJS's
header strip is 48 px and UWP's `PivotHeaderItem` is 48 — two teams, two stacks, one number." They
are not two lineages. WinJS is Windows 8.1's HTML stack and UWP XAML descends from Windows 8's XAML
stack, so both are the **desktop** line; WP8.1's WinRT XAML was that desktop line arriving on the
phone under the "universal apps" convergence. Two members of one family agreeing tells you about
that family. It says nothing about the phone, whose Pivot header is not a 48 px box with 24 px type
at all — it is a `PivotHeaderItem` with padding `21,0,1,0` set in `PhoneFontFamilySemiLight` at the
theme's own size.

**So these numbers are refiled rather than discarded.** They are evidence about the **Windows 8
profile** — the optional branch of [B-22](../backlog/B-22-win8-branch.md) — and they are good
evidence there, because that branch has no numeric source of its own yet. What they are not is a
reference for the phone's Pivot.

**And the drift has a direction.** The UWP template draws a `SelectedPipe`: a 2 px accent rectangle
under the selected header, margin `0,0,0,2`. WP8 had no underline at all — the selected header was
distinguished by opacity alone (§1.11, `Duration="0"`). The underline is where the desktop line went,
not where the phone was.

**The SDK was recovered, and with it the provenance.** Microsoft's own download is dead — the
`WPexpress_full.exe` bootstrapper still runs and its `/layout` mode still resolves its fwlinks, onto
`download.microsoft.com` URLs that all return **404**. The offline ISO survives in the Internet
Archive (item `wpsdk8`, `wpsdkv80_enu1.iso`, volume label `WPSDKV80_ENU1`, malware-checked by an
archive validator in 2021). The archive item is titled "v8.1"; the volume label and the 2012 file
dates say **8.0**, and the label wins.

Everything below was read out of that ISO, which makes each fact re-checkable by anyone with the
same file — the property the whole document is built on:

```
wpsdkv80_enu1.iso
  packages/MobileTools/wpsdkcore/WPSDK_en.cab
      REFASM_DESIGN_MICROSOFT_PHONE_DLL          the control templates
      WPDT_DESIGN_THEMERESOURCES_XAML            the values they reference
```

**The templates were verified by diff, not by trust.** They first came out of an assembly of unknown
origin. The same six templates were extracted from the SDK's own copy and compared: the XAML regions
are **identical, 18,572 characters each**, and `Pivot`, `PivotItem`, `PivotHeaderItem`, `Panorama`,
`PanoramaItem` and `PanningLayer` match one for one. The earlier copy is therefore no longer cited
anywhere and nothing rests on it.

**Pivot — WP8, verbatim from the template and its theme dictionary.**

| Fact | Where verified |
|---|---|
| `Pivot` is a three-row grid: `Auto` title, `Auto` headers, `*` content | `REFASM_DESIGN_MICROSOFT_PHONE_DLL`, `ControlTemplate TargetType="controls:Pivot"` |
| Title `ContentControl`, left-aligned, margin **24,17,0,-7** | same |
| `PivotTitleStyle`: `PhoneFontFamilySemiBold` at `PivotTitleFontSize` = **22.667 px** (`PhoneFontSizeMedium`) | `WPDT_DESIGN_THEMERESOURCES_XAML` |
| Header type: **`PivotHeaderFontSize` = 72 px** in `PhoneFontFamilySemiLight` — the largest step of the ramp, the same value as `PhoneFontSizeExtraExtraLarge` | both files |
| `PivotHeaderItem` padding **21,0,1,0**, margin **0** — the 21 px on the left is the gap between headers | template |
| An unselected header sits at **`PhonePivotUnselectedItemOpacity` = 0.4**; selecting animates opacity to 1 with **`Duration="0"`** — instantly, no crossfade | both files |
| `PivotItemMargin` = **12,28,12,0** | theme dictionary |
| `PivotHeadersControl`'s items panel is a **`Canvas`** — headers are absolutely positioned | template |
| `PivotItem`'s `Left`/`Center`/`Right` visual states are **empty** — markers the code reads, not animations | template |

**Panorama — and the brief was wrong about it.** The brief describes the Panorama title as
"72 px SemiLight". It is neither:

| Fact | Where verified |
|---|---|
| The **panorama title** is `FontSize` **170**, `PhoneFontFamilyLight`, `CharacterSpacing` **-35**, margin **10,-34,0,0** | `ControlTemplate TargetType="controls:Panorama"` |
| The **section header** is `PanoramaItemHeaderFontSize` = **66 px**, `PhoneFontFamilySemiLight`, tracking **-35**, margin **12,-2,0,38**, in a grid at margin **12,0,0,0** | template + theme dictionary |
| The section header carries its own `TranslateTransform` — the parallax is a translate on the header, not on its container | template |
| Wrap-around is structural: `PanningLayer` is a horizontal `StackPanel` with `LeftWraparound` and `RightWraparound` borders either side of the content, under a `TransformGroup` of two translates | template |

**Consequence 1 — 72 px was a conflation, and now both halves are known.** 66 px SemiLight is the
*section* header; 170 px Light is the *panorama* title. Building from the brief's number would have
produced a title less than half the size it should be, in the component whose whole point is a title
too big for the screen.

**Consequence 2 — the peek is not a metric, it is a layout.** Headers live on a `Canvas` at absolute
positions, so "how far the next header peeks" is not a number to recover: it falls out of where the
canvas is scrolled to. What has to be reproduced is the mechanism.

**Consequence 3 — the multiplier is not ours, it is annotated.** See the amendment to
[D5](#d5-metro-pixels-become-dp-and-sp-at-075-everywhere).

**What is still open** is one number and it is not in the theme: the Panorama parallax *coefficient*.
The template gives the transform; the rate it is driven at lives in code.

### 1.12 The right SDK, the wrong file, and a search stopped one step early

The SDK's `System.Windows.dll` contains fifty-three control templates — `CheckBox`, `RadioButton`,
`Slider`, `ProgressBar`, `TextBox` — which is exactly the set the brief marks as ⚠️ *partially
known*. It is the **desktop Silverlight** build: 98 `LinearGradientBrush`, 53 `MouseOver` states, and
zero references to `PhoneAccentBrush`, `PhoneForegroundBrush` or `PhoneBorderThickness`. Its
`CheckBox` sets `Background="#FF448DCA"` under a four-stop grey gradient. Transcribing it would have
produced glossy pre-Metro controls wearing Metro's colours.

**Then the search stopped, and that was the mistake.** Finding the wrong DLL was taken as evidence
that no phone template existed, and three controls were built from "the shape language" instead. The
phone's templates were in the same cabinet, in a **XAML file rather than an assembly**:

| Fact | Where verified |
|---|---|
| `WPDT_DESIGN_SYSTEM_WINDOWS_XAML`, 55 KB, holds the phone's `CheckBox`, `RadioButton`, `Slider`, `ProgressBar` and `TextBox` templates with 22 `Phone*` resource references | `WPSDK_en.cab` |
| `ToggleSwitch` is not there at all — it is the **Toolkit's**, in `Microsoft.Phone.Controls.Toolkit.WP8/Themes/Generic.xaml` | the Windows Phone Toolkit repository |

**What the reconstructions got wrong**, each of which the template settles:

| | reconstructed | actual |
|---|---|---|
| check box, checked | filled with the accent | **transparent**; the tick appears in the **foreground** colour |
| accent's role | the checked state | **neither** — see the correction below |
| the tick | two straight strokes | a **filled path**, `M0,123 L39,93 L124,164 L256,18 L295,49 L124,240`, stretched to 23×21 — its two arms have different weights |
| box and ring | 28 px, guessed | **32 px**, with the same 3 px border as everything else |
| radio dot | half the ring, guessed | **16 px against 32** — half, correct by luck |
| toggle, off | an empty bordered track | the track is **always accent-filled**; a 77×20 rectangle of the page background covers it, and switching on **slides that window away** rather than changing any colour |

**Consequence — a reconstruction can be right about the shape and wrong about the idea.** The box
being transparent rather than accent-filled is not a detail: it means the check mark is the signal
and the accent belongs to touch, which is the same rule the tilt follows. Guessing produced
something that looked plausible in a gallery and wrong to anyone who had used the phone — and it
took a person saying "crooked tick" to send the search back out.

**Consequence 2 — what is still without a reference is now one control, not three.** The `Slider`
template is in that XAML too and has not been read yet; the thumb dimensions remain this project's
and stay parameters until it is.

**Correction — the accent has no part in a press, and this document said it did.** The row above
used to read "the accent's role is the **pressed** state, and only that (`PhoneRadioCheckBoxPressedBrush`)".
That was the brush's *name* being read instead of its value. Resolved against the theme dictionary
it is:

| token | dark | light |
|---|---|---|
| `PhoneRadioCheckBoxColor` | `#BFFFFFFF` | `#26000000` |
| `PhoneRadioCheckBoxPressedColor` | `#FFFFFFFF` | **`#00000000`** |
| `PhoneRadioCheckBoxPressedBorderColor` | `#FFFFFFFF` | `#DE000000` |
| `PhoneRadioCheckBoxCheckColor` | `#FF000000` | `#DE000000` |
| `PhoneRadioCheckBoxDisabledColor` | `#66FFFFFF` | **`#00000000`** |
| `PhoneRadioCheckBoxCheckDisabledColor` | `#66000000` | `#4D000000` |

**The decisive check is cheaper than any of this reasoning: diff two accent dictionaries.** The
twenty `*ThemeResXaml` files differ in exactly one `<Color>` — `PhoneAccentColor`, `#FF1BA1E2` blue
against `#FFE51400` red — and the seventeen control templates are **byte-identical across all
twenty**. So the accent cannot enter a template except through `PhoneAccentBrush`, and every one of
its six appearances is a selection or a progress fill: `ListBoxItem` Selected, `PasswordBox` and
`TextBox` focus, `ProgressBar`, `Slider`. None is a press. *Consequence:* in Metro, **the accent
marks state, and touch is signalled by inversion.** The tilt, the button flooding to foreground and
the check box flooding to white are one rule, and `PhoneRadioCheckBoxPressedBrush` was the only name
that looked like an exception.

*Consequence 2 — light is not dark inverted, again.* Held down, a dark box floods to solid white; a
light box's 15 % fill drops to **nothing** while its border darkens. The rule was already written
down and still nearly got overridden by "the pressed state fills".

**Correction 2 — it was two controls, and the second one nobody thought to count.** The paragraph
above was written while looking at the controls the reconstruction had *known* it was guessing at.
`KvadrantButton` was not on that list, because a bordered rectangle that inverts on press reads as
too simple to have got wrong. It was wrong in four ways, and the file that settles them is the one
already named in the table above:

| | as built | `PhoneButtonBase` |
|---|---|---|
| type | `PhoneFontSizeNormal` 20, Normal | **`PhoneFontSizeMediumLarge` 25.333, `PhoneFontFamilySemiBold`** |
| padding | `18,6` symmetric, invented | **`10,3,10,5`** — three above the line, five below |
| hit area | the frame is the target | the `Border` carries **`Margin="{StaticResource PhoneTouchTargetOverhang}"`** (12) inside a `Grid` with `Background="Transparent"`: twelve pixels of invisible button on every side |
| disabled | did not exist | text and border to `PhoneDisabledBrush`, background forced back to `Transparent` |

**Consequence — "no source" and "source not consulted" look identical from inside the code, and
only one of them is an excuse.** Two of the four numbers above were this project's own and neither
said so, which is the thing [D5](#d5-metro-pixels-become-dp-and-sp-at-075-everywhere) and the KDoc
rule exist to prevent; the rule was followed for every control somebody had flagged as a guess, and
skipped for the one nobody had. The check that would have caught it is not a test — it is asking, of
each component in turn, *which file was open when this was written*.

**Consequence 2 — the file covers seventeen `TargetType`s, and the library has read five.** It
carries `Button`, `ButtonBase`, `CheckBox`, `ContentControl`, `HyperlinkButton`, `ListBox`,
`ListBoxItem`, `PasswordBox`, `ProgressBar`, `RadioButton`, `RepeatButton`, `ScrollBar`,
`ScrollViewer`, `Slider`, `TextBox`, `Thumb` and `ToggleButton`. Every remaining one of those in
this library is currently a reconstruction with a reference sitting unread —
[B-32](../backlog/B-32-read-the-remaining-platform-templates.md).

*Extraction note, because it cost an hour twice:* the cabinets are **LZX**, not MSZIP, so a
hand-rolled `zlib` reader silently produces the right number of wrong bytes — the file sizes match
and the content is noise. `cabextract` reads them; libarchive's `bsdtar` rejects the header outright.

### 1.13 The second renderer cost four compatibility walls and no code

[B-24](../backlog/B-24-add-the-android-target-next.md) added Android because it is the only planned
target that is not skiko. The library code needed nothing: `commonMain` compiled for Android
unchanged, and the 25 common tests passed on it first time. Everything below is toolchain, and it is
written down because each of the four looked like a version problem and only one of them was.

| Fact | Where verified |
|---|---|
| AGP 8.x cannot run on Gradle ≥ 9.6 — it uses `org.gradle.api.problems.internal.InternalProblems`, removed there | Gradle's own error, naming the API and the upgrade note |
| Since AGP 9.0 `com.android.library` and `com.android.application` **refuse** to sit in a Kotlin Multiplatform module | `IllegalStateException` from the plugin, in as many words |
| Since AGP 9.0 the Android plugin brings Kotlin itself, and applying `org.jetbrains.kotlin.android` beside it is a hard error | the Kotlin plugin's own diagnostic, pointing at kotl.in/gradle/agp-built-in-kotlin |
| `androidx.compose.animation:animation-core-android:1.12.0` carries AAR metadata demanding `compileSdk` 37 | eight identical failures from `checkDebugAarMetadata` at 36 |

Gradle 9.7.1 and AGP 9.3.2 are therefore the only pair available, and inside it a KMP library takes
`com.android.kotlin.multiplatform.library` and an application module cannot be multiplatform at all.
That last one is why `:sample` is a library holding the shared demo screen and `:sample-android` is a
thin activity that hosts it: not a design, a constraint.

**The one that was not a version problem.** With AGP declared only in `kvadrant-core`, the Compose
plugin died with `NoClassDefFoundError` on
`com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension` — a class that is
demonstrably present in AGP's `gradle-api` jar, in the same package, in every version from 8.13.2 to
9.3.2. Reading Compose's stack frame (`AndroidResourcesKt.configureAndroidComposeResources`) says
what the version sweep could not: the Compose plugin reads AGP's own extension types, and it can
only see them when both plugins land in the same build classloader. Declaring AGP in the **root**
build file with `apply false`, alongside every other plugin already there, fixes it.

Two hours of that was a version sweep across five AGP releases, all failing identically. The sweep
was not wasted — it is what ruled out a version problem — but it was also not evidence for one, and
after the second identical failure the stack trace was the cheaper question.

**What the move to compose-resources found.** Bundling the fonts for both targets
([B-07](../backlog/B-07-font-stack.md)) meant `Font("fonts/selawk.ttf")` — a JVM classpath idea —
had to become `Font(Res.font.selawik_regular)`. Fifteen of the sixteen font goldens came back
byte-identical. The sixteenth, `font_stack_per_run_source_sans_compensated`, changed completely: its
fixture asked for `fonts/SourceSans3-Light.ttf`, a file that has never existed in this repository, so
it had been drawing a system fallback face under the name of the font it claimed to be comparing.
The golden recorded that fallback and guarded it faithfully for as long as it existed.

The decisive images of the B-03 spike are not affected — the weight and fit groups use the variable
face, which is real — but this was one of the images the font stack was argued from. A screenshot
test fails when the render changes and says nothing at all about whether the render was ever the
thing you named.

---

## 2. Decisions

### D1. The core depends on no Material artefact

Decision: `kvadrant-core` depends on `compose.runtime`, `compose.foundation`, `compose.ui`,
`compose.ui-text` and `components-resources`, and on nothing from Material.

Why:

- §1.2 is the argument in numbers: the Material line under Compose Multiplatform has had **no stable
  release for three minor versions**, and the API the adapter needs most is being reshaped inside
  those alphas. A core that shared that dependency would inherit the whole problem;
- `compose-fluent-ui` shipped a complete CMP design system this way, so the cost is known rather
  than estimated (§1.1);
- Metro needs ~11 outright replacements out of ~22 Material components (§1.3), so what the
  dependency would buy is small;
- the price: a consumer who wants "everything at once" has to add a second artefact. That is bought
  back by the `KvadrantMaterialTheme { }` shortcut in [D2](#d2-interop-is-a-separate-artefact-strategy-b-plus-c).

### D2. Interop is a separate artefact: strategy B, plus C as a utility

Decision: `KvadrantMaterialAdapter { }` in `kvadrant-material-adapter` reads the current
`KvadrantTheme` and raises a `MaterialTheme` derived from it — `surfaceTint = Color.Transparent`
(this is what kills tonal surfaces), all six `Shapes` slots `RectangleShape`, ripple off via
`LocalRippleConfiguration provides null` and `LocalIndication provides TiltIndication`. The reverse
direction, `KvadrantColors.fromMaterial()`, ships as a utility for embedding Kvadrant islands in a
Material application. `AdaptiveWidget(kvadrant = {}, material = {})` follows `compose-cupertino`.

Rejected: strategy A, where `KvadrantTheme` raises `MaterialTheme` itself. It buys the best
ergonomics and costs [D1](#d1-the-core-depends-on-no-material-artefact) — the entire version problem
moves into the core. The ergonomics are recovered instead by a one-call
`KvadrantMaterialTheme { }` shortcut that lives in the adapter.

Known weakness, recorded rather than solved: the reverse adapter takes only the accent from
Material and forces an absolute background, so a Kvadrant island inside a Material screen shows a
visible seam at its edge. That is inherent, not a bug to fix later.

### D3. The adapter's Material version is decided by resolving the graph, not by a range

Brief: `api(compose.material3) { version { strictly("[1.12.0, 1.13.0)") } }`.
Decision: **do not use that snippet.** Pick between the two options in §1.2 by building both and
seeing which resolves and compiles, in [B-04](../backlog/B-04-repository-skeleton.md), and write the
answer into `gradle/libs.versions.toml` with the Jetpack version it maps to in a comment.

Why: on CMP 1.12.0 the snippet resolves into Jetpack M3 1.5.0-alpha22 — the exact line it was
written to keep out, including the ripple API it depends on. A version range cannot express
"the stable Material line", because in this ecosystem the stable Material line and the stable
Compose line are **different version numbers**.

### D4. `Kvadrant` in every identifier; `Metro` stays a word in prose *(deviation from the brief)*

Brief: `MetroTheme`, `metro-core`, `io.metro.theme`.
Decision: `KvadrantTheme`, `KvadrantColors`, `kvadrant-core`, `kvadrant-material-adapter`,
`kvadrant-icons`, `kvadrant-resources`. "Metro" continues to name the design language in
documentation, KDoc and prose, because that is what it is.

Why: Metro is Microsoft's name for the design language and was itself withdrawn as a brand; carrying
it in artefact coordinates and public class names attaches someone else's trademark to every import
statement a consumer writes. Renaming later is a breaking change to every consumer, and this is the
one moment when it is free.

The price: every Kotlin listing in the brief uses the other prefix, so the brief and the code will
not match name for name. That is why this decision is written here rather than left implicit.

### D5. Metro pixels become dp and sp at ×0.75, everywhere

Decision: one multiplier, applied to the whole specification, as described in §1.4.

Rejected: per-platform recalculation, and treating Win8's large-screen grid separately. Three
independent checks landing on the same number is stronger evidence than any per-platform tuning
would be, and a single multiplier is checkable by a test over `metro-tokens.json`.

**Amendment — the multiplier is stated in the source, not derived from it.** This decision was taken
on three independent checks that happened to agree. The WP8 theme dictionary
(`WPDT_DESIGN_THEMERESOURCES_XAML`, §1.11) annotates every size with its point value in an XML
comment, and there are **seventeen** such pairs:

```
<!--54pt-->   <System:Double x:Key="PivotHeaderFontSize">72</System:Double>
<!--15pt-->   <System:Double x:Key="PhoneFontSizeNormal">20</System:Double>
<!--140pt-->  <System:Double x:Key="PhoneFontSizeHuge">186.667</System:Double>
```

Sixteen are exactly 0.7500. The seventeenth, `PanoramaItemHeaderFontSize` 66 px against "50pt", is
0.7576 — because 66 × 0.75 is 49.5 and the comment rounds. So the ratio is not a convention this
project adopted: it is the conversion Microsoft was working in, written down beside the numbers.

Open at the edges: `pageMargin` 12 px → 9 dp and `tileSmall` 99 px → 74.25 dp are not round numbers.
Whether to round is deliberately deferred to the first screenshot comparison rather than settled on
paper.

**Second amendment — the multiplier is right and the result is still too small.** Running the sample
on a desktop window made the point that no screenshot could: Metro's numbers were drawn for a 480 px
phone, and at 0.75 they land on a 360 dp canvas. On anything larger the 9 dp page margin reads as
cramped. The answer is **not** to nudge the margin — it is the same 12 Metro pixels as the tile gap,
and moving one without the other breaks a relationship the design depends on. `KvadrantMetrics` gets
`scaled(factor)` instead: one knob over the whole set, so everything moves together or nothing does.

Two things deliberately do **not** scale with it. The type ramp, because text has its own reason to
be the size it is — the 72 px header is the design rather than a consequence of the canvas, and
scaling both at once is how a faithful layout becomes a merely large one. And the 48 dp touch
minimum, because that is a number of millimetres under a thumb and a bigger window does not make
thumbs bigger.

### D6. The dark background stays absolute `#000000`

Decision: keep it. Rejected: softening to `#0A0A0A` on desktop and web, where an OLED panel is not
doing the work. The absolute black is a load-bearing part of the identity — it is what makes the
tiles read as floating — and a library that softens it by platform ships two different design
languages. A consumer who wants the softer value can pass their own `KvadrantColors`.

### D7. Authentic visuals, always-extended hit areas, opt-in contrast

Decision, as a single cross-cutting policy: the **visual** stays canonical (touch targets 25.5 dp,
subtle text at its original opacity), the **hit area** is always extended to 48 dp with invisible
padding, and higher-contrast variants are **opt-in** (`KvadrantColors.accessible`).

Why: the three places Metro breaks current norms are known and measured (§1.4 sources) — 34 px touch
targets against 48 dp, subtle text at ≈2.8:1 in the light theme against WCAG AA's 4.5:1, and the
`lime`/`amber`/`yellow` accents at ≈2.2:1 with either text colour. A library that silently fixes
these is not the library anyone came for; one that ignores them is unusable in a product that has an
accessibility bar. Splitting visual from hit area gets both, and the split costs nothing because
they are separate properties in Compose.

**Correction, found by writing the test (B-05).** This used to say the contrast test would fail for
`lime`, `amber` and `yellow`. Computed over the whole palette, **nine of the twenty** fall below
WCAG AA at their authentic text colour, and `yellow` is not among them — it is the **best** in the
palette at 12.53:1, because the luminance rule is what flips it to black text. The brief had it
backwards.

| | ratio |
|---|---|
| lime, amber | 2.00, 2.11 |
| pink, teal, **cyan**, green, orange | 2.57 – 2.98 |
| olive, taupe | 3.96, 4.31 |
| the other eleven | 4.53 – 12.53 |

`cyan` is in that list at **2.90:1**, and it is Windows Phone's own default accent. So the
authenticity-versus-accessibility trade is not a footnote about three unusual colours: it is the
palette's normal condition, and the opt-in higher-contrast variant is load-bearing rather than a
courtesy. The test names all nine, so that a tenth joining them is a change somebody made rather
than one nobody noticed —
`kvadrant-core/src/commonTest/kotlin/io/github/youndie/kvadrant/theme/KvadrantColorsTest.kt`.

### D8. Selawik for Latin, Source Sans 3 for Cyrillic, joined per script run

Brief: `Selawik → Inter → Noto Sans → platform default` as a `FontFamily` fallback chain.
Decision: **Selawik** for Latin and **Source Sans 3** for Cyrillic, selected per run of text rather
than by family order, with the Cyrillic run rendered at the **same size** as the Latin and
instanced on the variable `wght` axis at **370**, which is where the two runs stop reading as two
weights (§1.7).

Why not the brief's stack: it does not work. A `FontFamily` list selects weight and style variants
of one family; it is not a glyph-fallback chain, and three stacks differing only in their declared
fallback render byte-identically (§1.7). What fills the gap is the host's font manager, which means
a different typeface per operating system and tofu where the host has nothing — while looking
correct on the machine of whoever wrote it.

Why Source Sans 3 over the others, on measurements in §1.7:

- it is the closest to Selawik of every candidate that has a Light — 0.972× declared x-height
  against Fira's 1.054× and Inter's 1.092× — and in the render it needs **no** size compensation at
  all, so the ramp is not distorted anywhere;
- it is humanist, like Segoe and therefore like Selawik. Inter is a neo-grotesque, and next to
  Selawik's Latin it reads as a second voice rather than the same one;
- Light, Cyrillic and Greek are all present, and it is OFL.

Rejected: **PT Sans**, which matches Selawik exactly (1.000× on both x-height and cap-height) and
ships no weight below Regular — fatal for a design whose signature is a 72 px SemiLight header;
**Inter alone or any single font for everything**, which buys one rhythm at the cost of Segoe's
shapes, and Selawik's whole value is that it has them; **forking Selawik and drawing Cyrillic**,
correct and a font project measured in months; **the system Segoe on Windows only**, kept as a later
improvement rather than the answer.

The price: the library carries its own script segmentation, roughly the fifty lines of
`FontStackScreenshots.kt`'s `mixed()`, and every text style has to go through it. That is the cost
of a UI that looks the same on five targets, and it is paid once.

**A note on how this was arrived at, because it repeats.** Both numbers in this decision were first
derived from the fonts' declared metrics and both were wrong: the size compensation entirely, the
weight by five. Declared metrics are a hypothesis about what a font will draw. The render is the
fact.

### D9. `@Immutable data class` tokens behind `staticCompositionLocalOf`, accessor object only

Decision: `KvadrantColors` / `KvadrantTypography` / `KvadrantMetrics` / `KvadrantMotion` are
`@Immutable data class`es; all four locals are `staticCompositionLocalOf` with working defaults and
are `internal`; the public surface is the `KvadrantTheme.colors` accessor with
`@Composable @ReadOnlyComposable` getters, plus a scoped `KvadrantThemeConfiguration` for
overriding inside a subtree.

Why: the theme changes rarely and is read on every `Text`, which is exactly the shape
`staticCompositionLocalOf` is for; keeping the locals `internal` leaves room to change the mechanism
later without breaking the public API; `compositionLocalOf { error(...) }` — what Metro-Compose
does — turns a missing provider into a crash instead of a sane default.

The price, stated because it will surface: animating the accent through a static local invalidates
the whole subtree every frame. The answer is to animate at the call site
(`animateColorAsState(KvadrantTheme.colors.accent)`), and the fallback if that is not enough is
Material 3's `@Stable class` + `mutableStateOf` + `updateFrom()` pattern.

### D10. Icons: a content slot in v0.1, drawn glyphs later, Segoe never

Decision: `ApplicationBar` and friends take a `content` slot in the first release; a set of ~40
glyphs is drawn from scratch as `ImageVector` in phase 3; no Segoe MDL2 or Segoe Fluent glyph enters
this repository, including test assets.

Why: 40 glyphs in one style on a 26×26 grid inside a 48 dp circle is a designer-week, and it is the
part of projects like this one that is routinely mistaken for a programmer-afternoon. A slot ships
now and costs the consumer one lambda.

### D11. Apache-2.0 for code, SIL OFL 1.1 for the resources module, declared separately

Decision: code Apache-2.0; `kvadrant-resources` carries the bundled OFL fonts with
`META-INF/licenses/OFL.txt` inside the artefact and its own POM licence section. This mirrors what
Metro-Compose (MIT code, OFL font, shipped on Play) and `compose-fluent-ui` (Apache-2.0) already do,
so the pattern has precedent rather than being an opinion.

### D12. The token layer is generated from `references/metro-tokens.json`, not typed

Decision: a small generator emits the Kotlin constants for colours, accents, both type ramps,
metrics, tile sizes and motion curves from the JSON dump; the JSON is vendored into the repository
next to the generator so the build does not reach outside it.

Why: several hundred numbers transcribed by hand is several hundred chances to be wrong in a way no
test would catch, because the test would be typed from the same source by the same person. A
generator makes the JSON the single source, and the confidence marks (✅/🟡/❌) travel into the KDoc
so a consumer can see which numbers are Microsoft's and which are this project's.

### D16. The target is Windows Phone 8, and "8.1" is two different things

Decision: the design this library reproduces is **WP8-era Metro**. Where a WP8.1 artefact is easier
to obtain, its **Silverlight** half is an acceptable source for the same design; its **WinRT XAML**
half is not a source at all, only evidence of drift.

The distinction is not old-versus-new. It is **which lineage**: Windows Phone 8 → 8.1 Silverlight is
the *phone* iteration of Metro, while Windows 8 → 8.1 WinRT XAML → UWP is the *desktop* one, which
reached the phone in 8.1 through the "universal apps" convergence. This library is the phone
iteration. That framing also explains why the Windows 8 profile was always a separate module
([B-22](../backlog/B-22-win8-branch.md)) rather than a flag: it is not a variant of what we are
building, it is the other lineage.

Why it matters more than the version number:

- **WP8.1 Silverlight** is WP8 continued — the same `Microsoft.Phone.dll` controls, the same Pivot
  and Panorama, the same oversized SemiLight headers. For the templates in §1.11, 8.0 and 8.1 are to
  all appearances the same document, and a diff settles it rather than an argument;
- **WP8.1 WinRT XAML** is the desktop stack on phone hardware. Its Pivot is the direct ancestor of
  the UWP template in §1.11: a 24 px header instead of an enormous one, and later an accent
  underline. **Panorama is deprecated there** — Microsoft's own guidance was to use Pivot or Hub
  instead, which is what a desktop control set does to a phone-native idea;
- so choosing "8.1" in the WinRT sense means giving up Panorama, shrinking the headers several-fold
  and stepping onto the road that ends in Fluent — a niche `compose-fluent-ui` already occupies with
  a complete implementation (§1.1). It would mean arriving somewhere already taken, with less;
- the whole token specification is transcribed from WP8 theme resources. Changing target does not
  adjust the foundation, it discards it.

**Verified against 8.1, because "maybe the buttons went coloured there" is a fair question.** They
did not, and the accent has nothing to do with it:

| WinRT 8.x brush | Dark | Light |
|---|---|---|
| `ButtonBackgroundThemeBrush` | Transparent | **`#B3B6B6B6`** |
| `ButtonBorderThemeBrush` | `#FFFFFFFF` | `#33000000` |
| `ButtonPressedBackgroundThemeBrush` | `#FFFFFFFF` | `#FF000000` |
| `ButtonPressedForegroundThemeBrush` | `#FF000000` | `#FFFFFFFF` |

*(`dn518235(v=win.10)`, the archived XAML theme resources reference.)* The press is the same
inversion the phone does — background to foreground, text to background — in both themes and in both
lineages. **The word "accent" does not appear anywhere in that reference**: Windows 8.x XAML had no
user accent brush at all, the personalisation colour was not exposed to app XAML until `SystemAccent*`
in Windows 10, and the twenty phone dictionaries are the only place a Metro accent lives.

What *did* change is smaller and goes the other way: in the **light** theme the resting WinRT button
is **filled** — 70 % grey with a nearly invisible 20 % border — where the phone's is transparent with
a solid one. That is the desktop lineage softening a Metro rule, not adding colour to it, and it is
one more reason [B-22](../backlog/B-22-win8-branch.md) is a separate module rather than a flag.

The price, stated plainly: this library reproduces a design that stopped being current in 2014, and
every later Microsoft artefact will read as "newer" without being the thing being built. That is the
point of the project, and D16 exists so the pull towards the newer artefact is a decision each time
rather than a drift.

**Provenance, and how it gets fixed.** The templates in §1.11 were read out of an assembly of
uncertain origin, which is a weak spot in a document whose value is that its facts are re-checkable.
The fix is mechanical rather than argued: pull the same resource out of the SDK's own copy and
**diff it against what was read**. Matching replaces the citation with the SDK and the question
closes; differing means 8.0 and 8.1 Silverlight are not identical after all — a finding in its own
right, and the tie goes to 8.0.

### D15. The Pivot's numbers are transcription — *superseded, and worth keeping*

**This decision has been overtaken by evidence and is kept as a record of what it cost to find
out.** It read: `KvadrantPivot` exposes the header height, the type styles, the paddings and the
peek as parameters carrying this project's own defaults, each marked in KDoc as an interpretation
rather than a Microsoft number — because the metrics were unrecoverable and a guess dressed as a
transcription is worse than an admitted one.

The metrics were recovered (§1.11). What replaces it:

- the header is **72 px SemiLight**, the title **22.667 px SemiBold**, the header padding
  **21,0,1,0**, the item margin **12,28,12,0**, the unselected opacity **0.4**, and selection is
  instant. These are transcription and their KDoc cites the artefact rather than apologising;
- the **peek** stays out of the parameter list for a different reason than before: it is not a
  number at all. Headers sit on a `Canvas`, so the peek falls out of the scroll position;
- one number keeps the old treatment — the Panorama **parallax coefficient**, which lives in code
  rather than in the theme and has not been read. It ships as a parameter with a KDoc line naming it
  as ours.

The rule the decision was made of survives the decision: **an invented number ships as a parameter
and says so.** It now applies to one value instead of six.

### D13. One gate, and everything that formats runs inside it

Decision: Gradle **9.7.1**, a **Java 25** toolchain, Kotlin 2.4.10, ktlint via the plugin (14.2.0)
running a pinned CLI (**1.8.0**), and the screenshot suite (viddik, §1.9) — all of it inside
`./gradlew check`. Style lives in `.editorconfig`, not in plugin defaults.

Why:

- a lint or a screenshot suite that is a separate command is a command somebody remembers, which
  means it runs on one machine and not on the branch. `viddikVerify` sits outside `check` by default
  and is moved in with `verifyOnCheck`; the gate was then **tested by changing a golden and watching
  it fail**, because a guard nobody has seen bite is a guard nobody knows is wired;
- the ktlint plugin version and the ktlint version are pinned separately on purpose: the plugin is a
  launcher, the CLI is what decides how the code looks, and a plugin bump must not silently restyle
  the repository;
- style in `.editorconfig` rather than in the build script means it shifts when we decide it does,
  not when a plugin is upgraded. Two entries there are not cosmetic: composables are exempt from
  function naming, and generated sources are excluded, because the plugin fixes its file list before
  its own filters would apply.

### D18. Finger-tracking is an opt-in modifier, not a change to the indication

Decision: `Modifier.kvadrantTilt(onClick)` exists beside the indication and tracks the finger;
`TiltIndication` keeps leaning once, at touch-down, and stays the default everywhere.

**Measured before anything was built, because the item required it.** On a 158 px surface the drawn
quad's leading column is 152 px for a centre press and **119** for a corner one, and whole frames
differ by 1 882 pixels of 90 000. So a finger dragged across a tile leaves a fifth of the effect
unused; the answer to "is this visible outside a deliberate drag" is that a deliberate drag is
exactly where it shows, and that it shows plainly.

The modifier is small because it reuses everything: it emits a fresh `PressInteraction.Press` on
every move, and `TiltIndication` already reads a second press as "the finger is now here". No
geometry is duplicated, which was the point — a second copy of the tilt maths would be a second
place for it to drift. It replaces `clickable` rather than joining it, because two sources of `Press`
on one element fight over the same indication.

**It is canon and therefore not behind `remastered`** ([D17](#d17-one-flag-for-everything-the-phone-did-not-do-remastered)):
`TiltEffect.cs` calls `ApplyTiltEffect` from three handlers, so the phone did this and this library
did not.

*Consequence — the residue is physical, not a bug.* A press dragged to the corner does not match one
that started there to the pixel: by the time the second press arrives the surface has already leaned
under the finger, so the coordinate it reports is not quite the one a still finger would give. 425
pixels of 90 000, against 1 914 for the centre press. `TiltFollowsFingerTest` therefore asserts a
ratio rather than a threshold — an absolute bound there would be a number chosen to fit today's
renderer.

### D17. One flag for everything the phone did not do: `remastered`

Decision: `KvadrantTheme(remastered = …)`, **off by default**, is the single switch between "reproduce
Windows Phone" and "a modern application in a Metro skin". It is a theme value rather than a build
flag so that it can differ per subtree and be rendered both ways in one test, and rather than a
per-component parameter because a per-component parameter is a decision each caller makes silently,
which is how a design system ends up with no design.

**Restoring behaviour the original had is not a deviation and is not gated.** Finger-tracking
([B-27](../backlog/B-27-tilt-does-not-follow-the-finger.md)) is canon; animating a press *in* is not.
Blurring the two would empty the flag of meaning, which is why the distinction is stated before the
table rather than after it.

| Behind the flag | Canon it replaces | Source |
|---|---|---|
| The press sinks over 100 ms | The press is applied outright; only the return is animated | `TiltEffect.cs` holds one storyboard, `tiltReturnStoryboard`, with `TiltReturnAnimationDelay` 200 ms and `Duration` 100 ms — there is no press storyboard to have got wrong |

**One row, and the survey that produced it is the more useful half of the item.** Everything else the
code admits to is a *number* nobody published — the panorama's settle, the live tile's interval, the
placeholder's opacity — rather than a behaviour the phone lacked, and a flag cannot help with those:
turning a number off leaves no number. The remaining candidate the backlog named was the 48 dp
minimum touch target, and the claim that it is "on with no way to turn it off" is **false**: it is
`KvadrantMetrics.touchTargetMin`, a field with a documented default, which a caller can set to
`touchTargetVisual` today. [D7](#d7-authentic-visuals-always-extended-hit-areas-opt-in-contrast)
stands: an extended hit area changes no pixel, and a library whose default is hard to tap is a worse
outcome than a fidelity asterisk.

**Consequence — the flag's value is a convention, not a mechanism.** One boolean and one behaviour is
not worth a paragraph on its own; what is worth it is that the next improvement has somewhere to go
and a rule that says it must. That rule is in `CLAUDE.md` and the row above is the shape every
addition to it takes: what changes, what it replaces, and where the canon was read.

### D14. Desktop first; the other targets arrive when something runs on them

Decision: `kvadrant-core` declares `jvm("desktop")` and nothing else for now. Android, iOS and wasm
are added when there is a component worth running on them.

**Amendment — Android is promoted ahead of iOS and wasm** ([B-24](../backlog/B-24-add-the-android-target-next.md)).
The rule stands; the ordering under it does not follow from "how important is the platform" but from
**which renderer can disagree**. Desktop, iOS and wasm are all skiko: adding either of the latter
two corroborates measurements that were already taken through Skia. Android is `RenderNode` and
hwui, and it produced its first contradiction before a single Android build existed (§1.6,
`cameraDistance`). A target that can only agree with you is worth less than one that can catch you.

Why the rule itself: a target added early is a target whose failures nobody looks at, and every one
of them widens the matrix that each change has to survive. Desktop is also the only target a screenshot suite can
cover here (§1.9), so it is where a visual regression is actually caught. The price is that a
platform-specific problem — a wasm 3D-transform limitation, an iOS text metric — is found later
rather than sooner, and the tilt spike ([B-01](../backlog/B-01-spike-tilt-indication.md)) is
deliberately exempt from this decision: it checks all four, because its whole purpose is to find out
where the technique does not work.

---

## 3. Risks and open questions

**Risk 1 — `LayoutModifierNode` inside `IndicationNodeFactory` may not survive contact.**
**Retired on desktop.** It survives: the same press through `Modifier.clickable` and through
`Modifier.indication` renders pixel-identically (§1.6), so tilt can be the default indication and
the designed fallback — an explicit `Modifier.kvadrantTilt()` — is not needed. What is still open is
narrower and is not about the mechanism: the frame budget on a mid-range Android device, and the
other three targets, neither of which exists to test against yet
([B-01](../backlog/B-01-spike-tilt-indication.md)).

**Risk 2 — the Pivot numbers may not be recoverable.** Four routes, in order of cost: decompile
`Microsoft.Phone.dll` from the WP8 SDK; "Edit a Copy" in Blend on a machine that has that SDK;
frame-by-frame analysis of a screen recording from a device or emulator; and the WinJS `Pivot`
sources for semantics, though not for metrics. *Mitigation:* [B-02](../backlog/B-02-spike-pivot-metrics.md)
is time-boxed, and the fallback is to ship this project's own numbers as **parameters of the public
API**, marked in KDoc as an interpretation rather than a specification. Pivot is the component the
library exists for, so it ships either way; what is at stake is whether it is a reproduction or an
homage.

**Risk 3 — Latin and Cyrillic will have different rhythm.** Not a possibility but a certainty under
[D8](#d8-selawik-for-latin-source-sans-3-for-cyrillic-joined-per-script-run): the fallback font is not
metrically compatible with Selawik. *Mitigation:* the decision is made against a screenshot of the
same screen in both languages before any typography code exists, and the type ramp keeps its numbers
from Selawik's metrics so a later fork of Selawik does not move the whole scale.

**Risk 4 — the Material line moves under the adapter.** Measured in §1.2: three CMP minor versions
without a stable Material, Jetpack M3 1.5 at alpha26. *Mitigation:* the core shares none of it
([D1](#d1-the-core-depends-on-no-material-artefact)); the adapter is a separate artefact with its own
version; two adapter branches cost one build file
([D3](#d3-the-adapters-material-version-is-decided-by-resolving-the-graph-not-by-a-range)); every
Material coordinate lives in the version catalog and nowhere else.

**Risk 5 — visual regressions on iOS and wasm will not be caught by the test suite.** §1.9. The
suite covers Android and desktop; the other two targets are covered by a person looking at the
sample gallery. *Mitigation:* the gallery is a release gate rather than a demo, and the gap is
stated in the contributing notes so nobody reads a green suite as full coverage.

**Risk 6 — icons are a designer-week that will be planned as a programmer-afternoon.**
*Mitigation:* [D10](#d10-icons-a-content-slot-in-v01-drawn-glyphs-later-segoe-never) removes them
from the critical path entirely; the slot ships first and the glyphs arrive when they arrive.

**Open question 1 — is the whole Win8 branch part of this library or a separate one?** Windows Phone
and Windows 8 disagree about things as basic as the order of buttons in a dialog, so they cannot
share a theme without one of them being wrong. The current hypothesis is that the Win8 *tokens*
(type ramp, grid, silhouette) live in the core from the start and Win8 *components* do not exist
until there is a reason for them. Settled when the first desktop consumer appears.

**Open question 2 — how long should a live tile hold each face?** Microsoft never specified it and
deliberately never allowed applications to control it (§1.10), so any value is this library's
invention. The proposal is 6000 ms with ±25 % randomisation, because a grid of tiles flipping in
lockstep is the tell of every reimplementation. It ships as a parameter with that default and a KDoc
sentence saying it is not a Microsoft number.

**Open question 3 — does `PhoneBorderColor` really not differ between the themes?** §1.5. Cheap to
settle against a second dump, and it must be settled before the token is frozen, because a wrong
border colour is invisible in review and obvious on screen.

---

## 4. What happens next

The order of work and the acceptance criteria live in [backlog.md](../../backlog.md).

Three spikes come before any estimate is meaningful, and they are the entire content of stage 0:
tilt on `IndicationNodeFactory` ([B-01](../backlog/B-01-spike-tilt-indication.md)), the Pivot metrics
([B-02](../backlog/B-02-spike-pivot-metrics.md)), and the Cyrillic font
([B-03](../backlog/B-03-spike-cyrillic-font.md)). Each of the three can change the architecture, and
two of them can change the shape of the public API.

The fourth thing that has to happen early is not a spike but a build:
[B-04](../backlog/B-04-repository-skeleton.md) settles §1.2 by resolving the dependency graph, which
is the one open question that a spike cannot answer because it is not about behaviour at all.
