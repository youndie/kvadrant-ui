#!/usr/bin/env python3
"""
Every image a document points at exists.

    python3 scripts/doc_images.py

WHY THIS EXISTS. The README shows four pictures, and they are not marketing renders — they are
goldens out of the screenshot suite, referenced by path. That buys something real: a component whose
appearance changes fails `viddikVerify`, re-recording rewrites the PNG, and the README is current
without anybody remembering it. It also opens a hole in the other direction. Rename or delete a
golden and the README keeps the path, GitHub renders a broken image, and nothing in either gate
notices — the screenshot suite guards goldens against *fixtures*, not against documents.

Thirty lines closes it, and it closes it for every document rather than for the four images that
prompted it: a diagram moved out of `docs/`, a screenshot deleted with the feature it showed.

WHAT IS NOT CHECKED: remote images and links of any kind. A URL needs the network, the network makes
a gate flaky, and a flaky gate is one people learn to re-run rather than read.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# `![alt](path)` and `<img src="path">`. Both forms are in this tree — markdown in the documents,
# HTML in the README where the pictures need sizing.
PATTERNS = (
    re.compile(r"!\[[^\]]*\]\(([^)\s]+)"),
    re.compile(r"<img[^>]*\ssrc=\"([^\"]+)\""),
)


def documents():
    yield ROOT / "README.md"
    yield from sorted((ROOT / "docs").rglob("*.md"))


def main():
    missing = []
    checked = 0
    for document in documents():
        text = document.read_text()
        for pattern in PATTERNS:
            for reference in pattern.findall(text):
                if reference.startswith(("http://", "https://", "data:", "#")):
                    continue
                target = (document.parent / reference.split("#")[0]).resolve()
                checked += 1
                if not target.is_file():
                    missing.append(f"{document.relative_to(ROOT)} -> {reference}")

    if missing:
        sys.exit(
            "images referenced by a document but not on disk:\n  " + "\n  ".join(missing)
        )
    print(f"{checked} local images, all present")


if __name__ == "__main__":
    main()
