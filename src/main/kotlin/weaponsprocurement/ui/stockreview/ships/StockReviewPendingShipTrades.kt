package weaponsprocurement.ui.stockreview.ships

import java.util.Collections
import java.util.LinkedHashMap

class StockReviewPendingShipTrades {
    private val tradesByKey = LinkedHashMap<String, StockReviewPendingShipTrade>()
    private var revision = 0

    fun getRevision(): Int = revision
    fun isEmpty(): Boolean = tradesByKey.isEmpty()
    fun size(): Int = tradesByKey.size
    fun contains(recordKey: String?): Boolean = !recordKey.isNullOrEmpty() && tradesByKey.containsKey(recordKey)
    fun asList(): List<StockReviewPendingShipTrade> = immutableCopy(tradesByKey.values)

    fun replaceWith(source: List<StockReviewPendingShipTrade>?) {
        tradesByKey.clear()
        if (!source.isNullOrEmpty()) {
            for (trade in source) {
                tradesByKey[trade.recordKey] = trade.copy()
            }
        }
        revision++
    }

    fun toggle(record: StockReviewShipRecord?): Boolean {
        val trade = StockReviewPendingShipTrade.fromRecord(record) ?: return false
        if (tradesByKey.remove(trade.recordKey) == null) {
            tradesByKey[trade.recordKey] = trade
        }
        revision++
        return true
    }

    fun reset(recordKey: String?): Boolean {
        if (recordKey.isNullOrEmpty() || tradesByKey.remove(recordKey) == null) {
            return false
        }
        revision++
        return true
    }

    fun clear() {
        if (tradesByKey.isNotEmpty()) {
            tradesByKey.clear()
            revision++
        }
    }

    companion object {
        @JvmStatic
        fun immutableCopy(source: Collection<StockReviewPendingShipTrade>?): List<StockReviewPendingShipTrade> {
            if (source.isNullOrEmpty()) return emptyList()
            val result = ArrayList<StockReviewPendingShipTrade>()
            for (trade in source) {
                result.add(trade.copy())
            }
            return Collections.unmodifiableList(result)
        }
    }
}
