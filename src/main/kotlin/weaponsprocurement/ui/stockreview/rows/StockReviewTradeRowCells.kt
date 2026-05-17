package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewQuoteBook
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockRecord
import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.trade.quote.CreditFormat
import java.awt.Color
import kotlin.math.abs

class StockReviewTradeRowCells private constructor() {
    companion object {
        @JvmStatic
        fun tradeCells(
            record: WeaponStockRecord,
            tradeContext: StockReviewTradeContext,
            layout: StockReviewRowLayout,
        ): List<WimGuiRowCell<StockReviewAction>> {
            val planQuantity = tradeContext.netQuantityForItem(record.itemKey)
            val sellRemaining = tradeContext.negativeAdjustmentRemaining(record, Int.MAX_VALUE)
            val transactionCost = tradeContext.transactionCostForItem(record.itemKey)
            val buyStepQuantity = tradeContext.positiveAdjustmentRemaining(record, 10)
            val sellStepQuantity = minOf(10, sellRemaining)
            val sufficientDelta = tradeContext.deltaToSufficient(record)
            return WimGuiRowCell.of(
                storage(record.storageCount, planQuantity, layout),
                unitPrice(tradeContext.unitPriceForItem(record), layout),
                plan(planQuantity, transactionCost, layout),
                step(
                    "-",
                    sellStepQuantity,
                    StockReviewStyle.SELL_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, -sellStepQuantity),
                    StockReviewTooltips.decreasePlan(sellStepQuantity),
                ),
                WimGuiRowCell.standardAction(
                    "-1",
                    StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                    StockReviewStyle.SELL_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, -1),
                    sellRemaining >= 1,
                    StockReviewTooltips.decreasePlan(1),
                ),
                WimGuiRowCell.standardAction(
                    "+1",
                    StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                    StockReviewStyle.BUY_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, 1),
                    tradeContext.positiveAdjustmentRemaining(record, 1) >= 1,
                    StockReviewTooltips.increasePlan(1),
                ),
                step(
                    "+",
                    buyStepQuantity,
                    StockReviewStyle.BUY_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, buyStepQuantity),
                    StockReviewTooltips.increasePlan(buyStepQuantity),
                ),
                WimGuiRowCell.standardAction(
                    "Sufficient",
                    StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                    if (sufficientDelta < 0) StockReviewStyle.SELL_BUTTON else StockReviewStyle.BUY_BUTTON,
                    StockReviewAction.adjustToSufficient(record.itemKey, sufficientDelta),
                    sufficientDelta != 0,
                    StockReviewTooltips.sufficient(record),
                ),
                WimGuiRowCell.standardAction(
                    "Reset",
                    StockReviewStyle.RESET_BUTTON_WIDTH,
                    StockReviewStyle.ACTION_BACKGROUND,
                    StockReviewAction.resetPlan(record.itemKey),
                    planQuantity != 0,
                    StockReviewTooltips.resetPlan(),
                ),
            )
        }

        @JvmStatic
        fun reviewCells(
            record: WeaponStockRecord,
            trade: StockReviewPendingTrade,
            tradeContext: StockReviewTradeContext,
            layout: StockReviewRowLayout,
        ): List<WimGuiRowCell<StockReviewAction>> =
            WimGuiRowCell.of(
                storage(record.storageCount, trade.quantity, layout),
                plan(trade.quantity, tradeContext.transactionCostForLine(trade.itemKey, trade.submarketId), layout),
            )

        @JvmStatic
        fun worstCaseCells(layout: StockReviewRowLayout): List<WimGuiRowCell<StockReviewAction>> {
            val cells = ArrayList<WimGuiRowCell<StockReviewAction>>()
            cells.add(WimGuiRowCell.info("Storage: 99+ [-99+]", layout.stockCellWidth, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.STORAGE))
            if (layout.hasPriceCell) {
                cells.add(WimGuiRowCell.info("Price: 99,999+${CreditFormat.CREDIT_SYMBOL}", layout.priceCellWidth, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE))
            }
            cells.add(WimGuiRowCell.info("Selling: 99+ [999,999+${CreditFormat.CREDIT_SYMBOL}]", layout.planCellWidth, StockReviewStyle.PLAN_NEGATIVE, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN))
            if (layout.hasTradeControls) {
                cells.add(WimGuiRowCell.standardAction("-10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.decreasePlan(10)))
                cells.add(WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.decreasePlan(1)))
                cells.add(WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.increasePlan(1)))
                cells.add(WimGuiRowCell.standardAction("+10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.increasePlan(10)))
                cells.add(WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true, "Adjust the queued trade quantity so that your stock of this item just meets the sufficiency threshold (99)."))
                cells.add(WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugNoop(), true, StockReviewTooltips.resetPlan()))
            }
            return cells
        }

        @JvmStatic
        fun storage(ownedCount: Int, planQuantity: Int, layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> =
            storage(ownedCount, planQuantity, layout.stockCellWidth)

        @JvmStatic
        fun storage(ownedCount: Int, planQuantity: Int, width: Float): WimGuiRowCell<StockReviewAction> =
            WimGuiRowCell.info(
                storageLabel(ownedCount, planQuantity),
                width,
                StockReviewStyle.CELL_BACKGROUND,
                StockReviewStyle.TEXT,
                Alignment.LMID,
                StockReviewTooltips.STORAGE,
            )

        @JvmStatic
        fun plan(planQuantity: Int, transactionCost: Long, layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> {
            val quantity = cappedCount(abs(planQuantity))
            val total = cappedCredits(transactionCost, 999999)
            val label = if (planQuantity > 0) {
                "Buying: $quantity [$total]"
            } else if (planQuantity < 0) {
                "Selling: $quantity [$total]"
            } else {
                "Buying: 0 [${StockReviewFormat.credits(0)}]"
            }
            val fill = if (planQuantity > 0) {
                StockReviewStyle.PLAN_POSITIVE
            } else if (planQuantity < 0) {
                StockReviewStyle.PLAN_NEGATIVE
            } else {
                StockReviewStyle.CELL_BACKGROUND
            }
            return WimGuiRowCell.info(label, layout.planCellWidth, fill, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN)
        }

        @JvmStatic
        fun unitPrice(unitPrice: Int, layout: StockReviewRowLayout): WimGuiRowCell<StockReviewAction> {
            if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
                return WimGuiRowCell.info("Price: ?", layout.priceCellWidth, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE)
            }
            return WimGuiRowCell.info("Price: ${cappedCredits(unitPrice.toLong(), 99999)}", layout.priceCellWidth, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE)
        }

        @JvmStatic
        fun step(sign: String, quantity: Int, fill: Color, action: StockReviewAction, tooltip: String): WimGuiRowCell<StockReviewAction> {
            val enabled = quantity > 1
            val label = if (enabled) sign + quantity else sign + "10"
            return WimGuiRowCell.standardAction(label, StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, fill, action, enabled, tooltip)
        }

        private fun storageLabel(ownedCount: Int, planQuantity: Int): String {
            if (planQuantity == 0) {
                return "Storage: ${cappedCount(ownedCount)}"
            }
            return "Storage: ${cappedCount(ownedCount)} [${signedCappedCount(planQuantity)}]"
        }

        private fun cappedCount(value: Int): String = if (value >= 99) "99+" else Math.max(0, value).toString()

        private fun signedCappedCount(value: Int): String {
            val sign = if (value > 0) "+" else if (value < 0) "-" else ""
            val absolute = abs(value)
            return sign + if (absolute >= 99) "99+" else absolute.toString()
        }

        private fun cappedCredits(credits: Long, cap: Int): String {
            if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
                return "?"
            }
            val absolute = abs(credits)
            if (absolute >= cap) {
                return CreditFormat.grouped(cap.toLong()) + "+" + CreditFormat.CREDIT_SYMBOL
            }
            return StockReviewFormat.credits(absolute)
        }
    }
}
