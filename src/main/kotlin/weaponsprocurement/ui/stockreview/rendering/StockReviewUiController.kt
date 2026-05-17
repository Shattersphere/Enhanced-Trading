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
    private val rowExpansion = StockReviewRowExpansionController(state, host)
    private val sourceTransitions = StockReviewSourceTransitionController(state, modes, pendingTrades, localMarketIntent, host)
    private val scroll = StockReviewScrollController(state, host)
    private val filters = StockReviewFilterActionController(state, modes, host)
    private val debugMode = StockReviewDebugModeController(state, modes, host)
    private val navigation = StockReviewNavigationController(state, modes, pendingTrades, host)
    private val actionDispatcher = StockReviewUiActionDispatcher(rowExpansion, sourceTransitions, scroll, filters, debugMode, navigation)

    interface Host :
        StockReviewRowExpansionController.Host,
        StockReviewSourceTransitionController.Host,
        StockReviewScrollController.Host,
        StockReviewFilterActionController.Host,
        StockReviewDebugModeController.Host,
        StockReviewNavigationController.Host {
    }

    fun handleCloseRequested() {
        navigation.closeRequested()
    }

    fun handle(action: StockReviewAction?): Boolean = actionDispatcher.handle(action)
}
