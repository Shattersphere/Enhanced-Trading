package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.ui.stockreview.state.StockReviewState

object StockReviewWorstCaseItemRows {
    private const val TOOLTIP = "Worst-case row-width test sample. It does not affect trades."

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        layout: StockReviewRowLayout,
        itemType: StockItemType,
        state: StockReviewState?,
    ) {
        val record = StockReviewDebugItemRecords.forItemType(itemType)
        val itemTooltip = StockReviewTooltips.itemDataToggle(record)
        val expanded = state?.isItemExpanded(record.itemKey) ?: false
        rows.add(
            StockReviewItemRowFrame.build(
                StockReviewItemDetailHeadingRows.itemLabel(record, expanded),
                StockReviewTradeRowCells.worstCaseCells(layout),
                StockReviewActionRef.rowExpansion(StockReviewAction.toggleItem(record.itemKey)),
                TOOLTIP,
                StockReviewItemTooltip.forRecord(record, itemTooltip),
                layout,
                StockReviewRowIcon.item(record),
            ),
        )
        if (expanded) {
            StockReviewItemInfoRows.add(rows, record, state, layout)
        }
    }
}
