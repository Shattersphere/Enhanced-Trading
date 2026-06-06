package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import weaponsprocurement.trade.plan.TradeMoney

object StockReviewShipPreflight {
    @JvmStatic
    fun canConfirm(trades: List<StockReviewPendingShipTrade>?): Boolean {
        if (trades.isNullOrEmpty()) return false
        val credits = Global.getSector()?.playerFleet?.cargo?.credits?.get() ?: return false
        return canConfirm(trades, credits)
    }

    @JvmStatic
    fun canConfirm(trades: List<StockReviewPendingShipTrade>, credits: Float): Boolean {
        val cost = netCreditCost(trades)
        if (cost > TradeMoney.MAX_EXECUTABLE_CREDITS) return false
        return cost <= 0L || credits + 0.01f >= cost.toFloat()
    }

    @JvmStatic
    fun netCreditCost(trades: List<StockReviewPendingShipTrade>): Long {
        var total = 0L
        total = addMatchingSide(total, trades, false)
        return addMatchingSide(total, trades, true)
    }

    private fun addMatchingSide(
        initial: Long,
        trades: List<StockReviewPendingShipTrade>,
        buy: Boolean,
    ): Long {
        var total = initial
        for (trade in trades) {
            if (trade.isBuy() != buy) continue
            val value = trade.unitPrice.toLong()
            total = if (buy) {
                TradeMoney.safeAdd(total, value)
            } else {
                TradeMoney.safeAdd(total, -value)
            }
        }
        return total
    }
}
