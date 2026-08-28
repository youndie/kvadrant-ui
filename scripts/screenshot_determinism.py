#!/usr/bin/env python3
"""Record the screenshot suite twice and report every image that came back different.

B-31: six of the sixty-eight goldens once changed between two recordings of unchanged source, by
about 147 pixels of 280 000 - which is 0.052 %, and viddik's tolerance is 0.05 %. Fixtures sitting
*on* the threshold pass and fail at random, and the first thing anyone learns from that is that a
red screenshot run means nothing.

This is the check the item asked for: a script rather than an eye. It is not part of `make check`,
because it records the suite twice and a gate that takes a minute to say nothing is a gate people
stop running. Run it after adding a fixture, and run it before trusting a "no golden moved".

    python3 scripts/screenshot_determinism.py [--rounds N]

Exit code 1 names the images that moved. The suite's own goldens are left holding the last
recording, which is what `viddikRecord` would have left anyway.
"""
import argparse
import filecmp
import os
import shutil
import subprocess
import sys
import tempfile

# Every module that has a golden set, found rather than listed. The first version named
# `kvadrant-core` and nothing else, so when `kvadrant-previews` grew forty-seven goldens they were
# outside the only check that asks whether a golden is reproducible at all — and nothing would have
# said so.
def suites() -> list[tuple[str, str]]:
    found = []
    for module in sorted(os.listdir(".")):
        snapshots = os.path.join(module, "src/desktopTest/snapshots")
        if os.path.isdir(snapshots):
            found.append((snapshots, f":{module}:viddikRecord"))
    return found


def record(tasks: list[str]) -> None:
    env = dict(os.environ)
    result = subprocess.run(
        ["./gradlew", "--console=plain", "-q", *tasks],
        capture_output=True,
        text=True,
        env=env,
    )
    if result.returncode != 0:
        sys.stderr.write(result.stdout + result.stderr)
        sys.exit("recording failed")


def snapshot_into(directory: str, sources: list[str]) -> None:
    shutil.rmtree(directory, ignore_errors=True)
    os.makedirs(directory)
    for source in sources:
        for name in os.listdir(source):
            if name.endswith(".png"):
                # Prefixed by module, because two suites may name a golden the same thing and the
                # comparison below is by file name.
                shutil.copy2(
                    os.path.join(source, name),
                    os.path.join(directory, f"{source.split('/')[0]}__{name}"),
                )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--rounds", type=int, default=2, help="recordings to compare (at least 2)")
    args = parser.parse_args()
    rounds = max(2, args.rounds)

    found = suites()
    if not found:
        sys.exit("no golden sets found; run from the repository root")
    sources = [s for s, _ in found]
    tasks = [t for _, t in found]
    print(f"comparing {len(found)} suite(s): {', '.join(sources)}")

    with tempfile.TemporaryDirectory() as work:
        reference = os.path.join(work, "reference")
        record(tasks)
        snapshot_into(reference, sources)
        names = sorted(n for n in os.listdir(reference) if n.endswith(".png"))
        if not names:
            sys.exit("no goldens were recorded; the suite may be empty")

        moved: dict[str, int] = {}
        for round_number in range(2, rounds + 1):
            record(tasks)
            current = os.path.join(work, f"round{round_number}")
            snapshot_into(current, sources)
            for name in names:
                a = os.path.join(reference, name)
                b = os.path.join(current, name)
                if not os.path.exists(b) or not filecmp.cmp(a, b, shallow=False):
                    moved[name] = moved.get(name, 0) + 1
            print(f"round {round_number} of {rounds}: {len(moved)} image(s) have moved so far")

    if moved:
        print()
        print("These images are not deterministic; each holds something that animates:")
        for name, count in sorted(moved.items(), key=lambda item: -item[1]):
            print(f"  {name}: differed in {count} of {rounds - 1} comparison(s)")
        print()
        print("Hold the animation still in the fixture rather than widening the tolerance -")
        print("a wider tolerance hides a real regression of the same size. See B-31.")
        return 1

    print(f"{len(names)} goldens, {rounds} recordings, no image moved.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
