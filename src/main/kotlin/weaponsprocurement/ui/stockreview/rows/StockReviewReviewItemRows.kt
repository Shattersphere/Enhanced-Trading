package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext

object StockReviewReviewItemRows {
    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot,
        trade: StockReviewPendingTrade,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ) {
        val record: WeaponStockRecord? = snapshot.getRecord(trade.itemKey)
        if (record == null) {
            rows.add(StockReviewListRow.fromSpec(StockReviewRowSpecs.reviewMissing(trade.itemKey)))
            return
        }

        val expanded = state.isItemExpanded(record.itemKey)
        rows.add(row(record, trade, expanded, tradeContext, layout))
        if (!expanded) {
            return
        }

        StockReviewItemInfoRows.add(rows, record, state, layout)
        if (trade.isBuy()) {
            StockReviewSourceAllocationRows.add(rows, tradeContext.sellerAllocations(trade), layout)
        }
    }

    private fun row(
        record: WeaponStockRecord,
        trade: StockReviewPendingTrade,
        expanded: Boolean,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ): WimGuiListRow<StockReviewAction> {
        val itemTooltip = StockReviewTooltips.itemDataToggle(record)
        return StockReviewItemRowFrame.build(
            StockReviewItemDetailHeadingRows.itemLabel(record, expanded),
            StockReviewTradeRowCells.reviewCells(record, trade, tradeContext, layout),
            StockReviewActionRef.rowExpansion(StockReviewAction.toggleItem(record.itemKey)),
            itemTooltip,
            StockReviewItemTooltip.forRecord(record, itemTooltip),
            layout,
            StockReviewRowIcon.item(record),
        )
    }
}
