package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.trade.StockReviewReviewItemGroup
import java.util.Locale

object StockReviewReviewItemGroupHeadingRows {
    @JvmStatic
    fun reviewItemGroup(
        group: StockReviewReviewItemGroup,
        count: Int,
        expanded: Boolean,
        topGap: Boolean,
        layout: StockReviewRowLayout,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(
            StockReviewRowSpecs.nestedHeading(
                WimGuiToggleHeading.countedLabel(group.label, count, expanded),
                layout.itemIndent,
                0f,
                StockReviewAction.toggle(group),
                topGap,
                "Show or hide queued ${group.label.lowercase(Locale.US)} trades in this section.",
            ),
        )
}
