package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
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
                rows.add(StockReviewListRow.empty("No trades are planned."))
                return rows
            }
            val buying = reviewTradesForGroup(pendingTrades, StockReviewTradeGroup.BUYING)
            val selling = reviewTradesForGroup(pendingTrades, StockReviewTradeGroup.SELLING)
            if (buying.isEmpty() && selling.isEmpty()) {
                rows.add(StockReviewListRow.empty("No trades are planned."))
                return rows
            }
            addReviewGroup(rows, snapshot, buying, state, tradeContext, layout, StockReviewTradeGroup.BUYING)
            addReviewGroup(rows, snapshot, selling, state, tradeContext, layout, StockReviewTradeGroup.SELLING)
            return rows
        }

        private fun addReviewGroup(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            snapshot: WeaponStockSnapshot,
            groupTrades: List<StockReviewPendingTrade>,
            state: StockReviewState,
            tradeContext: StockReviewTradeContext,
            layout: StockReviewRowLayout,
            tradeGroup: StockReviewTradeGroup,
        ) {
            val expanded = state.isExpanded(tradeGroup)
            rows.add(
                StockReviewHeadingRows.reviewGroup(
                    tradeGroup,
                    groupTrades.size,
                    expanded,
                    StockReviewTradeGroup.SELLING == tradeGroup,
                ),
            )
            if (!expanded) {
                return
            }
            if (StockReviewStyle.SHOW_WIDTH_TEST_ROWS && StockReviewTradeGroup.BUYING == tradeGroup) {
                StockReviewItemRows.addWorstCaseRow(rows, layout)
            }
            for (trade in groupTrades) {
                StockReviewItemRows.addReviewRow(rows, snapshot, trade, state, tradeContext, layout)
            }
        }

        private fun reviewTradesForGroup(
            pendingTrades: List<StockReviewPendingTrade>?,
            tradeGroup: StockReviewTradeGroup,
        ): List<StockReviewPendingTrade> {
            val result = ArrayList<StockReviewPendingTrade>()
            if (pendingTrades == null) {
                return result
            }
            for (trade in pendingTrades) {
                if (StockReviewTradeGroup.BUYING == tradeGroup && trade.isBuy()) {
                    result.add(trade)
                } else if (StockReviewTradeGroup.SELLING == tradeGroup && trade.isSell()) {
                    result.add(trade)
                }
            }
            return result
        }
    }
}
