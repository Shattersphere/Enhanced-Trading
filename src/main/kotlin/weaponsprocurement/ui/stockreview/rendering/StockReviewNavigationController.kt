package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrades
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup

class StockReviewNavigationController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val pendingShipTrades: StockReviewPendingShipTrades,
    private val host: Host,
) {
    interface Host {
        fun requestContentRebuild()
        fun reopen(review: Boolean)
        fun requestClose()
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.NAVIGATION) { action ->
            when (action.getType()) {
                Type.REVIEW_PURCHASE -> openReviewIfNeeded()
                Type.GO_BACK -> goBack()
                else -> return@group
            }
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)

    fun closeRequested() {
        if (leaveTransientMode()) {
            return
        }
        if (modes.isReviewMode()) {
            state.setListScrollOffset(0)
            host.reopen(false)
            return
        }
        host.requestClose()
    }

    private fun openReviewIfNeeded() {
        if (state.isShipTrading() && pendingShipTrades.isEmpty()) {
            return
        }
        if (!state.isShipTrading() && pendingTrades.isEmpty()) {
            return
        }
        state.setListScrollOffset(0)
        if (!state.isShipTrading()) {
            state.setExpanded(StockReviewTradeGroup.BUYING, true)
            state.setExpanded(StockReviewTradeGroup.SELLING, true)
        }
        host.reopen(true)
    }

    private fun goBack() {
        if (leaveTransientMode()) {
            return
        }
        state.setListScrollOffset(0)
        host.reopen(false)
    }

    private fun leaveTransientMode(): Boolean {
        if (!modes.leaveTransientMode(state)) {
            return false
        }
        host.requestContentRebuild()
        return true
    }
}
