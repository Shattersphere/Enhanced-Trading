package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips

object StockReviewFilterRows {
    @JvmStatic
    fun addActive(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        activeFilters: Set<StockReviewFilter>,
    ) {
        if (activeFilters.isEmpty()) {
            return
        }
        for (filter in StockReviewFilter.values()) {
            if (activeFilters.contains(filter)) {
                rows.add(filter(filter, true))
            }
        }
    }

    @JvmStatic
    fun available(filter: StockReviewFilter): WimGuiListRow<StockReviewAction> = filter(filter, false)

    private fun filter(filter: StockReviewFilter, active: Boolean): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.filter(
            filter.label,
            active,
            StockReviewAction.toggleFilter(filter),
            false,
            StockReviewTooltips.filter(filter, active),
        )
}
