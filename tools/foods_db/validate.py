#!/usr/bin/env python3
"""Validate the bundled food database and write tools/foods_db/report.md.

Fails (exit 1) on anything that would make the app ship a broken food list:
wrong counts, unknown categories, values the seeder cannot read, duplicates,
impossible energy values, or a category list that drifted from the Kotlin code.
"""

from __future__ import annotations

import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import (  # noqa: E402
    ASSETS,
    CACHE,
    CATEGORIES,
    GTIN_LENGTHS,
    MAX_CALORIES_PER_100G,
    atwater_ok,
    normalize_barcode,
    normalize_key,
    read_json,
)

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
MODELS = os.path.join(
    ROOT, "app", "src", "main", "java", "com", "bulgarcal", "tracker", "domain", "Models.kt"
)

MIN_PRODUCTS = 25_000
MIN_DISHES = 5_000
EXPECTED_FOODS = 92
# Scans are answered from the bundled database first, so the number of shipped
# barcodes is a product guarantee, not a nice-to-have.
MIN_BARCODES = 25_000

REQUIRED = ("name", "category", "calories", "carbs", "protein", "fats")
OPTIONAL = (
    "saturated_fats", "mono_fats", "poly_fats", "trans_fats", "cholesterol",
    "sugars", "fiber", "glycemic_index", "glycemic_load", "salt", "potassium",
    "water", "barcode", "source",
)
ALLOWED = set(REQUIRED) | set(OPTIONAL)


def kotlin_categories() -> list[str]:
    """Read FoodCategories out of Models.kt so the two lists cannot drift apart."""
    with open(MODELS, encoding="utf-8") as fh:
        text = fh.read()
    block = re.search(r"val FoodCategories = listOf\((.*?)\n\)", text, re.S)
    if not block:
        raise SystemExit("could not find FoodCategories in Models.kt")
    return re.findall(r'"([^"]+)"', block.group(1))


def main() -> int:
    failures: list[str] = []
    notes: list[str] = []
    files = {
        "foods.json": read_json(os.path.join(ASSETS, "foods.json")),
        "products.json": read_json(os.path.join(ASSETS, "products.json")),
        "dishes.json": read_json(os.path.join(ASSETS, "dishes.json")),
    }
    counts = {name: len(rows) for name, rows in files.items()}

    if counts["products.json"] < MIN_PRODUCTS:
        failures.append(f"products.json has {counts['products.json']} rows (< {MIN_PRODUCTS})")
    if counts["dishes.json"] < MIN_DISHES:
        failures.append(f"dishes.json has {counts['dishes.json']} rows (< {MIN_DISHES})")
    if counts["foods.json"] != EXPECTED_FOODS:
        failures.append(f"foods.json has {counts['foods.json']} rows (expected {EXPECTED_FOODS})")

    known = set(CATEGORIES)
    browse = [c for c in kotlin_categories() if c != "Моите Рецепти"]
    if kotlin_categories() != CATEGORIES:
        failures.append("Models.kt FoodCategories and tools CATEGORIES differ")

    per_category: dict[str, int] = {}
    per_source: dict[str, int] = {}
    seen_names: dict[str, str] = {}
    duplicates: list[str] = []
    atwater_bad: list[str] = []
    alcohol_exempt = 0
    range_bad: list[str] = []
    unknown_keys: list[str] = []
    bad_barcodes: list[str] = []
    seen_barcodes: dict[str, str] = {}
    barcode_duplicates: list[str] = []
    with_barcode = 0
    asset_bytes = 0

    for filename, rows in files.items():
        asset_bytes += os.path.getsize(os.path.join(ASSETS, filename))
        for index, row in enumerate(rows):
            where = f"{filename}[{index}] {row.get('name', '?')}"
            extra = set(row) - ALLOWED
            if extra:
                unknown_keys.append(f"{where}: {sorted(extra)}")
            if row.get("category") not in known:
                failures.append(f"{where}: category {row.get('category')!r} is not browsable")
                continue
            category = row["category"]
            per_category[category] = per_category.get(category, 0) + 1
            per_source[str(row.get("source", "?"))] = per_source.get(str(row.get("source", "?")), 0) + 1
            name = row.get("name", "")
            if not isinstance(name, str) or len(name) < 3:
                range_bad.append(f"{where}: bad name")
            for key in REQUIRED:
                if key not in row:
                    range_bad.append(f"{where}: missing {key}")
            if not isinstance(row.get("calories"), int) or not 0 <= row["calories"] <= MAX_CALORIES_PER_100G:
                range_bad.append(f"{where}: calories {row.get('calories')}")
            for key in ("carbs", "protein", "fats"):
                value = row.get(key)
                if not isinstance(value, (int, float)) or not 0 <= value <= 100:
                    range_bad.append(f"{where}: {key} {value}")
            if not row.get("source"):
                range_bad.append(f"{where}: no source")
            barcode = row.get("barcode")
            if barcode is not None:
                if not isinstance(barcode, str) or normalize_barcode(barcode) != barcode:
                    bad_barcodes.append(f"{where}: barcode {barcode!r}")
                elif barcode in seen_barcodes:
                    barcode_duplicates.append(
                        f"{barcode} in {filename} and {seen_barcodes[barcode]}"
                    )
                else:
                    seen_barcodes[barcode] = filename
                    with_barcode += 1
            key = normalize_key(name)
            if key in seen_names:
                duplicates.append(f"{name!r} in {filename} and {seen_names[key]}")
            else:
                seen_names[key] = filename
            value = None
            try:
                value = atwater_ok(
                    row.get("calories"), row.get("carbs"), row.get("protein"), row.get("fats"),
                    row.get("fiber") or 0.0,
                )
            except TypeError:
                value = False
            if category == "Алкохол":
                # Alcohol carries 7 kcal/g that macros do not describe, and the app
                # does not track alcohol grams, so these rows are exempt.
                alcohol_exempt += 1
            elif not value:
                atwater_bad.append(f"{where}: {row.get('calories')} kcal vs macros")

    if duplicates:
        failures.append(f"{len(duplicates)} duplicate names across the files")
    if range_bad:
        failures.append(f"{len(range_bad)} rows with unusable numbers")
    if unknown_keys:
        failures.append(f"{len(unknown_keys)} rows with unknown keys")
    if bad_barcodes:
        failures.append(f"{len(bad_barcodes)} rows with an unusable barcode")
    if barcode_duplicates:
        failures.append(f"{len(barcode_duplicates)} barcodes shared by more than one row")
    if with_barcode < MIN_BARCODES:
        failures.append(f"only {with_barcode} rows carry a barcode (< {MIN_BARCODES})")

    empty = [c for c in browse if c != "Общи" and per_category.get(c, 0) == 0]
    if empty:
        failures.append(f"categories with no food: {empty}")
    total = sum(counts.values())
    biggest = max(per_category.values()) if per_category else 0
    if biggest > total * 0.25:
        failures.append(f"one category holds {biggest}/{total} rows (> 25%)")

    # GI/GL must agree with each other, otherwise the details screen lies.
    gh_slips = 0
    for rows in files.values():
        for row in rows:
            gi = row.get("glycemic_index", 0)
            gl = row.get("glycemic_load", 0)
            carbs = row.get("carbs", 0)
            fiber = row.get("fiber", 0)
            expected = round(gi * max(0.0, carbs - fiber) / 100, 2)
            if abs(expected - gl) > 0.05:
                gh_slips += 1
    if gh_slips:
        failures.append(f"{gh_slips} rows where glycemic_load does not match the GI")

    report = [
        "# Bundled food database report",
        "",
        "Generated by `python tools/foods_db/validate.py`.",
        "",
        "## Counts",
        "",
        "| File | Rows |",
        "| --- | --- |",
    ]
    for name, count in counts.items():
        report.append(f"| {name} | {count:,} |")
    report += [
        f"| **total** | **{total:,}** |",
        "",
        f"Assets: {asset_bytes / 1024 / 1024:.2f} MB of JSON in `app/src/main/assets`.",
        "",
        "## Sources",
        "",
        "| Source | Rows |",
        "| --- | --- |",
    ]
    for source, count in sorted(per_source.items(), key=lambda kv: -kv[1]):
        report.append(f"| {source} | {count:,} |")
    report += ["", "## By category", "", "| Category | Rows |", "| --- | --- |"]
    for category in browse:
        report.append(f"| {category} | {per_category.get(category, 0):,} |")
    report += ["", "## Checks", ""]
    report.append(f"- duplicates across files: **{len(duplicates)}**")
    report.append(f"- rows failing the energy/macro check: **{len(atwater_bad)}**")
    report.append(f"- alcohol rows exempt from it (7 kcal/g, not trackable): **{alcohol_exempt}**")
    report.append(f"- rows with unusable numbers: **{len(range_bad)}**")
    report.append(f"- rows with unknown keys: **{len(unknown_keys)}**")
    report.append(f"- rows with a usable barcode: **{with_barcode:,}**")
    report.append(f"- unusable barcodes: **{len(bad_barcodes)}**")
    report.append(f"- barcodes shared by two rows: **{len(barcode_duplicates)}**")
    report.append(f"- barcode lengths shipped: **{sorted({len(b) for b in seen_barcodes})}** "
                  f"(allowed {list(GTIN_LENGTHS)})")
    report.append(f"- categories with no food (other than Общи): **{len(empty)}**")
    report.append(f"- biggest category share: **{biggest / total * 100:.1f}%**")
    report.append(f"- GI/GL inconsistencies: **{gh_slips}**")
    if atwater_bad:
        report += ["", "### Rows failing the energy/macro check", ""]
        report += [f"- {item}" for item in atwater_bad[:40]]
    if duplicates:
        report += ["", "### Duplicates", ""]
        report += [f"- {item}" for item in duplicates[:40]]
    if range_bad:
        report += ["", "### Unusable numbers", ""]
        report += [f"- {item}" for item in range_bad[:40]]
    if unknown_keys:
        report += ["", "### Unknown keys", ""]
        report += [f"- {item}" for item in unknown_keys[:20]]
    if bad_barcodes or barcode_duplicates:
        report += ["", "### Barcode problems", ""]
        report += [f"- {item}" for item in (bad_barcodes + barcode_duplicates)[:20]]
    for extra in ("products_build.json", "dishes_build.json", "foods_build.json"):
        path = os.path.join(CACHE, extra)
        if os.path.exists(path):
            summary = read_json(path)
            report += ["", f"## {extra}", "", "```json", json.dumps(summary, ensure_ascii=False, indent=2), "```"]

    notes.append(f"{total:,} rows total, {asset_bytes / 1024 / 1024:.2f} MB of assets")
    report_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "report.md")
    with open(report_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(report) + "\n")

    for note in notes:
        print("OK:", note)
    print(f"report: {report_path}")
    if failures:
        print("\nFAILED CHECKS:")
        for failure in failures:
            print(" -", failure)
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
