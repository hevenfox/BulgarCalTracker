package com.bulgarcal.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The search key is what makes "musaka" find "Мусака" on the food screen. */
class SearchKeyTest {

    @Test
    fun `cyrillic names are transliterated to latin`() {
        assertEquals("musaka", SearchKey.ofQuery("Мусака"))
        assertEquals("kyufteta", SearchKey.ofQuery("кюфтета"))
        assertEquals("shkembe chorba", SearchKey.ofQuery("Шкембе чорба"))
    }

    @Test
    fun `every cyrillic letter maps to a latin key`() {
        val alphabet = "абвгдежзийклмнопрстуфхцчшщъюя"
        assertTrue(alphabet.all { SearchKey.ofQuery(it.toString()).isNotEmpty() })
        assertEquals("a", SearchKey.ofQuery("ъ"))
        assertEquals("sht", SearchKey.ofQuery("щ"))
        assertEquals("", SearchKey.ofQuery("ь"))
    }

    @Test
    fun `punctuation collapses instead of breaking the match`() {
        assertEquals("kiselo mlyako 2", SearchKey.ofQuery("Кисело мляко 2%"))
        assertEquals("pileshko file", SearchKey.ofQuery("Пилешко   филе!"))
        assertEquals("0 5 l", SearchKey.ofQuery("0,5 л"))
    }

    @Test
    fun `latin names pass through unchanged`() {
        assertEquals("monster ultra white 500 ml", SearchKey.ofQuery("Monster Ultra White 500 ml"))
        assertTrue(SearchKey.of("Monster Ultra White 500 ml").startsWith("monster ultra white 500 ml"))
    }

    @Test
    fun `a stored key carries both the latin and the original name`() {
        val key = SearchKey.of("Мусака")
        assertTrue(key.startsWith("musaka"))
        assertTrue(key.contains("мусака"))
    }

    @Test
    fun `matching is done on the key in both directions`() {
        assertTrue(SearchKey.of("Кисело мляко 2%").contains(SearchKey.ofQuery("kiselo mlyako")))
        assertTrue(SearchKey.of("Monster Energy").contains(SearchKey.ofQuery("Monster")))
    }
}
