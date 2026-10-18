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

Not affiliated with or endorsed by Open Food Facts.

## USDA FoodData Central (public domain)

The dish rows come from the **Survey Foods (FNDDS) 2021–2023** release of USDA
FoodData Central, a work of the US Government and therefore in the public domain.
It is credited in the app anyway: every USDA-derived food is marked in the food
sheet ("Данни: USDA FoodData Central (FNDDS)").

## What the values mean

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
python tools/foods_db/validate.py                 # gates + report.md
python tools/foods_db/migration_smoke.py          # Room 3 -> 4 migration check
```

The scripts cache their downloads under `tools/foods_db/cache/` (git-ignored).
