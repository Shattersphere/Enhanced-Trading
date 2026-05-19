package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.stock.item.WeaponStockRecord

object StockReviewItemInfoRows {
    @JvmStatic
    fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, record: WeaponStockRecord, state: StockReviewState?) {
        add(rows, record, state, StockReviewRowLayout.trade())
    }

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState?,
        layout: StockReviewRowLayout,
    ) {
        val basicRows = fields(record, layout, StockReviewItemInfoFields.basic(record))
        val basicExpanded = isInfoSectionExpanded(state, StockReviewItemDetailHeadingRows.basicInfoSectionKey(record))
        rows.add(StockReviewItemDetailHeadingRows.basicInfo(record, basicExpanded, layout))
        if (basicExpanded) {
            rows.addAll(basicRows)
        }

        val advancedRows = fields(record, layout, StockReviewItemInfoFields.advanced(record))
        if (advancedRows.isEmpty()) {
            return
        }
        val advancedExpanded = isInfoSectionExpanded(state, StockReviewItemDetailHeadingRows.advancedInfoSectionKey(record))
        rows.add(StockReviewItemDetailHeadingRows.advancedInfo(record, advancedExpanded, layout))
        if (advancedExpanded) {
            rows.addAll(advancedRows)
        }
    }

    private fun isInfoSectionExpanded(state: StockReviewState?, sectionKey: String): Boolean =
        state == null || !state.isItemExpanded(sectionKey)

    private fun fields(
        record: WeaponStockRecord,
        layout: StockReviewRowLayout,
        fields: List<StockReviewItemInfoField>,
    ): List<WimGuiListRow<StockReviewAction>> =
        fields.mapNotNull { it.row(record, layout) }
}
