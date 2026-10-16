# BulgarCalTracker (Калории BG)

Bulgarian calorie & macro tracker for Android — Kotlin + Jetpack Compose + Room.
Fully local: no accounts, no cloud sync, no paywalls. The food database ships inside the app.

## Download

Grab the APK from the [Releases](https://github.com/hevenfox/BulgarCalTracker/releases) page.

- **Requirements:** Android 8.0+ (API 26), ~19 MB
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
- **Food logging** — 1000 bundled foods across categories, custom products and recipes, favourite (star) foods, a dedicated Любими screen, micro-nutrient details and a 100–3230 kcal macro-balance editor.
- **Smart meal default** — when you add a food without picking a meal, the sheet selects it from the clock: 05–10 Закуска, 11–15 Обяд, 16–17 Хапване, 18–04 Вечеря.
- **Meal reminders** — **Още → Напомняния** (defaults 08:00, 13:00, 19:00); tapping a notification opens food search, alarms reschedule daily and after reboot.

## Tech stack

Kotlin 2.0 · Jetpack Compose (Material 3) · Navigation Compose · Room + KSP · DataStore · AlarmManager
`minSdk 26` · `targetSdk 35` · `versionName 1.0.0`

```
app/src/main/java/com/bulgarcal/tracker/
├── data/         Room entities, DAOs, repository, bundled food seeder
├── domain/       calculation engine, progress analytics, models
├── navigation/   routes and the main shell (tabs, deep links)
├── reminders/    notification scheduling and receivers
└── ui/           home · progress · diary · food · details · more · onboarding · theme · components
```

## Food database (`foods.json`)

`app/src/main/assets/foods.json` seeds Room on first launch. Both **snake_case** and **camelCase** keys are accepted:

```json
{
  "name": "Кисело мляко 2%",
  "category": "Млечни продукти",
  "calories": 50,
  "carbs": 4.5,
  "protein": 3.2,
  "fats": 2.0,
  "saturated_fats": 1.3,
  "sugars": 4.5,
  "fiber": 0.0,
  "glycemic_index": 35.0,
  "salt": 0.1
}
```

- `category` is optional; unknown categories fall under **Общи**.
- Values are per **100 g** unless a `servingGrams` is given.
- The seed version lives in `FoodSeeder.kt` (`CURRENT_VERSION`); bumping it refreshes bundled foods on the next launch of an installed app.

## License

MIT — see [LICENSE](LICENSE).
