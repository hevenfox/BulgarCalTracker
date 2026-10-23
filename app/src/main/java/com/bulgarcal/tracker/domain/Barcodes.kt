package com.bulgarcal.tracker.domain

/**
 * Retail barcode helpers shared by the scanner and by manual entry.
 *
 * Only GTIN lengths are accepted (EAN-8, UPC-A, EAN-13, GTIN-14): those are what
 * packaged food carries, and restricting the set keeps a stray QR code or a code
 * on the packaging from being looked up as a product number.
 */
object Barcodes {

    private val LENGTHS = setOf(8, 12, 13, 14)

    /** Digits-only GTIN, or null when the text is not a usable barcode. */
    fun normalize(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text.any { !it.isDigit() }) return null
        return text.takeIf { it.length in LENGTHS }
    }

    /** True for a digits-only GTIN of an accepted length (check digit not checked). */
    fun isPlausible(raw: String?): Boolean = normalize(raw) != null

    /**
     * GS1 check digit. A camera read that passes can be trusted immediately; one
     * that fails is usually a glare artefact and needs a second identical read.
     */
    fun hasValidCheckDigit(code: String): Boolean {
        if (!isPlausible(code)) return false
        var sum = 0
        var weight = 3
        for (index in code.length - 2 downTo 0) {
            sum += (code[index] - '0') * weight
            weight = if (weight == 3) 1 else 3
        }
        val expected = (10 - sum % 10) % 10
        return expected == code.last().toString().toInt()
    }
}
