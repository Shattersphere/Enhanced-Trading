package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.ships.StockReviewShipExecutionController
import weaponsprocurement.ui.stockreview.ships.StockReviewShipTradeController
import weaponsprocurement.ui.stockreview.state.StockReviewState

class StockReviewTradeActionDispatcher(
    private val state: StockReviewState,
    private val trades: StockReviewTradeController,
    private val shipTrades: StockReviewShipTradeController,
    private val execution: StockReviewExecutionController,
    private val shipExecution: StockReviewShipExecutionController,
) {
    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.PLAN_ADJUSTMENT) { action ->
            when (action.getType()) {
                StockReviewAction.Type.TOGGLE_SHIP_PLAN -> shipTrades.toggleShipPlan(action)
                else -> trades.adjustPendingTrade(action)
            }
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.PLAN_RESET) { action ->
            when (action.getType()) {
                StockReviewAction.Type.RESET_SHIP_PLAN -> shipTrades.resetShipPlan(action)
                else -> trades.resetPlan(action.getItemKey())
            }
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.BULK_SUFFICIENT_PURCHASE) {
            trades.purchaseAllUntilSufficient()
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.BULK_SUFFICIENT_SALE) {
            trades.sellAllUntilSufficient()
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.CONFIRMED_EXECUTION) {
            if (state.isShipTrading()) {
                shipExecution.confirmPendingShipTrades()
            } else {
                execution.confirmPendingTrades()
            }
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
