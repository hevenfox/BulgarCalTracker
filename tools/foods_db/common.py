"""Shared helpers for building BulgarCalTracker's food database.

Everything here is data-build tooling that runs on a desktop, never on the phone.
Values written to the assets are per 100 g / 100 ml, matching FoodSeeder.
"""

from __future__ import annotations

import json
import math
import os
import re
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
CACHE = os.path.join(HERE, "cache")

# ---------------------------------------------------------------------------
# Category taxonomy. Index 0 is special ("Моите Рецепти" = user recipes) and is
# rendered separately, so it must stay first. This list must stay in sync with
# FoodCategories in app/src/main/java/com/bulgarcal/tracker/domain/Models.kt.
# ---------------------------------------------------------------------------
CATEGORIES = [
    "Моите Рецепти",
    "Протеинови барове",
    "Спортни добавки",
    "Заместители на храна",
    "Енергийни напитки",
    "Безалкохолни напитки",
    "Кафе и чай",
    "Вода",
    "Кисели млека и скир",
    "Млечни продукти",
    "Сирена и кашкавал",
    "Месо и колбаси",
    "Риба и морски дарове",
    "Яйца",
    "Хляб и зърнени",
    "Зърнени закуски",
    "Паста и ориз",
    "Тестени и сладкиши",
    "Шоколад и бонбони",
    "Сладолед и десерти",
    "Снаксове",
    "Ядки и семена",
    "Мазнини и масла",
    "Сосове и подправки",
    "Готови ястия",
    "Домашни ястия",
    "Детски храни",
    "Плодове и зеленчуци",
    "Бобови и зърнени храни",
    "Алкохол",
    "Общи",
]
DEFAULT_CATEGORY = "Общи"

# 100 g of pure fat is ~900 kcal (USDA reports 902 for lard/shortening), so the
# sanity ceiling has to sit just above that rather than at 900.
MAX_CALORIES_PER_100G = 910

# OFF category tag -> our category, most specific tags first. The first tag in a
# product's categories_tags that appears in this list wins.
OFF_TAG_CATEGORY: dict[str, str] = {
    # fitness
    "en:protein-bars": "Протеинови барове",
    "en:cereal-bars": "Протеинови барове",
    "en:energy-bars": "Протеинови барове",
    "en:protein-powders": "Спортни добавки",
    "en:whey-protein": "Спортни добавки",
    "en:dietary-supplements": "Спортни добавки",
    "en:creatine": "Спортни добавки",
    "en:protein-shakes": "Заместители на храна",
    "en:meal-replacements": "Заместители на храна",
    "en:energy-drinks": "Енергийни напитки",
    "en:sports-drinks": "Енергийни напитки",
    "en:isotonic-drinks": "Енергийни напитки",
    # drinks
    "en:sodas": "Безалкохолни напитки",
    "en:fruit-juices": "Безалкохолни напитки",
    "en:syrups": "Безалкохолни напитки",
    "en:plant-based-milks": "Безалкохолни напитки",
    "en:mineral-waters": "Вода",
    "en:spring-waters": "Вода",
    "en:waters": "Вода",
    "en:branded-waters": "Вода",
    "en:iced-coffees": "Кафе и чай",
    "en:coffees": "Кафе и чай",
    "en:teas": "Кафе и чай",
    "en:coffee-based-beverages": "Кафе и чай",
    "en:beers": "Алкохол",
    "en:wines": "Алкохол",
    "en:spirits": "Алкохол",
    "en:alcoholic-beverages": "Алкохол",
    # dairy
    "en:yogurts": "Кисели млека и скир",
    "en:skyr": "Кисели млека и скир",
    "en:quarks": "Кисели млека и скир",
    "en:greek-yogurts": "Кисели млека и скир",
    "en:fermented-milk-products": "Кисели млека и скир",
    "en:yogurt": "Кисели млека и скир",
    "en:kefir": "Кисели млека и скир",
    "en:dairy-desserts": "Сладолед и десерти",
    "en:milks": "Млечни продукти",
    "en:creams": "Млечни продукти",
    "en:condensed-milks": "Млечни продукти",
    "en:cheeses": "Сирена и кашкавал",
    "en:fresh-cheeses": "Сирена и кашкавал",
    "en:hard-cheeses": "Сирена и кашкавал",
    "en:fermented-dairy-products": "Сирена и кашкавал",
    # meat / fish / eggs
    "en:meats": "Месо и колбаси",
    "en:hams": "Месо и колбаси",
    "en:sausages": "Месо и колбаси",
    "en:poultry": "Месо и колбаси",
    "en:charcuterie": "Месо и колбаси",
    "en:salmons": "Риба и морски дарове",
    "en:fishes": "Риба и морски дарове",
    "en:canned-fishes": "Риба и морски дарове",
    "en:seafood": "Риба и морски дарове",
    "en:eggs": "Яйца",
    # grains
    "en:breads": "Хляб и зърнени",
    "en:breakfast-cereals": "Зърнени закуски",
    "en:mueslis": "Зърнени закуски",
    "en:pastas": "Паста и ориз",
    "en:rices": "Паста и ориз",
    "en:cereal-grains": "Паста и ориз",
    "en:legumes": "Бобови и зърнени храни",
    "en:pulses": "Бобови и зърнени храни",
    # bakery / sweets
    "en:croissants": "Тестени и сладкиши",
    "en:viennoiseries": "Тестени и сладкиши",
    "en:pastries": "Тестени и сладкиши",
    "en:cakes": "Тестени и сладкиши",
    "en:biscuits": "Тестени и сладкиши",
    "en:cookies": "Тестени и сладкиши",
    "en:waffles": "Тестени и сладкиши",
    "en:chocolates": "Шоколад и бонбони",
    "en:chocolate-bars": "Шоколад и бонбони",
    "en:candies": "Шоколад и бонбони",
    "en:confectioneries": "Шоколад и бонбони",
    "en:ice-creams": "Сладолед и десерти",
    "en:desserts": "Сладолед и десерти",
    # snacks
    "en:crisps": "Снаксове",
    "en:salty-snacks": "Снаксове",
    "en:popcorn": "Снаксове",
    "en:appetizers": "Снаксове",
    "en:nuts": "Ядки и семена",
    "en:seeds": "Ядки и семена",
    "en:peanut-butters": "Ядки и семена",
    # fats / sauces
    "en:butters": "Мазнини и масла",
    "en:vegetable-oils": "Мазнини и масла",
    "en:margarines": "Мазнини и масла",
    "en:fats": "Мазнини и масла",
    "en:sauces": "Сосове и подправки",
    "en:condiments": "Сосове и подправки",
    "en:dressings": "Сосове и подправки",
    "en:spices": "Сосове и подправки",
    "en:herbs": "Сосове и подправки",
    # prepared
    "en:meals": "Готови ястия",
    "en:prepared-meals": "Готови ястия",
    "en:pizzas": "Готови ястия",
    "en:sandwiches": "Готови ястия",
    "en:soups": "Готови ястия",
    "en:salads": "Готови ястия",
    "en:frozen-foods": "Готови ястия",
    # baby
    "en:baby-foods": "Детски храни",
    "en:infant-formulas": "Детски храни",
    "en:baby-milks": "Детски храни",
    # produce
    "en:fruits": "Плодове и зеленчуци",
    "en:vegetables": "Плодове и зеленчуци",
}
# Specificity order: intent tags (protein bars, skyr) must beat generic parents
# (dietary supplements, dairy) that OFF products are usually tagged with too.
# OFF tags a product from parent to child, so "most specific" is decided here.
# Order matters twice: intent tags first (a protein bar must not be filed as a
# generic supplement), then what the product *is* (fish, cheese) before what was
# done to it (frozen, canned), and form/meal tags last.
OFF_TAG_PRIORITY = [
    "en:protein-bars", "en:energy-bars", "en:cereal-bars", "en:protein-powders",
    "en:whey-protein", "en:creatine", "en:protein-shakes", "en:meal-replacements",
    "en:dietary-supplements", "en:energy-drinks", "en:isotonic-drinks",
    "en:sports-drinks", "en:skyr", "en:greek-yogurts", "en:quarks", "en:yogurt",
    "en:yogurts", "en:kefir", "en:fermented-milk-products", "en:dairy-desserts",
    "en:coffee-based-beverages", "en:iced-coffees", "en:coffees", "en:teas",
    "en:mineral-waters", "en:spring-waters", "en:branded-waters", "en:waters",
    "en:sodas", "en:syrups", "en:fruit-juices", "en:plant-based-milks",
    "en:cheeses", "en:fresh-cheeses", "en:hard-cheeses",
    "en:fermented-dairy-products", "en:milks", "en:creams", "en:condensed-milks",
    "en:eggs", "en:meats", "en:hams", "en:sausages", "en:poultry",
    "en:charcuterie", "en:salmons", "en:fishes", "en:canned-fishes",
    "en:seafood", "en:legumes", "en:pulses", "en:nuts", "en:seeds",
    "en:peanut-butters", "en:fruits", "en:vegetables",
    "en:breakfast-cereals", "en:mueslis", "en:breads", "en:pastas", "en:rices",
    "en:cereal-grains", "en:croissants", "en:viennoiseries", "en:pastries",
    "en:cakes", "en:biscuits", "en:cookies", "en:waffles", "en:chocolates",
    "en:chocolate-bars", "en:candies", "en:confectioneries", "en:ice-creams",
    "en:desserts", "en:crisps", "en:salty-snacks", "en:popcorn",
    "en:appetizers", "en:butters", "en:vegetable-oils", "en:margarines",
    "en:fats", "en:sauces", "en:condiments", "en:dressings", "en:spices",
    "en:herbs", "en:meals", "en:prepared-meals", "en:pizzas", "en:sandwiches",
    "en:soups", "en:salads", "en:frozen-foods", "en:baby-foods",
    "en:infant-formulas", "en:baby-milks", "en:beers", "en:wines", "en:spirits",
    "en:alcoholic-beverages",
]
OFF_TAG_RANK = {tag: index for index, tag in enumerate(OFF_TAG_PRIORITY)}

# Categories where a per-100 g estimate of glycemic index is meaningful at all.
# GI is not sold on any label, so it is a documented category-level estimate and
# is only filled in when the product carries a real amount of available carbs.
GI_BY_CATEGORY: dict[str, float] = {
    "Протеинови барове": 45.0,
    "Спортни добавки": 30.0,
    "Заместители на храна": 40.0,
    "Енергийни напитки": 65.0,
    "Безалкохолни напитки": 60.0,
    "Кафе и чай": 55.0,
    "Вода": 0.0,
    "Кисели млека и скир": 35.0,
    "Млечни продукти": 30.0,
    "Сирена и кашкавал": 0.0,
    "Месо и колбаси": 0.0,
    "Риба и морски дарове": 0.0,
    "Яйца": 0.0,
    "Хляб и зърнени": 70.0,
    "Зърнени закуски": 60.0,
    "Паста и ориз": 55.0,
    "Тестени и сладкиши": 65.0,
    "Шоколад и бонбони": 55.0,
    "Сладолед и десерти": 60.0,
    "Снаксове": 60.0,
    "Ядки и семена": 20.0,
    "Мазнини и масла": 0.0,
    "Сосове и подправки": 40.0,
    "Готови ястия": 55.0,
    "Домашни ястия": 55.0,
    "Детски храни": 55.0,
    "Плодове и зеленчуци": 35.0,
    "Бобови и зърнени храни": 35.0,
    "Алкохол": 0.0,
    "Общи": 0.0,
}

# A small Europe set, used to prefer products actually sold where the app is used.
EUROPEAN_COUNTRIES = {
    "en:bulgaria", "en:romania", "en:greece", "en:germany", "en:netherlands",
    "en:belgium", "en:france", "en:italy", "en:spain", "en:portugal", "en:poland",
    "en:czech-republic", "en:slovakia", "en:slovenia", "en:hungary", "en:croatia",
    "en:serbia", "en:macedonia", "en:north-macedonia", "en:albania", "en:turkey",
    "en:austria", "en:switzerland", "en:denmark", "en:sweden", "en:norway",
    "en:finland", "en:ireland", "en:united-kingdom", "en:estonia", "en:latvia",
    "en:lithuania", "en:ukraine", "en:moldova", "en:bosnia-and-herzegovina",
    "en:montenegro", "en:iceland", "en:luxembourg", "en:malta", "en:cyprus",
}

JUNK_NAMES = {
    "", "-", "--", "n/a", "na", "unknown", "неизвестно", "няма данни",
    "test", "test product", "product", "продукт", "food", "храна", "item",
    "todo", "asdf", "xxx", "нет налични данни", "без име", "no name",
}
NON_LATIN_CYRILLIC = re.compile(
    "[\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\uac00-\ud7af"
    "\u0600-\u06ff\u0e00-\u0e7f\u0900-\u097f\u0590-\u05ff]"
)
WHITESPACE = re.compile(r"\s+")
QUANTITY = re.compile(
    r"^\s*(\d+(?:[.,]\d+)?)\s*(x\s*\d+(?:[.,]\d+)?\s*)?"
    r"(g|gr|gram|grams|kg|ml|l|cl|dl|oz|lb|pcs|бр|гр|кг|мл|л)\s*$",
    re.IGNORECASE,
)
PACKAGE_IN_NAME = re.compile(
    r"(\d+(?:[.,]\d+)?)\s*(x\s*\d+(?:[.,]\d+)?\s*)?(kg|g|gr|ml|l|cl|dl)\b",
    re.IGNORECASE,
)


def normalize_key(text: str) -> str:
    """Case/diacritic/punctuation-insensitive key used for dedupe."""
    text = unicodedata.normalize("NFKD", text.lower())
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = re.sub(r"[^0-9a-zа-я]+", " ", text)
    return WHITESPACE.sub(" ", text).strip()


def clean_text(text) -> str:
    if not isinstance(text, str):
        return ""
    text = WHITESPACE.sub(" ", text.replace("\u00a0", " ")).strip()
    return text.strip(" ,.;:-–—|*_")


def clean_quantity(text) -> str:
    if not isinstance(text, str):
        return ""
    q = clean_text(text).lower().replace(",", ".")
    q = re.sub(r"(?<=\d)(?=[a-zа-я])", " ", q)
    q = WHITESPACE.sub(" ", q).strip()
    if len(q) > 24 or not re.search(r"\d", q):
        return ""
    if not QUANTITY.match(q) and not re.match(r"^\d+(\.\d+)?\s*x\s*\d+", q):
        return ""
    return q


def round_num(value, digits: int):
    if value is None:
        return None
    try:
        f = float(value)
    except (TypeError, ValueError):
        return None
    if math.isnan(f) or math.isinf(f):
        return None
    return round(f, digits)


def energy_estimate(carbs, protein, fats, fiber=0.0) -> float:
    """kcal implied by the macros, counting fibre at 2 kcal/g (Atwater/EU rule).

    Sources disagree on whether "carbohydrate" already includes fibre: USDA's
    "carbohydrate by difference" does, OpenFoodFacts usually does not. Counting
    fibre separately makes both readings comparable with the stated energy.
    """
    fiber = min(max(fiber or 0.0, 0.0), carbs or 0.0)
    return 4.0 * (carbs - fiber) + 2.0 * fiber + 4.0 * protein + 9.0 * fats


def atwater_ok(calories, carbs, protein, fats, fiber=0.0, tolerance=0.22) -> bool:
    """Reject rows whose stated energy cannot come from their macros.

    Alcohol carries 7 kcal/g that macros do not describe, so rows in the
    Алкохол category are exempted at the call site (see validate.py).
    """
    if None in (calories, carbs, protein, fats):
        return False
    computed = energy_estimate(carbs, protein, fats, fiber)
    return abs(computed - calories) <= max(25.0, tolerance * calories)


def in_range(value, low, high) -> bool:
    return value is not None and low <= value <= high


def category_for_off_tags(tags, fallback: str) -> str:
    if isinstance(tags, str):
        tags = [tags]
    tags = tags or []
    ranked = []
    for tag in tags:
        if not isinstance(tag, str):
            continue
        if tag in OFF_TAG_CATEGORY:
            ranked.append((OFF_TAG_RANK.get(tag, len(OFF_TAG_RANK)), tag))
    if not ranked:
        return fallback
    ranked.sort()
    return OFF_TAG_CATEGORY[ranked[0][1]]


def is_european(countries_tags) -> bool:
    if not countries_tags:
        return False
    return any(c in EUROPEAN_COUNTRIES for c in countries_tags)


def estimate_gi(category: str, carbs, fiber) -> float:
    base = GI_BY_CATEGORY.get(category, 0.0)
    available = (carbs or 0.0) - (fiber or 0.0)
    if base <= 0 or available < 2.0:
        return 0.0
    return base


def glycemic_load(gi, carbs, fiber) -> float:
    if not gi:
        return 0.0
    available = max(0.0, (carbs or 0.0) - (fiber or 0.0))
    return round(gi * available / 100.0, 2)


def write_json_array(path: str, rows: list[dict]) -> int:
    """Write one compact object per line inside a JSON array; returns bytes."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    body = "[\n" + ",\n".join(
        json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in rows
    ) + "\n]\n"
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(body)
    return len(body.encode("utf-8"))


def read_json(path: str):
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def food_row(
    name: str,
    category: str,
    calories,
    carbs,
    protein,
    fats,
    *,
    saturated_fats=0.0,
    mono_fats=0.0,
    poly_fats=0.0,
    trans_fats=0.0,
    cholesterol=0.0,
    sugars=0.0,
    fiber=0.0,
    glycemic_index=0.0,
    salt=0.0,
    potassium=0.0,
    water=0.0,
    source: str = "",
) -> dict:
    """One asset row. Zero-valued optional keys are dropped: the seeder defaults
    every missing numeric key to 0, so the file stays small without losing
    meaning. `name`/`category`/`calories`/macros are always written."""
    row: dict = {
        "name": name,
        "category": category,
        "calories": int(round(calories)),
        "carbs": round_num(carbs, 2) or 0.0,
        "protein": round_num(protein, 2) or 0.0,
        "fats": round_num(fats, 2) or 0.0,
    }
    optional = {
        "saturated_fats": round_num(saturated_fats, 2),
        "mono_fats": round_num(mono_fats, 2),
        "poly_fats": round_num(poly_fats, 2),
        "trans_fats": round_num(trans_fats, 2),
        "cholesterol": round_num(cholesterol, 1),
        "sugars": round_num(sugars, 2),
        "fiber": round_num(fiber, 2),
        "glycemic_index": round_num(glycemic_index, 1),
        "glycemic_load": glycemic_load(glycemic_index, carbs, fiber),
        "salt": round_num(salt, 2),
        "potassium": round_num(potassium, 1),
        "water": round_num(water, 1),
        "source": source or None,
    }
    for key, value in optional.items():
        if value in (None, 0, 0.0, ""):
            continue
        row[key] = value
    return row
