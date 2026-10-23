# Third-party data in the bundled food database

The app ships three JSON files in `app/src/main/assets/`. Two of them contain data
derived from third-party sources, which changes what those files are licensed under
(the app's own source code stays MIT — see [LICENSE](LICENSE)).

| File | Rows | Content | Source | Licence |
| --- | --- | --- | --- | --- |
| `foods.json` | 92 | Bulgarian foods and traditional dishes | hand-curated for this project | MIT |
| `products.json` | ~26 200 | branded products with label values | [Open Food Facts](https://world.openfoodfacts.org) | ODbL 1.0 (database) + DbCL 1.0 (contents) |
| `dishes.json` | ~4 760 | generic dishes and foods | [USDA FoodData Central](https://fdc.nal.usda.gov) — Survey Foods (FNDDS) 2021–2023, a US Government work | public domain |
| `dishes.json` | ~280 | Bulgarian home cooking | hand-authored for this project | MIT |

## Open Food Facts (ODbL / DbCL)

> Contains data from Open Food Facts, available under the Open Database License.

- Data: **Open Database License (ODbL) v1.0** — https://opendatacommons.org/licenses/odbl/1-0/
- Individual contents: **Database Contents License (DbCL) v1.0**
- Retrieved through the official Open Food Facts search service
  (`https://search.openfoodfacts.org`), which was used instead of scraping the web.

**Share-alike.** `products.json` is an adapted, filtered subset of the Open Food
Facts database. Under ODbL §4.4 it is therefore published under the ODbL as well,
and it is published here: this repository contains both the derived database
(`app/src/main/assets/products.json`) and the exact, re-runnable build script that
produced it (`tools/foods_db/build_products.py`), so anyone can obtain, inspect and
reuse the adapted database. The obligations apply to the *database*; the app's
source code remains MIT.

**Attribution in the app.** Required attribution is provided in the UI: the More →
**Информация** screen states that the data comes from Open Food Facts under the
ODbL, with a clickable link, and every product's source is shown in the food sheet
("Данни: Open Food Facts (ODbL 1.0)").

**Runtime lookups.** Scanning a barcode the app does not know queries the same
service one product at a time (`api/v3/product/{barcode}`, anonymous, with the app's
User-Agent). A product found this way is stored on the device like any bundled
product, keeps `source: "off"`, and therefore shows the same ODbL attribution in the
food sheet. Those stored rows are a local copy of the adapted database, not a
separate one. Only the barcode is sent — no profile, no diary, no device id.

Not affiliated with or endorsed by Open Food Facts.

## USDA FoodData Central (public domain)

The dish rows come from the **Survey Foods (FNDDS) 2021–2023** release of USDA
FoodData Central, a work of the US Government and therefore in the public domain.
It is credited in the app anyway: every USDA-derived food is marked in the food
sheet ("Данни: USDA FoodData Central (FNDDS)").

## Consistency rules applied to third-party rows

Both upstream sources are filtered so the app's own arithmetic cannot contradict
the label:

- the four macros must be present and within 0–100 g per 100 g;
- the stated energy must be explicable from those macros (`4 × (carbs − fibre) +
  2 × fibre + 4 × protein + 9 × fat`, tolerance ±22 % or 25 kcal);
- saturated fat, sugars and fibre may not exceed total fat or total carbohydrate.

Alcohol is the one documented exception: it carries 7 kcal/g that no macro
describes, and the app does not track alcohol grams, so **Алкохол** rows are exempt
from the energy check while still passing the range checks. The exact numbers of
kept, dropped and exempt rows per source are written into
[tools/foods_db/report.md](tools/foods_db/report.md) on every validation run.

## What the values mean

Barcodes come from Open Food Facts' `code` field and are kept only when they are a
usable GTIN (8, 12, 13 or 14 digits). One barcode may map to exactly one row, so a
scan is never ambiguous; duplicates are dropped when the database is built.

All values are **per 100 g / 100 ml**. The package size shown in a product name
(`Monster Ultra White 500 ml`) is informational: logging 250 g of it means 250 ml.

Glycemic index is **not** on any label. It is a documented, category-level estimate
applied only when a food carries at least 2 g of available carbohydrate per 100 g,
and glycemic load is derived as `GI × (carbs − fiber) / 100`. Rows where no
estimate applies carry 0, which the app shows as "—".

## Rebuilding the database

```bash
python tools/foods_db/build_foods.py --write      # clean the 92 curated foods
python tools/foods_db/build_products.py           # fetch + filter Open Food Facts
python tools/foods_db/build_dishes.py --min 5000  # USDA FNDDS + Bulgarian dishes
python tools/foods_db/validate.py                 # gates + report.md (incl. barcodes)
python tools/foods_db/migration_smoke.py          # Room 3 -> 4 -> 5 migration check
```

The scripts cache their downloads under `tools/foods_db/cache/` (git-ignored).
