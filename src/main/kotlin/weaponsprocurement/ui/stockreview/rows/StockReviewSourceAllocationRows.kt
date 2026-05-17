package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.trade.StockReviewSellerAllocation
import java.util.ArrayList

object StockReviewSourceAllocationRows {
    private const val FALLBACK_SOURCE_LABEL = "Purchase Source"
    private const val UNAVAILABLE_VALUE = "Unavailable"

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        allocations: List<StockReviewSellerAllocation>,
        layout: StockReviewRowLayout,
    ) {
        rows.addAll(build(allocations, layout))
    }

    @JvmStatic
    fun build(
        allocations: List<StockReviewSellerAllocation>,
        layout: StockReviewRowLayout,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        if (allocations.isEmpty()) {
            rows.add(sourceAllocationRow(FALLBACK_SOURCE_LABEL, UNAVAILABLE_VALUE, layout, true))
            return rows
        }
        for (i in allocations.indices) {
            val allocation = allocations[i]
            rows.add(sourceAllocationRow(sourceLabel(allocation), allocationSummary(allocation), layout, i == 0))
        }
        return rows
    }

    private fun sourceAllocationRow(
        label: String?,
        value: String?,
        layout: StockReviewRowLayout,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewDetailRows.fromSpec(StockReviewDetailRowSpec.sourceAllocation(label, value, layout, topGap))

    private fun sourceLabel(allocation: StockReviewSellerAllocation): String =
        if (allocation.submarketName.isNullOrEmpty()) FALLBACK_SOURCE_LABEL else allocation.submarketName

    private fun allocationSummary(allocation: StockReviewSellerAllocation): String =
        allocation.quantity.toString() + " / " + StockReviewFormat.credits(allocation.cost)
}
