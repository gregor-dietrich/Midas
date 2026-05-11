#!/usr/bin/env python3
"""Generate a markdown coverage report from a JaCoCo CSV file."""

import csv
import sys
from typing import Dict, List, Tuple


def get_module_from_path(csv_path: str) -> str:
    """Extract module name from CSV file path."""
    if "midas-api" in csv_path:
        return "API"
    elif "midas-gui" in csv_path:
        return "GUI"
    return "Unknown"


def calculate_coverage(rows_list: List[Dict[str, str]]) -> Tuple[float, float]:
    """Calculate line and branch coverage percentages."""
    total_lm = sum(int(r["LINE_MISSED"]) for r in rows_list)
    total_lc = sum(int(r["LINE_COVERED"]) for r in rows_list)
    total_bm = sum(int(r["BRANCH_MISSED"]) for r in rows_list)
    total_bc = sum(int(r["BRANCH_COVERED"]) for r in rows_list)

    tl = total_lm + total_lc
    tb = total_bm + total_bc

    line_pct = (total_lc / tl * 100) if tl > 0 else 100.0
    branch_pct = (total_bc / tb * 100) if tb > 0 else 100.0

    return line_pct, branch_pct


def main() -> None:
    if len(sys.argv) < 3:
        print("Usage: coverage.py <report_path> <csv_path1> [csv_path2 ...]")
        sys.exit(1)

    report_path: str = sys.argv[1]
    csv_paths: List[str] = sys.argv[2:]

    rows: List[Dict[str, str]] = []
    module_rows: Dict[str, List[Dict[str, str]]] = {}

    for csv_path in csv_paths:
        module = get_module_from_path(csv_path)
        if module not in module_rows:
            module_rows[module] = []
        try:
            with open(csv_path, "r") as file:
                csv_data = list(csv.DictReader(file))
                rows.extend(csv_data)
                module_rows[module].extend(csv_data)
        except FileNotFoundError:
            print(f"Warning: CSV file not found: {csv_path}")

    if not rows:
        print("Error: No coverage data found.")
        sys.exit(1)

    md: str = "# Coverage Report\n\n"

    # Generate module summary section
    md += "## Summary by Module\n\n"
    md += "| Module | Line % | Branch % |\n"
    md += "|--------|-------:|---------:|\n"

    module_coverages: Dict[str, Tuple[float, float]] = {}
    for module in sorted(module_rows.keys()):
        if module_rows[module]:
            line_pct, branch_pct = calculate_coverage(module_rows[module])
            module_coverages[module] = (line_pct, branch_pct)
            md += f"| {module} | {line_pct:.1f}% | {branch_pct:.1f}% |\n"

    # Add average row
    if module_coverages:
        avg_line = sum(pct[0] for pct in module_coverages.values()) / len(module_coverages)
        avg_branch = sum(pct[1] for pct in module_coverages.values()) / len(module_coverages)
        md += f"| **Average** | **{avg_line:.1f}%** | **{avg_branch:.1f}%** |\n"

    md += "\n"

    pkg_data: Dict[str, Dict[str, int]] = {}
    for r in rows:
        pkg = r["PACKAGE"]
        if pkg not in pkg_data:
            pkg_data[pkg] = {"lm": 0, "lc": 0, "bm": 0, "bc": 0}
        pkg_data[pkg]["lm"] += int(r["LINE_MISSED"])
        pkg_data[pkg]["lc"] += int(r["LINE_COVERED"])
        pkg_data[pkg]["bm"] += int(r["BRANCH_MISSED"])
        pkg_data[pkg]["bc"] += int(r["BRANCH_COVERED"])

    pkg_sorted: List[Tuple[str, Dict[str, int]]] = sorted(
        pkg_data.items(),
        key=lambda x: (
            x[1]["lc"] / (x[1]["lm"] + x[1]["lc"])
            if (x[1]["lm"] + x[1]["lc"]) > 0
            else 0
        ),
    )

    md += "## By Package (sorted by line coverage)\n\n"
    md += "| Package | Lines Missed | Lines Covered | Line % | Branch % |\n"
    md += "|---------|-------------:|--------------:|-------:|---------:|\n"

    total_lm: int = 0
    total_lc: int = 0
    total_bm: int = 0
    total_bc: int = 0
    for pkg, d in pkg_sorted:
        total_lm += d["lm"]
        total_lc += d["lc"]
        total_bm += d["bm"]
        total_bc += d["bc"]
        lt: int = d["lm"] + d["lc"]
        bt: int = d["bm"] + d["bc"]
        lp = f"{d['lc'] / lt * 100:.1f}" if lt > 0 else "100.0"
        bp = f"{d['bc'] / bt * 100:.1f}" if bt > 0 else "100.0"
        md += f"| {pkg} | {d['lm']} | {d['lc']} | {lp}% | {bp}% |\n"

    tl: int = total_lm + total_lc
    tb: int = total_bm + total_bc
    if tl > 0:
        md += f"| **Total** | **{total_lm}** | **{total_lc}** | **{total_lc / tl * 100:.1f}%** | **{total_bc / tb * 100:.1f}%** |\n"
    else:
        md += "| **Total** | **0** | **0** | **100.0%** | **100.0%** |\n"

    rows_sorted = sorted(
        rows,
        key=lambda r: (
            (int(r["LINE_COVERED"]) / (int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])))
            if (int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])) > 0
            else 0
        ),
    )

    md += "\n## By File (sorted by line coverage, worst first)\n\n"
    md += "| Package | Class | Lines Missed | Lines Covered | Line % | Branch % |\n"
    md += "|---------|-------|-------------:|--------------:|-------:|---------:|\n"
    for r in rows_sorted:
        pkg, cls = r["PACKAGE"], r["CLASS"]
        lm, lc = int(r["LINE_MISSED"]), int(r["LINE_COVERED"])
        bm, bc = int(r["BRANCH_MISSED"]), int(r["BRANCH_COVERED"])
        lt, bt = lm + lc, bm + bc
        lp = f"{lc / lt * 100:.1f}" if lt > 0 else "100.0"
        bp = f"{bc / bt * 100:.1f}" if bt > 0 else "100.0"
        md += f"| {pkg} | {cls} | {lm} | {lc} | {lp}% | {bp}% |\n"

    with open(report_path, "w") as file:
        file.write(md)

    print(f"Written to {report_path}")
    if tl > 0:
        print(
            f"Overall: {total_lc / tl * 100:.1f}% lines, {total_bc / tb * 100:.1f}% branches"
        )
    else:
        print("Overall: 100.0% lines, 100.0% branches (no data)")


if __name__ == "__main__":
    main()
