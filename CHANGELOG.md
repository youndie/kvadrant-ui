# Changelog

## 0.2.0 — 2026-09-01

**Two breaking changes, both making a theme able to state something it could not.** Neither moves a
pixel by default: every golden but one is byte-identical, and the one that moves does so because the
app bar now scales.

- `KvadrantColors.onAccent` is a constructor parameter instead of a derivation
  ([B-48](docs/backlog/B-48-the-ink-on-an-accent-cannot-be-chosen.md)). The default is unchanged —
  `contrastOn(accent)`, white on cyan, the transcription — so a caller who does not pass it sees
  nothing new. What it adds is the lever an application with a fixed brand hex needs: `accessible()`
  reaches WCAG AA by moving the *accent*, which is no use when the accent is the thing that cannot
  move. The data class gains a parameter, so its component functions renumber and `copy()`'s
  signature changes.
- `KvadrantMetrics` gains the application bar's five measurements
  ([B-49](docs/backlog/B-49-the-app-bar-is-the-one-surface-a-theme-cannot-reach.md)), which lived in
  the component and therefore did not follow `scaled()` — a scaled window grew everything around a
  bar that stayed 54 dp. `KvadrantAppBarGlyphSize` is deprecated in favour of
  `KvadrantTheme.metrics.appBarGlyph`.

The bar's ring is the one number here that is **not** Microsoft's, and its KDoc says so: the WP8
SDK's design assembly holds ten control templates and the ApplicationBar is not one of them, because
on the phone it was drawn by the shell rather than from a template.

## 0.1.0 — 2026-08-30

**The first version of this that exists anywhere.** Nothing had been published before it, not even a
snapshot — see [B-46](docs/backlog/B-46-the-first-release.md), which also says what spending the
number costs.

Fifty-one public composables in two artefacts:

| | |
|---|---|
| `io.github.youndie:kvadrant-core` | The theme, the tilt every surface inherits, the Pivot and the Panorama, the Start-screen tiles including the live ones, ten base controls, the pickers, the date and time pickers, the application bar, the page transitions, the overscroll, and forty drawn icons. |
| `io.github.youndie:kvadrant-material-adapter` | Optional. Lets the library sit beside `androidx.compose.material3` in both directions. |

Targets, and **the two artefacts do not carry the same set**: `kvadrant-core` publishes desktop
(JVM), Android, wasm, `iosArm64` and `iosSimulatorArm64`; `kvadrant-material-adapter` publishes the
first three and **has no iOS variant**. Read out of `~/.m2` after a publish rather than off the build
files — an application on iOS that reaches for the adapter gets a resolution failure, and it should
find that here rather than in its own build log.

Where the numbers come from: colours, the type ramp, tile metrics and motion curves are generated
from a vendored dump of Microsoft's theme resources. Eight metrics have no public source, the
Pivot's among them; each of those ships as a **parameter** and its KDoc says it is ours.

### Known, written down, and not fixed in this version

- The screenshot suite renders Cyrillic differently under FreeType than under macOS
  ([B-35](docs/backlog/B-35-cyrillic-renders-differently-on-linux.md)).
- The tilt gives each surface its own camera where the phone had one camera for the screen
  ([B-26](docs/backlog/B-26-per-layer-camera-versus-a-global-one.md)). The alternative is in the
  library and in the demo's settings, off by default.
- Only the SemiLight weight of the Cyrillic companion is calibrated against the render; the other
  four Metro weights are not ([B-07](docs/backlog/B-07-font-stack.md)).
- `wasmJsBrowserTest` is skipped, which in a green build reads exactly like a pass.
