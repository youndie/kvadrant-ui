# One gate, and CI runs exactly this target.
#
# A local check set that differs from the CI one turns "green here, red there" into the normal
# state of affairs, and people stop reading either. So: whatever is not in `make check` is not a
# gate, and whatever is in it runs the same way in both places.

PY ?= python3

.PHONY: check gate report fix help

help:
	@echo "make check   - the gate: blocking checks, exactly what CI runs"
	@echo "make report  - non-blocking reports: BDD coverage, code anchors"
	@echo "make fix     - regenerate the backlog index, fill in missing coverage-map lines"

check: gate report

gate:
	$(PY) scripts/backlog_index.py --check
	$(PY) scripts/docs_check.py
	$(PY) scripts/coverage_map.py --check

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
