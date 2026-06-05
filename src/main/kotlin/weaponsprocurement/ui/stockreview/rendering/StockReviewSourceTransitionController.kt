package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketIntent
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketRebalancer
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrades
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeWarnings

/**
 * Owns top-row mode/source transitions. Source changes are guarded because item pending
 * trades are source-specific and cannot be safely reinterpreted across Local/Sector/Fixer.
 */
class StockReviewSourceTransitionController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val pendingShipTrades: StockReviewPendingShipTrades,
    private val localMarketIntent: StockReviewLocalMarketIntent,
    private val host: Host,
) {
    interface Host {
        fun snapshot(): WeaponStockSnapshot?
        fun updateTradeWarning(explicitWarning: String?)
        fun rebuildSnapshot()
        fun requestContentRebuild()
        fun postMessage(message: String?)
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.SOURCE_TRANSITIONS) { action ->
            when (action.getType()) {
                Type.CYCLE_SORT_MODE -> cycleSortMode()
                Type.TOGGLE_TRADE_KIND -> toggleTradeKind()
                Type.CYCLE_SOURCE_MODE -> cycleSourceMode()
                Type.TOGGLE_BLACK_MARKET -> toggleBlackMarket()
                Type.RESET_ALL_TRADES -> resetAllTrades()
                else -> return@group
            }
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)

    fun cycleSortMode() {
        state.cycleSortMode()
        rebuildAndRender()
    }

    fun toggleTradeKind() {
        state.cycleTradeKind()
        clearSourceWarningAndReviewMode()
        state.setListScrollOffset(0)
        rebuildAndRender()
    }

    fun cycleSourceMode() {
        if (state.isShipTrading()) {
            host.postMessage("Ship trading is local-only for now.")
            host.requestContentRebuild()
            return
        }
        if (!pendingTrades.isEmpty()) {
            host.postMessage("Reset or confirm queued trades before changing source mode.")
            host.requestContentRebuild()
            return
        }
        state.cycleSourceMode()
        pendingTrades.clear()
        localMarketIntent.clear()
        clearSourceWarningAndReviewMode()
        state.setListScrollOffset(0)
        rebuildAndRender()
    }

    fun toggleBlackMarket() {
        if (state.isShipTrading()) {
            state.toggleBlackMarket()
            pendingShipTrades.clear()
            clearSourceWarningAndReviewMode()
            state.setListScrollOffset(0)
            host.rebuildSnapshot()
            host.requestContentRebuild()
            return
        }
        if (!state.getSourceMode().supportsBlackMarketToggle()) {
            host.requestContentRebuild()
            return
        }
        if (state.getSourceMode().isRemote()) {
            state.toggleBlackMarket()
            pendingTrades.clear()
            localMarketIntent.clear()
            clearSourceWarningAndReviewMode()
            state.setListScrollOffset(0)
            rebuildAndRender()
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
        if (state.isShipTrading()) {
            pendingShipTrades.clear()
        } else {
            pendingTrades.clear()
            localMarketIntent.clear()
        }
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
