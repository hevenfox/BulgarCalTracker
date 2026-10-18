#!/usr/bin/env python3
"""Build app/src/main/assets/products.json from Open Food Facts.

Source: search.openfoodfacts.org (data: ODbL 1.0 + DbCL 1.0, attribution text in
THIRD-PARTY-DATA.md). Only rows whose per-100 g nutrition is complete AND whose
stated energy agrees with its macros (Atwater check) are kept, so the file is a
subset of OFF, not a copy of it: the derived database is published back with the
app under the ODbL, which satisfies the share-alike obligation.

Usage:
  python tools/foods_db/build_products.py                 # cached, incremental
  python tools/foods_db/build_products.py --refresh       # refetch every page
"""

from __future__ import annotations

import argparse
import gzip
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import (  # noqa: E402
    ASSETS,
    CACHE,
    MAX_CALORIES_PER_100G,
    CATEGORIES,
    DEFAULT_CATEGORY,
    JUNK_NAMES,
    NON_LATIN_CYRILLIC,
    atwater_ok,
    category_for_off_tags,
    clean_quantity,
    clean_text,
    estimate_gi,
    food_row,
    is_european,
    normalize_key,
    write_json_array,
)

UA = "BulgarCalTracker/1.1 (food database build; https://github.com/hevenfox/BulgarCalTracker)"
ENDPOINT = "https://search.openfoodfacts.org/search?"
PAGE_SIZE = 1000
TARGET = 26000  # selected rows we aim for; validation requires >= 25000
SLEEP = 1.0
RETRIES = 4

NUT = (
    ' AND nutriments.energy-kcal_100g:* AND nutriments.proteins_100g:*'
    ' AND nutriments.carbohydrates_100g:* AND nutriments.fat_100g:*'
)
COMPLETE = 'nutriments.energy-kcal_100g:* AND nutriments.proteins_100g:*' \
           ' AND nutriments.carbohydrates_100g:* AND nutriments.fat_100g:*'

# key, extra OFF query, category, quota (rows kept), max pages, top-up priority
SEGMENTS = [
    # -- what the app is used for most: fitness products ----------------------
    {"key": "protein_bars", "q": 'categories_tags:"en:protein-bars"', "cat": "Протеинови барове", "quota": 1500, "max": 4, "topup": 5},
    {"key": "protein_powders", "q": 'categories_tags:"en:protein-powders"', "cat": "Спортни добавки", "quota": 1300, "max": 4, "topup": 6},
    {"key": "supplements", "q": 'categories_tags:"en:dietary-supplements"', "cat": "Спортни добавки", "quota": 700, "max": 3, "topup": 7},
    {"key": "shakes_mrp", "q": '(categories_tags:"en:protein-shakes" OR categories_tags:"en:meal-replacements")', "cat": "Заместители на храна", "quota": 400, "max": 2, "topup": 4},
    {"key": "energy_drinks", "q": 'categories_tags:"en:energy-drinks"', "cat": "Енергийни напитки", "quota": 1100, "max": 3, "topup": 5},
    {"key": "bars_cereal", "q": 'categories_tags:"en:cereal-bars"', "cat": "Протеинови барове", "quota": 300, "max": 2, "topup": 3},
    # -- drinks ---------------------------------------------------------------
    {"key": "sodas", "q": 'categories_tags:"en:sodas"', "cat": "Безалкохолни напитки", "quota": 900, "max": 3, "topup": 6},
    {"key": "juices", "q": 'categories_tags:"en:fruit-juices"', "cat": "Безалкохолни напитки", "quota": 900, "max": 3, "topup": 6},
    {"key": "coffees", "q": '(categories_tags:"en:coffees" OR categories_tags:"en:iced-coffees")', "cat": "Кафе и чай", "quota": 700, "max": 3, "topup": 6},
    {"key": "teas", "q": 'categories_tags:"en:teas"', "cat": "Кафе и чай", "quota": 450, "max": 2, "topup": 4},
    {"key": "waters", "q": '(categories_tags:"en:mineral-waters" OR categories_tags:"en:spring-waters")', "cat": "Вода", "quota": 500, "max": 3, "topup": 5},
    # -- dairy (the user's "different skyrs") ---------------------------------
    {"key": "yogurts", "q": 'categories_tags:"en:yogurts"', "cat": "Кисели млека и скир", "quota": 1800, "max": 4, "topup": 7},
    {"key": "quarks", "q": 'categories_tags:"en:quarks"', "cat": "Кисели млека и скир", "quota": 700, "max": 2, "topup": 4},
    {"key": "fermented", "q": 'categories_tags:"en:fermented-milk-products"', "cat": "Кисели млека и скир", "quota": 900, "max": 3, "topup": 5},
    {"key": "dairy_desserts", "q": 'categories_tags:"en:dairy-desserts"', "cat": "Сладолед и десерти", "quota": 700, "max": 3, "topup": 5},
    {"key": "milks", "q": 'categories_tags:"en:milks"', "cat": "Млечни продукти", "quota": 700, "max": 3, "topup": 5},
    {"key": "cheeses", "q": '(categories_tags:"en:cheeses" OR categories_tags:"en:fresh-cheeses")', "cat": "Сирена и кашкавал", "quota": 1000, "max": 4, "topup": 6},
    # -- bakery, sweets, snacks ----------------------------------------------
    {"key": "breads", "q": 'categories_tags:"en:breads"', "cat": "Хляб и зърнени", "quota": 600, "max": 3, "topup": 5},
    {"key": "cereals", "q": 'categories_tags:"en:breakfast-cereals"', "cat": "Зърнени закуски", "quota": 700, "max": 3, "topup": 6},
    {"key": "croissants", "q": 'categories_tags:"en:croissants"', "cat": "Тестени и сладкиши", "quota": 600, "max": 3, "topup": 5},
    {"key": "viennoiseries", "q": 'categories_tags:"en:viennoiseries"', "cat": "Тестени и сладкиши", "quota": 500, "max": 2, "topup": 4},
    {"key": "pastries", "q": 'categories_tags:"en:pastries"', "cat": "Тестени и сладкиши", "quota": 700, "max": 3, "topup": 5},
    {"key": "cakes", "q": 'categories_tags:"en:cakes"', "cat": "Тестени и сладкиши", "quota": 600, "max": 3, "topup": 5},
    {"key": "biscuits", "q": 'categories_tags:"en:biscuits"', "cat": "Тестени и сладкиши", "quota": 600, "max": 3, "topup": 5},
    {"key": "chocolates", "q": 'categories_tags:"en:chocolates"', "cat": "Шоколад и бонбони", "quota": 800, "max": 3, "topup": 6},
    {"key": "candies", "q": 'categories_tags:"en:candies"', "cat": "Шоколад и бонбони", "quota": 600, "max": 3, "topup": 5},
    {"key": "ice_creams", "q": 'categories_tags:"en:ice-creams"', "cat": "Сладолед и десерти", "quota": 600, "max": 3, "topup": 5},
    {"key": "crisps", "q": 'categories_tags:"en:crisps"', "cat": "Снаксове", "quota": 500, "max": 2, "topup": 4},
    {"key": "salty_snacks", "q": 'categories_tags:"en:salty-snacks"', "cat": "Снаксове", "quota": 450, "max": 2, "topup": 4},
    {"key": "nuts", "q": 'categories_tags:"en:nuts"', "cat": "Ядки и семена", "quota": 400, "max": 2, "topup": 4},
    # -- savoury / meals -----------------------------------------------------
    {"key": "hams", "q": 'categories_tags:"en:hams"', "cat": "Месо и колбаси", "quota": 600, "max": 3, "topup": 5},
    {"key": "sausages", "q": 'categories_tags:"en:sausages"', "cat": "Месо и колбаси", "quota": 600, "max": 3, "topup": 5},
    {"key": "salmons", "q": '(categories_tags:"en:salmons" OR categories_tags:"en:canned-fishes")', "cat": "Риба и морски дарове", "quota": 400, "max": 2, "topup": 4},
    {"key": "sauces", "q": 'categories_tags:"en:sauces"', "cat": "Сосове и подправки", "quota": 600, "max": 3, "topup": 5},
    {"key": "condiments", "q": 'categories_tags:"en:condiments"', "cat": "Сосове и подправки", "quota": 400, "max": 2, "topup": 4},
    {"key": "soups", "q": 'categories_tags:"en:soups"', "cat": "Готови ястия", "quota": 350, "max": 2, "topup": 4},
    {"key": "meals", "q": 'categories_tags:"en:meals"', "cat": "Готови ястия", "quota": 600, "max": 3, "topup": 5},
    {"key": "pizzas", "q": 'categories_tags:"en:pizzas"', "cat": "Готови ястия", "quota": 400, "max": 2, "topup": 4},
    {"key": "sandwiches", "q": 'categories_tags:"en:sandwiches"', "cat": "Готови ястия", "quota": 300, "max": 2, "topup": 3},
    {"key": "butters_fats", "q": '(categories_tags:"en:butters" OR categories_tags:"en:peanut-butters")', "cat": "Мазнини и масла", "quota": 350, "max": 2, "topup": 4},
    {"key": "pastas", "q": 'categories_tags:"en:pastas"', "cat": "Паста и ориз", "quota": 450, "max": 2, "topup": 4},
    {"key": "rices", "q": 'categories_tags:"en:rices"', "cat": "Паста и ориз", "quota": 350, "max": 2, "topup": 4},
    {"key": "legumes", "q": 'categories_tags:"en:legumes"', "cat": "Бобови и зърнени храни", "quota": 250, "max": 2, "topup": 3},
    {"key": "baby", "q": 'categories_tags:"en:baby-foods"', "cat": "Детски храни", "quota": 500, "max": 2, "topup": 4},
    {"key": "beers_wines", "q": '(categories_tags:"en:beers" OR categories_tags:"en:wines")', "cat": "Алкохол", "quota": 250, "max": 2, "topup": 3},
    # -- brands the user named, so they are guaranteed to be present ----------
    {"key": "brand_monster", "q": 'brands_tags:"monster"', "cat": "Енергийни напитки", "quota": 160, "max": 1, "topup": 0},
    {"key": "brand_redbull", "q": 'brands_tags:"red-bull"', "cat": "Енергийни напитки", "quota": 160, "max": 1, "topup": 0},
    {"key": "brand_hell", "q": 'brands_tags:"hell"', "cat": "Енергийни напитки", "quota": 90, "max": 1, "topup": 0},
    {"key": "brand_burn", "q": 'brands_tags:"burn"', "cat": "Енергийни напитки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_rockstar", "q": 'brands_tags:"rockstar"', "cat": "Енергийни напитки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_barebells", "q": 'brands_tags:"barebells"', "cat": "Протеинови барове", "quota": 150, "max": 1, "topup": 0},
    {"key": "brand_grenade", "q": 'brands_tags:"grenade"', "cat": "Протеинови барове", "quota": 60, "max": 1, "topup": 0},
    {"key": "brand_myprotein", "q": 'brands_tags:"myprotein"', "cat": "Спортни добавки", "quota": 250, "max": 2, "topup": 2},
    {"key": "brand_optimum", "q": 'brands_tags:"optimum-nutrition"', "cat": "Спортни добавки", "quota": 120, "max": 1, "topup": 0},
    {"key": "brand_dymatize", "q": 'brands_tags:"dymatize"', "cat": "Спортни добавки", "quota": 100, "max": 1, "topup": 0},
    {"key": "brand_biotech", "q": 'brands_tags:"biotech-usa"', "cat": "Спортни добавки", "quota": 100, "max": 1, "topup": 0},
    {"key": "brand_promin", "q": 'brands_tags:"prom-in"', "cat": "Спортни добавки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_activlab", "q": 'brands_tags:"activ-lab"', "cat": "Спортни добавки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_amix", "q": 'brands_tags:"amix"', "cat": "Спортни добавки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_nutrend", "q": 'brands_tags:"nutrend"', "cat": "Спортни добавки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_cocacola", "q": 'brands_tags:"coca-cola"', "cat": "Безалкохолни напитки", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_pepsi", "q": 'brands_tags:"pepsi"', "cat": "Безалкохолни напитки", "quota": 60, "max": 1, "topup": 0},
    {"key": "brand_fanta", "q": 'brands_tags:"fanta"', "cat": "Безалкохолни напитки", "quota": 50, "max": 1, "topup": 0},
    {"key": "brand_sprite", "q": 'brands_tags:"sprite"', "cat": "Безалкохолни напитки", "quota": 50, "max": 1, "topup": 0},
    {"key": "brand_lays", "q": 'brands_tags:"lay-s"', "cat": "Снаксове", "quota": 60, "max": 1, "topup": 0},
    {"key": "brand_chio", "q": 'brands_tags:"chio"', "cat": "Снаксове", "quota": 60, "max": 1, "topup": 0},
    {"key": "brand_haribo", "q": 'brands_tags:"haribo"', "cat": "Шоколад и бонбони", "quota": 60, "max": 1, "topup": 0},
    {"key": "brand_milka", "q": 'brands_tags:"milka"', "cat": "Шоколад и бонбони", "quota": 60, "max": 1, "topup": 0},
    {"key": "brand_snickers", "q": 'brands_tags:"snickers"', "cat": "Шоколад и бонбони", "quota": 50, "max": 1, "topup": 0},
    {"key": "brand_milbona", "q": 'brands_tags:"milbona"', "cat": "Млечни продукти", "quota": 250, "max": 2, "topup": 2},
    {"key": "brand_arla", "q": 'brands_tags:"arla"', "cat": "Кисели млека и скир", "quota": 120, "max": 1, "topup": 0},
    {"key": "brand_danone", "q": 'brands_tags:"danone"', "cat": "Кисели млека и скир", "quota": 100, "max": 1, "topup": 0},
    {"key": "brand_fage", "q": 'brands_tags:"fage"', "cat": "Кисели млека и скир", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_lindahls", "q": 'brands_tags:"lindahls"', "cat": "Кисели млека и скир", "quota": 80, "max": 1, "topup": 0},
    {"key": "brand_olympus", "q": 'brands_tags:"olympus"', "cat": "Кисели млека и скир", "quota": 80, "max": 1, "topup": 0},
    # -- everything Bulgarian, last so the specific segments above win --------
    {"key": "bg_market", "q": 'countries_tags:"en:bulgaria"', "cat": None, "quota": 4500, "max": 7, "topup": 8},
]

# Distinctive non-English tokens (function words and nouns that are not shared
# with English). Rows carrying them are ranked last inside their segment so the
# database stays readable without dropping volume a segment may still need.
FOREIGN_MARKERS = {
    "und", "mit", "aus", "ohne", "geroestet", "geröstet", "gesalzen", "zuckerfrei",
    "molkeneiweiss", "molkeneiweiß", "käse", "milch", "saft", "brötchen", "brot",
    "vollkorn", "geschmack", "weizen", "natur", "zucker", "sahne", "joghurt",
    "avec", "sans", "aux", "lait", "fromage", "yaourt", "blé", "saveur", "arôme",
    "poisson", "poulet", "bœuf", "boeuf", "noix", "tranchée", "fumée", "gout",
    "goût", "bio", "purée", "crème", "biscuits",
    "con", "sin", "leche", "queso", "yogur", "sabor", "pollo", "cerdo", "fresa",
    "integral", "azúcar", "azucar", "bebida", "galletas", "quesos",
    "senza", "latte", "formaggio", "gusto", "grissini", "biscotti",
    "zonder", "melk", "kaas", "smaak", "volkoren", "suiker",
}
FOREIGN_DIACRITICS = "äöüßéèêëàâçñøåæœ"


def foreign_score(name: str) -> int:
    tokens = normalize_key(name).split()
    score = sum(1 for token in tokens if token in FOREIGN_MARKERS)
    score += sum(1 for char in name.lower() if char in FOREIGN_DIACRITICS)
    return score


NUTRIENT_ALIASES = {
    "mono": ("mono-unsaturated-fat_100g", "mono-unsaturated-fat_100g", "monounsaturated-fat_100g"),
    "poly": ("poly-unsaturated-fat_100g", "polyunsaturated-fat_100g"),
    "trans": ("trans-fat_100g",),
    "cholesterol": ("cholesterol_100g",),
    "potassium": ("potassium_100g",),
}


def http_json(url: str):
    last = None
    for attempt in range(RETRIES):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=120) as resp:
                return json.loads(resp.read())
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, OSError) as exc:
            last = exc
            wait = 2 ** attempt * 3
            print(f"    retry {attempt + 1}/{RETRIES} after {wait}s ({exc})", flush=True)
            time.sleep(wait)
    raise RuntimeError(f"giving up on {url}: {last}")


def fetch_page(seg_key: str, query: str, page: int, refresh: bool):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, f"off_{seg_key}_p{page}.json.gz")
    if os.path.exists(path) and not refresh:
        try:
            with gzip.open(path, "rt", encoding="utf-8") as fh:
                return json.load(fh), True
        except (OSError, json.JSONDecodeError):
            pass
    url = ENDPOINT + urllib.parse.urlencode(
        {"q": query, "page": page, "page_size": PAGE_SIZE}
    )
    data = http_json(url)
    with gzip.open(path, "wt", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False)
    time.sleep(SLEEP)
    return data, False


def num(nutriments: dict, *keys):
    for key in keys:
        if key in nutriments:
            value = nutriments.get(key)
            if isinstance(value, (int, float)):
                return float(value)
    return None


def parse_hit(hit: dict, fallback_category: str):
    """Return ((row, meta), "ok") or (None, reason-string)."""
    nutriments = hit.get("nutriments") or {}
    kcal = num(nutriments, "energy-kcal_100g")
    if kcal is None:
        kj = num(nutriments, "energy-kj_100g", "energy_100g")
        kcal = None if kj is None else kj / 4.184
    protein = num(nutriments, "proteins_100g")
    fats = num(nutriments, "fat_100g")
    carbs = num(nutriments, "carbohydrates_100g")
    if kcal is None or protein is None or fats is None or carbs is None:
        return None, "incomplete"
    if not (0 <= kcal <= MAX_CALORIES_PER_100G
            and 0 <= protein <= 100 and 0 <= fats <= 100 and 0 <= carbs <= 100):
        return None, "range"
    if protein + carbs + fats > 110:
        return None, "macro-sum"
    fibers = num(nutriments, "fiber_100g") or 0.0
    if not atwater_ok(kcal, carbs, protein, fats, fibers):
        return None, "atwater"

    sat = num(nutriments, "saturated-fat_100g", "saturated_fat_100g") or 0.0
    sugars = num(nutriments, "sugars_100g") or 0.0
    fiber = fibers
    salt = num(nutriments, "salt_100g")
    if salt is None:
        sodium = num(nutriments, "sodium_100g")
        salt = None if sodium is None else sodium * 2.5
    salt = min(max(salt or 0.0, 0.0), 12.0)
    if sat > fats + 1 or sugars > carbs + 1 or fiber > carbs + 1:
        return None, "consistency"

    name = clean_text(hit.get("product_name_bg")) or clean_text(hit.get("product_name"))
    if len(name) < 3 or len(name) > 120:
        return None, "name-length"
    if name.lower() in JUNK_NAMES or normalize_key(name) in {"", "produkt", "product"}:
        return None, "junk-name"
    if name.isdigit() or NON_LATIN_CYRILLIC.search(name):
        return None, "name-script"

    brands = hit.get("brands")
    if isinstance(brands, list):
        brands = brands[0] if brands else ""
    brand = clean_text(brands).split(",")[0].strip()
    if not brand and isinstance(hit.get("brands_tags"), list) and hit["brands_tags"]:
        brand = hit["brands_tags"][0].split(":")[-1].replace("-", " ").title()
    if not brand:
        return None, "no-brand"
    if brand.isupper() and len(brand) > 2:
        brand = brand.title()
    if len(brand) > 40:
        return None, "brand-length"

    quantity = clean_quantity(hit.get("quantity"))
    if quantity.lower() in {"1", "1 g", "1 ml"}:
        quantity = ""

    label = name
    if brand.lower() not in label.lower():
        label = f"{name} {brand}"
    # Compare with spaces removed so "600g" in the name matches quantity "600 g".
    if quantity and quantity.replace(" ", "") not in label.lower().replace(" ", ""):
        label = f"{label} {quantity}"
    label = clean_text(label)[:90].strip(" ,-")

    category = category_for_off_tags(hit.get("categories_tags"), fallback_category)
    if category not in CATEGORIES:
        category = fallback_category
    if category == "Моите Рецепти":
        category = DEFAULT_CATEGORY

    gi = estimate_gi(category, carbs, fiber)
    row = food_row(
        label,
        category,
        kcal,
        carbs,
        protein,
        fats,
        saturated_fats=sat,
        mono_fats=num(nutriments, *NUTRIENT_ALIASES["mono"]) or 0.0,
        poly_fats=num(nutriments, *NUTRIENT_ALIASES["poly"]) or 0.0,
        trans_fats=num(nutriments, *NUTRIENT_ALIASES["trans"]) or 0.0,
        cholesterol=(num(nutriments, *NUTRIENT_ALIASES["cholesterol"]) or 0.0) * 1000.0,
        sugars=sugars,
        fiber=fiber,
        glycemic_index=gi,
        salt=salt,
        potassium=(num(nutriments, *NUTRIENT_ALIASES["potassium"]) or 0.0) * 1000.0,
        source="off",
    )
    meta = {
        "key": normalize_key(f"{brand} {name} {quantity}"),
        "brand": normalize_key(brand),
        "europe": is_european(hit.get("countries_tags")) or bool(
            hit.get("product_name_bg")
        ),
        "foreign": foreign_score(f"{name} {brand}"),
        "completeness": float(hit.get("completeness") or 0.0),
        "popularity": float(hit.get("unique_scans_n") or hit.get("scans_n") or 0.0),
    }
    return (row, meta), "ok"


def hit_rank(hit: dict):
    """Readable first: Bulgarian name, then English/neutral, then foreign names,
    and within each group the most complete and most scanned products."""
    name = f"{hit.get('product_name_bg') or ''} {hit.get('product_name') or ''}"
    brand = hit.get("brands") or ""
    if isinstance(brand, list):
        brand = brand[0] if brand else ""
    return (
        foreign_score(f"{name} {brand}"),
        0 if (is_european(hit.get("countries_tags")) or hit.get("product_name_bg")) else 1,
        -(float(hit.get("completeness") or 0.0)),
        -(float(hit.get("unique_scans_n") or hit.get("scans_n") or 0.0)),
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--refresh", action="store_true")
    parser.add_argument("--target", type=int, default=TARGET)
    args = parser.parse_args()

    selected: list[tuple[dict, dict]] = []
    seen_keys: set[str] = set()
    # Two products can share a brand+name+quantity key yet end up with the same
    # visible label (e.g. "Coca cola zero" from different packs), so the label is
    # deduplicated as well - users must never see the same name twice.
    seen_labels: set[str] = set()
    brand_category_count: dict[tuple[str, str], int] = {}
    dropped: dict[str, int] = {}
    stats: list[dict] = []
    cursors = {seg["key"]: {"page": 1, "exhausted": False} for seg in SEGMENTS}
    per_brand_cap = 40
    per_brand_total_cap = 400
    brand_totals: dict[str, int] = {}

    def consider(raw: dict, segment: dict) -> bool:
        """Run a hit through the gates and add it if it fits. True if kept."""
        reasons = {}
        for hit in raw:
            parsed, reason = parse_hit(hit, segment["cat"] or DEFAULT_CATEGORY)
            if reason != "ok" or parsed is None:
                reasons[reason] = reasons.get(reason, 0) + 1
                dropped[reason] = dropped.get(reason, 0) + 1
                continue
            row, meta = parsed
            label_key = normalize_key(row["name"])
            if not meta["key"] or meta["key"] in seen_keys or label_key in seen_labels:
                reasons["duplicate"] = reasons.get("duplicate", 0) + 1
                dropped["duplicate"] = dropped.get("duplicate", 0) + 1
                continue
            bc = (meta["brand"], row["category"])
            if brand_category_count.get(bc, 0) >= per_brand_cap:
                reasons["brand-cap"] = reasons.get("brand-cap", 0) + 1
                dropped["brand-cap"] = dropped.get("brand-cap", 0) + 1
                continue
            if brand_totals.get(meta["brand"], 0) >= per_brand_total_cap:
                reasons["brand-total-cap"] = reasons.get("brand-total-cap", 0) + 1
                dropped["brand-total-cap"] = dropped.get("brand-total-cap", 0) + 1
                continue
            selected.append((row, meta))
            seen_keys.add(meta["key"])
            seen_labels.add(label_key)
            brand_category_count[bc] = brand_category_count.get(bc, 0) + 1
            brand_totals[meta["brand"]] = brand_totals.get(meta["brand"], 0) + 1
            return True
        return False

    for segment in SEGMENTS:
        query = f'({segment["q"]}) AND {COMPLETE}'
        kept = 0
        cursor = cursors[segment["key"]]
        while kept < segment["quota"] and cursor["page"] <= segment["max"]:
            page = cursor["page"]
            try:
                data, cached = fetch_page(segment["key"], query, page, args.refresh)
            except RuntimeError as exc:
                print(f"  {segment['key']:<18} page {page:>2} FAILED: {exc}", flush=True)
                cursor["exhausted"] = True
                break
            hits = data.get("hits") or []
            hits.sort(key=hit_rank)
            before = len(selected)
            for hit in hits:
                if kept >= segment["quota"]:
                    break
                if consider([hit], segment):
                    kept += 1
            cursor["page"] += 1
            print(
                f"  {segment['key']:<18} page {page:>2} hits {len(hits):>4} "
                f"kept {kept:>4}/{segment['quota']:<4} total {len(selected):>6}"
                f"{' (cached)' if cached else ''}",
                flush=True,
            )
            if not hits or len(hits) < PAGE_SIZE:
                cursor["exhausted"] = True
                break
        stats.append(
            {
                "segment": segment["key"],
                "category": segment["cat"] or "auto (off categories)",
                "quota": segment["quota"],
                "kept": kept,
                "pages": cursor["page"] - 1,
                "exhausted": cursor["exhausted"],
            }
        )
        if len(selected) >= args.target:
            print(f"target reached early at {len(selected)} rows", flush=True)
            break

    # Top-up: keep pulling pages from the segments with the most remaining supply
    # until we are safely above the target (validation requires >= 25 000).
    topup_order = sorted(
        [s for s in SEGMENTS if s["topup"] > 0],
        key=lambda s: -s["topup"],
    )
    while len(selected) < args.target:
        progressed = False
        for segment in topup_order:
            if len(selected) >= args.target:
                break
            cursor = cursors[segment["key"]]
            if cursor["exhausted"]:
                continue
            query = f'({segment["q"]}) AND {COMPLETE}'
            page = cursor["page"]
            try:
                data, cached = fetch_page(segment["key"], query, page, args.refresh)
            except RuntimeError as exc:
                print(f"  topup {segment['key']:<18} page {page:>2} FAILED: {exc}", flush=True)
                cursor["exhausted"] = True
                continue
            hits = data.get("hits") or []
            hits.sort(key=hit_rank)
            cursor["page"] += 1
            if not hits or len(hits) < PAGE_SIZE:
                cursor["exhausted"] = True
            added = 0
            for hit in hits:
                if consider([hit], segment):
                    added += 1
            progressed = progressed or added > 0
            print(
                f"  topup {segment['key']:<18} page {page:>2} added {added:>4} "
                f"total {len(selected):>6}{' (cached)' if cached else ''}",
                flush=True,
            )
        if not progressed:
            break

    selected.sort(key=lambda pair: (pair[0]["category"], pair[0]["name"].lower()))
    rows = [row for row, _ in selected]
    out_path = os.path.join(ASSETS, "products.json")
    size = write_json_array(out_path, rows)

    by_category: dict[str, int] = {}
    for row in rows:
        by_category[row["category"]] = by_category.get(row["category"], 0) + 1
    summary = {
        "total": len(rows),
        "bytes": size,
        "by_category": dict(sorted(by_category.items(), key=lambda kv: -kv[1])),
        "segments": stats,
        "dropped": dropped,
        "unique_brands": len(brand_totals),
    }
    os.makedirs(CACHE, exist_ok=True)
    with open(os.path.join(CACHE, "products_build.json"), "w", encoding="utf-8") as fh:
        json.dump(summary, fh, ensure_ascii=False, indent=2)
    print(f"wrote {out_path}: {len(rows)} rows, {size} bytes")
    for cat, count in summary["by_category"].items():
        print(f"  {cat}: {count}")
    print("dropped:", dropped)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
