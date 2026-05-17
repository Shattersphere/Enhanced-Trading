package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewSellerAllocation
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot

object StockReviewItemRows {
    private const val WORST_CASE_LABEL = "Debug Worst-Case Suzuki-Clapteryon Thermal Prokector [Observed/Very Rare] (+)"

    @JvmStatic
    fun addTradeRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ) {
        val expanded = state.isItemExpanded(record.itemKey)
        addItemRow(
            rows,
            layout,
            record,
            WimGuiToggleHeading.label(record.displayNameWithFixerMarker, expanded),
            StockReviewTradeRowCells.tradeCells(record, tradeContext, layout),
            StockReviewAction.toggleItem(record.itemKey),
        )
        if (!expanded) {
            return
        }
        StockReviewItemInfoRows.add(rows, record, state, layout)
    }

    @JvmStatic
    fun addReviewRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot,
        trade: StockReviewPendingTrade,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ) {
        val record: WeaponStockRecord? = snapshot.getRecord(trade.itemKey)
        if (record == null) {
            rows.add(StockReviewListRow.review(trade.itemKey))
            return
        }
        val expanded = state.isItemExpanded(record.itemKey)
        addItemRow(
            rows,
            layout,
            record,
            WimGuiToggleHeading.label(record.displayNameWithFixerMarker, expanded),
            StockReviewTradeRowCells.reviewCells(record, trade, tradeContext, layout),
            StockReviewAction.toggleItem(record.itemKey),
        )
        if (!expanded) {
            return
        }
        StockReviewItemInfoRows.add(rows, record, state, layout)
        if (trade.isBuy()) {
            addSourceAllocationRows(rows, tradeContext.sellerAllocations(trade), layout)
        }
    }

    @JvmStatic
    fun addWorstCaseRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        layout: StockReviewRowLayout,
    ) {
        rows.add(
            StockReviewListRow.item(
                WORST_CASE_LABEL,
                StockReviewTradeRowCells.worstCaseCells(layout),
                StockReviewAction.debugNoop(),
                "Worst-case row-width test sample. It does not affect trades.",
                layout.itemIndent,
            ),
        )
    }

    @JvmStatic
    fun addSourceAllocationRows(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        allocations: List<StockReviewSellerAllocation>,
        layout: StockReviewRowLayout,
    ) {
        if (allocations.isEmpty()) {
            rows.add(
                StockReviewListRow.labelTextIndented(
                    "Purchase Source",
                    "Unavailable",
                    layout,
                    layout.dataIndent,
                    true,
                ),
            )
            return
        }
        for (i in allocations.indices) {
            val allocation = allocations[i]
            rows.add(
                StockReviewListRow.labelTextIndented(
                    sourceLabel(allocation),
                    allocationSummary(allocation),
                    layout,
                    layout.dataIndent,
                    i == 0,
                ),
            )
        }
    }

    private fun addItemRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        layout: StockReviewRowLayout,
        record: WeaponStockRecord,
        label: String,
        cells: List<WimGuiRowCell<StockReviewAction>>,
        action: StockReviewAction,
    ) {
        val itemTooltip = StockReviewTooltips.itemDataToggle(record)
        rows.add(
            StockReviewListRow.item(
                label,
                cells,
                action,
                itemTooltip,
                StockReviewItemTooltip.forRecord(record, itemTooltip),
                layout.itemIndent,
                StockReviewRowIcon.item(record),
            ),
        )
    }

    private fun sourceLabel(allocation: StockReviewSellerAllocation): String =
        if (allocation.submarketName.isNullOrEmpty()) "Purchase Source" else allocation.submarketName

    private fun allocationSummary(allocation: StockReviewSellerAllocation): String =
        allocation.quantity.toString() + " / " + StockReviewFormat.credits(allocation.cost)
}
