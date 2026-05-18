package weaponsprocurement.ui.stockreview.ships

import weaponsprocurement.stock.item.StockSortMode
import java.util.Collections
import java.util.HashMap

class StockReviewShipSnapshot(
    buyRecords: List<StockReviewShipRecord>,
    sellRecords: List<StockReviewShipRecord>,
) {
    private val buyRecords = Collections.unmodifiableList(ArrayList(buyRecords))
    private val sellRecords = Collections.unmodifiableList(ArrayList(sellRecords))
    private val recordsByKey: Map<String, StockReviewShipRecord> = buildRecordMap(this.buyRecords, this.sellRecords)

    fun buyRecords(sortMode: StockSortMode): List<StockReviewShipRecord> = sorted(buyRecords, sortMode)
    fun sellRecords(sortMode: StockSortMode): List<StockReviewShipRecord> = sorted(sellRecords, sortMode)
    fun allRecords(sortMode: StockSortMode): List<StockReviewShipRecord> {
        val result = ArrayList<StockReviewShipRecord>(buyRecords.size + sellRecords.size)
        result.addAll(buyRecords(sortMode))
        result.addAll(sellRecords(sortMode))
        return result
    }

    fun getRecord(key: String?): StockReviewShipRecord? =
        if (key.isNullOrEmpty()) null else recordsByKey[key]

    fun isEmpty(): Boolean = buyRecords.isEmpty() && sellRecords.isEmpty()

    companion object {
        @JvmField val EMPTY = StockReviewShipSnapshot(emptyList(), emptyList())

        private fun buildRecordMap(
            buyRecords: List<StockReviewShipRecord>,
            sellRecords: List<StockReviewShipRecord>,
        ): Map<String, StockReviewShipRecord> {
            val result = HashMap<String, StockReviewShipRecord>()
            for (record in buyRecords) result[record.key] = record
            for (record in sellRecords) result[record.key] = record
            return Collections.unmodifiableMap(result)
        }

        private fun sorted(records: List<StockReviewShipRecord>, sortMode: StockSortMode): List<StockReviewShipRecord> {
            val result = ArrayList(records)
            result.sortWith { left, right ->
                when (sortMode) {
                    StockSortMode.PRICE -> compareValues(left.price.finalCredits, right.price.finalCredits)
                    StockSortMode.NAME -> compareValues(left.displayName(), right.displayName())
                    StockSortMode.RARITY -> compareValues(left.member.hullSpec?.hullSize?.ordinal ?: 0, right.member.hullSpec?.hullSize?.ordinal ?: 0)
                    StockSortMode.NEED -> compareValues(left.side.ordinal, right.side.ordinal)
                }.let { if (it != 0) it else compareValues(left.displayName(), right.displayName()) }
            }
            return result
        }
    }
}
