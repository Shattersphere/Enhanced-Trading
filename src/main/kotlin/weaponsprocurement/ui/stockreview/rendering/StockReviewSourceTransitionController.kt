package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketIntent
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketRebalancer
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeWarnings

class StockReviewSourceTransitionController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val localMarketIntent: StockReviewLocalMarketIntent,
    private val host: Host,
) {
    interface Host {
        fun snapshot(): WeaponStockSnapshot?
        fun updateTradeWarning(explicitWarning: String?)
        fun rebuildSnapshot()
        fun requestContentRebuild()
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.one("sort mode", Type.CYCLE_SORT_MODE) {
            cycleSortMode()
        },
        StockReviewActionHandlerGroup.one("source mode", Type.CYCLE_SOURCE_MODE) {
            cycleSourceMode()
        },
        StockReviewActionHandlerGroup.one("black market mode", Type.TOGGLE_BLACK_MARKET) {
            toggleBlackMarket()
        },
        StockReviewActionHandlerGroup.one("reset all trades", Type.RESET_ALL_TRADES) {
            resetAllTrades()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)

    fun cycleSortMode() {
        state.cycleSortMode()
        rebuildAndRender()
    }

    fun cycleSourceMode() {
        state.cycleSourceMode()
        pendingTrades.clear()
        localMarketIntent.clear()
        clearSourceWarningAndReviewMode()
        state.setListScrollOffset(0)
        rebuildAndRender()
    }

    fun toggleBlackMarket() {
        if (!state.getSourceMode().supportsBlackMarketToggle()) {
            host.requestContentRebuild()
            return
        }
        val previousSnapshot = host.snapshot()
        val previousTrades = ArrayList(pendingTrades.asList())
        localMarketIntent.seedFromTrades(previousTrades)
        state.toggleBlackMarket()
        clearSourceWarningAndReviewMode()
        host.rebuildSnapshot()
        pendingTrades.replaceWith(
            StockReviewLocalMarketRebalancer.rebalanceBlackMarketToggle(
                previousSnapshot,
                host.snapshot(),
                previousTrades,
                localMarketIntent,
                state.isIncludeBlackMarket(),
            ),
        )
        StockReviewTradeWarnings.clear(state)
        host.requestContentRebuild()
    }

    fun resetAllTrades() {
        pendingTrades.clear()
        localMarketIntent.clear()
        host.updateTradeWarning(null)
        host.requestContentRebuild()
    }

    private fun rebuildAndRender() {
        host.rebuildSnapshot()
        host.requestContentRebuild()
    }

    private fun clearSourceWarningAndReviewMode() {
        StockReviewTradeWarnings.clear(state)
        modes.setReviewMode(false)
    }
}
