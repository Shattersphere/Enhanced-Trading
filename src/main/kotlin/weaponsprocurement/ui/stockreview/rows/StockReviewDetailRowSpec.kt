package weaponsprocurement.ui.stockreview.rows

class StockReviewDetailRowSpec private constructor(
    @JvmField val label: String?,
    @JvmField val value: String?,
    @JvmField val layout: StockReviewRowLayout,
    @JvmField val indent: Float,
    @JvmField val topGap: Boolean,
    @JvmField val tooltip: String?,
) {
    companion object {
        @JvmStatic
        fun labelValue(
            label: String?,
            value: String?,
            layout: StockReviewRowLayout,
            indent: Float,
            topGap: Boolean,
            tooltip: String?,
        ): StockReviewDetailRowSpec = StockReviewDetailRowSpec(label, value, layout, indent, topGap, tooltip)

        @JvmStatic
        fun itemInfo(
            label: String?,
            value: String?,
            layout: StockReviewRowLayout,
            tooltip: String?,
        ): StockReviewDetailRowSpec = labelValue(label, value, layout, layout.dataIndent, false, tooltip)

        @JvmStatic
        fun sourceAllocation(
            label: String?,
            value: String?,
            layout: StockReviewRowLayout,
            topGap: Boolean,
        ): StockReviewDetailRowSpec = labelValue(label, value, layout, layout.dataIndent, topGap, null)
    }
}
