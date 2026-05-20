package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewAutoRulesController
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketIntent
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrades
import weaponsprocurement.stock.item.WeaponStockSnapshot

class StockReviewUiController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val pendingShipTrades: StockReviewPendingShipTrades,
    private val localMarketIntent: StockReviewLocalMarketIntent,
    private val autoRulesController: StockReviewAutoRulesController,
    private val host: Host,
) {
    private val rowExpansion = StockReviewRowExpansionController(state, host)
    private val sourceTransitions = StockReviewSourceTransitionController(state, modes, pendingTrades, pendingShipTrades, localMarketIntent, host)
    private val scroll = StockReviewScrollController(state, host)
    private val filters = StockReviewFilterActionController(state, modes, host)
    private val debugMode = StockReviewDebugModeController(state, modes, host)
    private val navigation = StockReviewNavigationController(state, modes, pendingTrades, pendingShipTrades, host)
    private val autoRules = StockReviewAutoRulesActionController(autoRulesController, host)
    private val actionDispatcher = StockReviewUiActionDispatcher(rowExpansion, sourceTransitions, scroll, filters, debugMode, navigation, autoRules)

    interface Host :
        StockReviewRowExpansionController.Host,
        StockReviewSourceTransitionController.Host,
        StockReviewScrollController.Host,
        StockReviewFilterActionController.Host,
        StockReviewDebugModeController.Host,
        StockReviewNavigationController.Host,
        StockReviewAutoRulesActionController.Host {
    }

    fun handleCloseRequested() {
        navigation.closeRequested()
    }

    fun handle(action: StockReviewAction?): Boolean = actionDispatcher.handle(action)
}
