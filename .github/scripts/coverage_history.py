#!/usr/bin/env python3
#
# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Track test coverage over time from JaCoCo-format XML reports.

Two independent test suites are recorded, because they measure different
things and cannot be merged from XML alone:

  unit             Kover reports from ./gradlew koverXmlReportDebug, at
                   <module>/build/reports/kover/reportDebug.xml
  instrumentation  AGP reports from ./gradlew connectedAndroidTest /
                   createDebugCoverageReport, at
                   <module>/build/reports/coverage/androidTest/debug/
                   connected/report.xml

Kover emits JaCoCo's schema, so one parser handles both. Commands:

  summary  print the parsed coverage as JSON (debugging / ad-hoc use)
  append   append one row per module to the append-only history CSV
  render   regenerate the human-readable COVERAGE.md from that CSV
  compare  render a markdown coverage diff against the last recorded entry,
           for posting on a pull request
  merge    re-apply locally added rows onto a refreshed history CSV
  badge    emit shields.io endpoint JSON for the latest recorded total

Standard library only, so it runs on any CI image without extra setup.
"""

from __future__ import annotations

import argparse
import csv
import datetime
import glob
import json
import os
import sys
import xml.etree.ElementTree as ET

# JaCoCo counter types we record. The reports also carry METHOD and CLASS, but
# line and branch coverage are what people actually track; instructions are
# kept because they move more smoothly and make small changes visible.
COUNTERS = ("LINE", "BRANCH", "INSTRUCTION")

# Name used for the synthetic row holding the sum across all modules.
TOTAL = "TOTAL"

# Where each suite leaves its reports, relative to the repo root.
SUITES = {
    "unit": "*/build/reports/kover/reportDebug.xml",
    "instrumentation": "*/build/reports/coverage/androidTest/debug/connected/report.xml",
}

CSV_FIELDS = [
    "timestamp",
    "commit",
    "pr",
    "subject",
    "suite",
    "module",
    "lines_covered",
    "lines_total",
    "line_pct",
    "branches_covered",
    "branches_total",
    "branch_pct",
    "instructions_covered",
    "instructions_total",
    "instruction_pct",
]

# Modules each suite records. None means "every module that produced a report".
MODULE_ALLOWLIST = {
    "unit": None,
    "instrumentation": ("app", "golden-testing", "visual-testing"),
}

DEFAULT_CSV = "coverage/history.csv"
DEFAULT_MARKDOWN = "coverage/COVERAGE.md"

# How many past commits the trend table in COVERAGE.md shows.
TREND_LIMIT = 30


def module_name(report_path: str) -> str:
    """Derive the Gradle module name from a report path.

    Two layouts are supported. In a Gradle build directory the module is the
    path component before ``build``::

        visual-testing/build/reports/kover/reportDebug.xml -> visual-testing

    Instrumentation reports may be staged into a flat directory before being
    uploaded as a CI artifact, because actions/upload-artifact strips the
    common parent when only one file matches. There the file is named after
    its module::

        instrumentation-coverage/app.xml -> app
    """
    path = os.path.normpath(report_path)
    # Reports are normally globbed relative to the repo root. Re-anchor absolute
    # paths so the module name does not absorb the directories above the
    # checkout.
    if os.path.isabs(path):
        relative = os.path.relpath(path, os.getcwd())
        path = relative if not relative.startswith("..") else path

    parts = [p for p in path.split(os.sep) if p not in (".", "")]
    if "build" in parts:
        prefix = parts[: parts.index("build")]
        if not prefix:
            return "unknown"
        # Nested Gradle modules read as "features:foo". An absolute path that
        # could not be re-anchored contributes only its innermost directory.
        return ":".join(prefix) if not os.path.isabs(path) else prefix[-1]
    return os.path.splitext(parts[-1])[0]


def parse_report(report_path: str) -> dict[str, dict[str, int]]:
    """Read the report-level counters from one JaCoCo-format XML report."""
    root = ET.parse(report_path).getroot()
    counters: dict[str, dict[str, int]] = {}
    # findall on the root matches direct children only, which is exactly the
    # report-level total rather than the per-package or per-class counters.
    for counter in root.findall("counter"):
        kind = counter.get("type", "")
        if kind not in COUNTERS:
            continue
        covered = int(counter.get("covered", 0))
        missed = int(counter.get("missed", 0))
        counters[kind] = {"covered": covered, "total": covered + missed}
    for kind in COUNTERS:
        counters.setdefault(kind, {"covered": 0, "total": 0})
    return counters


def collect(
    pattern: str,
    required: bool = True,
    allow: tuple[str, ...] | None = None,
) -> dict[str, dict[str, dict[str, int]]]:
    """Parse every matching report and add a TOTAL across the tracked modules.

    ``allow`` restricts which modules are recorded; ``None`` records all of
    them. TOTAL is summed after filtering, so it reflects only what is tracked.
    """
    reports = sorted(glob.glob(pattern))
    if not reports:
        if required:
            sys.exit(f"No coverage reports matched '{pattern}'.")
        return {}

    modules = {module_name(path): parse_report(path) for path in reports}
    if allow is not None:
        missing = [name for name in allow if name not in modules]
        if missing:
            print(f"Warning: no report found for {', '.join(missing)}", file=sys.stderr)
        modules = {name: c for name, c in modules.items() if name in allow}
        if not modules:
            if required:
                sys.exit(f"No reports matched the tracked modules {list(allow)}.")
            return {}

    total = {kind: {"covered": 0, "total": 0} for kind in COUNTERS}
    for counters in modules.values():
        for kind in COUNTERS:
            total[kind]["covered"] += counters[kind]["covered"]
            total[kind]["total"] += counters[kind]["total"]
    modules[TOTAL] = total
    return modules


def pct(covered: int, total: int) -> float:
    """Coverage percentage, rounded to two decimals. Empty counts as 0%."""
    return round(covered / total * 100, 2) if total else 0.0


def sort_key(module: str) -> tuple[int, str]:
    """Order modules alphabetically but keep TOTAL last."""
    return (1, "") if module == TOTAL else (0, module)


def to_row(module: str, counters: dict[str, dict[str, int]], meta: dict) -> dict:
    line, branch, instr = counters["LINE"], counters["BRANCH"], counters["INSTRUCTION"]
    return {
        "timestamp": meta["timestamp"],
        "commit": meta["commit"],
        "pr": meta["pr"],
        "subject": meta["subject"],
        "suite": meta["suite"],
        "module": module,
        "lines_covered": line["covered"],
        "lines_total": line["total"],
        "line_pct": pct(line["covered"], line["total"]),
        "branches_covered": branch["covered"],
        "branches_total": branch["total"],
        "branch_pct": pct(branch["covered"], branch["total"]),
        "instructions_covered": instr["covered"],
        "instructions_total": instr["total"],
        "instruction_pct": pct(instr["covered"], instr["total"]),
    }


def read_history(csv_path: str) -> list[dict]:
    if not os.path.exists(csv_path):
        return []
    with open(csv_path, newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def latest_entry(rows: list[dict], suite: str) -> dict[str, dict]:
    """The most recently appended commit for one suite, as ``{module: row}``.

    The CSV is append-only, so the last block of rows sharing a commit is the
    newest entry. Walking backwards avoids depending on timestamp parsing.
    """
    rows = [row for row in rows if row.get("suite") == suite]
    if not rows:
        return {}
    newest = rows[-1]["commit"]
    return {row["module"]: row for row in rows if row["commit"] == newest}


def resolve_pattern(args: argparse.Namespace) -> str:
    """The report glob: an explicit --reports wins, else the suite default."""
    return args.reports or SUITES[args.suite]


def resolve_allow(args: argparse.Namespace) -> tuple[str, ...] | None:
    """Tracked modules: --modules overrides, "all" disables filtering."""
    if not args.modules:
        return MODULE_ALLOWLIST.get(args.suite)
    if args.modules.strip().lower() == "all":
        return None
    return tuple(m.strip() for m in args.modules.split(",") if m.strip())


def cmd_summary(args: argparse.Namespace) -> None:
    modules = collect(resolve_pattern(args), allow=resolve_allow(args))
    summary = {
        module: {
            "line_pct": pct(c["LINE"]["covered"], c["LINE"]["total"]),
            "branch_pct": pct(c["BRANCH"]["covered"], c["BRANCH"]["total"]),
            "instruction_pct": pct(
                c["INSTRUCTION"]["covered"], c["INSTRUCTION"]["total"]
            ),
            "counters": c,
        }
        for module, c in sorted(modules.items(), key=lambda kv: sort_key(kv[0]))
    }
    print(json.dumps({"suite": args.suite, "modules": summary}, indent=2))


def cmd_append(args: argparse.Namespace) -> None:
    modules = collect(
        resolve_pattern(args), required=not args.optional, allow=resolve_allow(args)
    )
    if not modules:
        print(f"No {args.suite} reports found; skipping (--optional).")
        return

    meta = {
        "timestamp": datetime.datetime.now(datetime.timezone.utc).strftime(
            "%Y-%m-%dT%H:%M:%SZ"
        ),
        "commit": args.commit[:7],
        "pr": args.pr or "",
        "subject": args.subject or "",
        "suite": args.suite,
    }

    existing = read_history(args.csv)
    # Each suite is appended by a separate step, so dedupe on the pair.
    if any(
        row["commit"] == meta["commit"] and row.get("suite") == args.suite
        for row in existing
    ):
        print(
            f"Commit {meta['commit']} already has {args.suite} coverage recorded; "
            "nothing to append."
        )
        return

    os.makedirs(os.path.dirname(args.csv) or ".", exist_ok=True)
    with open(args.csv, "a", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS, lineterminator="\n")
        if not existing:
            writer.writeheader()
        for module, counters in sorted(modules.items(), key=lambda kv: sort_key(kv[0])):
            writer.writerow(to_row(module, counters, meta))
    print(
        f"Appended {len(modules)} {args.suite} rows for {meta['commit']} to {args.csv}"
    )


def group(count: str | int) -> str:
    """Thousands separators, so large line counts stay readable."""
    return f"{int(count):,}"


def bar(percentage: float, width: int = 20) -> str:
    """A fixed-width text meter, so the table scans at a glance."""
    filled = round(percentage / 100 * width)
    return "█" * filled + "░" * (width - filled)


def render_current(rows: list[dict], suite: str, heading: str, note: str) -> list[str]:
    latest = latest_entry(rows, suite)
    if not latest:
        return [f"### {heading}", "", "_No data recorded yet._", ""]

    head = next(iter(latest.values()))
    lines = [
        f"### {heading}",
        "",
        note,
        "",
        f"Measured at `{head['commit']}`"
        + (f" (#{head['pr']})" if head["pr"] else "")
        + f", recorded {head['timestamp']}.",
        "",
        "| Module | Lines | Line % | | Branches | Branch % |",
        "| --- | ---: | ---: | --- | ---: | ---: |",
    ]
    for module in sorted(latest, key=sort_key):
        row = latest[module]
        name = f"**{module}**" if module == TOTAL else f"`{module}`"
        line_pct = float(row["line_pct"])
        lines.append(
            f"| {name} "
            f"| {group(row['lines_covered'])}/{group(row['lines_total'])} "
            f"| {line_pct:.2f}% "
            f"| `{bar(line_pct)}` "
            f"| {group(row['branches_covered'])}/{group(row['branches_total'])} "
            f"| {float(row['branch_pct']):.2f}% |"
        )
    lines.append("")
    return lines


def row_key(row: dict) -> tuple[str, str, str]:
    """Identity of a history row: one module, one suite, one commit."""
    return (row.get("commit", ""), row.get("suite", ""), row.get("module", ""))


def cmd_merge(args: argparse.Namespace) -> None:
    """Re-apply locally added rows on top of a refreshed history file.

    Two runs appending to the end of the CSV cannot be merged by git: both
    touch the same region and the rebase conflicts. The rows are independent
    records, though, so the correct resolution is simply "keep both". This
    reads the rows we added, rebases them onto whatever is now on the branch,
    and drops any that are already there.
    """
    base = read_history(args.csv)
    ours = read_history(args.ours)

    present = {row_key(row) for row in base}
    added = [row for row in ours if row_key(row) not in present]
    if not added:
        print("No local rows to re-apply.")
        return

    with open(args.csv, "a", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS, lineterminator="\n")
        if not base:
            writer.writeheader()
        writer.writerows(added)
    print(f"Re-applied {len(added)} row(s) onto {args.csv}")


def generate_svg_graph(order: list[str], rows: list[dict], out_path: str) -> None:
    """Generate a responsive SVG line chart of total line coverage across commits."""
    if not order:
        return
    recent = order[-25:]
    if len(recent) < 2:
        return

    def total_pct(commit: str, suite: str) -> float | None:
        for r in rows:
            if r.get("commit") == commit and r.get("suite") == suite and r.get("module") == TOTAL:
                try:
                    return float(r["line_pct"])
                except (ValueError, KeyError):
                    return None
        return None

    w, h = 800, 280
    ml, mr, mt, mb = 55, 35, 45, 45
    gw = w - ml - mr
    gh = h - mt - mb

    def y_coord(pct: float) -> float:
        return mt + gh - (pct / 100.0) * gh

    n = len(recent)
    xs = [ml + (i * gw / (n - 1) if n > 1 else gw / 2) for i in range(n)]

    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="100%" height="{h}">',
        '  <style>',
        '    .title { font: bold 14px system-ui, -apple-system, sans-serif; fill: #24292f; }',
        '    .grid { stroke: #d0d7de; stroke-dasharray: 3,3; stroke-width: 1; }',
        '    .axis-label { font: 11px system-ui, -apple-system, sans-serif; fill: #57606a; }',
        '    .commit-label { font: 10px monospace; fill: #57606a; text-anchor: middle; }',
        '    .legend { font: 12px system-ui, -apple-system, sans-serif; fill: #24292f; }',
        '    @media (prefers-color-scheme: dark) {',
        '      .title { fill: #f0f6fc; }',
        '      .grid { stroke: #30363d; }',
        '      .axis-label, .commit-label { fill: #8b949e; }',
        '      .legend { fill: #f0f6fc; }',
        '    }',
        '  </style>',
        f'  <text x="{ml}" y="25" class="title">Total Line Coverage Trend</text>',
        f'  <circle cx="{w - 240}" cy="22" r="5" fill="#4285F4" />',
        f'  <text x="{w - 230}" y="26" class="legend">Unit</text>',
        f'  <circle cx="{w - 140}" cy="22" r="5" fill="#34A853" />',
        f'  <text x="{w - 130}" y="26" class="legend">Instrumentation</text>',
    ]

    for y_pct in (0, 25, 50, 75, 100):
        y = y_coord(y_pct)
        svg.append(f'  <line x1="{ml}" y1="{y:.1f}" x2="{w - mr}" y2="{y:.1f}" class="grid" />')
        svg.append(f'  <text x="{ml - 8}" y="{y + 4:.1f}" text-anchor="end" class="axis-label">{y_pct}%</text>')

    # Unit data
    unit_pts = [(xs[i], y_coord(total_pct(c, "unit"))) for i, c in enumerate(recent) if total_pct(c, "unit") is not None]
    if unit_pts:
        path_d = "M " + " L ".join(f"{x:.1f},{y:.1f}" for x, y in unit_pts)
        area_d = f"{path_d} L {unit_pts[-1][0]:.1f},{y_coord(0):.1f} L {unit_pts[0][0]:.1f},{y_coord(0):.1f} Z"
        svg.append(f'  <path d="{area_d}" fill="#4285F4" fill-opacity="0.12" />')
        svg.append(f'  <path d="{path_d}" fill="none" stroke="#4285F4" stroke-width="2.5" stroke-linejoin="round" />')
        for (x, y), c in zip(unit_pts, recent):
            pct = total_pct(c, "unit")
            if pct is not None:
                svg.append(f'  <circle cx="{x:.1f}" cy="{y:.1f}" r="4" fill="#4285F4" stroke="#ffffff" stroke-width="1.5">')
                svg.append(f'    <title>{c}: {pct:.2f}% (unit)</title>')
                svg.append('  </circle>')

    # Instrumentation data
    instr_pts = [(xs[i], y_coord(total_pct(c, "instrumentation"))) for i, c in enumerate(recent) if total_pct(c, "instrumentation") is not None]
    if instr_pts:
        path_d = "M " + " L ".join(f"{x:.1f},{y:.1f}" for x, y in instr_pts)
        svg.append(f'  <path d="{path_d}" fill="none" stroke="#34A853" stroke-width="2.5" stroke-linejoin="round" stroke-dasharray="4,4" />')
        for (x, y), c in zip(instr_pts, recent):
            pct = total_pct(c, "instrumentation")
            if pct is not None:
                svg.append(f'  <circle cx="{x:.1f}" cy="{y:.1f}" r="4" fill="#34A853" stroke="#ffffff" stroke-width="1.5">')
                svg.append(f'    <title>{c}: {pct:.2f}% (instrumentation)</title>')
                svg.append('  </circle>')

    for i, c in enumerate(recent):
        x = xs[i]
        svg.append(f'  <text x="{x:.1f}" y="{h - 15}" class="commit-label">{c[:7]}</text>')

    svg.append('</svg>\n')

    os.makedirs(os.path.dirname(out_path) or ".", exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(svg))
    print(f"Wrote {out_path}")


def render_mermaid_chart(order: list[str], rows: list[dict]) -> list[str]:
    """Render a GitHub-supported Mermaid xychart-beta trend chart."""
    if not order:
        return []
    recent = order[-25:]
    if len(recent) < 2:
        return []

    x_labels = ", ".join(f'"{c[:7]}"' for c in recent)

    def total_pct(commit: str, suite: str) -> float | None:
        for r in rows:
            if r.get("commit") == commit and r.get("suite") == suite and r.get("module") == TOTAL:
                try:
                    return float(r["line_pct"])
                except (ValueError, KeyError):
                    return None
        return None

    unit_values = [total_pct(c, "unit") for c in recent]
    has_unit = any(v is not None for v in unit_values)

    lines = [
        "```mermaid",
        "xychart-beta",
        '    title "Line Coverage Trend (%)"',
        f"    x-axis [{x_labels}]",
        '    y-axis "Coverage %" 0 --> 100',
    ]
    if has_unit:
        formatted = ", ".join(f"{v:.2f}" if v is not None else "0" for v in unit_values)
        lines.append(f"    line [{formatted}]")
    lines.append("```")
    lines.append("")
    return lines


def cmd_render(args: argparse.Namespace) -> None:
    rows = read_history(args.csv)
    if not rows:
        sys.exit(f"{args.csv} has no entries to render.")

    lines = [
        "<!-- Generated by .github/scripts/coverage_history.py. Do not edit by hand. -->",
        "# Test coverage",
        "",
        "Raw data lives in [`history.csv`](history.csv), one row per module per",
        "suite per merged commit. This page is regenerated from it and should not",
        "be edited by hand.",
        "",
        "The two suites are reported separately and are **not additive**. They",
        "instrument different code with different runners, and merging them would",
        "require combining the raw execution data rather than the XML reports.",
        "",
        "> **What the instrumentation number measures.** It is dominated by",
        "> `app`, which is the internal sample app, not a published artifact.",
        "> It is *not* a direct measurement of the published `android-maps-testing`",
        "> libraries. The library's code is exercised heavily by `app`'s tests,",
        "> but coverage attributes to the module that runs the tests, so it",
        "> is counted under `app`. Read the instrumentation figure as how",
        "> much of the sample app, and through it the library, is exercised on a",
        "> device. The unit table is the one that measures the published library",
        "> modules directly.",
        "",
        "## Current",
        "",
    ]
    lines += render_current(
        rows,
        "unit",
        "Unit tests",
        "JVM tests run by `./gradlew koverXmlReportDebug`, covering the three "
        "published library modules (`robolectric-testing`, `golden-testing`, `visual-testing`).",
    )
    lines += render_current(
        rows,
        "instrumentation",
        "Instrumentation tests",
        "Emulator tests run by `./gradlew connectedAndroidTest`, covering "
        "the modules that own androidTest sources.",
    )

    # Trend: one line per recorded commit, newest first, with the change in
    # total line coverage for each suite against the commit before it.
    order: list[str] = []
    for row in rows:
        if row["commit"] not in order:
            order.append(row["commit"])

    def total_pct(commit: str, suite: str) -> float | None:
        for row in rows:
            if (
                row["commit"] == commit
                and row.get("suite") == suite
                and row["module"] == TOTAL
            ):
                return float(row["line_pct"])
        return None

    def cell(current: float | None, previous: float | None) -> tuple[str, str]:
        if current is None:
            return "n/a", "n/a"
        if previous is None:
            return f"{current:.2f}%", "n/a"
        difference = current - previous
        change = f"{difference:+.2f}" if abs(difference) >= 0.005 else "0.00"
        return f"{current:.2f}%", change

    svg_path = getattr(args, "svg", "")
    if svg_path:
        generate_svg_graph(order, rows, svg_path)

    lines += [
        f"## Trend (last {TREND_LIMIT} commits)",
        "",
    ]
    if svg_path:
        lines += [
            f"![Coverage Trend]({os.path.basename(svg_path)})",
            "",
        ]

    lines += render_mermaid_chart(order, rows)

    lines += [
        "Total line coverage per suite, newest first.",
        "",
        "| Date | Commit | PR | Unit % | Change | Instr. % | Change | Subject |",
        "| --- | --- | --- | ---: | ---: | ---: | ---: | --- |",
    ]
    for index in range(len(order) - 1, max(len(order) - TREND_LIMIT, 0) - 1, -1):
        commit = order[index]
        previous = order[index - 1] if index > 0 else None
        meta = next(row for row in rows if row["commit"] == commit)
        unit, unit_change = cell(
            total_pct(commit, "unit"),
            total_pct(previous, "unit") if previous else None,
        )
        instr, instr_change = cell(
            total_pct(commit, "instrumentation"),
            total_pct(previous, "instrumentation") if previous else None,
        )
        pr = f"#{meta['pr']}" if meta["pr"] else "n/a"
        subject = meta["subject"].replace("|", "\\|") or "n/a"
        lines.append(
            f"| {meta['timestamp'][:10]} | `{commit}` | {pr} "
            f"| {unit} | {unit_change} | {instr} | {instr_change} | {subject} |"
        )
    lines.append("")

    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    print(f"Wrote {args.out}")


def cmd_compare(args: argparse.Namespace) -> None:
    modules = collect(resolve_pattern(args), allow=resolve_allow(args))
    baseline = latest_entry(read_history(args.csv), args.suite)

    lines = [args.marker, f"## Coverage ({args.suite} tests)", ""]
    if baseline:
        head = next(iter(baseline.values()))
        lines += [
            f"Compared against `{head['commit']}` on `{args.base}`"
            + (f" (#{head['pr']})" if head["pr"] else "")
            + ".",
            "",
        ]
    else:
        lines += [
            f"No {args.suite} baseline recorded in `{args.csv}` yet, so this run "
            "only reports absolute numbers.",
            "",
        ]

    lines += [
        "| Module | Line % | Change | Branch % | Change |",
        "| --- | ---: | ---: | ---: | ---: |",
    ]

    def delta(current: float, module: str, field: str) -> str:
        if module not in baseline:
            return "new"
        difference = current - float(baseline[module][field])
        if abs(difference) < 0.005:
            return "no change"
        return f"{'🔺' if difference > 0 else '🔻'} {difference:+.2f}"

    for module, counters in sorted(modules.items(), key=lambda kv: sort_key(kv[0])):
        line_pct = pct(counters["LINE"]["covered"], counters["LINE"]["total"])
        branch_pct = pct(counters["BRANCH"]["covered"], counters["BRANCH"]["total"])
        name = f"**{module}**" if module == TOTAL else f"`{module}`"
        lines.append(
            f"| {name} | {line_pct:.2f}% | {delta(line_pct, module, 'line_pct')} "
            f"| {branch_pct:.2f}% | {delta(branch_pct, module, 'branch_pct')} |"
        )

    lines += [
        "",
        f"<sub>Line and branch coverage from {args.suite} test reports. History "
        f"is recorded in `coverage/history.csv` after each merge to "
        f"`{args.base}`.</sub>",
        "",
    ]

    body = "\n".join(lines)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(body)
        print(f"Wrote {args.out}")
    else:
        print(body)


# shields.io endpoint schema: https://shields.io/badges/endpoint-badge
BADGE_SCHEMA_VERSION = 1

# Floor percentages, highest first. Anything under the last floor is red.
BADGE_COLORS = (
    (90.0, "brightgreen"),
    (75.0, "green"),
    (60.0, "yellowgreen"),
    (40.0, "yellow"),
    (20.0, "orange"),
)


def badge_color(percentage: float) -> str:
    for floor, color in BADGE_COLORS:
        if percentage >= floor:
            return color
    return "red"


def cmd_badge(args: argparse.Namespace) -> None:
    """Write shields.io endpoint JSON for one suite's latest TOTAL row.

    Read by an endpoint badge in README.md. The file is published on the
    coverage history branch rather than main, so refreshing the badge costs
    contributors nothing (see coverage-history.yml for why that matters).

    A suite with no recorded entry yet renders as "unknown" rather than
    failing, so the badge degrades quietly instead of breaking the README.
    """
    total = latest_entry(read_history(args.csv), args.suite).get(TOTAL)
    if total is None:
        payload = {"message": "unknown", "color": "lightgrey"}
    else:
        percentage = float(total[args.metric])
        payload = {
            "message": f"{percentage:.0f}%",
            "color": badge_color(percentage),
        }

    payload = {"schemaVersion": BADGE_SCHEMA_VERSION, "label": args.label, **payload}
    text = json.dumps(payload, indent=2) + "\n"
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(text)
        print(f"Wrote {args.out}")
    else:
        sys.stdout.write(text)


def add_common(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--suite",
        choices=sorted(SUITES),
        default="unit",
        help="which test suite these reports came from (default: unit)",
    )
    parser.add_argument(
        "--reports",
        default="",
        help="glob matching the XML reports; defaults to the suite's usual path",
    )
    parser.add_argument(
        "--modules",
        default="",
        help='comma-separated modules to track, or "all" for every module '
        "reported; defaults to the suite's tracked list",
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    summary = subparsers.add_parser("summary", help="print parsed coverage as JSON")
    add_common(summary)
    summary.set_defaults(func=cmd_summary)

    append = subparsers.add_parser("append", help="append an entry to the history CSV")
    add_common(append)
    append.add_argument("--csv", default=DEFAULT_CSV)
    append.add_argument("--commit", required=True, help="full commit SHA")
    append.add_argument("--pr", default="", help="pull request number, if known")
    append.add_argument("--subject", default="", help="commit subject line")
    append.add_argument(
        "--optional",
        action="store_true",
        help="skip quietly instead of failing when no reports are present",
    )
    append.set_defaults(func=cmd_append)

    merge = subparsers.add_parser(
        "merge", help="re-apply locally added rows onto a refreshed history CSV"
    )
    merge.add_argument("--csv", default=DEFAULT_CSV, help="refreshed file to merge into")
    merge.add_argument("--ours", required=True, help="our copy, holding the new rows")
    merge.set_defaults(func=cmd_merge)

    render = subparsers.add_parser("render", help="regenerate COVERAGE.md from the CSV")
    render.add_argument("--csv", default=DEFAULT_CSV)
    render.add_argument("--out", default=DEFAULT_MARKDOWN)
    render.add_argument("--svg", default="", help="write SVG trend graph here")
    render.set_defaults(func=cmd_render)

    compare = subparsers.add_parser(
        "compare", help="render a markdown coverage diff against the last entry"
    )
    add_common(compare)
    compare.add_argument("--csv", default=DEFAULT_CSV)
    compare.add_argument("--base", default="main", help="branch the baseline came from")
    compare.add_argument("--out", default="", help="write here instead of stdout")
    compare.add_argument(
        "--marker",
        default="<!-- coverage-history-comment -->",
        help="hidden marker used to find and update the existing PR comment",
    )
    compare.set_defaults(func=cmd_compare)

    badge = subparsers.add_parser(
        "badge", help="emit shields.io endpoint JSON for the latest total"
    )
    badge.add_argument(
        "--suite",
        choices=sorted(SUITES),
        default="unit",
        help="which suite to report (default: unit)",
    )
    badge.add_argument("--csv", default=DEFAULT_CSV)
    badge.add_argument("--out", default="", help="write here instead of stdout")
    badge.add_argument(
        "--label",
        default="coverage (unit)",
        help="text on the left half of the badge",
    )
    badge.add_argument(
        "--metric",
        default="line_pct",
        choices=("line_pct", "branch_pct", "instruction_pct"),
        help="which percentage to display (default: line_pct)",
    )
    badge.set_defaults(func=cmd_badge)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
