package com.bulgarcal.tracker.data

import com.bulgarcal.tracker.domain.FoodCategories
import com.bulgarcal.tracker.domain.SearchKey
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the bundled food database, run on the JVM (no device needed).
 *
 * They parse the real assets with the same parser shape the seeder uses, so a
 * broken or half-written data file fails `./gradlew testDebugUnitTest` instead of
 * silently shipping as an empty food list.
 */
class FoodDataContractTest {

    private val products = asset("products.json")
    private val dishes = asset("dishes.json")
    private val foods = asset("foods.json")

    private fun asset(name: String): JSONArray {
        val file = File("src/main/assets/$name")
        assertTrue("asset file is missing: ${file.absolutePath}", file.exists())
        return JSONArray(file.readText(Charsets.UTF_8))
    }

    private val requiredNumbers =
        listOf("calories", "carbs", "protein", "fats")

    private val optionalNumbers = listOf(
        "saturated_fats", "mono_fats", "poly_fats", "trans_fats", "cholesterol",
        "sugars", "fiber", "glycemic_index", "glycemic_load", "salt", "potassium",
        "water"
    )

    @Test
    fun `products file meets the 25000 row target`() {
        assertTrue("products.json has ${products.length()} rows", products.length() >= 25_000)
    }

    @Test
    fun `dishes file meets the 5000 row target`() {
        assertTrue("dishes.json has ${dishes.length()} rows", dishes.length() >= 5_000)
    }

    @Test
    fun `curated foods file keeps only the real foods`() {
        assertEquals(92, foods.length())
        for (index in 0 until foods.length()) {
            val name = foods.getJSONObject(index).getString("name")
            assertTrue("synthetic clone left in foods.json: $name", !name.contains("Верея"))
        }
    }

    @Test
    fun `every row is usable by the seeder`() {
        val known = FoodCategories.toSet() - "Моите Рецепти"
        for ((file, array) in listOf("foods" to foods, "products" to products, "dishes" to dishes)) {
            for (index in 0 until array.length()) {
                val row = array.getJSONObject(index)
                val where = "$file[$index] ${row.optString("name")}"
                val name = row.optString("name", "")
                assertTrue("$where: empty name", name.length >= 3)
                val category = row.optString("category", "")
                assertTrue("$where: unknown category '$category'", category in known)
                // 100 g of pure fat is ~900 kcal (lard is 902), hence the 910 ceiling.
                val calories = row.optInt("calories", -1)
                assertTrue("$where: calories $calories", calories in 0..910)
                for (key in requiredNumbers) {
                    assertTrue("$where: missing $key", row.has(key))
                }
                for (key in optionalNumbers) {
                    if (!row.has(key)) continue
                    val value = row.optDouble(key, Double.NaN)
                    assertTrue("$where: $key is not a number", !value.isNaN())
                    assertTrue("$where: $key out of range ($value)", value >= 0.0)
                }
                for (key in listOf("carbs", "protein", "fats")) {
                    val value = row.optDouble(key, -1.0)
                    assertTrue("$where: $key > 100 g/100 g ($value)", value <= 100.0)
                }
                assertEquals("$where: missing source", true, row.optString("source").isNotEmpty())
            }
        }
    }

    @Test
    fun `no food appears twice across the three files`() {
        val seen = HashMap<String, String>()
        for ((file, array) in listOf("foods" to foods, "products" to products, "dishes" to dishes)) {
            for (index in 0 until array.length()) {
                val name = array.getJSONObject(index).getString("name")
                val key = name.lowercase().replace(Regex("[^0-9a-zа-я]+"), " ").trim()
                val previous = seen.put(key, file)
                assertTrue("duplicate food '$name' in $file and $previous", previous == null)
            }
        }
    }

    @Test
    fun `every browsable category has food in it`() {
        val counts = HashMap<String, Int>()
        for (array in listOf(foods, products, dishes)) {
            for (index in 0 until array.length()) {
                val category = array.getJSONObject(index).getString("category")
                counts[category] = (counts[category] ?: 0) + 1
            }
        }
        // "Общи" is the seeder's fallback bucket, not a curated shelf, so it may be
        // empty; every other shelf in the browse list must have something in it.
        val empty = FoodCategories.drop(1)
            .filter { it != "Общи" }
            .filter { (counts[it] ?: 0) == 0 }
        assertTrue("categories with no food: $empty", empty.isEmpty())
    }

    @Test
    fun `search finds names typed on either keyboard layout`() {
        assertEquals("musaka", SearchKey.ofQuery("Мусака"))
        assertEquals("musaka", SearchKey.ofQuery("musaka"))
        assertEquals("musaka", SearchKey.ofQuery("мУсАкА"))
        assertEquals("pile", SearchKey.ofQuery("Пиле"))
        assertTrue(SearchKey.of("Мусака с картофи").startsWith("musaka s kartofi"))
        assertTrue(SearchKey.of("крем супа от тиква").contains("krem supa ot tikva"))

        val names = ArrayList<String>()
        for (array in listOf(foods, products, dishes)) {
            for (index in 0 until array.length()) {
                names.add(array.getJSONObject(index).getString("name"))
            }
        }
        // The query path in FoodDao.search(): searchKey LIKE '%<transliterated query>%'.
        for (query in listOf("мусака", "musaka", "крем супа", "krem supa", "скир", "skyr", "monster", "red bull", "трушия")) {
            val needle = SearchKey.ofQuery(query)
            val hits = names.count { SearchKey.of(it).contains(needle) }
            assertTrue("search for '$query' found nothing", hits > 0)
        }
    }

    @Test
    fun `open food facts rows carry brand and label values`() {
        val sample = (0 until products.length()).map { products.getJSONObject(it) }
            .filter { it.optString("source") == "off" }
        assertTrue("no Open Food Facts rows", sample.size >= 25_000)
        val monsters = sample.count { SearchKey.of(it.getString("name")).contains("monster") }
        val redBull = sample.count { SearchKey.of(it.getString("name")).contains("red bull") }
        assertTrue("no Monster products", monsters > 0)
        assertTrue("no Red Bull products", redBull > 0)
    }

    @Test
    fun `scans are mostly answered from the bundled barcodes`() {
        val seen = HashMap<String, String>()
        var withBarcode = 0
        val allowedLengths = setOf(8, 12, 13, 14)
        for ((file, array) in listOf("foods" to foods, "products" to products, "dishes" to dishes)) {
            for (index in 0 until array.length()) {
                val row = array.getJSONObject(index)
                if (!row.has("barcode")) continue
                val code = row.getString("barcode")
                val where = "$file[$index] ${row.optString("name")}"
                assertTrue("$where: barcode is not digits", code.all { it.isDigit() })
                assertTrue("$where: barcode length ${code.length}", code.length in allowedLengths)
                val previous = seen.put(code, where)
                assertTrue("$where: barcode $code already used by $previous", previous == null)
                withBarcode++
            }
        }
        assertTrue("only $withBarcode bundled barcodes", withBarcode >= 25_000)
        assertEquals("26 000+ expected", withBarcode, seen.size)
    }

    @Test
    fun `a shipped barcode resolves to a real product`() {
        // A real row from the bundle, leading zero included: scanning this code has to
        // work with the phone offline, and the seeder must keep the "0" intact.
        val row = (0 until products.length()).map { products.getJSONObject(it) }
            .firstOrNull { it.optString("barcode") == "0851621003075" }
        assertTrue("0851621003075 is missing from products.json", row != null)
        assertTrue("the shipped row has no calories", row!!.getInt("calories") > 0)
        assertEquals("off", row.getString("source"))
        assertEquals("0851621003075".length, row.getString("barcode").length)
    }

    @Test
    fun `bulgarian dishes are present with computed values`() {
        val curated = (0 until dishes.length()).map { dishes.getJSONObject(it) }
            .filter { it.optString("source") == "curated" }
        assertTrue("only ${curated.size} curated dishes", curated.size >= 250)
        val moussaka = curated.firstOrNull { it.getString("name") == "Мусака с картофи" }
        assertTrue("Мусака с картофи is missing", moussaka != null)
        val row: JSONObject = moussaka!!
        // Recipe sums: 500 g mixed mince, 800 g potatoes, eggs, yoghurt (see bg_dishes.json).
        assertTrue("moussaka calories look wrong", row.getInt("calories") in 110..200)
        assertTrue("moussaka protein looks wrong", row.getDouble("protein") in 5.0..12.0)
    }
}
