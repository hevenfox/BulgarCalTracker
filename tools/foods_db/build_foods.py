#!/usr/bin/env python3
"""Clean app/src/main/assets/foods.json.

The original file held 1 000 rows: 92 real Bulgarian foods plus 908 synthetic
"Верея <marketing suffix>" clones of them (the same food with invented calories,
e.g. Агнешки котлет at 155 and at 269 kcal). This script keeps the 92 real rows,
gives each one a browsable category and marks the source, and refuses to run if
the file's shape is not what it expects - the numbers themselves are untouched.

Usage: python tools/foods_db/build_foods.py [--write]
"""

from __future__ import annotations

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import ASSETS, CATEGORIES, CACHE, read_json, write_json_array  # noqa: E402

SUFFIXES = [
    "Верея Класик", "Верея Традиционен БДС вкус", "Верея Премиум селекция",
    "Верея Домашен стил", "Верея Нискомаслена алтернатива",
    "Верея Ръчно приготвен продукт", "Верея За фитнес и диети",
    "Верея Стандартна рецепта", "Верея Екоферма Екстра",
    "Верея Избран фермерски продукт",
]

CATEGORY_BY_NAME = {
    "Кисело мляко 2%": "Кисели млека и скир",
    "Кисело мляко 3.6%": "Кисели млека и скир",
    "Кисело мляко 0.1%": "Кисели млека и скир",
    "Цедено кисело мляко 10%": "Кисели млека и скир",
    "Кефир": "Кисели млека и скир",
    "Извара нискомаслена": "Кисели млека и скир",
    "Извара маслена": "Кисели млека и скир",
    "Българско бяло саламурено сирене краве": "Сирена и кашкавал",
    "Българско бяло саламурено сирене овче": "Сирена и кашкавал",
    "Българско бяло саламурено сирене козе": "Сирена и кашкавал",
    "Български кашкавал краве": "Сирена и кашкавал",
    "Български кашкавал овче": "Сирена и кашкавал",
    "Сметана заквасена 20%": "Млечни продукти",
    "Краве масло": "Мазнини и масла",
    "Пилешко филе от гърди": "Месо и колбаси",
    "Пилешко бутче с кожа": "Месо и колбаси",
    "Свински врат без кост": "Месо и колбаси",
    "Свинско филе бонфиле": "Месо и колбаси",
    "Свинска кайма смес 60/40": "Месо и колбаси",
    "Телешко шол": "Месо и колбаси",
    "Телешка кайма": "Месо и колбаси",
    "Агнешко бутче": "Месо и колбаси",
    "Агнешки котлет": "Месо и колбаси",
    "Кебапче свинско": "Месо и колбаси",
    "Кюфте свинско": "Месо и колбаси",
    "Кебапче пилешко": "Месо и колбаси",
    "Кюфте пилешко": "Месо и колбаси",
    "Татарско кюфте": "Месо и колбаси",
    "Нервозно кюфте люто": "Месо и колбаси",
    "Свинска наденица": "Месо и колбаси",
    "Карначе": "Месо и колбаси",
    "Българска луканка": "Месо и колбаси",
    "Суджук говежди": "Месо и колбаси",
    "Филе Елена": "Месо и колбаси",
    "Пастърма говежда": "Месо и колбаси",
    "Бански старец": "Месо и колбаси",
    "Еленски бут": "Месо и колбаси",
    "Свински шпеков салам": "Месо и колбаси",
    "Кренвирш пилешки": "Месо и колбаси",
    "Кренвирш свински": "Месо и колбаси",
    "Традиционна домашна баница": "Тестени и сладкиши",
    "Мекица": "Тестени и сладкиши",
    "Пържени филии": "Тестени и сладкиши",
    "Милинки със сирене": "Тестени и сладкиши",
    "Традиционна мусака": "Домашни ястия",
    "Пълнени чушки с ориз и кайма": "Домашни ястия",
    "Пълнени сухи чушки с боб": "Домашни ястия",
    "Сарми лозови с ориз": "Домашни ястия",
    "Сарми лозови с кайма и ориз": "Домашни ястия",
    "Сарми зелеви с ориз": "Домашни ястия",
    "Сарми зелеви с кайма и ориз": "Домашни ястия",
    "Боб чорба": "Домашни ястия",
    "Боб яхния": "Домашни ястия",
    "Леща чорба": "Домашни ястия",
    "Леща яхния": "Домашни ястия",
    "Шкембе чорба": "Домашни ястия",
    "Курбан чорба": "Домашни ястия",
    "Пилешка супа застроена": "Домашни ястия",
    "Кавърма свинска": "Домашни ястия",
    "Кавърма пилешка": "Домашни ястия",
    "Винен кебап": "Домашни ястия",
    "Гювеч със свинско": "Домашни ястия",
    "Капама": "Домашни ястия",
    "Пълнен шаран с орехи (Никулденски)": "Риба и морски дарове",
    "Чушки бюрек": "Домашни ястия",
    "Миш-маш с олио и сирене": "Домашни ястия",
    "Яйца по панагюрски": "Домашни ястия",
    "Грис халва": "Сладолед и десерти",
    "Мляко с ориз (Сутляш)": "Сладолед и десерти",
    "Тиквеник с орехи": "Тестени и сладкиши",
    "Крем карамел": "Сладолед и десерти",
    "Традиционна лютеница фина": "Сосове и подправки",
    "Лютеница едросмляна домашна": "Сосове и подправки",
    "Айвар български вкус": "Сосове и подправки",
    "Кьопоолу": "Сосове и подправки",
    "Шопска салата": "Готови ястия",
    "Овчарска салата": "Готови ястия",
    "Салата Снежанка": "Готови ястия",
    "Таратор": "Готови ястия",
    "Зелева салата с моркови и олио": "Готови ястия",
    "Салата от печени чушки с чесън": "Готови ястия",
    "Трушия царска": "Готови ястия",
    "Кисело зеле": "Плодове и зеленчуци",
    "Хляб бял обикновен": "Хляб и зърнени",
    "Хляб Добруджа": "Хляб и зърнени",
    "Хляб типов": "Хляб и зърнени",
    "Хляб пълнозърнест ржаен": "Хляб и зърнени",
    "Ориз бял варен": "Паста и ориз",
    "Картофи варени": "Плодове и зеленчуци",
    "Качамак без добавка": "Паста и ориз",
    "Айрян": "Безалкохолни напитки",
    "Традиционна боза": "Безалкохолни напитки",
}

REQUIRED_KEYS = {"calories", "carbs", "protein", "fats"}

NUMBER_KEYS = [
    "calories", "carbs", "protein", "fats", "saturated_fats", "sugars", "fiber",
    "glycemic_index", "salt",
]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    path = os.path.join(ASSETS, "foods.json")
    raw = read_json(path)
    bases = [row for row in raw if not any(s in row["name"] for s in SUFFIXES)]
    suffixes = [row for row in raw if any(s in row["name"] for s in SUFFIXES)]

    problems: list[str] = []
    unknown = [row["name"] for row in bases if row["name"] not in CATEGORY_BY_NAME]
    if unknown:
        problems.append(f"base foods with no category: {unknown}")
    mapped = [name for name in CATEGORY_BY_NAME if name not in {r['name'] for r in bases}]
    if mapped:
        problems.append(f"categories mapped for names that no longer exist: {mapped}")
    bad_category = [
        name for name, category in CATEGORY_BY_NAME.items() if category not in CATEGORIES
    ]
    if bad_category:
        problems.append(f"unknown categories in the map: {bad_category}")
    if problems:
        for problem in problems:
            print("FAIL:", problem)
        return 1

    rows = []
    for row in bases:
        clean = {"name": row["name"], "category": CATEGORY_BY_NAME[row["name"]]}
        # The seeder reads calories/carbs/protein/fats unconditionally, so those four
        # are always written even when 0 (e.g. Агнешки котлет has 0 g carbs); only the
        # optional keys are dropped when empty.
        for key in NUMBER_KEYS:
            value = row.get(key)
            if value is None:
                if key in REQUIRED_KEYS:
                    clean[key] = 0 if key == "calories" else 0.0
                continue
            if key == "calories":
                clean[key] = int(round(float(value)))
            else:
                value = round(float(value), 2)
                if value or key in REQUIRED_KEYS:
                    clean[key] = value
        # Glycemic load is derived, not measured: GI x available carbs / 100, the same
        # formula the other two files use, so the details screen adds it up consistently.
        gi = float(clean.get("glycemic_index", 0) or 0)
        available = max(0.0, float(clean.get("carbs", 0) or 0) - float(clean.get("fiber", 0) or 0))
        gl = round(gi * available / 100.0, 2)
        if gl:
            clean["glycemic_load"] = gl
        clean["source"] = "curated"
        rows.append(clean)

    rows.sort(key=lambda r: (r["category"], r["name"].lower()))
    summary = {
        "kept": len(rows),
        "dropped_synthetic": len(suffixes),
        "by_category": {},
    }
    for row in rows:
        summary["by_category"][row["category"]] = (
            summary["by_category"].get(row["category"], 0) + 1
        )
    print(json.dumps(summary, ensure_ascii=False, indent=2))

    if not args.write:
        print("dry run - pass --write to rewrite foods.json")
        return 0
    size = write_json_array(path, rows)
    os.makedirs(CACHE, exist_ok=True)
    with open(os.path.join(CACHE, "foods_build.json"), "w", encoding="utf-8") as fh:
        json.dump({**summary, "bytes": size}, fh, ensure_ascii=False, indent=2)
    print(f"wrote {path}: {len(rows)} rows, {size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
