package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext

object StockReviewTradeItemRows {
    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ) {
        val expanded = state.isItemExpanded(record.itemKey)
        rows.add(row(record, expanded, tradeContext, layout))
        if (expanded) {
            StockReviewItemInfoRows.add(rows, record, state, layout)
        }
    }

    private fun row(
        record: WeaponStockRecord,
        expanded: Boolean,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ): WimGuiListRow<StockReviewAction> {
        val itemTooltip = StockReviewTooltips.itemDataToggle(record)
        return StockReviewItemRowFrame.build(
            StockReviewItemDetailHeadingRows.itemLabel(record, expanded),
            StockReviewTradeRowCells.tradeCells(record, tradeContext, layout),
            StockReviewActionRef.rowExpansion(StockReviewAction.toggleItem(record.itemKey)),
            itemTooltip,
            StockReviewItemTooltip.forRecord(record, itemTooltip),
            layout,
            StockReviewRowIcon.item(record),
        )
    }
}
