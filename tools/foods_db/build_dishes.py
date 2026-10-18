#!/usr/bin/env python3
"""Build app/src/main/assets/dishes.json.

Two sources:
  * USDA FoodData Central "Survey Foods (FNDDS)" 2021-2023 - 5,4xx prepared
    dishes and generic foods with real per-100 g nutrient values. A US government
    work: public domain, no attribution obligation (credited anyway).
  * tools/foods_db/bg_dishes.json - hand-authored Bulgarian dishes whose values
    are computed from their ingredient list, so every number is auditable.

USDA descriptions are English; they are translated with bg_glossary.json. Rows
whose description cannot be translated with confidence are dropped, and the
translation coverage is reported.
"""

from __future__ import annotations

import argparse
import io
import json
import os
import re
import sys
import unicodedata
import urllib.request
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import (  # noqa: E402
    ASSETS,
    CACHE,
    CATEGORIES,
    DEFAULT_CATEGORY,
    atwater_ok,
    clean_text,
    estimate_gi,
    food_row,
    normalize_key,
    read_json,
    write_json_array,
)

FNDDS_URL = (
    "https://fdc.nal.usda.gov/fdc-datasets/"
    "FoodData_Central_survey_food_json_2024-10-31.zip"
)
ZIP_PATH = os.path.join(CACHE, "fndds.zip")
GLOSSARY = os.path.join(os.path.dirname(os.path.abspath(__file__)), "bg_glossary.json")
BG_DISHES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "bg_dishes.json")

# USDA nutrient numbers (first choice) and ids (fallback) -> our key.
NUTRIENTS = {
    "calories": (("208",), (1008,)),
    "protein": (("203",), (1003,)),
    "fats": (("204",), (1004,)),
    "carbs": (("205",), (1005,)),
    "saturated_fats": (("606",), (1258,)),
    "mono_fats": (("645",), (1292,)),
    "poly_fats": (("646",), (1293,)),
    "trans_fats": (("605",), (1257,)),
    "cholesterol": (("601",), (1253,)),
    "sodium": (("307",), (1093,)),
    "potassium": (("306",), (1092,)),
    "water": (("255",), (1051,)),
    "sugars": (("269",), (2000,)),
    "fiber": (("291",), (1079,)),
}

MEASURES = {
    "cup", "cups", "tablespoon", "tablespoons", "tbsp", "teaspoon", "teaspoons",
    "tsp", "ounce", "ounces", "oz", "gram", "grams", "g", "kg", "fluid", "fl",
    "slice", "slices", "piece", "pieces", "each", "serving", "servings",
    "container", "package", "portion", "quart", "pint", "lb", "pound", "ml",
    "liter", "litre", "size", "sizes", "count", "unit", "units", "spoon",
}

# WWEIA food category -> our category, checked by keyword, first hit wins.
WWEIA_CATEGORY_RULES = [
    ("baby", "Детски храни"),
    ("infant", "Детски храни"),
    ("formula", "Детски храни"),
    ("toddler", "Детски храни"),
    ("alcoholic", "Алкохол"),
    ("beer", "Алкохол"),
    ("wine", "Алкохол"),
    ("liquor", "Алкохол"),
    ("coffee", "Кафе и чай"),
    ("tea", "Кафе и чай"),
    ("water", "Вода"),
    ("sports drink", "Енергийни напитки"),
    ("energy drink", "Енергийни напитки"),
    ("soft drink", "Безалкохолни напитки"),
    ("juice", "Безалкохолни напитки"),
    ("soda", "Безалкохолни напитки"),
    ("protein bar", "Протеинови барове"),
    ("sport", "Спортни добавки"),
    ("nutrition", "Спортни добавки"),
    ("yogurt", "Кисели млека и скир"),
    ("dairy", "Млечни продукти"),
    ("milk", "Млечни продукти"),
    ("cheese", "Сирена и кашкавал"),
    ("egg", "Яйца"),
    ("poultry", "Месо и колбаси"),
    ("chicken", "Месо и колбаси"),
    ("beef", "Месо и колбаси"),
    ("pork", "Месо и колбаси"),
    ("lamb", "Месо и колбаси"),
    ("veal", "Месо и колбаси"),
    ("meat", "Месо и колбаси"),
    ("sausage", "Месо и колбаси"),
    ("cold cut", "Месо и колбаси"),
    ("fish", "Риба и морски дарове"),
    ("seafood", "Риба и морски дарове"),
    ("shellfish", "Риба и морски дарове"),
    ("bread", "Хляб и зърнени"),
    ("roll", "Хляб и зърнени"),
    ("bagel", "Хляб и зърнени"),
    ("tortilla", "Хляб и зърнени"),
    ("cereal", "Зърнени закуски"),
    ("granola", "Зърнени закуски"),
    ("grain", "Паста и ориз"),
    ("rice", "Паста и ориз"),
    ("pasta", "Паста и ориз"),
    ("noodle", "Паста и ориз"),
    ("quick bread", "Тестени и сладкиши"),
    ("cookie", "Тестени и сладкиши"),
    ("cake", "Тестени и сладкиши"),
    ("pie", "Тестени и сладкиши"),
    ("pastr", "Тестени и сладкиши"),
    ("doughnut", "Тестени и сладкиши"),
    ("danish", "Тестени и сладкиши"),
    ("croissant", "Тестени и сладкиши"),
    ("cracker", "Снаксове"),
    ("chocolate", "Шоколад и бонбони"),
    ("candy", "Шоколад и бонбони"),
    ("ice cream", "Сладолед и десерти"),
    ("frozen dessert", "Сладолед и десерти"),
    ("pudding", "Сладолед и десерти"),
    ("gelatin", "Сладолед и десерти"),
    ("dessert", "Сладолед и десерти"),
    ("chips", "Снаксове"),
    ("snack", "Снаксове"),
    ("popcorn", "Снаксове"),
    ("pretzel", "Снаксове"),
    ("nut", "Ядки и семена"),
    ("seed", "Ядки и семена"),
    ("fat", "Мазнини и масла"),
    ("oil", "Мазнини и масла"),
    ("butter", "Мазнини и масла"),
    ("margarine", "Мазнини и масла"),
    ("salad dressing", "Сосове и подправки"),
    ("sauce", "Сосове и подправки"),
    ("condiment", "Сосове и подправки"),
    ("spice", "Сосове и подправки"),
    ("jam", "Сосове и подправки"),
    ("honey", "Сосове и подправки"),
    ("syrup", "Сосове и подправки"),
    ("sugar", "Сосове и подправки"),
    ("soup", "Готови ястия"),
    ("sandwich", "Готови ястия"),
    ("burger", "Готови ястия"),
    ("pizza", "Готови ястия"),
    ("meal", "Готови ястия"),
    ("mixture", "Готови ястия"),
    ("casserole", "Готови ястия"),
    ("stew", "Готови ястия"),
    ("beans", "Бобови и зърнени храни"),
    ("legume", "Бобови и зърнени храни"),
    ("peas", "Бобови и зърнени храни"),
    ("lentil", "Бобови и зърнени храни"),
    ("vegetable", "Плодове и зеленчуци"),
    ("fruit", "Плодове и зеленчуци"),
]

TRANSLIT = {
    "а": "a", "б": "b", "в": "v", "г": "g", "д": "d", "е": "e", "ж": "zh",
    "з": "z", "и": "i", "й": "y", "к": "k", "л": "l", "м": "m", "н": "n",
    "о": "o", "п": "p", "р": "r", "с": "s", "т": "t", "у": "u", "ф": "f",
    "х": "h", "ц": "ts", "ч": "ch", "ш": "sh", "щ": "sht", "ъ": "a", "ь": "",
    "ю": "yu", "я": "ya",
}
WORD_RE = re.compile(r"[A-Za-z0-9%'&./-]+")


def transliterate_latin(word: str) -> str:
    """Latin -> Cyrillic for loanwords we have no Bulgarian for (quinoa -> киноа)."""
    w = word.lower()
    out = []
    i = 0
    pairs = {
        "shch": "щ", "zh": "ж", "ch": "ч", "sh": "ш", "ts": "ц", "ya": "я",
        "yu": "ю", "yo": "ьо", "kh": "х", "ph": "ф", "th": "т", "ck": "к",
        "qu": "ку", "ee": "и", "oo": "у", "ai": "ей", "ea": "и",
    }
    letters = {
        "a": "а", "b": "б", "c": "к", "d": "д", "e": "е", "f": "ф", "g": "г",
        "h": "х", "i": "и", "j": "дж", "k": "к", "l": "л", "m": "м", "n": "н",
        "o": "о", "p": "п", "q": "к", "r": "р", "s": "с", "t": "т", "u": "у",
        "v": "в", "w": "у", "x": "кс", "y": "и", "z": "з",
    }
    while i < len(w):
        for src, dst in pairs.items():
            if w.startswith(src, i):
                out.append(dst)
                i += len(src)
                break
        else:
            char = w[i]
            out.append(letters.get(char, char))
            i += 1
    return "".join(out)


def load_glossary() -> tuple[dict, dict]:
    data = read_json(GLOSSARY)
    return data.get("phrases", {}), data.get("words", {})


def translate_description(desc: str, phrases: dict, words: dict):
    parts = [clean_text(p) for p in desc.split(",")]
    translated_parts: list[str] = []
    unknown: list[str] = []
    known = 0
    for part in parts:
        if not part:
            continue
        low = part.lower()
        if low in phrases:
            value = phrases[low]
            if value:
                translated_parts.append(value)
            known += 1
            continue
        tokens = WORD_RE.findall(part)
        if not tokens:
            continue
        pieces: list[str] = []
        for token in tokens:
            key = token.lower().strip(".'&-")
            if key in phrases:
                if phrases[key]:
                    pieces.append(phrases[key])
                known += 1
                continue
            if key in words:
                if words[key]:
                    pieces.append(words[key])
                known += 1
                continue
            if key in MEASURES:
                known += 1
                continue
            if key.isdigit() or re.fullmatch(r"\d+%", key):
                pieces.append(key)
                known += 1
                continue
            unknown.append(key)
            pieces.append(transliterate_latin(key))
        text = " ".join(p for p in pieces if p)
        text = re.sub(r"\s+", " ", text).strip()
        if not text:
            continue
        if translated_parts and normalize_key(translated_parts[-1]) == normalize_key(text):
            continue
        translated_parts.append(text)
    deduped: list[str] = []
    for part in translated_parts:
        if deduped and normalize_key(deduped[-1]) == normalize_key(part):
            continue
        deduped.append(part)
    name = ", ".join(deduped)
    name = name[:1].upper() + name[1:] if name else ""
    total = known + len(unknown)
    return name, unknown, total


def category_from_wweia(wweia: str) -> str:
    low = (wweia or "").lower()
    for keyword, category in WWEIA_CATEGORY_RULES:
        if keyword in low:
            return category
    return "Готови ястия"


def ensure_fndds() -> str:
    os.makedirs(CACHE, exist_ok=True)
    if not os.path.exists(ZIP_PATH):
        req = urllib.request.Request(FNDDS_URL, headers={"User-Agent": "BulgarCalTracker/1.1"})
        with urllib.request.urlopen(req, timeout=240) as resp:
            body = resp.read()
        with open(ZIP_PATH, "wb") as fh:
            fh.write(body)
        print(f"downloaded FNDDS survey foods ({len(body)} bytes)")
    return ZIP_PATH


def load_survey_foods():
    with zipfile.ZipFile(ensure_fndds()) as z:
        name = next(n for n in z.namelist() if n.endswith(".json"))
        data = json.loads(z.read(name))
    key = next(k for k in data if isinstance(data[k], list))
    return data[key]


def nutrient_values(food: dict) -> dict:
    values: dict[str, float] = {}
    for entry in food.get("foodNutrients") or []:
        nutrient = entry.get("nutrient") or {}
        number = str(nutrient.get("number") or "")
        nid = nutrient.get("id")
        amount = entry.get("amount")
        if amount is None:
            continue
        for field, (numbers, ids) in NUTRIENTS.items():
            if number and number in numbers:
                values.setdefault(field, float(amount))
            elif nid in ids:
                values.setdefault(field, float(amount))
    return values


def build_from_fndds(phrases: dict, words: dict):
    foods = load_survey_foods()
    rows = []
    stats = {"total": len(foods), "kept": 0, "dropped": 0, "perfect": 0,
             "partial": 0, "missing_nutrients": 0, "dropped_reasons": {}}
    for food in foods:
        desc = clean_text(food.get("description"))
        values = nutrient_values(food)
        needed = ("calories", "protein", "fats", "carbs")
        if any(values.get(k) is None for k in needed) or not values.get("calories"):
            stats["missing_nutrients"] += 1
            stats["dropped"] += 1
            stats["dropped_reasons"]["nutrients"] = stats["dropped_reasons"].get("nutrients", 0) + 1
            continue
        name, unknown, total = translate_description(desc, phrases, words)
        if len(name) < 3 or not re.search(r"[А-Яа-я]", name):
            stats["dropped"] += 1
            stats["dropped_reasons"]["untranslated"] = stats["dropped_reasons"].get("untranslated", 0) + 1
            continue
        if total and len(unknown) / total > 0.35:
            stats["dropped"] += 1
            stats["dropped_reasons"]["too-foreign"] = stats["dropped_reasons"].get("too-foreign", 0) + 1
            continue
        wweia = ((food.get("wweiaFoodCategory") or {}).get("wweiaFoodCategoryDescription") or "")
        category = category_from_wweia(wweia)
        calories = values["calories"]
        carbs = values["carbs"]
        protein = values["protein"]
        fats = values["fats"]
        fiber = values.get("fiber") or 0.0
        # FNDDS energy uses food-specific Atwater factors; a row whose energy is
        # not explicable from its macros would break the app's macro totals.
        if not atwater_ok(calories, carbs, protein, fats, fiber):
            stats["dropped"] += 1
            stats["dropped_reasons"]["energy"] = stats["dropped_reasons"].get("energy", 0) + 1
            continue
        sodium_mg = values.get("sodium") or 0.0
        salt = min(max(sodium_mg * 2.54 / 1000.0, 0.0), 12.0)
        gi = estimate_gi(category, carbs, fiber)
        row = food_row(
            name,
            category,
            calories,
            carbs,
            protein,
            fats,
            saturated_fats=values.get("saturated_fats") or 0.0,
            mono_fats=values.get("mono_fats") or 0.0,
            poly_fats=values.get("poly_fats") or 0.0,
            trans_fats=values.get("trans_fats") or 0.0,
            cholesterol=values.get("cholesterol") or 0.0,
            sugars=values.get("sugars") or 0.0,
            fiber=fiber,
            glycemic_index=gi,
            salt=salt,
            potassium=values.get("potassium") or 0.0,
            water=values.get("water") or 0.0,
            source="usda",
        )
        if unknown:
            stats["partial"] += 1
        else:
            stats["perfect"] += 1
        rows.append(row)
    stats["kept"] = len(rows)
    return rows, stats


# Short keys keep the hand-written ingredient table readable and small.
INGREDIENT_KEYS = {
    "cal": "calories", "c": "carbs", "p": "protein", "f": "fats",
    "sat": "saturated_fats", "sug": "sugars", "fib": "fiber", "salt": "salt",
}


def build_from_bg_dishes():
    """Bulgarian dishes: every value is the sum of its ingredients, so a recipe can
    be checked by hand. `cooked_grams` is the weight of the finished dish, which
    is what the per-100 g numbers are divided by (soups weigh more than their
    ingredients, roasts weigh less)."""
    if not os.path.exists(BG_DISHES):
        return [], {"curated": 0, "skipped": 0, "unknown_ingredients": []}
    data = read_json(BG_DISHES)
    table = data.get("ingredients") or {}
    missing: set[str] = set()
    rows = []
    skipped = 0
    for dish in data.get("dishes", []):
        ingredients = dish.get("ingredients") or {}
        totals = {key: 0.0 for key in INGREDIENT_KEYS.values()}
        weight = 0.0
        for ingredient, grams in ingredients.items():
            if ingredient in {"вода", "вода за супа", "бульон"}:
                per100 = {"calories": 0.0}
            else:
                per100 = table.get(ingredient)
            if per100 is None:
                missing.add(ingredient)
                continue
            factor = float(grams) / 100.0
            weight += float(grams)
            for short, key in INGREDIENT_KEYS.items():
                totals[key] += float(per100.get(short) or 0.0) * factor
        if weight <= 0 or not ingredients:
            skipped += 1
            continue
        cooked_weight = float(dish.get("cooked_grams") or weight)
        per100_factor = 100.0 / cooked_weight if cooked_weight else 100.0 / weight
        category = dish.get("category") or "Домашни ястия"
        if category not in CATEGORIES:
            category = "Домашни ястия"
        gi_category = "Домашни ястия" if category == "Готови ястия" else category
        row = food_row(
            dish["name"],
            category,
            totals["calories"] * per100_factor,
            totals["carbs"] * per100_factor,
            totals["protein"] * per100_factor,
            totals["fats"] * per100_factor,
            saturated_fats=totals["saturated_fats"] * per100_factor,
            sugars=totals["sugars"] * per100_factor,
            fiber=totals["fiber"] * per100_factor,
            glycemic_index=estimate_gi(gi_category, totals["carbs"] * per100_factor,
                                       totals["fiber"] * per100_factor),
            salt=totals["salt"] * per100_factor,
            source="curated",
        )
        rows.append(row)
    return rows, {
        "curated": len(rows),
        "skipped": skipped,
        "unknown_ingredients": sorted(missing),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--min", type=int, default=None)
    args = parser.parse_args()

    phrases, words = load_glossary()
    fndds_rows, fndds_stats = build_from_fndds(phrases, words)
    bg_rows, bg_stats = build_from_bg_dishes()

    # The 92 curated Bulgarian foods in foods.json win any name collision
    # (Айрян, Боб чорба, ...): one food must exist exactly once in the app.
    foods_path = os.path.join(ASSETS, "foods.json")
    existing = {normalize_key(row["name"]) for row in read_json(foods_path)}
    fndds_rows = [row for row in fndds_rows if normalize_key(row["name"]) not in existing]
    bg_rows = [row for row in bg_rows if normalize_key(row["name"]) not in existing]

    rows: list[dict] = []
    seen: set[str] = set(existing)
    duplicates = 0
    for row in bg_rows + fndds_rows:
        key = normalize_key(row["name"])
        if key in seen:
            duplicates += 1
            continue
        seen.add(key)
        rows.append(row)

    rows.sort(key=lambda r: (r["category"], r["name"].lower()))
    out_path = os.path.join(ASSETS, "dishes.json")
    size = write_json_array(out_path, rows)
    by_category: dict[str, int] = {}
    for row in rows:
        by_category[row["category"]] = by_category.get(row["category"], 0) + 1
    summary = {
        "total": len(rows),
        "bytes": size,
        "fndds": fndds_stats,
        "bulgarian": bg_stats,
        "duplicates_dropped": duplicates,
        "by_category": dict(sorted(by_category.items(), key=lambda kv: -kv[1])),
    }
    os.makedirs(CACHE, exist_ok=True)
    with open(os.path.join(CACHE, "dishes_build.json"), "w", encoding="utf-8") as fh:
        json.dump(summary, fh, ensure_ascii=False, indent=2)
    print(f"wrote {out_path}: {len(rows)} dishes, {size} bytes")
    print("FNDDS:", fndds_stats)
    print("Bulgarian:", bg_stats)
    for cat, count in summary["by_category"].items():
        print(f"  {cat}: {count}")
    if args.min is not None and len(rows) < args.min:
        print(f"FAIL: only {len(rows)} dishes, need at least {args.min}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
