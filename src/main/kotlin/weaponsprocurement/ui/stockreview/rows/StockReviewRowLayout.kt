package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle

enum class StockReviewRowMode {
    TRADE,
    REVIEW,
}

class StockReviewRowLayout private constructor(
    @JvmField val mode: StockReviewRowMode,
    @JvmField val listWidth: Float,
    @JvmField val stockCellWidth: Float,
    @JvmField val priceCellWidth: Float,
    @JvmField val planCellWidth: Float,
    @JvmField val tradeControlBlockWidth: Float,
    @JvmField val hasPriceCell: Boolean,
    @JvmField val hasTradeControls: Boolean,
    @JvmField val itemIndent: Float,
    @JvmField val infoIndent: Float,
    @JvmField val dataIndent: Float,
    @JvmField val iconSlotWidth: Float,
    @JvmField val textLeftPad: Float,
) {
    @JvmField val cellGap: Float = StockReviewStyle.BUTTON_GAP
    @JvmField val rightBlockWidth: Float = computeRightBlockWidth()
    @JvmField val detailRightReserveWidth: Float = rightBlockWidth + textLeftPad

    private fun computeRightBlockWidth(): Float {
        var width = stockCellWidth
        var cellCount = 1
        if (hasPriceCell) {
            width += priceCellWidth
            cellCount++
        }
        width += planCellWidth
        cellCount++
        if (hasTradeControls) {
            width += tradeControlBlockWidth
            cellCount++
        }
        return width + (cellCount - 1) * cellGap
    }

    companion object {
        @JvmStatic
        fun trade(): StockReviewRowLayout = StockReviewRowLayout(
            StockReviewRowMode.TRADE,
            StockReviewStyle.LIST_WIDTH,
            StockReviewStyle.STOCK_CELL_WIDTH,
            StockReviewStyle.PRICE_CELL_WIDTH,
            StockReviewStyle.PLAN_CELL_WIDTH,
            StockReviewStyle.TRADE_CONTROL_BLOCK_WIDTH,
            true,
            true,
            StockReviewStyle.WEAPON_INDENT,
            StockReviewStyle.DETAIL_INDENT,
            StockReviewStyle.DATA_INDENT,
            StockReviewStyle.ROW_ICON_INDENT,
            StockReviewStyle.TEXT_LEFT_PAD,
        )

        @JvmStatic
        fun review(): StockReviewRowLayout = StockReviewRowLayout(
            StockReviewRowMode.REVIEW,
            StockReviewStyle.REVIEW_LIST_WIDTH,
            StockReviewStyle.STOCK_CELL_WIDTH,
            StockReviewStyle.PRICE_CELL_WIDTH,
            StockReviewStyle.PLAN_CELL_WIDTH,
            0f,
            false,
            false,
            StockReviewStyle.WEAPON_INDENT,
            StockReviewStyle.DETAIL_INDENT,
            StockReviewStyle.DATA_INDENT,
            StockReviewStyle.ROW_ICON_INDENT,
            StockReviewStyle.TEXT_LEFT_PAD,
        )

        @JvmStatic
        fun forReviewMode(reviewMode: Boolean): StockReviewRowLayout =
            if (reviewMode) review() else trade()
    }
}
