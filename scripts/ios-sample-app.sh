#!/usr/bin/env bash
# The demo on an iOS simulator: the same `KvadrantSampleApp` the desktop opens and the Android build
# hosts, on the third renderer.
#
# **No Xcode project.** Kotlin/Native links a Mach-O, and a `.app` for the simulator is a directory
# holding that binary and an `Info.plist` — so the thing that runs is built by the same compiler out
# of the same source set as everything it draws. See `IosEntryPoint.kt` for the three lines of UIKit
# under it.
#
# This is the half of B-07 a person looks at. The half that is asserted is `IosFontStackTest`, which
# Gradle runs on a simulator inside `check` and needs none of this.
#
# Usage:
#   scripts/ios-sample-app.sh
#   KVADRANT_SIMULATOR="iPhone 17" scripts/ios-sample-app.sh
set -euo pipefail

cd "$(dirname "$0")/.."

BUNDLE_ID="io.github.youndie.kvadrant.sample"
DEVICE="${KVADRANT_SIMULATOR:-$(xcrun simctl list devices available -j |
  python3 -c 'import json,sys; d=json.load(sys.stdin)["devices"];
print(next(x["udid"] for k,v in d.items() if "iOS" in k for x in v))')}"

./gradlew :sample:linkDebugExecutableIosSimulatorArm64 -q

BIN="sample/build/bin/iosSimulatorArm64/debugExecutable/sample.kexe"
[ -f "$BIN" ] || { echo "no executable at $BIN"; exit 1; }

APP="$(mktemp -d)/KvadrantSample.app"
mkdir -p "$APP"
cp "$BIN" "$APP/KvadrantSample"

# **The fonts, and leaving them out is the defect B-07 is about.** compose-resources on iOS reads
# `compose-resources/composeResources/…` out of the *bundle*, and a hand-assembled `.app` has
# whatever this script puts in it. Without this the demo dies on its first frame with
# `MissingResourceException` naming a path inside the bundle — which is the honest failure, and
# better than Android's, where a missing font is silently substituted and looks like a design
# choice (B-37).
#
# The Gradle-assembled directory rather than `src/`: it is what the library actually publishes, so
# a resource that never made it into the build is missing here too.
RESOURCES="kvadrant-core/build/processedResources/iosSimulatorArm64/main/composeResources"
[ -d "$RESOURCES" ] || { echo "no assembled resources at $RESOURCES"; exit 1; }
mkdir -p "$APP/compose-resources"
cp -R "$RESOURCES" "$APP/compose-resources/"

cat > "$APP/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleExecutable</key><string>KvadrantSample</string>
  <key>CFBundleIdentifier</key><string>$BUNDLE_ID</string>
  <key>CFBundleName</key><string>kvadrant</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleShortVersionString</key><string>1.0</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>LSRequiresIPhoneOS</key><true/>
  <key>UIDeviceFamily</key><array><integer>1</integer></array>
  <key>MinimumOSVersion</key><string>15.0</string>
  <!-- Compose Multiplatform REFUSES TO START WITHOUT THIS and says so by name: without the key it
       throws a sanity check on the main queue, the window never appears, and the application sits
       alive on the springboard with nothing on screen. Xcode's own templates carry it, so a
       hand-written bundle is the one place it goes missing. -->
  <key>CADisableMinimumFrameDurationOnPhone</key><true/>
  <!-- WITHOUT THIS THE APPLICATION IS LETTERBOXED. A bundle with no launch screen tells iOS it was
       built for a legacy screen, so the system runs it in a compatibility canvas: black bands above
       and below and a window smaller than the display. For a library whose subject is a page whose
       background reaches the glass, that is not cosmetic. An EMPTY dictionary is the whole
       declaration — it says "this application supports whatever screen it is given". -->
  <key>UILaunchScreen</key><dict/>
</dict>
</plist>
PLIST

xcrun simctl boot "$DEVICE" 2>/dev/null || true
xcrun simctl bootstatus "$DEVICE" -b >/dev/null 2>&1 || true
xcrun simctl install "$DEVICE" "$APP"
xcrun simctl launch --terminate-running-process "$DEVICE" "$BUNDLE_ID"
