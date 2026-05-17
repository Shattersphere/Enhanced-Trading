package weaponsprocurement.ui.stockreview.trade

import java.util.Collections
import java.util.HashMap

class StockReviewLocalMarketIntent(source: Map<String, Int>? = null) {
    private val desiredBuysByItem = HashMap<String, Int>()

    init {
        replaceWith(source)
    }

    fun asMap(): Map<String, Int> = Collections.unmodifiableMap(HashMap(desiredBuysByItem))

    fun replaceWith(source: Map<String, Int>?) {
        desiredBuysByItem.clear()
        if (source == null) return
        for ((itemKey, quantity) in source) {
            if (itemKey.isNotEmpty() && quantity > 0) {
                desiredBuysByItem[itemKey] = quantity
            }
        }
    }

    fun seedFromTrades(trades: List<StockReviewPendingTrade>?) {
        if (trades.isNullOrEmpty()) return
        for ((itemKey, quantity) in buyQuantitiesByItem(trades)) {
            if (!desiredBuysByItem.containsKey(itemKey) && quantity > 0) {
                desiredBuysByItem[itemKey] = quantity
            }
        }
    }

    fun captureFromTrades(trades: List<StockReviewPendingTrade>?) {
        desiredBuysByItem.clear()
        desiredBuysByItem.putAll(buyQuantitiesByItem(trades))
    }

    fun desiredBuyQuantity(itemKey: String?, fallbackQuantity: Int): Int {
        if (itemKey == null) return Math.max(0, fallbackQuantity)
        return desiredBuysByItem[itemKey] ?: Math.max(0, fallbackQuantity)
    }

    fun clear() {
        desiredBuysByItem.clear()
    }

    fun clearItem(itemKey: String?) {
        if (itemKey != null) {
            desiredBuysByItem.remove(itemKey)
        }
    }

    companion object {
        private fun buyQuantitiesByItem(trades: List<StockReviewPendingTrade>?): Map<String, Int> {
            val result = HashMap<String, Int>()
            if (trades == null) return result
            for (trade in trades) {
                if (trade.isBuy()) {
                    result[trade.itemKey] = (result[trade.itemKey] ?: 0) + trade.quantity
                }
            }
            return result
        }
    }
}
