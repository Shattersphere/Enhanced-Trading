package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.StockItemType
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
        displayed += addItemType(rows, snapshot, state, tradeContext, layout, StockItemType.WEAPON, false)
        displayed += addItemType(rows, snapshot, state, tradeContext, layout, StockItemType.WING, true)
        if (displayed == 0) {
            rows.add(StockReviewListEmptyRows.main(snapshot, state))
        }
        return rows
    }

    private fun addItemType(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
        itemType: StockItemType,
        topGap: Boolean,
    ): Int {
        val count = snapshot?.getCount(itemType) ?: 0
        val expanded = state.isExpanded(itemType)
        StockReviewListSection.addHeading(rows, StockReviewHeadingRows.itemType(itemType, count, expanded, topGap))
        if (!expanded) return count

        var displayed = 0
        for (categorySection in StockReviewStockCategorySections.ORDERED) {
            displayed += categorySection.addTo(rows, snapshot, state, tradeContext, layout, itemType)
        }
        return displayed
    }
}
