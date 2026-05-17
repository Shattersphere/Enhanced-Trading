package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockSnapshot

object StockReviewListModel {
    @JvmStatic
    fun build(
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState?,
        tradeContext: StockReviewTradeContext,
    ): List<WimGuiListRow<StockReviewAction>> = build(snapshot, state, tradeContext, StockReviewRowLayout.trade())

    @JvmStatic
    fun build(
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState?,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        if (state == null) {
            rows.add(StockReviewListEmptyRows.main(snapshot, null))
            return rows
        }
        var displayed = 0
        for (itemTypeSection in StockReviewItemTypeSections.ORDERED) {
            displayed += itemTypeSection.addTo(rows, snapshot, state, tradeContext, layout)
        }
        if (displayed == 0) {
            rows.add(StockReviewListEmptyRows.main(snapshot, state))
        }
        return rows
    }
}
