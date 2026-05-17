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
        val basicExpanded = isInfoSectionExpanded(state, StockReviewHeadingRows.basicInfoSectionKey(record))
        rows.add(StockReviewHeadingRows.basicInfo(record, basicExpanded, layout))
        if (basicExpanded) {
            addBasicInfo(rows, record, layout)
        }
        if (record.isWing()) {
            return
        }
        val advancedExpanded = isInfoSectionExpanded(state, StockReviewHeadingRows.advancedInfoSectionKey(record))
        rows.add(StockReviewHeadingRows.advancedInfo(record, advancedExpanded, layout))
        if (advancedExpanded) {
            addAdvancedInfo(rows, record, layout)
        }
    }

    private fun addBasicInfo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        layout: StockReviewRowLayout,
    ) = addFields(rows, record, layout, StockReviewItemInfoFields.basic(record))

    private fun addAdvancedInfo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        layout: StockReviewRowLayout,
    ) = addFields(rows, record, layout, StockReviewItemInfoFields.advanced(record))

    private fun isInfoSectionExpanded(state: StockReviewState?, sectionKey: String): Boolean =
        state == null || !state.isItemExpanded(sectionKey)

    private fun addFields(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        layout: StockReviewRowLayout,
        fields: List<StockReviewItemInfoField>,
    ) {
        for (field in fields) {
            val row = field.row(record, layout)
            if (row != null) {
                rows.add(row)
            }
        }
    }
}
