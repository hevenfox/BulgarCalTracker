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

val FoodCategories = listOf(
    "Моите Рецепти",
    "Агнешки продукти",
    "Билки и подправки",
    "Бобови култури",
    "Говежди продукти",
    "Дивеч",
    "Заместители на храна",
    "Захарни изделия",
    "Зеленчуци"
)
