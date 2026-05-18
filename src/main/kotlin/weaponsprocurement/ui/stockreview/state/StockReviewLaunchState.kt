package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrade
import java.util.ArrayList
import java.util.Collections

class StockReviewLaunchState(
    state: StockReviewState?,
    pendingTrades: List<StockReviewPendingTrade>?,
    pendingShipTrades: List<StockReviewPendingShipTrade>?,
    localBuyIntent: Map<String, Int>?,
    private val reviewMode: Boolean,
) {
    private val state: StockReviewState? = if (state == null) null else StockReviewState(state)
    private val pendingTrades: List<StockReviewPendingTrade> = immutableCopy(pendingTrades)
    private val pendingShipTrades: List<StockReviewPendingShipTrade> = immutableShipCopy(pendingShipTrades)
    private val localBuyIntent: Map<String, Int> = immutableIntentCopy(localBuyIntent)

    constructor(
        state: StockReviewState?,
        pendingTrades: List<StockReviewPendingTrade>?,
        reviewMode: Boolean,
    ) : this(state, pendingTrades, null, null, reviewMode)

    constructor(
        state: StockReviewState?,
        pendingTrades: List<StockReviewPendingTrade>?,
        localBuyIntent: Map<String, Int>?,
        reviewMode: Boolean,
    ) : this(state, pendingTrades, null, localBuyIntent, reviewMode)

    fun getState(): StockReviewState? = state

    fun getPendingTrades(): List<StockReviewPendingTrade> = pendingTrades

    fun getPendingShipTrades(): List<StockReviewPendingShipTrade> = pendingShipTrades

    fun getLocalBuyIntent(): Map<String, Int> = localBuyIntent

    fun isReviewMode(): Boolean = reviewMode

    companion object {
        private fun immutableCopy(source: List<StockReviewPendingTrade>?): List<StockReviewPendingTrade> {
            if (source.isNullOrEmpty()) {
                return emptyList()
            }
            val result = ArrayList<StockReviewPendingTrade>()
            for (trade in source) {
                val copy = trade?.copy()
                if (copy != null) {
                    result.add(copy)
                }
            }
            return Collections.unmodifiableList(result)
        }

        private fun immutableShipCopy(source: List<StockReviewPendingShipTrade>?): List<StockReviewPendingShipTrade> {
            if (source.isNullOrEmpty()) {
                return emptyList()
            }
            val result = ArrayList<StockReviewPendingShipTrade>()
            for (trade in source) {
                result.add(trade.copy())
            }
            return Collections.unmodifiableList(result)
        }

        private fun immutableIntentCopy(source: Map<String, Int>?): Map<String, Int> {
            if (source.isNullOrEmpty()) {
                return emptyMap()
            }
            val result = HashMap<String, Int>()
            for ((itemKey, quantity) in source) {
                if (itemKey.isNotEmpty() && quantity > 0) {
                    result[itemKey] = quantity
                }
            }
            return Collections.unmodifiableMap(result)
        }
    }
}
