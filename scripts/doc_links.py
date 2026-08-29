#!/usr/bin/env python3
"""
Every link into a document in this repository points at something that is here.

    python3 scripts/doc_links.py

WHY THIS EXISTS. `doc_images.py` next door does this for images and stops at images, and
`backlog_index.py` does it for backlog items and stops at the backlog. Between the two of them
nothing looked at the links out of KDoc — and **all sixteen of them were broken**, which is not a
proportion anybody would have guessed. They used four different depths of `../`, none of them the
right one, which is by itself proof enough that no one had ever followed one.

THE RULE, AND WHY IT IS NOT "COUNT THE DOTS PROPERLY". A component's KDoc is published twice: as the
prose on that component's page of the site, where pages are flat at the root of `build/site`, and as
the body of its Dokka entry, which is nested several directories under `api/`. The two outputs are at
different depths and neither ships `docs/` at all, so **there is no relative path that can work in
both** — correcting the count fixes the source tree and leaves the two places a reader actually meets
the text still broken. The canonical GitHub URL works in the source tree, in an IDE, on the site and
in Dokka, at the price of naming a branch.

That price is what this script buys back. A URL under `CANONICAL` is checked by its **path**, against
this working copy, with no network involved: a document renamed without its references fails here
rather than three months later in somebody's browser. `doc_images.py` refuses to check remote images
because a gate that needs the network is a gate people re-run instead of reading; the same rule
applies, and a link to our own repository is not a remote resource in any sense that matters.

WHAT IS NOT CHECKED: any other absolute URL, for exactly that reason. The count is printed so that a
growing pile of unchecked links is at least visible.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CANONICAL = "https://github.com/youndie/kvadrant-ui/blob/main/"

# A file reference into this repository that is *not* the canonical form — a different branch, a
# `tree/` URL, a permalink to a commit. Each of them points at a real file and none of them can be
# checked here, so the rule would be escapable by writing the link slightly differently, which is
# the failure mode of every checker that only looks at one spelling. An issue or pull-request URL
# is not a file reference and is deliberately not matched.
SELF_FILE_URL = re.compile(r"https://github\.com/youndie/kvadrant-ui/(?:blob|tree|raw)/")

# `[text](target)`, and not `![alt](target)` — images are `doc_images.py`'s job, and checking them
# here as well would report every missing picture twice under two different names.
LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)\)")

# A target that wrapped onto the next line, which is what a long URL in a hundred-column comment
# invites. **The first version of this script did not do this and missed a broken link in the very
# commit that added it** — the one file whose author had wrapped the line was invisible, and the
# 404 was found in the built site instead. A checker that only sees the shape it was written
# against says nothing about the shape it was not.
WRAPPED = re.compile(r"\]\(\s*(?:\*|//)?[ \t]*")


def joined(text):
    """One link, however many lines it was written over."""
    return WRAPPED.sub("](", text)

# Templates are copied into other repositories, so their links are relative to a tree that does not
# exist here — `<service-id>.md`, `B-12-some-slug.md`. Excluded as a directory rather than by
# pattern because the frame of reference is what differs, not the spelling; the count of what was
# skipped is printed, so an exclusion that quietly grew to cover the documentation would show.
SKIPPED_DOCUMENTS = (ROOT / "docs" / "templates",)


def documents():
    yield ROOT / "README.md"
    yield ROOT / "CLAUDE.md"
    yield ROOT / "backlog.md"
    yield from sorted((ROOT / "docs").rglob("*.md"))


def sources():
    for module in sorted(ROOT.glob("*/src")):
        yield from sorted(module.rglob("*.kt"))


def check_document(path, problems):
    """A markdown document links relatively, and GitHub renders those correctly. Just resolve them."""
    checked = 0
    for target in LINK.findall(joined(path.read_text())):
        if target.startswith("#"):
            continue
        if target.startswith(CANONICAL):
            target = target[len(CANONICAL) :]
            base = ROOT
        elif SELF_FILE_URL.match(target):
            problems.append(non_canonical(path, target))
            continue
        elif target.startswith(("http://", "https://", "mailto:")):
            continue
        else:
            base = path.parent
        checked += 1
        if not (base / target.split("#")[0]).exists():
            problems.append(f"{path.relative_to(ROOT)} -> {target}")
    return checked


def check_source(path, problems):
    """
    KDoc is stricter: a link to a document has to be the canonical URL, resolving is not enough.

    A relative one that happens to point at a real file still 404s on both of the pages the KDoc is
    published to, and it is the *reachable* form that is worth having. So the shape is checked before
    the target, and the message says what to write instead — an error that only says "broken" invites
    somebody to fix the dots and reintroduce the same defect in a form the checker then accepts.
    """
    checked = 0
    for target in LINK.findall(joined(path.read_text())):
        # Square brackets in KDoc are usually a code reference — `[KvadrantTile]` — and only the ones
        # followed by a parenthesised target are links at all, which is what the pattern requires.
        if target.startswith("#"):
            continue
        if target.startswith(CANONICAL):
            checked += 1
            if not (ROOT / target[len(CANONICAL) :].split("#")[0]).exists():
                problems.append(f"{path.relative_to(ROOT)} -> {target}")
        elif SELF_FILE_URL.match(target):
            checked += 1
            problems.append(non_canonical(path, target))
        elif target.startswith(("http://", "https://")):
            continue
        else:
            checked += 1
            problems.append(
                f"{path.relative_to(ROOT)} -> {target}\n"
                f"      a relative link from KDoc 404s on the site and in Dokka whatever its depth; "
                f"write {CANONICAL}{suggest(path, target)}"
            )
    return checked


def non_canonical(path, target):
    return (
        f"{path.relative_to(ROOT)} -> {target}\n"
        f"      a file in this repository, written in a form this script cannot check; "
        f"start it with {CANONICAL}"
    )


def suggest(path, target):
    """
    The path from the root that the author meant, for the error message.

    Resolving the link is the obvious way and it is the one that does not work: these links are
    broken *because* the depth is wrong, so what they resolve to is a directory that does not exist
    and the suggestion comes out as a shrug. Two guesses that do work, in order: strip the leading
    `../` and see whether what is left is a real path from the root, which is the case for every
    wrong-depth link; failing that, resolve it properly, which covers a link that is broken for some
    other reason.
    """
    without_dots = target.split("#")[0].lstrip("./").replace("../", "")
    if (ROOT / without_dots).exists():
        return without_dots
    resolved = (path.parent / target.split("#")[0]).resolve()
    if resolved.is_relative_to(ROOT) and resolved.exists():
        return str(resolved.relative_to(ROOT))
    return "<path from the repository root>"


def main():
    problems = []
    checked = 0
    skipped_documents = 0
    for document in documents():
        if any(document.is_relative_to(skip) for skip in SKIPPED_DOCUMENTS):
            skipped_documents += 1
            continue
        checked += check_document(document, problems)
    for source in sources():
        checked += check_source(source, problems)

    if problems:
        sys.exit("links that do not resolve:\n  " + "\n  ".join(sorted(problems)))
    print(
        f"{checked} links into this repository, all present"
        f" ({skipped_documents} template documents skipped)"
    )


if __name__ == "__main__":
    main()
