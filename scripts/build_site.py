#!/usr/bin/env python3
"""
Builds the documentation site: a page per component, with that component running in it.

    ./gradlew :kvadrant-previews:wasmJsBrowserDistribution :kvadrant-previews:previewIndex
    python3 scripts/build_site.py                  # -> build/site
    python3 scripts/build_site.py --out somewhere

WHY A RUNNING COMPONENT AND NOT A SCREENSHOT. Every claim this library makes is about how something
looks *and moves*: a tilt that follows a finger, a dot that switches off the instant it lands, a
picker page tipping in from -50 degrees, a panorama that wraps. None of those survives a still, and
a recorded GIF of a component that has since changed looks exactly like a GIF of one that has not.
A running build is wrong only when the library is.

ONE BUNDLE, MANY MOUNTS. The wasm distribution is fourteen megabytes, so every page loads the same
one and marks where a component goes with `data-kvadrant-preview="<id>"`. The bundle's entry point
reads that attribute and mounts the preview registered under it -- see
`kvadrant-previews/src/wasmJsMain`. A binary per component would be that fourteen megabytes once per
page in the best case and once per component in the worst.

THE PAGES ARE FLAT, all at the site root, and that is not a style. Compose's resource loader fetches
`composeResources/...` relative to the *document*, so a page one directory down asks for fonts that
are not there, gets a 404, and silently falls back to a system face -- a site about a typeface,
rendered in the wrong one, with nothing on screen saying so.

THE DEMO is the sample application, the same one `:sample:run` opens on the desktop and
`:sample-android:installDebug` puts on a phone, copied in whole under `demo/` and framed by
`demo.html`. It answers a different question from the component pages: those show what one control
looks like, and this shows whether a screen made of them holds together. It lives in its own
directory rather than flat with the rest precisely because it is self-contained — its bundle and its
`composeResources` travel together, so the loader resolves them relative to `demo/index.html` and
the flat-pages rule below does not apply to it.

THE API REFERENCE is Dokka's, copied in under `api/` and linked per component. The link is emitted
only when the file it points at is actually on disk: Dokka's URLs are derived from the declaration's
name by a rule ("every capital becomes a dash and a lower-case letter"), and a rule applied blindly
produces a link that looks right and 404s. The count of components whose page could not be found is
printed, so a change in that rule is a number rather than a silence.

WHAT IS GENERATED AND WHAT IS NOT. The prose on a component's page is its own KDoc, extracted here
rather than written twice. That KDoc is where the transcription lives -- which Microsoft template a
number came from, and which numbers came from nowhere -- and it is the half of this documentation
that no other Metro library can show. Prose written beside it would be a second copy that nothing
checks.
"""
import argparse
import html
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CORE = ROOT / "kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant"
DIST = ROOT / "kvadrant-previews/build/dist/wasmJs/productionExecutable"
DOKKA = ROOT / "build/dokka/html"
DEMO = ROOT / "sample/build/dist/wasmJs/productionExecutable"
INDEX = ROOT / "kvadrant-previews/build/preview-index.json"
REPO = "https://github.com/youndie/kvadrant-ui"

# The order of the site's index. Named here rather than sorted, because "foundation, then controls,
# then the things built out of them" is how a component library is read and alphabetical is not.
GROUP_ORDER = ["foundation", "controls", "lists and pickers", "tiles", "navigation", "motion"]

DECLARATION = re.compile(
    r"^public\s+(?:inline\s+)?(?:fun|object|data class|class)\s*(?:<[^>]*>\s*)?"
    r"(?:(?P<receiver>[A-Za-z_][\w.]*)\.)?(?P<name>[A-Za-z_]\w*)\b"
)


def fail(message):
    sys.exit(f"build_site: {message}")


# --------------------------------------------------------------------------------------------
# Reading the sources


def kdoc_blocks():
    """
    Every public declaration in the core, with the KDoc above it and the file it is in.

    Keyed by name. Overloads collapse onto the first, which is the same choice the catalogue makes
    and for the same reason: two entries under one name read as two components.
    """
    blocks = {}
    for path in sorted(CORE.rglob("*.kt")):
        lines = path.read_text().splitlines()
        for index, line in enumerate(lines):
            match = DECLARATION.match(line)
            if not match:
                continue
            name = match.group("name")
            if name in blocks:
                continue
            blocks[name] = {
                "path": str(path.relative_to(ROOT)),
                "kdoc": kdoc_above(lines, index),
            }
    return blocks


def kdoc_above(lines, index):
    """The KDoc block above a declaration, as a list of stripped lines. Empty when there is none."""
    cursor = index - 1
    while cursor >= 0 and lines[cursor].lstrip().startswith("@"):
        cursor -= 1
    if cursor < 0 or not lines[cursor].strip().endswith("*/"):
        return []
    end = cursor
    start = end
    while start >= 0 and not lines[start].lstrip().startswith("/**"):
        start -= 1
    if start < 0:
        return []
    body = []
    for line in lines[start:end + 1]:
        text = line.strip()
        text = re.sub(r"^/\*\*", "", text)
        text = re.sub(r"\*/$", "", text)
        text = re.sub(r"^\*\s?", "", text)
        body.append(text.rstrip())
    return body


# --------------------------------------------------------------------------------------------
# KDoc -> HTML


def inline(text):
    """
    KDoc's inline markup, in the order that matters.

    Escaping happens first and everything after it emits tags, so a component whose KDoc contains a
    less-than sign does not silently open an element. `[Foo]` is a reference to a declaration and
    becomes code rather than a link: linking it would need an API site that does not exist yet, and
    a dead link is a worse answer than none.
    """
    text = html.escape(text)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', text)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"(?<![*\w])\*([^*\n]+)\*(?!\w)", r"<em>\1</em>", text)
    text = re.sub(r"\[([A-Za-z_][\w.]*)\]", r"<code>\1</code>", text)
    return text


def render_kdoc(block):
    """
    Paragraphs, bullet lists, and the `@param` tags as a table.

    Tags are separated out rather than dropped: `@param` on these components carries the argument
    that a number is not Microsoft's, and that is precisely the sentence worth publishing.
    """
    paragraphs = []
    params = []
    buffer = []
    bullets = []
    tag = None

    def flush():
        nonlocal buffer, bullets
        if bullets:
            items = "".join(f"<li>{inline(item)}</li>" for item in bullets)
            paragraphs.append(f"<ul>{items}</ul>")
            bullets = []
        if buffer:
            paragraphs.append(f"<p>{inline(' '.join(buffer))}</p>")
            buffer = []

    for line in block:
        stripped = line.strip()
        tag_match = re.match(r"@(param|property|see|throws)\s+(\S+)\s*(.*)", stripped)
        if tag_match:
            flush()
            tag = [tag_match.group(2), tag_match.group(3)]
            params.append(tag)
            continue
        if tag is not None:
            if stripped:
                tag[1] += " " + stripped
                continue
            tag = None
            continue
        if not stripped:
            flush()
            continue
        bullet = re.match(r"[-*]\s+(.*)", stripped)
        if bullet:
            if buffer:
                flush()
            bullets.append(bullet.group(1))
            continue
        if bullets:
            bullets[-1] += " " + stripped
            continue
        buffer.append(stripped)
    flush()

    body = "\n".join(paragraphs)
    if params:
        rows = "".join(
            f"<tr><td><code>{html.escape(name)}</code></td><td>{inline(text)}</td></tr>"
            for name, text in params
        )
        body += f'\n<table class="params"><tbody>{rows}</tbody></table>'
    return body


# --------------------------------------------------------------------------------------------
# The pages


def slug(component):
    return component.lower()


def dokka_link(component, source_path, out):
    """
    Dokka's page for a declaration, or None when there is not one on disk.

    The rule is Dokka's: every capital becomes a dash followed by its lower-case form, so
    `KvadrantButton` is `-kvadrant-button.html`. It is checked against the filesystem rather than
    trusted, because a wrong link here is indistinguishable from a right one until somebody clicks
    it.
    """
    parts = Path(source_path).parts
    module = parts[0]
    package = ".".join(parts[parts.index("kotlin") + 1:-1])
    name = re.sub(r"[A-Z]", lambda m: "-" + m.group(0).lower(), component)
    # A function is a file; an object, a class or a data class is a directory with an index in it.
    # Both forms are tried, because "no page for KvadrantAccents" was the first version's answer and
    # it was wrong — the page existed, under the other shape.
    for relative in (
        f"api/{module}/{package}/{name}.html",
        f"api/{module}/{package}/{name}/index.html",
    ):
        if (out / relative).is_file():
            return relative
    return None


def page(title, subtitle, body, depth_note=""):
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)} — Kvadrant UI</title>
<link rel="stylesheet" href="style.css">
</head>
<body>
<header class="chrome">
  <a class="app-title" href="index.html">KVADRANT UI</a>
  <a class="api-link" href="api/index.html">api reference</a>
  <a class="api-link" href="demo.html">demo</a>
  <h1 class="page-title">{html.escape(title)}</h1>
  {f'<p class="subtitle">{subtitle}</p>' if subtitle else ''}
</header>
<main>
{body}
</main>
<footer>
  <p>Metro for Compose Multiplatform. <a href="{REPO}">source</a> ·
     Apache-2.0, fonts under SIL OFL 1.1 · set in Selawik, the same face the library bundles.</p>
  {depth_note}
</footer>
<script src="previews.js"></script>
</body>
</html>
"""


def preview_block(preview):
    """One preview, in both palettes side by side."""
    frames = "".join(
        f"""
      <figure class="frame">
        <figcaption>{palette}</figcaption>
        <div class="canvas" style="height: {preview['heightDp']}px"
             data-kvadrant-preview="{html.escape(preview['id'])}"
             data-kvadrant-theme="{palette}"></div>
      </figure>"""
        for palette in ("dark", "light")
    )
    return f"""
  <section class="preview">
    <h3>{html.escape(preview['summary'])}</h3>
    <div class="frames">{frames}
    </div>
    <p class="mounted-as">mounted as <code>{html.escape(preview['id'])}</code></p>
  </section>"""


def component_page(component, previews, block, api):
    prose = render_kdoc(block["kdoc"]) if block else ""
    if not prose:
        prose = (
            "<p class=\"gap\">This component has no KDoc yet. That is a gap in the library rather "
            "than in this page — the transcription notes are supposed to live beside the code.</p>"
        )
    source = f'{REPO}/blob/main/{block["path"]}' if block else REPO
    # The running component comes first and the argument for it second. Somebody arriving here
    # wants to see the thing; the transcription is why it looks like that, and it is read after.
    body = [
        "".join(preview_block(preview) for preview in previews),
        '<h2 class="why">why it looks like this</h2>',
        '<section class="prose">',
        prose,
        "</section>",
        f'<p class="source">Read it: <a href="{source}">{html.escape(block["path"] if block else "")}</a>'
        + (f' · <a href="{api}">the full signature and every parameter</a>' if api else "")
        + "</p>",
    ]
    groups = sorted({preview["group"] for preview in previews})
    return page(component, " · ".join(groups), "\n".join(body))


def demo_page():
    """
    The sample application, framed.

    An iframe rather than a link straight into `demo/`, because that page is a full-screen canvas
    with nothing on it but the application — a visitor who lands there has no way back and no
    indication of what they are looking at. The frame carries the page chrome; the application
    inside it is untouched.
    """
    body = """
  <section class="prose">
    <p>The sample application, running. It is the same screen
    <code>./gradlew :sample:run</code> opens on the desktop and
    <code>:sample-android:installDebug</code> puts on a phone — one source, three renderers, so a
    browser cannot show you a version of this library the other two do not have.</p>
    <p>Swipe the pivot sideways. Press a tile and drag your finger across it: the lean follows, and
    it does not steal the scroll from the list underneath. Open the picker; tap the ellipsis on the
    application bar.</p>
  </section>
  <div class="demo-frame">
    <iframe src="demo/index.html" title="the Kvadrant UI sample application"></iframe>
  </div>
  <p class="source">Built from
  <a href="%s/tree/main/sample/src/commonMain">sample/src/commonMain</a>. Sized to a Lumia: the
  metrics scale to whatever width they are given, and this is the width they were designed at.</p>""" % REPO
    return page("the demo", "one application, three renderers", body)


def index_page(previews, blocks):
    by_group = {}
    for preview in previews:
        by_group.setdefault(preview["group"], []).append(preview)

    unknown = [group for group in by_group if group not in GROUP_ORDER]
    if unknown:
        # Silently appending it would put a whole section at the bottom of the page where nobody
        # looks, which is the same as losing it.
        fail(f"previews are in groups the site has no order for: {unknown}. Add them to GROUP_ORDER.")

    sections = []
    for group in GROUP_ORDER:
        entries = by_group.get(group)
        if not entries:
            continue
        seen = []
        cards = []
        for preview in entries:
            if preview["component"] in seen:
                continue
            seen.append(preview["component"])
            summary = next(p["summary"] for p in entries if p["component"] == preview["component"])
            cards.append(
                f"""
      <a class="card" href="{slug(preview['component'])}.html">
        <span class="card-name">{html.escape(preview['component'])}</span>
        <span class="card-summary">{html.escape(summary)}</span>
      </a>"""
            )
        sections.append(
            f"""
  <section class="group">
    <h2>{html.escape(group)}</h2>
    <div class="cards">{''.join(cards)}
    </div>
  </section>"""
        )

    intro = """
  <section class="prose">
    <p>A component library for <strong>Compose Multiplatform</strong> in the Metro design language —
    Windows Phone 8 and Windows 8 — with an optional adapter that lets it sit beside
    <code>androidx.compose.material3</code>.</p>
    <p>Every component below was built from a document rather than from a screenshot: the Windows
    Phone SDK's own template dictionary, its theme resources, and Microsoft's published UX
    guidelines. Where no public source exists the number is named as such in the component's KDoc
    and ships as a parameter rather than a constant. Anything the phone did not do is behind
    <code>KvadrantTheme(remastered = true)</code>, which is off by default — that is what keeps the
    fidelity claim falsifiable.</p>
    <p><strong>Press things.</strong> Each page below runs the component itself, compiled to
    WebAssembly from the same sources the library publishes, in both palettes at once. The light
    theme is not an inversion of the dark one, and putting them side by side is the quickest way to
    see that.</p>
    <p class="elsewhere"><a href="demo.html">the sample application, running</a> ·
    <a href="api/index.html">the API reference</a> ·
    <a href="%s">source</a> ·
    <a href="%s/blob/main/docs/components.md">the catalogue, including what has no preview</a> ·
    <a href="%s/blob/main/docs/research/research-architecture.md">why the architecture is this</a></p>
  </section>""" % (REPO, REPO, REPO)
    return page("components", "", intro + "".join(sections))


STYLE = """
/* Set in Selawik — Microsoft's own metric-compatible stand-in for Segoe UI, and the face the
   library bundles. It is served out of the wasm distribution's resources rather than copied, so the
   site cannot drift from what the components are drawn in. */
@font-face {
  font-family: "Selawik";
  font-weight: 300;
  src: url("composeResources/io.github.youndie.kvadrant.resources/font/selawik_semilight.ttf");
}
@font-face {
  font-family: "Selawik";
  font-weight: 400;
  src: url("composeResources/io.github.youndie.kvadrant.resources/font/selawik_regular.ttf");
}
@font-face {
  font-family: "Selawik";
  font-weight: 600;
  src: url("composeResources/io.github.youndie.kvadrant.resources/font/selawik_semibold.ttf");
}

:root {
  --background: #1f1f1f;
  --foreground: #ffffff;
  --subtle: rgba(255, 255, 255, 0.55);
  --chrome: #1f1f1f;
  --accent: #1ba1e2;
  --line: rgba(255, 255, 255, 0.18);
}

* { box-sizing: border-box; }

body {
  margin: 0;
  padding: 0 24px 64px;
  background: var(--background);
  color: var(--foreground);
  font-family: "Selawik", "Segoe UI", system-ui, sans-serif;
  font-weight: 400;
  font-size: 15px;
  line-height: 1.5;
  max-width: 1100px;
}

a { color: var(--accent); text-decoration: none; }
a:hover { text-decoration: underline; }
code { font-family: "SF Mono", Menlo, Consolas, monospace; font-size: 0.92em; color: #d6d6d6; }

/* The page chrome is the phone's: a small upper-case application title, and a page title several
   times its size sitting directly under it with no rule between them. */
.chrome { padding: 32px 0 24px; }
.app-title {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--foreground);
  opacity: 0.75;
}
.page-title {
  margin: 4px 0 0;
  font-size: 42px;
  font-weight: 300;
  line-height: 1.05;
}
.subtitle { margin: 6px 0 0; color: var(--subtle); }
.api-link {
  float: right;
  margin-left: 18px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* The demo is a phone-shaped application and is framed as one. A border rather than a bezel
   drawing: this is a component library's documentation, not a device mock-up. */
.demo-frame { margin: 28px 0; }
.demo-frame iframe {
  width: 400px;
  height: 720px;
  max-width: 100%;
  border: 1px solid var(--line);
  background: #000;
}

.prose { max-width: 68ch; }
.prose p { margin: 0 0 14px; }
.prose ul { margin: 0 0 14px 18px; padding: 0; }
.prose li { margin-bottom: 6px; }
.prose .gap { color: var(--subtle); }

table.params { border-collapse: collapse; margin: 0 0 18px; }
table.params td { border-top: 1px solid var(--line); padding: 8px 14px 8px 0; vertical-align: top; }
table.params td:first-child { white-space: nowrap; }

.group { margin-top: 40px; }
.group h2 { font-size: 24px; font-weight: 300; margin: 0 0 14px; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 2px; }
.card {
  display: block;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--foreground);
}
.card:hover { background: var(--accent); text-decoration: none; }
.card:hover .card-summary { color: rgba(255, 255, 255, 0.9); }
.card-name { display: block; font-size: 17px; }
.card-summary { display: block; margin-top: 4px; font-size: 13px; color: var(--subtle); }

.preview { margin-top: 36px; }
.preview h3 { font-size: 19px; font-weight: 300; margin: 0 0 12px; max-width: 68ch; }
.frames { display: flex; flex-wrap: wrap; gap: 16px; }
.frame { margin: 0; }
.frame figcaption {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--subtle);
  margin-bottom: 6px;
}
/* A fixed width, because a component that reflows with the browser window is a component being
   shown at a size no phone had. 360 is the width the previews are measured at in the render
   guard. */
.canvas { width: 360px; border: 1px solid var(--line); }
.mounted-as { font-size: 12px; color: var(--subtle); }
.why { font-size: 24px; font-weight: 300; margin: 48px 0 14px; }
/* The path to the source is longer than the column it sits in, and a link that overflows takes the
   page's horizontal scrollbar with it. */
.source { margin-top: 40px; overflow-wrap: anywhere; }

footer {
  margin-top: 64px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  font-size: 13px;
  color: var(--subtle);
}

@media (max-width: 800px) {
  .page-title { font-size: 32px; }
  .canvas { width: 100%; max-width: 360px; }
}
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(ROOT / "build/site"))
    arguments = parser.parse_args()

    if not INDEX.exists():
        fail(f"{INDEX.relative_to(ROOT)} is absent. Run ./gradlew :kvadrant-previews:previewIndex")
    if not DIST.exists():
        fail(
            f"{DIST.relative_to(ROOT)} is absent. "
            "Run ./gradlew :kvadrant-previews:wasmJsBrowserDistribution"
        )
    # Required rather than optional. A site that quietly builds without the reference is a site
    # whose every "the full signature" link is missing, and nothing on the page says one should
    # have been there.
    if not DOKKA.is_dir():
        fail(f"{DOKKA.relative_to(ROOT)} is absent. Run ./gradlew dokkaGenerate")
    if not DEMO.is_dir():
        fail(
            f"{DEMO.relative_to(ROOT)} is absent. "
            "Run ./gradlew :sample:wasmJsBrowserDistribution"
        )

    previews = json.loads(INDEX.read_text())
    if not previews:
        fail("the preview registry is empty, so the site would be an index of nothing")
    blocks = kdoc_blocks()

    out = Path(arguments.out)
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)

    # The bundle, its wasm and the fonts. Copied rather than linked: GitHub Pages publishes a
    # directory, and a symlink out of it publishes nothing.
    for item in DIST.iterdir():
        # The distribution's own index.html is the local smoke-test page from
        # `src/wasmJsMain/resources`, and it would overwrite the site's index.
        if item.name in ("index.html", "previews.js.map"):
            continue
        if item.is_dir():
            shutil.copytree(item, out / item.name)
        else:
            shutil.copy2(item, out / item.name)

    shutil.copytree(DOKKA, out / "api")
    # Copied whole, its own `index.html` included: that page is the application, not a stand-in for
    # one, and it is what the iframe in `demo.html` loads.
    shutil.copytree(DEMO, out / "demo", ignore=shutil.ignore_patterns("*.js.map"))

    (out / "style.css").write_text(STYLE)
    (out / "index.html").write_text(index_page(previews, blocks))
    (out / "demo.html").write_text(demo_page())

    by_component = {}
    for preview in previews:
        by_component.setdefault(preview["component"], []).append(preview)

    missing = sorted(name for name in by_component if name not in blocks)
    if missing:
        fail(f"previews name components with no declaration in the core: {missing}")

    unlinked = []
    for component, entries in by_component.items():
        api = dokka_link(component, blocks[component]["path"], out)
        if api is None:
            unlinked.append(component)
        (out / f"{slug(component)}.html").write_text(
            component_page(component, entries, blocks[component], api)
        )

    # GitHub Pages runs Jekyll over what it is given unless told not to, and Jekyll drops every
    # directory whose name starts with an underscore. Nothing here has one today; the file costs a
    # byte and removes a class of failure that shows up as a missing asset in production only.
    (out / ".nojekyll").write_text("")

    print(f"{out}: {len(by_component)} component pages, {len(previews)} previews")
    if unlinked:
        # Not a failure: `kvadrantTilt` is an extension on `Modifier` and Dokka files it under the
        # receiver, and a preview about a palette is not a declaration at all. It is printed because
        # a rule that silently stops matching would otherwise take every link with it.
        print(f"  no API page found for {len(unlinked)}: {', '.join(sorted(unlinked))}")


if __name__ == "__main__":
    main()
