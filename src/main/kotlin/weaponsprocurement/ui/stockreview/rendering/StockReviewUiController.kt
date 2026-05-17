package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketIntent
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.stock.item.WeaponStockSnapshot

class StockReviewUiController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val localMarketIntent: StockReviewLocalMarketIntent,
    private val host: Host,
) {
    private val actionDispatcher = StockReviewUiActionDispatcher(state, modes, pendingTrades, localMarketIntent, host)

    interface Host {
        fun currentMaxScrollOffset(): Int
        fun snapshot(): WeaponStockSnapshot?
        fun updateTradeWarning(explicitWarning: String?)
        fun rebuildSnapshot()
        fun requestContentRebuild()
        fun reopen(review: Boolean)
        fun requestClose()
    }

    fun handleCloseRequested() {
        if (modes.leaveTransientMode(state)) {
            host.requestContentRebuild()
            return
        }
        if (modes.isReviewMode()) {
            state.setListScrollOffset(0)
            host.reopen(false)
            return
        }
        host.requestClose()
    }

    fun handle(action: StockReviewAction?): Boolean = actionDispatcher.handle(action)
}
