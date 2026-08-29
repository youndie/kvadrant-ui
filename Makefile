# One gate, and CI runs exactly this target.
#
# A local check set that differs from the CI one turns "green here, red there" into the normal
# state of affairs, and people stop reading either. So: whatever is not in `make check` is not a
# gate, and whatever is in it runs the same way in both places.

PY ?= python3

.PHONY: check gate report fix screenshots site help

help:
	@echo "make check   - the gate: blocking checks, exactly what CI runs"
	@echo "make report  - non-blocking reports: BDD coverage, code anchors"
	@echo "make fix     - regenerate the backlog index, fill in missing coverage-map lines"
	@echo "make screenshots - record the suite twice and name any image that moved (B-31)"
	@echo "make site    - build the documentation site into build/site (needs gradle)"

check: gate report

gate:
	$(PY) scripts/backlog_index.py --check
	$(PY) scripts/docs_check.py
	$(PY) scripts/coverage_map.py --check
	$(PY) scripts/component_catalog.py --check
	$(PY) scripts/doc_images.py
	$(PY) scripts/doc_links.py

# Non-blocking, on purpose. bdd_report counts scenarios, and there are none while there is no
# behaviour to describe. code_anchors will report the research anchors as absent for as long as the
# research points at artefacts that live outside this repository - which is by design, and is
# exactly the kind of judgement a machine cannot make.
report:
	$(PY) scripts/bdd_report.py --repos ..
	$(PY) scripts/code_anchors.py --repos ..

fix:
	$(PY) scripts/backlog_index.py
	$(PY) scripts/coverage_map.py --fix

# Deliberately outside `check`. It records the whole suite twice, and a gate that takes a minute to
# say nothing is a gate people stop running. Run it after adding a fixture, and before trusting a
# claim that no golden moved. ROUNDS=5 raises the number of recordings compared.
screenshots:
	$(PY) scripts/screenshot_determinism.py --rounds $(or $(ROUNDS),2)

# The documentation site (B-34): one wasm bundle, a page per component, and the previews mounted
# into it by name. Outside `check` because it builds a wasm distribution, which takes a minute — the
# gate that keeps it honest is `:kvadrant-previews:check`, which fails when a preview stops
# compiling or stops drawing.
site:
	./gradlew :kvadrant-previews:wasmJsBrowserDistribution :kvadrant-previews:previewIndex \
		:sample:wasmJsBrowserDistribution dokkaGenerate
	$(PY) scripts/build_site.py
