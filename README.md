# BulgarCalTracker (Калории BG)

Bulgarian calorie & macro tracker for Android — Kotlin + Jetpack Compose + Room.
Fully local: no accounts, no cloud sync, no paywalls. The food database ships inside the app.

## Download

Grab the APK from the [Releases](https://github.com/hevenfox/BulgarCalTracker/releases) page.

- **Requirements:** Android 8.0+ (API 26), ~20 MB
- The APK is a **debug-signed development build** — Android will ask you to allow installing from unknown sources, and Play Protect may warn about it. It is fine for personal use; a Play-ready release would need a proper signing key.

```bash
adb install -r app-debug.apk
```

## Build from source

Requirements: JDK 17+, Android SDK with API 35 (or just open the project in Android Studio).

```bash
# Windows
gradlew.bat :app:assembleDebug

# macOS / Linux — only gradlew.bat is committed, so bootstrap the wrapper once:
gradle wrapper --gradle-version 8.11.1
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.
`local.properties` (your `sdk.dir`) is intentionally not committed.

## Features

- **Onboarding** — goal → profile → activity → calorie target and pace (Mifflin–St Jeor + TDEE).
- **Начало** — calorie ring with remaining budget, macro bars, meal cards (Закуска / Обяд / Хапване / Вечеря), calorie-ratio donut, water tracker with a selectable serving size, target-weight card and a logging streak.
- **Прогрес** — **Тегло** and **Хранене** tabs with time ranges, weight line, weekly calorie and water bars, average macros against your plan.
- **Дневник** — the day's log per meal, favourites stripe, tap an entry to edit grams / meal / date or delete it.
- **Още** — read-only profile, **Вашите числа** (BMR, TDEE, ideal weight, kg and weeks to goal), reminders, theme and language.
- **Food logging** — 31 000+ bundled foods in 30 browse categories (branded products, fitness products, drinks, everyday groceries, dishes), searchable from either keyboard layout ("musaka" finds "Мусака"), plus custom products and recipes, favourite (star) foods, a dedicated Любими screen, micro-nutrient details (net carbs, sugars, fiber, GI/GL, saturated/mono/poly/trans fats, cholesterol, potassium, water) and a 100–3230 kcal macro-balance editor.
- **Smart meal default** — when you add a food without picking a meal, the sheet selects it from the clock: 05–10 Закуска, 11–15 Обяд, 16–17 Хапване, 18–04 Вечеря.
- **Meal reminders** — **Още → Напомняния** (defaults 08:00, 13:00, 19:00); tapping a notification opens food search, alarms reschedule daily and after reboot.

## Tech stack

Kotlin 2.0 · Jetpack Compose (Material 3) · Navigation Compose · Room + KSP · DataStore · AlarmManager
`minSdk 26` · `targetSdk 35` · `versionName 1.1.0`

```
app/src/main/java/com/bulgarcal/tracker/
├── data/         Room entities, DAOs, repository, bundled food seeder
├── domain/       calculation engine, progress analytics, models
├── navigation/   routes and the main shell (tabs, deep links)
├── reminders/    notification scheduling and receivers
└── ui/           home · progress · diary · food · details · more · onboarding · theme · components
```

## Food database

The database is seeded into Room on first launch from three assets and is versioned
as one set (`FoodSeeder.CURRENT_VERSION`; bumping it replaces the bundled rows on the
next launch, keeping user products, recipes, diary, weight and water history).

| File | Rows | What | Licence |
| --- | --- | --- | --- |
| `assets/foods.json` | 92 | curated Bulgarian foods and traditional dishes | MIT |
| `assets/products.json` | ~26 200 | branded products (protein bars, skyr, protein powders, energy drinks, croissants, sodas, snacks, ready meals …) with label values | ODbL / DbCL |
| `assets/dishes.json` | 5 023 | 4 732 USDA FNDDS dishes plus 308 hand-authored Bulgarian dishes (мусака, крем супа, шкембе чорба …) | public domain / MIT |

Row format — **snake_case** and **camelCase** keys are both accepted, and every key
after the first four is optional:

```json
{
  "name": "Мусака с картофи",
  "category": "Домашни ястия",
  "calories": 155,
  "carbs": 12.3,
  "protein": 7.4,
  "fats": 8.6,
  "saturated_fats": 3.4,
  "mono_fats": 3.1,
  "poly_fats": 1.2,
  "trans_fats": 0.3,
  "cholesterol": 41.0,
  "sugars": 2.6,
  "fiber": 1.7,
  "glycemic_index": 55.0,
  "glycemic_load": 5.83,
  "salt": 0.85,
  "potassium": 318.0,
  "water": 68.2,
  "source": "curated"
}
```

- Values are per **100 g / 100 ml**; an unsupported value is simply absent and shows as 0 in the app.
- `category` must be one of `FoodCategories` in `domain/Models.kt`, which must stay in sync with `CATEGORIES` in `tools/foods_db/common.py` (`validate.py` fails if they drift).
- `source` is `off` (Open Food Facts), `usda`, `curated` or `user`, and is displayed in the food sheet.

### Where the data comes from

- **Open Food Facts** — real branded products with real label values, filtered to rows whose per-100 g nutrition is complete and whose stated energy agrees with its macros (Atwater check, ±22 % or 25 kcal, counting fibre at 2 kcal/g). See [THIRD-PARTY-DATA.md](THIRD-PARTY-DATA.md) for the ODbL attribution and share-alike terms.
- **USDA FoodData Central (FNDDS 2021–2023)** — public domain, 4 700+ prepared dishes and generic foods after the same energy check, translated to Bulgarian by `tools/foods_db/bg_glossary.json`; rows whose description cannot be translated confidently are dropped.
- **Hand-authored Bulgarian dishes** — `tools/foods_db/bg_dishes.json`; every value is the sum of its ingredient list divided by the cooked weight, so it can be recomputed by hand.
- **Glycemic index is an estimate**: no label carries it, so it comes from a per-category table and is only applied when a food has ≥ 2 g available carbs per 100 g. Glycemic load is `GI × (carbs − fiber) / 100`.

### Rebuilding / extending

```bash
python tools/foods_db/build_foods.py --write      # clean the curated foods
python tools/foods_db/build_products.py           # fetch + filter Open Food Facts
python tools/foods_db/build_dishes.py --min 5000  # USDA FNDDS + Bulgarian dishes
python tools/foods_db/validate.py                 # gates, writes tools/foods_db/report.md
python tools/foods_db/migration_smoke.py          # Room 3 -> 4 migration smoke test
```

JVM tests (`gradlew.bat testDebugUnitTest`) parse the real assets and fail if the
counts, categories, numbers or search keys are wrong, so a broken data file cannot
ship silently.

## License

MIT — see [LICENSE](LICENSE). The bundled data keeps its own licences: see
[THIRD-PARTY-DATA.md](THIRD-PARTY-DATA.md).
