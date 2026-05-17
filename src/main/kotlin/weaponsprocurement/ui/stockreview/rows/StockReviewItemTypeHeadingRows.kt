package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import java.util.Locale

object StockReviewItemTypeHeadingRows {
    @JvmStatic
    fun itemType(itemType: StockItemType, count: Int, expanded: Boolean, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.filterHeading(
            WimGuiToggleHeading.countedLabel(itemType.sectionLabel, count, expanded),
            StockReviewAction.toggle(itemType),
            topGap,
            "Show or hide ${itemType.sectionLabel.lowercase(Locale.US)}.",
        )
}
