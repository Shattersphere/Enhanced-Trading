package weaponsprocurement.ui.stockreview.trade



import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.state.StockReviewFilters
import java.util.ArrayList

class StockReviewTradePlanner private constructor() {
    private class CheapestBuyRecordComparator(snapshot: WeaponStockSnapshot?) : Comparator<WeaponStockRecord> {
        private val quoteBook = StockReviewQuoteBook(snapshot)

        override fun compare(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            val result = quoteBook.cheapestUnitPrice(left).compareTo(quoteBook.cheapestUnitPrice(right))
            if (result != 0) {
                return result
            }
            return (left.displayName ?: "").compareTo(right.displayName ?: "", ignoreCase = true)
        }
    }

    companion object {
        private const val MATCH_SELL = 0
        private const val MATCH_SELLER_BUY = 1
        private const val MATCH_GENERIC_BUY = 2

        @JvmStatic
        fun tradeableRecords(snapshot: WeaponStockSnapshot?, category: StockCategory?): List<WeaponStockRecord> {
            val result = ArrayList<WeaponStockRecord>()
            if (snapshot == null || category == null) {
                return result
            }
            addTradeableRecords(result, snapshot.getRecords(category))
            return result
        }

        @JvmStatic
        fun tradeableRecords(
            snapshot: WeaponStockSnapshot?,
            itemType: StockItemType?,
            category: StockCategory?,
        ): List<WeaponStockRecord> {
            val result = ArrayList<WeaponStockRecord>()
            if (snapshot == null || category == null) {
                return result
            }
            addTradeableRecords(result, snapshot.getRecords(itemType, category))
            return result
        }

        @JvmStatic
        fun tradeableRecords(snapshot: WeaponStockSnapshot?): List<WeaponStockRecord> {
            val result = ArrayList<WeaponStockRecord>()
            if (snapshot == null) {
                return result
            }
            for (category in StockCategory.values()) {
                addTradeableRecords(result, snapshot.getRecords(category))
            }
            return result
        }

        @JvmStatic
        fun buyableRecords(snapshot: WeaponStockSnapshot?): List<WeaponStockRecord> {
            val result = ArrayList<WeaponStockRecord>()
            if (snapshot == null) {
                return result
            }
            for (category in StockCategory.values()) {
                addBuyableRecords(result, snapshot.getRecords(category))
            }
            return result
        }

        @JvmStatic
        fun filteredTradeableRecords(
            snapshot: WeaponStockSnapshot?,
            activeFilters: Set<StockReviewFilter>?,
        ): List<WeaponStockRecord> = filteredRecords(tradeableRecords(snapshot), activeFilters)

        @JvmStatic
        fun cheapestFirstFilteredBuyableRecords(
            snapshot: WeaponStockSnapshot?,
            activeFilters: Set<StockReviewFilter>?,
        ): List<WeaponStockRecord> {
            val result = filteredRecords(buyableRecords(snapshot), activeFilters)
            return result.sortedWith(CheapestBuyRecordComparator(snapshot))
        }

        @JvmStatic
        fun withAdjustment(
            pendingTrades: List<StockReviewPendingTrade>?,
            itemKey: String?,
            submarketId: String?,
            delta: Int,
        ): List<StockReviewPendingTrade> {
            val result = ArrayList<StockReviewPendingTrade>()
            var adjusted = false
            if (pendingTrades != null) {
                for (trade in pendingTrades) {
                    var quantity = trade.quantity
                    if (trade.matches(itemKey, submarketId)) {
                        quantity += delta
                        adjusted = true
                    }
                    if (quantity != 0) {
                        addPending(result, trade.itemKey, trade.submarketId, quantity)
                    }
                }
            }
            if (!adjusted && delta != 0) {
                addPending(result, itemKey, submarketId, delta)
            }
            return result
        }

        private fun addPending(result: MutableList<StockReviewPendingTrade>, itemKey: String?, submarketId: String?, quantity: Int) {
            val trade = StockReviewPendingTrade.create(itemKey, submarketId, quantity)
            if (trade != null) {
                result.add(trade)
            }
        }

        @JvmStatic
        fun executionOrder(pendingTrades: List<StockReviewPendingTrade>?): List<StockReviewPendingTrade> {
            val result = ArrayList<StockReviewPendingTrade>()
            if (pendingTrades.isNullOrEmpty()) {
                return result
            }
            addMatching(result, pendingTrades, MATCH_SELL)
            addMatching(result, pendingTrades, MATCH_SELLER_BUY)
            addMatching(result, pendingTrades, MATCH_GENERIC_BUY)
            return result
        }

        private fun addMatching(
            result: MutableList<StockReviewPendingTrade>,
            pendingTrades: List<StockReviewPendingTrade>,
            match: Int,
        ) {
            for (trade in pendingTrades) {
                if (matches(trade, match)) {
                    result.add(trade)
                }
            }
        }

        private fun matches(trade: StockReviewPendingTrade, match: Int): Boolean {
            if (match == MATCH_SELL) {
                return trade.isSell()
            }
            if (match == MATCH_SELLER_BUY) {
                return trade.isBuy() && trade.submarketId != null
            }
            return trade.isBuy() && trade.submarketId == null
        }

        private fun filteredRecords(
            records: List<WeaponStockRecord>,
            activeFilters: Set<StockReviewFilter>?,
        ): List<WeaponStockRecord> {
            if (StockReviewFilters.count(activeFilters) <= 0) {
                return records
            }
            val result = ArrayList<WeaponStockRecord>()
            for (record in records) {
                if (StockReviewFilters.matches(record, activeFilters)) {
                    result.add(record)
                }
            }
            return result
        }

        private fun addBuyableRecords(result: MutableList<WeaponStockRecord>, records: List<WeaponStockRecord>?) {
            if (records == null) {
                return
            }
            for (record in records) {
                if (record.buyableCount > 0) {
                    result.add(record)
                }
            }
        }

        private fun addTradeableRecords(result: MutableList<WeaponStockRecord>, records: List<WeaponStockRecord>?) {
            if (records == null) {
                return
            }
            for (record in records) {
                if (record.buyableCount > 0 || record.playerCargoCount > 0) {
                    result.add(record)
                }
            }
        }
    }
}
