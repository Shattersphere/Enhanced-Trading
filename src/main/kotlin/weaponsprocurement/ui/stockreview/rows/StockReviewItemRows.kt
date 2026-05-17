package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot

object StockReviewItemRows {
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
            StockReviewItemDetailHeadingRows.itemLabel(record, expanded),
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
            StockReviewItemDetailHeadingRows.itemLabel(record, expanded),
            StockReviewTradeRowCells.reviewCells(record, trade, tradeContext, layout),
            StockReviewAction.toggleItem(record.itemKey),
        )
        if (!expanded) {
            return
        }
        StockReviewItemInfoRows.add(rows, record, state, layout)
        if (trade.isBuy()) {
            StockReviewSourceAllocationRows.add(rows, tradeContext.sellerAllocations(trade), layout)
        }
    }

    @JvmStatic
    fun addWorstCaseRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        layout: StockReviewRowLayout,
    ) {
        rows.add(
            itemRow(
                StockReviewCellGroup.DEBUG_WORST_CASE_LABEL,
                StockReviewTradeRowCells.worstCaseCells(layout),
                StockReviewActionRef.debugMode(StockReviewAction.debugNoop()),
                "Worst-case row-width test sample. It does not affect trades.",
                null,
                layout,
            ),
        )
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
            itemRow(
                label,
                cells,
                StockReviewActionRef.rowExpansion(action),
                itemTooltip,
                StockReviewItemTooltip.forRecord(record, itemTooltip),
                layout,
                StockReviewRowIcon.item(record),
            ),
        )
    }

    private fun itemRow(
        label: String,
        cells: List<WimGuiRowCell<StockReviewAction>>,
        actionRef: StockReviewActionRef,
        tooltip: String?,
        tooltipCreator: com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator?,
        layout: StockReviewRowLayout,
        icon: StockReviewRowIcon? = null,
    ): WimGuiListRow<StockReviewAction> = StockReviewListRow.fromSpec(
        StockReviewRowSpec.builder(label)
            .fillColor(StockReviewStyle.ROW_BACKGROUND)
            .buttonFillColor(StockReviewStyle.CELL_BACKGROUND)
            .borderColor(StockReviewStyle.ROW_BORDER)
            .indent(layout.itemIndent)
            .action(actionRef)
            .cells(cells)
            .tooltip(tooltip)
            .tooltipCreator(tooltipCreator)
            .icon(icon)
            .build(),
    )
}
