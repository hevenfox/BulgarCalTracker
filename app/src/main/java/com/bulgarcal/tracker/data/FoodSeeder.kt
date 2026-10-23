package com.bulgarcal.tracker.data

import android.content.Context
import android.util.Log
import com.bulgarcal.tracker.domain.SearchKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the bundled food database from assets into Room.
 *
 * Three files, loaded together and versioned as one set:
 *  * foods.json    - 92 curated Bulgarian foods
 *  * products.json - branded products from Open Food Facts (ODbL, see THIRD-PARTY-DATA.md)
 *  * dishes.json   - USDA FNDDS dishes and hand-authored Bulgarian dishes
 *
 * Parsing and inserting happens on [Dispatchers.IO] in batches: the whole set is
 * ~30 000 rows / 8 MB of JSON, which would freeze the UI if it ran on the main
 * thread the way the 1 000-row version used to.
 */
object FoodSeeder {
    private const val TAG = "FoodSeeder"

    /**
     * Bump when replacing the assets.
     * v4 = 92 + ~26 000 + ~5 000 rows, v5 = the same rows with their barcodes.
     */
    private const val SEED_KEY = "foods_db_version"
    private const val CURRENT_VERSION = 5

    private const val DEFAULT_CATEGORY = "Общи"
    private const val BATCH_SIZE = 1_000

    private val ASSET_FILES = listOf("foods.json", "products.json", "dishes.json")

    suspend fun seedIfNeeded(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val meta = db.appMetaDao()
        val stored = meta.get(SEED_KEY)?.toIntOrNull() ?: 0
        if (stored >= CURRENT_VERSION) return@withContext

        db.foodDao().deleteBundled()
        for (file in ASSET_FILES) {
            try {
                val rows = seedFile(context, db, file)
                Log.i(TAG, "seeded $rows rows from $file")
            } catch (t: Throwable) {
                // A bad asset must never brick startup: the remaining files still load.
                Log.e(TAG, "failed to seed $file", t)
            }
        }
        backfillSearchKeys(db)
        meta.set(AppMetaEntity(SEED_KEY, CURRENT_VERSION.toString()))
    }

    private suspend fun seedFile(context: Context, db: AppDatabase, asset: String): Int {
        val json = context.assets.open(asset).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        var inserted = 0
        val batch = ArrayList<FoodEntity>(BATCH_SIZE)
        for (i in 0 until array.length()) {
            batch.add(parseFood(array.getJSONObject(i)))
            if (batch.size >= BATCH_SIZE) {
                db.foodDao().insertAll(batch.toList())
                inserted += batch.size
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            db.foodDao().insertAll(batch.toList())
            inserted += batch.size
        }
        return inserted
    }

    /** Custom products created before search keys existed still need one. */
    private suspend fun backfillSearchKeys(db: AppDatabase) {
        val rows = db.foodDao().rowsWithoutSearchKey()
        if (rows.isEmpty()) return
        rows.chunked(BATCH_SIZE).forEach { chunk ->
            db.foodDao().updateAll(chunk.map { it.copy(searchKey = SearchKey.of(it.name)) })
        }
        Log.i(TAG, "backfilled ${rows.size} search keys")
    }

    private fun parseFood(o: JSONObject): FoodEntity {
        val name = o.getString("name")
        return FoodEntity(
            name = name,
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
            salt = o.optNumber("salt"),
            monoFats = o.optNumber("mono_fats", "monoFats"),
            polyFats = o.optNumber("poly_fats", "polyFats"),
            transFats = o.optNumber("trans_fats", "transFats"),
            cholesterol = o.optNumber("cholesterol"),
            potassium = o.optNumber("potassium"),
            water = o.optNumber("water"),
            glycemicLoad = o.optNumber("glycemic_load", "glycemicLoad"),
            searchKey = SearchKey.of(name),
            source = o.optString("source"),
            barcode = o.optString("barcode").filter { c -> c.isDigit() }
        )
    }

    /** Tolerant on purpose: a missing key means "no data", never a crash. */
    private fun JSONObject.getNumber(key: String): Double {
        if (!has(key) || isNull(key)) return 0.0
        return when (val v = get(key)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
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
