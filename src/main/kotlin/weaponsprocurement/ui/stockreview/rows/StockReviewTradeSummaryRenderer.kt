package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import com.fs.starfarer.api.ui.CustomPanelAPI

class StockReviewTradeSummaryRenderer private constructor() {
    companion object {
        @JvmStatic
        fun render(root: CustomPanelAPI, tradeContext: StockReviewTradeContext, state: StockReviewState?, layout: StockReviewRowLayout) {
            val width = layout.listWidth
            var rowY = StockReviewStyle.summaryTop(StockReviewRowMode.REVIEW == layout.mode)
            for (field in StockReviewTradeSummaryFields.build(tradeContext, state)) {
                field.render(root, width, rowY)
                rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP
            }
        }
    }
}
