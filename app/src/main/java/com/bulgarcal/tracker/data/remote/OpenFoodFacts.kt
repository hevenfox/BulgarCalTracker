package com.bulgarcal.tracker.data.remote

import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.domain.FoodCategories
import com.bulgarcal.tracker.domain.SearchKey
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/** What a barcode lookup produced. */
sealed interface OffLookup {
    data class Found(val food: FoodEntity) : OffLookup

    /** The barcode exists (or not at all) but carries no usable label values. */
    data class NotFound(val preview: FoodEntity?) : OffLookup

    /** No usable connection: the scan can still be answered by hand. */
    data object Unreachable : OffLookup

    /** The service asked us to slow down (HTTP 429) or is unavailable. */
    data object Busy : OffLookup
}

/**
 * Reads one product by barcode from Open Food Facts (ODbL 1.0, see
 * THIRD-PARTY-DATA.md) and maps its label into a [FoodEntity].
 *
 * The v3 product endpoint is the one the project documents as current; `fields`
 * keeps the payload at a couple of kilobytes. Reads are anonymous, but Open Food
 * Facts asks every client to identify itself with a User-Agent, and limits reads to
 * 15 per minute per IP - far more than a person can scan.
 */
class OpenFoodFactsClient(
    private val baseUrl: String = "https://world.openfoodfacts.org/api/v3/product/",
    private val userAgent: String = USER_AGENT,
    private val timeoutMs: Int = 8_000
) {

    suspend fun lookup(barcode: String): OffLookup = withContext(Dispatchers.IO) {
        when (val attempt = get("$baseUrl$barcode.json?fields=$FIELDS")) {
            is Attempt.Ok -> parse(attempt.body, barcode)
            Attempt.NotFound -> OffLookup.NotFound(null)
            Attempt.Busy -> OffLookup.Busy
            Attempt.Retryable -> {
                delay(600)
                when (val second = get("$baseUrl$barcode.json?fields=$FIELDS")) {
                    is Attempt.Ok -> parse(second.body, barcode)
                    Attempt.NotFound -> OffLookup.NotFound(null)
                    else -> OffLookup.Unreachable
                }
            }
            Attempt.Unreachable -> OffLookup.Unreachable
        }
    }

    private sealed interface Attempt {
        data class Ok(val body: String) : Attempt
        data object NotFound : Attempt
        data object Busy : Attempt
        data object Retryable : Attempt
        data object Unreachable : Attempt
    }

    private fun get(url: String): Attempt {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
            }
            when (val status = connection.responseCode) {
                200 -> Attempt.Ok(connection.inputStream.bufferedReader().use { it.readText() })
                404 -> Attempt.NotFound
                429, 503 -> Attempt.Busy
                else -> if (status >= 500) Attempt.Retryable else Attempt.Unreachable
            }
        } catch (io: IOException) {
            Attempt.Unreachable
        } catch (t: Throwable) {
            // A malformed URL or a blocked network must never crash a scan.
            Attempt.Unreachable
        } finally {
            connection?.disconnect()
        }
    }

    private fun parse(body: String, barcode: String): OffLookup = try {
        val root = JSONObject(body)
        if (root.optString("status") != "success") {
            OffLookup.NotFound(null)
        } else {
            val product = root.optJSONObject("product")
            if (product == null) OffLookup.NotFound(null) else OffProductMapper.map(product, barcode)
        }
    } catch (t: Throwable) {
        OffLookup.NotFound(null)
    }

    companion object {
        private const val USER_AGENT =
            "BulgarCalTracker/1.2 (github.com/hevenfox/BulgarCalTracker)"

        private const val FIELDS =
            "code,product_name,product_name_bg,brands,quantity,serving_size,serving_quantity," +
                "nutriments,categories_tags"
    }
}

/**
 * Maps an Open Food Facts product onto the app's food row, using the same
 * fallbacks the bundled database was built with: kJ when kcal is missing, sodium
 * times 2.5 when salt is missing, and the same category table.
 *
 * A product whose energy or macros are missing is not turned into a food (that
 * would put invented numbers in the diary); it comes back as a [preview] so the
 * user can finish the values themselves.
 */
object OffProductMapper {

    fun map(product: JSONObject, barcode: String): OffLookup {
        val label = readLabel(product)
        val food = label.toFood(barcode)
        return if (label.isComplete) OffLookup.Found(food) else OffLookup.NotFound(food)
    }

    private data class Label(
        val name: String,
        val calories: Int?,
        val carbs: Float?,
        val protein: Float?,
        val fats: Float?,
        val saturatedFats: Float,
        val sugars: Float,
        val fiber: Float,
        val salt: Float,
        val servingGrams: Float,
        val category: String
    ) {
        /** Everything the diary needs is on the label. */
        val isComplete: Boolean
            get() = name.length >= 3 && calories != null && carbs != null &&
                protein != null && fats != null

        /**
         * The row to store, or the pre-filled form the user finishes by hand when
         * the label was incomplete (missing values stay 0 rather than invented).
         */
        fun toFood(barcode: String): FoodEntity {
            val resolvedName = name.ifBlank { "Продукт $barcode" }
            return FoodEntity(
                name = resolvedName,
                category = category,
                calories = calories ?: 0,
                carbs = carbs ?: 0f,
                protein = protein ?: 0f,
                fats = fats ?: 0f,
                saturatedFats = saturatedFats,
                sugars = sugars,
                fiber = fiber,
                salt = salt,
                servingGrams = servingGrams,
                searchKey = SearchKey.of(resolvedName),
                source = "off",
                barcode = barcode
            )
        }
    }

    private fun readLabel(product: JSONObject): Label {
        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
        val kcalFromLabel = number(nutriments, "energy-kcal_100g")
        val kj = number(nutriments, "energy-kj_100g") ?: number(nutriments, "energy_100g")
        // Rounded, not truncated: 251 kJ is 60 kcal, not 59.
        val calories = (kcalFromLabel ?: kj?.div(4.184f))?.takeIf { it in 0f..910f }?.roundToInt()
        val carbs = number(nutriments, "carbohydrates_100g")?.takeIf { it in 0f..100f }
        val protein = number(nutriments, "proteins_100g")?.takeIf { it in 0f..100f }
        val fats = number(nutriments, "fat_100g")?.takeIf { it in 0f..100f }
        val salt = (number(nutriments, "salt_100g")
            ?: number(nutriments, "sodium_100g")?.times(2.5f))?.coerceIn(0f, 12f) ?: 0f

        return Label(
            name = labelName(product),
            calories = calories,
            carbs = carbs,
            protein = protein,
            fats = fats,
            saturatedFats = number(nutriments, "saturated-fat_100g", "saturated_fat_100g")
                ?.coerceIn(0f, fats ?: 100f) ?: 0f,
            sugars = number(nutriments, "sugars_100g")?.coerceIn(0f, carbs ?: 100f) ?: 0f,
            fiber = number(nutriments, "fiber_100g")?.coerceIn(0f, 100f) ?: 0f,
            salt = salt,
            servingGrams = servingGrams(product),
            category = categoryFor(product)
        )
    }

    /**
     * Bulgarian name first; the brand and the package size are appended the same way
     * the bundled rows do it, so the same product does not appear twice in search.
     */
    private fun labelName(product: JSONObject): String {
        val name = clean(product.optString("product_name_bg"))
            .ifBlank { clean(product.optString("product_name")) }
        if (name.isBlank()) return ""
        var label = name
        val brand = clean(brandsOf(product)).substringBefore(",").trim()
        if (brand.isNotBlank() && !label.contains(brand, ignoreCase = true)) label = "$label $brand"
        val quantity = clean(product.optString("quantity"))
        if (quantity.isNotBlank() &&
            !label.replace(" ", "").contains(quantity.replace(" ", ""), ignoreCase = true)
        ) {
            label = "$label $quantity"
        }
        val trimmed = label.take(90).trim().trim(',', '-', ' ')
        return if (trimmed.length < 3) "" else trimmed
    }

    private fun brandsOf(product: JSONObject): String = when (val brands = product.opt("brands")) {
        is JSONArray -> brands.optString(0)
        is String -> brands
        else -> ""
    }

    /** `serving_quantity` is grams for solids; "330 ml" and "1 portion" are not. */
    private fun servingGrams(product: JSONObject): Float {
        number(product, "serving_quantity")?.takeIf { it in 1f..2_000f }?.let { return it }
        val size = clean(product.optString("serving_size"))
        val value = Regex("^\\s*(\\d+(?:[.,]\\d+)?)\\s*g\\b").find(size)?.groupValues?.get(1)
        return value?.replace(',', '.')?.toFloatOrNull()?.takeIf { it in 1f..2_000f } ?: 100f
    }

    private fun categoryFor(product: JSONObject): String {
        val tags = product.optJSONArray("categories_tags") ?: return "Общи"
        val words = (0 until tags.length()).joinToString(" ") { tags.optString(it).lowercase() }
        return CATEGORY_RULES.firstOrNull { (needles, _) -> needles.any { it in words } }?.second
            ?: "Общи"
    }

    /** A rule for a category the app can actually browse; anything else falls back. */
    private fun rule(category: String, vararg needles: String): Pair<List<String>, String> =
        needles.toList() to (category.takeIf { it in FoodCategories } ?: "Общи")

    /** First match wins, so the specific rules sit above the broad ones. */
    private val CATEGORY_RULES: List<Pair<List<String>, String>> = listOf(
        rule("Протеинови барове", "protein-bar", "protein-shake", "meal-replacement", "sport-nutrition"),
        rule("Спортни добавки", "dietary-supplement", "food-supplement"),
        rule("Енергийни напитки", "energy-drink"),
        // Bars read as a snack, not as a breakfast cereal, even with a cereal tag.
        rule("Снаксове", "-bars", "en:bars"),
        rule("Вода", "mineral-water", "spring-water", "drinking-water"),
        rule("Безалкохолни напитки", "soda", "colas", "lemonade", "iced-tea"),
        rule("Безалкохолни напитки", "fruit-juice", "juice", "smoothie", "nectar"),
        rule("Кафе и чай", "coffee", "tea"),
        rule("Кисели млека и скир", "yogurt", "yoghurt", "quark", "kefir", "sour-milk", "ayran", "skyr"),
        rule("Сирена и кашкавал", "cheese", "feta", "kashkaval"),
        rule("Млечни продукти", "milk", "cream", "butter", "dairy"),
        rule("Месо и колбаси", "sausage", "ham", "salami", "meat", "poultry", "chicken", "pork", "beef"),
        rule("Риба и морски дарове", "fish", "seafood", "tuna", "salmon"),
        rule("Яйца", "egg"),
        rule("Хляб и зърнени", "bread", "bakery", "baguette"),
        rule("Зърнени закуски", "breakfast-cereal", "muesli", "oat", "granola", "cereal"),
        rule("Паста и ориз", "pasta", "rice", "noodle", "grain"),
        rule("Тестени и сладкиши", "pastry", "cake", "biscuit", "cookie", "wafer", "croissant"),
        rule("Шоколад и бонбони", "chocolate", "candy", "sweet"),
        rule("Сладолед и десерти", "ice-cream", "dessert", "pudding"),
        rule("Ядки и семена", "nut", "seed", "almond", "peanut"),
        rule("Мазнини и масла", "oil", "margarine", "lard"),
        rule("Сосове и подправки", "sauce", "ketchup", "mayonnaise", "spice", "condiment", "mustard"),
        rule("Детски храни", "baby-food", "infant"),
        rule("Алкохол", "alcohol", "beer", "wine", "spirit"),
        rule("Бобови и зърнени храни", "legume", "bean", "lentil", "chickpea"),
        rule("Плодове и зеленчуци", "vegetable", "fruit", "salad"),
        rule("Снаксове", "snack", "chips", "crisps", "cracker", "popcorn"),
        rule("Готови ястия", "ready-meal", "prepared-meal", "frozen-meal", "soup")
    )

    private fun number(source: JSONObject, vararg keys: String): Float? {
        for (key in keys) {
            if (!source.has(key) || source.isNull(key)) continue
            when (val value = source.opt(key)) {
                is Number -> return value.toFloat().takeIf { it.isFinite() }
                is String -> return value.replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
                else -> continue
            }
        }
        return null
    }

    private fun clean(text: String): String = text.replace(Regex("\\s+"), " ").trim()
}
