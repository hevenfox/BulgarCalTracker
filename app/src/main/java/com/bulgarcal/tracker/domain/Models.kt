package com.bulgarcal.tracker.domain

import java.time.LocalTime

enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("Закуска", "🥐"),
    LUNCH("Обяд", "🥘"),
    SNACK("Хапване", "🥤"),
    DINNER("Вечеря", "🍽️");

    companion object {
        /**
         * The meal that fits the clock, used as the default while adding food when no meal
         * was picked explicitly: 05–10 Закуска, 11–15 Обяд, 16–17 Хапване, 18–04 Вечеря.
         */
        fun forCurrentTime(now: LocalTime = LocalTime.now()): MealType = when (now.hour) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..17 -> SNACK
            else -> DINNER
        }
    }
}

/**
 * Browsable categories, in browse order. Index 0 is special: "Моите Рецепти" is
 * rendered as its own row, so it must stay first.
 *
 * Must stay in sync with CATEGORIES in tools/foods_db/common.py - every row in
 * assets/foods.json, products.json and dishes.json carries one of these, and the
 * data validation rejects anything else (a category that is not listed here would
 * be unreachable in the browser).
 */
val FoodCategories = listOf(
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
    "Общи"
)
