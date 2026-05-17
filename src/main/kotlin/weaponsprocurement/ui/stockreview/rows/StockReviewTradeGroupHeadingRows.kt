package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import java.awt.Color
import java.util.Locale

object StockReviewTradeGroupHeadingRows {
    @JvmStatic
    fun reviewGroup(
        tradeGroup: StockReviewTradeGroup,
        count: Int,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(
            StockReviewRowSpecs.category(
                WimGuiToggleHeading.countedLabel(tradeGroup.label, count, expanded),
                color(tradeGroup),
                StockReviewAction.toggle(tradeGroup),
                topGap,
                "Show or hide queued ${tradeGroup.label.lowercase(Locale.US)} trades.",
            ),
        )

    private fun color(tradeGroup: StockReviewTradeGroup): Color =
        if (StockReviewTradeGroup.BUYING == tradeGroup) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.CANCEL_BUTTON
}
