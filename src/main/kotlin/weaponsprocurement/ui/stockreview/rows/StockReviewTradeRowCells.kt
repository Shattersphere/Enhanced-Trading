package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockRecord

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
                StockReviewCellGroup.storage(record.storageCount, planQuantity, layout),
                StockReviewCellGroup.unitPrice(tradeContext.unitPriceForItem(record), layout),
                StockReviewCellGroup.plan(planQuantity, transactionCost, layout),
                StockReviewCellGroup.stepButton(
                    "-",
                    sellStepQuantity,
                    StockReviewStyle.SELL_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, -sellStepQuantity),
                    StockReviewTooltips.decreasePlan(sellStepQuantity),
                ),
                StockReviewCellGroup.adjustmentButton(
                    "-1",
                    StockReviewStyle.SELL_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, -1),
                    sellRemaining >= 1,
                    StockReviewTooltips.decreasePlan(1),
                ),
                StockReviewCellGroup.adjustmentButton(
                    "+1",
                    StockReviewStyle.BUY_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, 1),
                    tradeContext.positiveAdjustmentRemaining(record, 1) >= 1,
                    StockReviewTooltips.increasePlan(1),
                ),
                StockReviewCellGroup.stepButton(
                    "+",
                    buyStepQuantity,
                    StockReviewStyle.BUY_BUTTON,
                    StockReviewAction.adjustPlan(record.itemKey, buyStepQuantity),
                    StockReviewTooltips.increasePlan(buyStepQuantity),
                ),
                StockReviewCellGroup.sufficientButton(
                    if (sufficientDelta < 0) StockReviewStyle.SELL_BUTTON else StockReviewStyle.BUY_BUTTON,
                    StockReviewAction.adjustToSufficient(record.itemKey, sufficientDelta),
                    sufficientDelta != 0,
                    StockReviewTooltips.sufficient(record),
                ),
                StockReviewCellGroup.resetButton(
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
                StockReviewCellGroup.storage(record.storageCount, trade.quantity, layout),
                StockReviewCellGroup.plan(trade.quantity, tradeContext.transactionCostForLine(trade.itemKey, trade.submarketId), layout),
            )

        @JvmStatic
        fun worstCaseCells(layout: StockReviewRowLayout): List<WimGuiRowCell<StockReviewAction>> {
            val cells = ArrayList<WimGuiRowCell<StockReviewAction>>()
            cells.add(StockReviewCellGroup.debugStorage(layout))
            if (StockReviewCellGroup.hasPrice(layout)) {
                cells.add(StockReviewCellGroup.debugPrice(layout))
            }
            cells.add(StockReviewCellGroup.debugPlan(layout))
            if (StockReviewCellGroup.hasControls(layout)) {
                cells.addAll(StockReviewCellGroup.debugControlCells())
            }
            return cells
        }
    }
}
