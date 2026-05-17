package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.util.ArrayList

class StockReviewReviewListModel private constructor() {
    companion object {
        @JvmStatic
        fun build(
            snapshot: WeaponStockSnapshot,
            pendingTrades: List<StockReviewPendingTrade>?,
            state: StockReviewState,
            tradeContext: StockReviewTradeContext,
        ): List<WimGuiListRow<StockReviewAction>> = build(snapshot, pendingTrades, state, tradeContext, StockReviewRowLayout.review())

        @JvmStatic
        fun build(
            snapshot: WeaponStockSnapshot,
            pendingTrades: List<StockReviewPendingTrade>?,
            state: StockReviewState,
            tradeContext: StockReviewTradeContext,
            layout: StockReviewRowLayout,
        ): List<WimGuiListRow<StockReviewAction>> {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            if (pendingTrades.isNullOrEmpty()) {
                rows.add(StockReviewListEmptyRows.review())
                return rows
            }
            val groupedTrades = StockReviewTradeGroupSections.build(pendingTrades)
            if (groupedTrades.allEmpty()) {
                rows.add(StockReviewListEmptyRows.review())
                return rows
            }
            groupedTrades.addTo(rows, snapshot, state, tradeContext, layout)
            return rows
        }
    }
}
