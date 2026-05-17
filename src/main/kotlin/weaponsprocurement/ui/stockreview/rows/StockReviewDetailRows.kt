package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction

object StockReviewDetailRows {
    @JvmStatic
    fun fromSpec(spec: StockReviewDetailRowSpec): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.labelTextIndented(
            spec.label,
            spec.value,
            spec.layout,
            spec.indent,
            spec.topGap,
            spec.tooltip,
        )

    @JvmStatic
    fun labelValue(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        indent: Float,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> =
        fromSpec(StockReviewDetailRowSpec.labelValue(label, value, layout, indent, topGap, tooltip))

    @JvmStatic
    fun itemInfo(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> =
        fromSpec(StockReviewDetailRowSpec.itemInfo(label, value, layout, tooltip))

    @JvmStatic
    fun sourceAllocation(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        fromSpec(StockReviewDetailRowSpec.sourceAllocation(label, value, layout, topGap))
}
