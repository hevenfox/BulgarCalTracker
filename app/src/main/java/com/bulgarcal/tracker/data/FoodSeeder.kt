package com.bulgarcal.tracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FoodSeeder {
    /** Bump when replacing assets/foods.json (v3 = 1000-food database). */
    private const val SEED_KEY = "foods_db_version"
    private const val CURRENT_VERSION = 3

    private const val DEFAULT_CATEGORY = "Общи"

    suspend fun seedIfNeeded(context: Context, db: AppDatabase) {
        val meta = db.appMetaDao()
        val stored = meta.get(SEED_KEY)?.toIntOrNull() ?: 0
        if (stored >= CURRENT_VERSION) return

        db.foodDao().deleteBundled()
        val json = context.assets.open("foods.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val foods = buildList {
            for (i in 0 until array.length()) {
                add(parseFood(array.getJSONObject(i)))
            }
        }
        db.foodDao().insertAll(foods)
        meta.set(AppMetaEntity(SEED_KEY, CURRENT_VERSION.toString()))
    }

    private fun parseFood(o: JSONObject): FoodEntity = FoodEntity(
        name = o.getString("name"),
        category = o.optString("category").ifBlank {
            o.optString("категория").ifBlank { DEFAULT_CATEGORY }
        },
        calories = o.getNumber("calories").toInt(),
        carbs = o.getNumber("carbs").toFloat(),
        protein = o.getNumber("protein").toFloat(),
        fats = o.getNumber("fats").toFloat(),
        saturatedFats = o.optNumber("saturated_fats", "saturatedFats"),
        sugars = o.optNumber("sugars"),
        fiber = o.optNumber("fiber"),
        glycemicIndex = o.optNumber("glycemic_index", "glycemicIndex"),
        salt = o.optNumber("salt")
    )

    private fun JSONObject.getNumber(key: String): Double =
        when (val v = get(key)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun JSONObject.optNumber(vararg keys: String): Float {
        for (key in keys) {
            if (!has(key) || isNull(key)) continue
            return when (val v = get(key)) {
                is Number -> v.toFloat()
                is String -> v.toFloatOrNull() ?: 0f
                else -> 0f
            }
        }
        return 0f
    }
}
