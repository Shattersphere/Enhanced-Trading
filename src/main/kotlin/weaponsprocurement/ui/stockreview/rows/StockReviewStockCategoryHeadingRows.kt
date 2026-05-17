package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import java.awt.Color

object StockReviewStockCategoryHeadingRows {
    @JvmStatic
    fun stockCategory(
        label: String,
        itemType: StockItemType,
        category: StockCategory,
        color: Color,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(
            StockReviewRowSpecs.category(
                WimGuiToggleHeading.label(label, expanded),
                color,
                StockReviewAction.toggle(itemType, category),
                topGap,
                StockReviewTooltips.category(category),
                StockReviewStyle.WEAPON_INDENT,
            ),
        )
}
