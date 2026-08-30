#!/usr/bin/env python3
"""
The version the build publishes and the version the README tells people to depend on are the same
one, and they name the repository that will actually hold it.

    python3 scripts/version_guard.py

WHY THIS EXISTS. `version` lives in `gradle.properties` and is written down again in the
README's dependency snippet — three times, counting the two artefacts and the repository URL. That
is the shape a number drifts in: bumping the build is one edit, and the file a reader copies from is
another. The failure is silent in the worst possible way, because the README keeps looking like
instructions and the coordinate it gives simply does not resolve.

AND THE REPOSITORY IS PART OF THE COORDINATE. Reposilite keeps two trees and this project may write to
exactly one of them — `/snapshots` — because that is what its token permits, which was found by having
a publish refused with 403 under `/releases` (B-46). A reader pointed at the other tree gets a 404 and
no explanation, so the URL is held to the one the build actually publishes to.

THE NAME OF THAT TREE IS THE HOST'S, NOT A CLAIM ABOUT THE ARTEFACTS. `0.1.0` is fixed and lives under
`/snapshots`; the check below deliberately does **not** require a `-SNAPSHOT` version there, because
requiring it would encode an inference about the host that the host does not make.

WHAT IS NOT CHECKED: whether the version has actually been published. That needs the network, and a
gate that needs the network is a gate people re-run instead of reading — the same rule `doc_images.py`
applies to remote images. The build's own publish task does that check, at the moment it can act on
the answer.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOST = "https://reposilite.kotlin.website"

# `implementation("io.github.youndie:kvadrant-core:0.1.0")` — the group is matched rather than
# captured, so a snippet for somebody else's artefact is not silently held to our version.
COORDINATE = re.compile(r'io\.github\.youndie:([a-z0-9-]+):([^"\s)]+)')
REPOSITORY = re.compile(re.escape(HOST) + r"/(releases|snapshots)")


def declared():
    """The one place the version is written for the build."""
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("version="):
            return line.split("=", 1)[1].strip()
    sys.exit("gradle.properties declares no version")


def main():
    version = declared()
    expected = "snapshots"
    readme = ROOT / "README.md"
    text = readme.read_text()
    problems = []

    coordinates = COORDINATE.findall(text)
    if not coordinates:
        # The positive control. A regex that matches nothing passes every check it is given, and
        # this one is looking at a file somebody may reasonably restructure.
        problems.append(
            "README.md names no io.github.youndie coordinate at all, so this check verified nothing"
        )
    for artefact, quoted in coordinates:
        if quoted != version:
            problems.append(
                f"README.md offers {artefact}:{quoted} while the build publishes {version}"
            )

    repositories = REPOSITORY.findall(text)
    if not repositories:
        problems.append(f"README.md names no {HOST} repository, so a reader cannot resolve anything")
    for named in repositories:
        if named != expected:
            problems.append(
                f"README.md points at {HOST}/{named}, and this project publishes to /{expected} — "
                "the only tree its token may write to"
            )

    if problems:
        for problem in problems:
            print(f"version_guard: {problem}", file=sys.stderr)
        return 1
    print(f"version_guard: {version} agrees with README.md, published to /{expected}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
