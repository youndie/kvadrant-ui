# Changelog

## 0.1.0 — 2026-08-30

**The first version of this that exists anywhere.** Nothing had been published before it, not even a
snapshot — see [B-46](docs/backlog/B-46-the-first-release.md), which also says what spending the
number costs.

Fifty-one public composables in two artefacts:

| | |
|---|---|
| `io.github.youndie:kvadrant-core` | The theme, the tilt every surface inherits, the Pivot and the Panorama, the Start-screen tiles including the live ones, ten base controls, the pickers, the date and time pickers, the application bar, the page transitions, the overscroll, and forty drawn icons. |
| `io.github.youndie:kvadrant-material-adapter` | Optional. Lets the library sit beside `androidx.compose.material3` in both directions. |

Targets: desktop (JVM), Android, wasm, `iosArm64` and `iosSimulatorArm64`.

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
