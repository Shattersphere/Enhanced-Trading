package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import java.awt.Color
import java.util.Locale

object StockReviewGroupRows {
    @JvmStatic
    fun itemTypeHeading(itemType: StockItemType, count: Int, expanded: Boolean, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.filterHeading(
            WimGuiToggleHeading.countedLabel(itemType.sectionLabel, count, expanded),
            StockReviewAction.toggle(itemType),
            topGap,
            "Show or hide ${itemType.sectionLabel.lowercase(Locale.US)}.",
        )

    @JvmStatic
    fun stockCategoryHeading(
        label: String,
        itemType: StockItemType,
        category: StockCategory,
        color: Color,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.categoryIndented(
            WimGuiToggleHeading.label(label, expanded),
            color,
            StockReviewAction.toggle(itemType, category),
            topGap,
            StockReviewTooltips.category(category),
            StockReviewStyle.WEAPON_INDENT,
        )

    @JvmStatic
    fun reviewGroupHeading(label: String, count: Int, expanded: Boolean, color: Color, action: StockReviewAction, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.category(
            WimGuiToggleHeading.countedLabel(label, count, expanded),
            color,
            action,
            topGap,
            "Show or hide queued ${label.lowercase(Locale.US)} trades.",
        )
}
