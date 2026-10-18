package com.bulgarcal.tracker.domain

/**
 * Latin search key for a food name.
 *
 * Bulgarian users type on both layouts, so every food stores a key that carries
 * the name transliterated to Latin letters plus the name itself, and a query is
 * transliterated the same way before it is matched. "Мусака" and "musaka" then
 * both find "Мусака", and a Latin-named product ("Monster Ultra White 500 ml")
 * keeps working because Latin text passes through unchanged.
 *
 * Pure Kotlin, no Android dependencies, so it is covered by JVM unit tests.
 */
object SearchKey {

    private val CYRILLIC_TO_LATIN = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l",
        'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s",
        'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sht", 'ъ' to "a", 'ь' to "", 'ю' to "yu", 'я' to "ya"
    )

    /** Lowercase, transliterated, punctuation-free, single-spaced. */
    fun latin(text: String): String {
        val out = StringBuilder(text.length)
        for (raw in text.lowercase()) {
            val mapped = when {
                raw in CYRILLIC_TO_LATIN -> CYRILLIC_TO_LATIN.getValue(raw)
                raw in 'a'..'z' || raw in '0'..'9' -> raw.toString()
                raw.isLetter() -> raw.toString()
                else -> " "
            }
            out.append(mapped)
        }
        return out.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** Value stored in `foods.searchKey`. */
    fun of(name: String): String {
        val lower = name.lowercase().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
        return (latin(name) + " " + lower).trim()
    }

    /** Value to match with: the query, transliterated the same way. */
    fun ofQuery(query: String): String = latin(query)
}
