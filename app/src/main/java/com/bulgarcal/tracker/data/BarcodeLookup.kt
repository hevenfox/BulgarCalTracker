package com.bulgarcal.tracker.data

import com.bulgarcal.tracker.data.remote.OffLookup
import com.bulgarcal.tracker.data.remote.OpenFoodFactsClient
import com.bulgarcal.tracker.domain.Barcodes

/** Outcome of a scan, in the order the app tries to answer it. */
sealed interface ScanResult {
    /** Ready to log. [scanned] is true when the row was just fetched and stored. */
    data class Found(val food: FoodEntity, val scanned: Boolean) : ScanResult

    /** Nothing knows this barcode; [preview] carries whatever Open Food Facts had. */
    data class Unknown(val barcode: String, val preview: FoodEntity?) : ScanResult

    data class Offline(val barcode: String) : ScanResult

    data class Busy(val barcode: String) : ScanResult

    data class Invalid(val raw: String) : ScanResult
}

/**
 * Answers a barcode in three steps: the local database first (the ~26 000 shipped
 * barcodes and every product scanned before, so this needs no network), then Open
 * Food Facts. A product found there is stored before it is returned, which makes it
 * searchable by name later and makes the next scan of the same code instant.
 */
class BarcodeLookup(
    private val repo: TrackerRepository,
    private val client: OpenFoodFactsClient = OpenFoodFactsClient()
) {
    suspend fun resolve(raw: String): ScanResult {
        val code = Barcodes.normalize(raw) ?: return ScanResult.Invalid(raw)
        repo.foodByBarcode(code)?.let { return ScanResult.Found(it, scanned = false) }
        return when (val lookup = client.lookup(code)) {
            is OffLookup.Found ->
                ScanResult.Found(repo.saveScannedProduct(lookup.food), scanned = true)
            is OffLookup.NotFound -> ScanResult.Unknown(code, lookup.preview)
            OffLookup.Unreachable -> ScanResult.Offline(code)
            OffLookup.Busy -> ScanResult.Busy(code)
        }
    }
}
