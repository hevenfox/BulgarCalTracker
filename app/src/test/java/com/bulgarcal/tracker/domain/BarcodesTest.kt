package com.bulgarcal.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The scanner must only ever ask about a real retail barcode. */
class BarcodesTest {

    @Test
    fun `accepts every GTIN length a package can carry`() {
        assertEquals("96385074", Barcodes.normalize("96385074")) // EAN-8
        assertEquals("036000291452", Barcodes.normalize("036000291452")) // UPC-A
        assertEquals("4011800616519", Barcodes.normalize(" 4011800616519 ")) // EAN-13
        assertEquals("10012345678902", Barcodes.normalize("10012345678902")) // GTIN-14
    }

    @Test
    fun `rejects anything that is not a retail barcode`() {
        assertNull(Barcodes.normalize(null))
        assertNull(Barcodes.normalize(""))
        assertNull(Barcodes.normalize("   "))
        assertNull(Barcodes.normalize("40118006165")) // 11 digits is not a GTIN length
        assertNull(Barcodes.normalize("31106")) // Open Food Facts category ids look like this
        assertNull(Barcodes.normalize("https://example.com"))
        assertNull(Barcodes.normalize("4011 800 616 519"))
        assertNull(Barcodes.normalize("401180061651911")) // 15 digits
    }

    @Test
    fun `checks the GS1 check digit`() {
        assertTrue(Barcodes.hasValidCheckDigit("4011800616519"))
        assertTrue(Barcodes.hasValidCheckDigit("036000291452"))
        assertTrue(Barcodes.hasValidCheckDigit("96385074"))
        assertTrue(Barcodes.hasValidCheckDigit("4006381333931"))
        assertTrue(Barcodes.hasValidCheckDigit("5901234123457"))
        // One digit off, as a glare artefact would produce.
        assertFalse(Barcodes.hasValidCheckDigit("4011800616518"))
        assertFalse(Barcodes.hasValidCheckDigit(""))
        assertFalse(Barcodes.hasValidCheckDigit("31106"))
    }
}
