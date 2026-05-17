package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction

object StockReviewDetailRows {
    @JvmStatic
    fun labelValue(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        indent: Float,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.labelTextIndented(label, value, layout, indent, topGap, tooltip)

    @JvmStatic
    fun itemInfo(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> =
        labelValue(label, value, layout, layout.dataIndent, false, tooltip)

    @JvmStatic
    fun sourceAllocation(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        labelValue(label, value, layout, layout.dataIndent, topGap, null)
}
