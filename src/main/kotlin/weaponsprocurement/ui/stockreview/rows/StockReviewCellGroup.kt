package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.trade.quote.CreditFormat

object StockReviewCellGroup {
    const val MAX_DISPLAY_COUNT = 99
    const val MAX_UNIT_PRICE = 99999
    const val MAX_TRANSACTION_CREDITS = 999999
    const val DEBUG_STORAGE_LABEL = "Storage: 99+ [-99+]"
    const val DEBUG_WORST_CASE_LABEL = "Debug Worst-Case Suzuki-Clapteryon Thermal Prokector [Observed/Very Rare] (+)"

    @JvmStatic
    fun stockWidth(layout: StockReviewRowLayout): Float = layout.stockCellWidth

    @JvmStatic
    fun priceWidth(layout: StockReviewRowLayout): Float = layout.priceCellWidth

    @JvmStatic
    fun planWidth(layout: StockReviewRowLayout): Float = layout.planCellWidth

    @JvmStatic
    fun hasPrice(layout: StockReviewRowLayout): Boolean = layout.hasPriceCell

    @JvmStatic
    fun hasControls(layout: StockReviewRowLayout): Boolean = layout.hasTradeControls

    @JvmStatic
    fun debugPriceLabel(): String = "Price: ${CreditFormat.grouped(MAX_UNIT_PRICE.toLong())}+${CreditFormat.CREDIT_SYMBOL}"

    @JvmStatic
    fun debugPlanLabel(): String = "Selling: 99+ [${CreditFormat.grouped(MAX_TRANSACTION_CREDITS.toLong())}+${CreditFormat.CREDIT_SYMBOL}]"

    @JvmStatic
    fun stepWidth(): Float = StockReviewStyle.TRADE_STEP_BUTTON_WIDTH

    @JvmStatic
    fun sufficientWidth(): Float = StockReviewStyle.SUFFICIENT_BUTTON_WIDTH

    @JvmStatic
    fun resetWidth(): Float = StockReviewStyle.RESET_BUTTON_WIDTH
}
