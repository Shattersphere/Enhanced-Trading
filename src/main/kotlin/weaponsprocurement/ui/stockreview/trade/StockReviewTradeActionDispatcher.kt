package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup

class StockReviewTradeActionDispatcher(
    private val trades: StockReviewTradeController,
    private val execution: StockReviewExecutionController,
) {
    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.PLAN_ADJUSTMENT) { action ->
            trades.adjustPendingTrade(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.PLAN_RESET) { action ->
            trades.resetPlan(action.getItemKey())
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.BULK_SUFFICIENT_PURCHASE) {
            trades.purchaseAllUntilSufficient()
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.BULK_SUFFICIENT_SALE) {
            trades.sellAllUntilSufficient()
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.CONFIRMED_EXECUTION) {
            execution.confirmPendingTrades()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
