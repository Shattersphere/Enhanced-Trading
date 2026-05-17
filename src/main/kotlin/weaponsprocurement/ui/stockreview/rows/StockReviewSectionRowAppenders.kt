package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext

class StockReviewTradeRecordRowAppender(
    private val state: StockReviewState,
    private val tradeContext: StockReviewTradeContext,
    private val layout: StockReviewRowLayout,
) : StockReviewSectionRowAppender<WeaponStockRecord> {
    override fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, item: WeaponStockRecord) {
        StockReviewTradeItemRows.add(rows, item, state, tradeContext, layout)
    }
}

class StockReviewReviewTradeRowAppender(
    private val snapshot: WeaponStockSnapshot,
    private val state: StockReviewState,
    private val tradeContext: StockReviewTradeContext,
    private val layout: StockReviewRowLayout,
) : StockReviewSectionRowAppender<StockReviewPendingTrade> {
    override fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, item: StockReviewPendingTrade) {
        StockReviewReviewItemRows.add(rows, snapshot, item, state, tradeContext, layout)
    }
}
