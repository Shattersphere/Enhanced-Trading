package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef

object StockReviewWorstCaseItemRows {
    private const val TOOLTIP = "Worst-case row-width test sample. It does not affect trades."

    @JvmStatic
    fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, layout: StockReviewRowLayout) {
        rows.add(
            StockReviewItemRowFrame.build(
                StockReviewCellGroup.DEBUG_WORST_CASE_LABEL,
                StockReviewTradeRowCells.worstCaseCells(layout),
                StockReviewActionRef.debugMode(StockReviewAction.debugNoop()),
                TOOLTIP,
                null,
                layout,
                null,
            ),
        )
    }
}
