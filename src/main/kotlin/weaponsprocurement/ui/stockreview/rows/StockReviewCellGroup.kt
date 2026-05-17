package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionCells
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewQuoteBook
import weaponsprocurement.trade.quote.CreditFormat
import java.awt.Color
import kotlin.math.abs

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

    @JvmStatic
    fun infoCell(
        label: String?,
        width: Float,
        fill: Color? = StockReviewStyle.CELL_BACKGROUND,
        tooltip: String? = null,
        alignment: Alignment? = Alignment.LMID,
    ): WimGuiRowCell<StockReviewAction> =
        WimGuiRowCell.info(label, width, fill, StockReviewStyle.TEXT, alignment, tooltip)

    @JvmStatic
    fun storage(ownedCount: Int, planQuantity: Int, layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
        storage(ownedCount, planQuantity, stockWidth(layout))

    @JvmStatic
    fun storage(ownedCount: Int, planQuantity: Int, width: Float): WimGuiRowCell<StockReviewAction> =
        infoCell(storageLabel(ownedCount, planQuantity), width, StockReviewStyle.CELL_BACKGROUND, StockReviewTooltips.STORAGE)

    @JvmStatic
    fun unitPrice(unitPrice: Int, layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
        infoCell(unitPriceLabel(unitPrice), priceWidth(layout), StockReviewStyle.CELL_BACKGROUND, StockReviewTooltips.PRICE)

    @JvmStatic
    fun plan(planQuantity: Int, transactionCost: Long, layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
        infoCell(planLabel(planQuantity, transactionCost), planWidth(layout), planFill(planQuantity), StockReviewTooltips.PLAN)

    @JvmStatic
    fun debugStorage(layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
        infoCell(DEBUG_STORAGE_LABEL, stockWidth(layout), StockReviewStyle.CELL_BACKGROUND, StockReviewTooltips.STORAGE)

    @JvmStatic
    fun debugPrice(layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
        infoCell(debugPriceLabel(), priceWidth(layout), StockReviewStyle.CELL_BACKGROUND, StockReviewTooltips.PRICE)

    @JvmStatic
    fun debugPlan(layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
        infoCell(debugPlanLabel(), planWidth(layout), StockReviewStyle.PLAN_NEGATIVE, StockReviewTooltips.PLAN)

    @JvmStatic
    fun stepButton(
        sign: String,
        quantity: Int,
        fill: Color,
        action: StockReviewAction,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> {
        val enabled = quantity > 1
        val label = if (enabled) sign + quantity else sign + "10"
        return actionCell(label, stepWidth(), fill, action, enabled, tooltip)
    }

    @JvmStatic
    fun adjustmentButton(
        label: String,
        fill: Color,
        action: StockReviewAction,
        enabled: Boolean,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> =
        actionCell(label, stepWidth(), fill, action, enabled, tooltip)

    @JvmStatic
    fun sufficientButton(
        fill: Color,
        action: StockReviewAction,
        enabled: Boolean,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> =
        actionCell("Sufficient", sufficientWidth(), fill, action, enabled, tooltip)

    @JvmStatic
    fun resetButton(
        action: StockReviewAction,
        enabled: Boolean,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> =
        StockReviewActionCells.standard(
            StockReviewActionGroup.PLAN_RESET,
            "Reset",
            resetWidth(),
            StockReviewStyle.ACTION_BACKGROUND,
            action,
            enabled,
            tooltip,
        )

    @JvmStatic
    fun debugButton(label: String, width: Float, fill: Color, tooltip: String): WimGuiRowCell<StockReviewAction> =
        StockReviewActionCells.standard(
            StockReviewActionGroup.DEBUG_MODE,
            label,
            width,
            fill,
            StockReviewAction.debugNoop(),
            true,
            tooltip,
        )

    @JvmStatic
    fun debugControlCells(): List<WimGuiRowCell<StockReviewAction>> =
        WimGuiRowCell.of(
            debugButton("-10", stepWidth(), StockReviewStyle.SELL_BUTTON, StockReviewTooltips.decreasePlan(10)),
            debugButton("-1", stepWidth(), StockReviewStyle.SELL_BUTTON, StockReviewTooltips.decreasePlan(1)),
            debugButton("+1", stepWidth(), StockReviewStyle.BUY_BUTTON, StockReviewTooltips.increasePlan(1)),
            debugButton("+10", stepWidth(), StockReviewStyle.BUY_BUTTON, StockReviewTooltips.increasePlan(10)),
            debugButton("Sufficient", sufficientWidth(), StockReviewStyle.SELL_BUTTON, "Adjust the queued trade quantity so that your stock of this item just meets the sufficiency threshold (99)."),
            debugButton("Reset", resetWidth(), StockReviewStyle.ACTION_BACKGROUND, StockReviewTooltips.resetPlan()),
        )

    @JvmStatic
    fun storageLabel(ownedCount: Int, planQuantity: Int): String {
        if (planQuantity == 0) {
            return "Storage: ${cappedCount(ownedCount)}"
        }
        return "Storage: ${cappedCount(ownedCount)} [${signedCappedCount(planQuantity)}]"
    }

    @JvmStatic
    fun unitPriceLabel(unitPrice: Int): String {
        if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return "Price: ?"
        }
        return "Price: ${cappedCredits(unitPrice.toLong(), MAX_UNIT_PRICE)}"
    }

    @JvmStatic
    fun planLabel(planQuantity: Int, transactionCost: Long): String {
        val quantity = cappedCount(abs(planQuantity))
        val total = cappedCredits(transactionCost, MAX_TRANSACTION_CREDITS)
        return if (planQuantity > 0) {
            "Buying: $quantity [$total]"
        } else if (planQuantity < 0) {
            "Selling: $quantity [$total]"
        } else {
            "Buying: 0 [${StockReviewFormat.credits(0)}]"
        }
    }

    @JvmStatic
    fun cappedCount(value: Int): String =
        if (value >= MAX_DISPLAY_COUNT) "${MAX_DISPLAY_COUNT}+" else Math.max(0, value).toString()

    @JvmStatic
    fun signedCappedCount(value: Int): String {
        val sign = if (value > 0) "+" else if (value < 0) "-" else ""
        val absolute = abs(value)
        return sign + if (absolute >= MAX_DISPLAY_COUNT) "${MAX_DISPLAY_COUNT}+" else absolute.toString()
    }

    @JvmStatic
    fun cappedCredits(credits: Long, cap: Int): String {
        if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
            return "?"
        }
        val absolute = abs(credits)
        if (absolute >= cap) {
            return CreditFormat.grouped(cap.toLong()) + "+" + CreditFormat.CREDIT_SYMBOL
        }
        return StockReviewFormat.credits(absolute)
    }

    private fun planFill(planQuantity: Int): Color =
        if (planQuantity > 0) {
            StockReviewStyle.PLAN_POSITIVE
        } else if (planQuantity < 0) {
            StockReviewStyle.PLAN_NEGATIVE
        } else {
            StockReviewStyle.CELL_BACKGROUND
        }

    private fun actionCell(
        label: String,
        width: Float,
        fill: Color,
        action: StockReviewAction,
        enabled: Boolean,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> =
        StockReviewActionCells.standard(
            StockReviewActionGroup.PLAN_ADJUSTMENT,
            label,
            width,
            fill,
            action,
            enabled,
            tooltip,
        )
}
