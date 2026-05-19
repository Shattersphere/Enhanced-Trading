package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.stock.item.SubmarketWeaponStock
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.stock.market.MarketStockService
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

/**
 * Reallocates queued local buys when black-market inclusion changes. It preserves total
 * intended quantity, but only for Local source mode where submarket allocation is real.
 */
object StockReviewLocalMarketRebalancer {
    @JvmStatic
    fun rebalanceBlackMarketToggle(
        previousSnapshot: WeaponStockSnapshot?,
        currentSnapshot: WeaponStockSnapshot?,
        previousTrades: List<StockReviewPendingTrade>?,
        intent: StockReviewLocalMarketIntent?,
        includeBlackMarket: Boolean,
    ): List<StockReviewPendingTrade> {
        if (previousTrades.isNullOrEmpty()) {
            return Collections.emptyList()
        }
        if (previousSnapshot?.getSourceMode() != StockSourceMode.LOCAL ||
            currentSnapshot?.getSourceMode() != StockSourceMode.LOCAL
        ) {
            return copyTrades(previousTrades)
        }

        val result = ArrayList<StockReviewPendingTrade>()
        val buyQuantityByItem = HashMap<String, Int>()
        for (trade in previousTrades) {
            if (trade.isSell()) {
                add(result, trade.itemKey, trade.submarketId, trade.quantity)
                continue
            }
            if (trade.isBuy()) {
                buyQuantityByItem[trade.itemKey] = (buyQuantityByItem[trade.itemKey] ?: 0) + trade.quantity
            }
        }

        for ((itemKey, quantity) in buyQuantityByItem) {
            val targetQuantity = intent?.desiredBuyQuantity(itemKey, quantity) ?: Math.max(0, quantity)
            addSourceBuys(result, currentSnapshot.getRecord(itemKey), targetQuantity, includeBlackMarket)
        }
        return result
    }

    private fun addSourceBuys(
        result: MutableList<StockReviewPendingTrade>,
        record: WeaponStockRecord?,
        requestedQuantity: Int,
        includeBlackMarket: Boolean,
    ) {
        if (record == null || requestedQuantity <= 0) {
            return
        }
        var remaining = requestedQuantity
        for (stock in orderedStocks(record, includeBlackMarket)) {
            if (remaining <= 0) break
            val quantity = Math.min(remaining, stock.count)
            if (quantity <= 0) continue
            add(result, record.itemKey, stock.sourceId, quantity)
            remaining -= quantity
        }
    }

    private fun orderedStocks(record: WeaponStockRecord, includeBlackMarket: Boolean): List<SubmarketWeaponStock> {
        val result = ArrayList<SubmarketWeaponStock>()
        for (stock in record.submarketStocks) {
            if (!stock.isPurchasable() || stock.count <= 0) continue
            if (!includeBlackMarket && isBlackMarket(stock)) continue
            result.add(stock)
        }
        Collections.sort(
            result,
            Comparator { left, right ->
                val blackCompare = blackPriority(left, includeBlackMarket).compareTo(blackPriority(right, includeBlackMarket))
                if (blackCompare != 0) {
                    blackCompare
                } else {
                    val priceCompare = left.unitPrice.compareTo(right.unitPrice)
                    if (priceCompare != 0) {
                        priceCompare
                    } else {
                        left.displaySourceName.orEmpty().compareTo(right.displaySourceName.orEmpty(), ignoreCase = true)
                    }
                }
            },
        )
        return result
    }

    private fun blackPriority(stock: SubmarketWeaponStock, includeBlackMarket: Boolean): Int {
        if (!includeBlackMarket) return 0
        return if (isBlackMarket(stock)) 0 else 1
    }

    private fun isBlackMarket(stock: SubmarketWeaponStock): Boolean =
        MarketStockService.isBlackMarketSubmarket(stock.submarketId)

    private fun copyTrades(trades: List<StockReviewPendingTrade>): List<StockReviewPendingTrade> {
        val result = ArrayList<StockReviewPendingTrade>()
        for (trade in trades) {
            add(result, trade.itemKey, trade.submarketId, trade.quantity)
        }
        return result
    }

    private fun add(
        result: MutableList<StockReviewPendingTrade>,
        itemKey: String?,
        sourceId: String?,
        quantity: Int,
    ) {
        for (trade in result) {
            if (trade.matches(itemKey, sourceId)) {
                trade.addQuantity(quantity)
                return
            }
        }
        val trade = StockReviewPendingTrade.create(itemKey, sourceId, quantity)
        if (trade != null) {
            result.add(trade)
        }
    }
}
