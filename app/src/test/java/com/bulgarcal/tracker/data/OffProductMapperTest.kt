package com.bulgarcal.tracker.data

import com.bulgarcal.tracker.data.remote.OffLookup
import com.bulgarcal.tracker.data.remote.OffProductMapper
import com.bulgarcal.tracker.domain.FoodCategories
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Maps real Open Food Facts payloads (trimmed, but with the live field names and
 * values captured from `api/v3/product`) onto the app's food row.
 */
class OffProductMapperTest {

    /** Corny big dark chocolate, barcode 4011800616519, fetched from the live API. */
    private val corny = JSONObject(
        """
        {
          "code": "4011800616519",
          "product_name": "Corny big dark chocolate",
          "product_name_bg": "",
          "brands": "Corny, Schwartau",
          "quantity": "90 g",
          "serving_quantity": 100,
          "categories_tags": ["en:snacks", "en:sweet-snacks", "en:bars", "en:cereal-bars"],
          "nutriments": {
            "energy-kcal_100g": 424,
            "energy-kj_100g": 1774,
            "proteins_100g": 5.3,
            "carbohydrates_100g": 67.3,
            "fat_100g": 13.8,
            "saturated-fat_100g": 8.1,
            "sugars_100g": 34,
            "fiber_100g": 3.4,
            "salt_100g": 0.51
          }
        }
        """.trimIndent()
    )

    @Test
    fun `a complete product becomes a usable food`() {
        val result = OffProductMapper.map(corny, "4011800616519")
        assertTrue("expected Found, got $result", result is OffLookup.Found)
        val food = (result as OffLookup.Found).food

        assertEquals(424, food.calories)
        assertEquals(67.3f, food.carbs, 0.01f)
        assertEquals(5.3f, food.protein, 0.01f)
        assertEquals(13.8f, food.fats, 0.01f)
        assertEquals(8.1f, food.saturatedFats, 0.01f)
        assertEquals(34f, food.sugars, 0.01f)
        assertEquals(3.4f, food.fiber, 0.01f)
        assertEquals(0.51f, food.salt, 0.01f)
        assertEquals(100f, food.servingGrams, 0.01f)
        assertEquals("4011800616519", food.barcode)
        assertEquals("off", food.source)
        assertEquals("Снаксове", food.category)
        // The package size is folded into the name like the bundled rows do (the brand
        // is already part of the name, so it is not repeated).
        assertEquals("Corny big dark chocolate 90 g", food.name)
        assertTrue(food.searchKey.contains("corny"))
    }

    @Test
    fun `the Bulgarian name wins when Open Food Facts has one`() {
        val bg = JSONObject(corny.toString()).apply {
            put("product_name_bg", "Корни биг черен шоколад")
            put("brands", "Корни")
        }
        val food = (OffProductMapper.map(bg, "4011800616519") as OffLookup.Found).food
        assertEquals("Корни биг черен шоколад 90 g", food.name)
    }

    @Test
    fun `kilojoules and sodium stand in when kcal and salt are missing`() {
        val product = JSONObject(
            """
            {
              "product_name": "Прясно мляко 3,6%",
              "brands": ["Бор Чвор"],
              "serving_size": "250 ml",
              "categories_tags": ["en:dairies", "en:milks"],
              "nutriments": {
                "energy-kj_100g": 251,
                "proteins_100g": "3.3",
                "carbohydrates_100g": "4.7",
                "fat_100g": 3.6,
                "sodium_100g": 0.04
              }
            }
            """.trimIndent()
        )
        val food = (OffProductMapper.map(product, "3800000000017") as OffLookup.Found).food
        assertEquals(60, food.calories) // 251 kJ / 4.184
        assertEquals(0.1f, food.salt, 0.001f) // 0.04 g sodium * 2.5
        assertEquals(3.3f, food.protein, 0.01f)
        assertEquals("Млечни продукти", food.category)
        assertEquals(100f, food.servingGrams, 0.01f) // "250 ml" is not grams
        assertEquals("Прясно мляко 3,6% Бор Чвор", food.name)
    }

    @Test
    fun `an incomplete label is offered as a form to finish, not saved`() {
        val product = JSONObject(
            """
            {
              "product_name": "Домат",
              "nutriments": {"proteins_100g": 0.9}
            }
            """.trimIndent()
        )
        val result = OffProductMapper.map(product, "2000000000015")
        assertTrue("expected NotFound, got $result", result is OffLookup.NotFound)
        val preview = (result as OffLookup.NotFound).preview
        assertNotNull(preview)
        // Nothing is invented: the missing values stay 0 and the user finishes them.
        assertEquals("Домат", preview!!.name)
        assertEquals(0, preview.calories)
        assertEquals(0.9f, preview.protein, 0.01f)
        assertEquals("2000000000015", preview.barcode)
    }

    @Test
    fun `an empty product still yields a barcode-bound form`() {
        val result = OffProductMapper.map(JSONObject("{}"), "2000000000015")
        val preview = (result as OffLookup.NotFound).preview
        assertNotNull(preview)
        assertEquals("Продукт 2000000000015", preview!!.name)
        assertTrue(preview.category in FoodCategories)
        assertEquals(0, preview.calories)
    }

    @Test
    fun `every mapped product lands in a browsable category`() {
        val tags = listOf(
            "en:cheeses", "en:yogurts", "en:sausages", "en:breads", "en:pastas",
            "en:chocolates", "en:ice-creams", "en:nuts", "en:vegetable-oils",
            "en:mayonnaises", "en:baby-foods", "en:beers", "en:lentils",
            "en:fruit-juices", "en:mineral-waters", "en:coffees", "en:eggs",
            "en:fishes", "en:ready-meals", "en:something-unheard-of"
        )
        for (tag in tags) {
            val product = JSONObject(
                """
                {
                  "product_name": "Тестов продукт",
                  "categories_tags": ["$tag"],
                  "nutriments": {
                    "energy-kcal_100g": 100, "proteins_100g": 1,
                    "carbohydrates_100g": 2, "fat_100g": 3
                  }
                }
                """.trimIndent()
            )
            val food = (OffProductMapper.map(product, "2000000000015") as OffLookup.Found).food
            assertTrue("$tag mapped to an unbrowsable category", food.category in FoodCategories)
            assertTrue("$tag mapped to Моите Рецепти", food.category != "Моите Рецепти")
        }
    }
}
