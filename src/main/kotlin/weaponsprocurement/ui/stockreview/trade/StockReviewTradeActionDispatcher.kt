package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup

class StockReviewTradeActionDispatcher(
    private val trades: StockReviewTradeController,
    private val execution: StockReviewExecutionController,
) {
    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.many(
            "item plan adjustment",
            Type.ADJUST_PLAN,
            Type.ADJUST_TO_SUFFICIENT,
        ) { action ->
            trades.adjustPendingTrade(action)
        },
        StockReviewActionHandlerGroup.one("item plan reset", Type.RESET_PLAN) { action ->
            trades.resetPlan(action.getItemKey())
        },
        StockReviewActionHandlerGroup.one("bulk sufficient purchase", Type.PURCHASE_ALL_UNTIL_SUFFICIENT) {
            trades.purchaseAllUntilSufficient()
        },
        StockReviewActionHandlerGroup.one("bulk sufficient sale", Type.SELL_ALL_UNTIL_SUFFICIENT) {
            trades.sellAllUntilSufficient()
        },
        StockReviewActionHandlerGroup.one("confirmed execution", Type.CONFIRM_PURCHASE) {
            execution.confirmPendingTrades()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
