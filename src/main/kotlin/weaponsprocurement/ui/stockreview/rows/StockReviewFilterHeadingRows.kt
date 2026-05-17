package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewFilterGroup
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips

object StockReviewFilterHeadingRows {
    @JvmStatic
    fun filterGroup(
        group: StockReviewFilterGroup,
        activeCount: Int,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(
            StockReviewRowSpecs.filterControlHeading(
                WimGuiToggleHeading.countedLabel(group.label, activeCount, expanded),
                StockReviewAction.toggle(group),
                topGap,
                StockReviewTooltips.filterHeading(group),
            ),
        )
}
