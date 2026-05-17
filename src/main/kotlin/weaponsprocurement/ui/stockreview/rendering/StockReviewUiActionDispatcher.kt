package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup

class StockReviewUiActionDispatcher(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val sourceTransitions: StockReviewSourceTransitionController,
    private val host: StockReviewUiController.Host,
) {
    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.many(
            "row expansion",
            Type.TOGGLE_CATEGORY,
            Type.TOGGLE_ITEM_TYPE,
            Type.TOGGLE_TRADE_GROUP,
            Type.TOGGLE_ITEM,
        ) { action ->
            handleRowExpansion(action)
        },
        StockReviewActionHandlerGroup.many(
            "source and snapshot transitions",
            Type.CYCLE_SORT_MODE,
            Type.CYCLE_SOURCE_MODE,
            Type.TOGGLE_BLACK_MARKET,
            Type.RESET_ALL_TRADES,
        ) { action ->
            sourceTransitions.handle(action)
        },
        StockReviewActionHandlerGroup.one("list scroll", Type.SCROLL_LIST) { action ->
            state.adjustListScrollOffset(action.getQuantity(), host.currentMaxScrollOffset())
            host.requestContentRebuild()
        },
        StockReviewActionHandlerGroup.many(
            "filter controls",
            Type.OPEN_FILTERS,
            Type.TOGGLE_FILTER_GROUP,
            Type.TOGGLE_FILTER,
            Type.RESET_FILTERS,
        ) { action ->
            handleFilters(action)
        },
        StockReviewActionHandlerGroup.many(
            "debug mode controls",
            Type.OPEN_COLOR_DEBUG,
            Type.OPEN_SHIP_CATALOG_DEBUG,
            Type.DEBUG_CYCLE_TARGET,
            Type.DEBUG_TOGGLE_PERSISTENCE,
            Type.DEBUG_ADJUST_RED,
            Type.DEBUG_ADJUST_GREEN,
            Type.DEBUG_ADJUST_BLUE,
            Type.DEBUG_APPLY,
            Type.DEBUG_CONFIRM,
            Type.DEBUG_RESTORE,
            Type.DEBUG_NOOP,
        ) { action ->
            handleDebugMode(action)
        },
        StockReviewActionHandlerGroup.one("review screen", Type.REVIEW_PURCHASE) {
            if (!pendingTrades.isEmpty()) {
                state.setListScrollOffset(0)
                state.setExpanded(StockReviewTradeGroup.BUYING, true)
                state.setExpanded(StockReviewTradeGroup.SELLING, true)
                host.reopen(true)
            }
        },
        StockReviewActionHandlerGroup.one("back navigation", Type.GO_BACK) {
            handleGoBack()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)

    private fun handleRowExpansion(action: StockReviewAction) {
        when (action.getType()) {
            Type.TOGGLE_CATEGORY -> state.toggle(action.getItemType(), action.getCategory())
            Type.TOGGLE_ITEM_TYPE -> state.toggle(action.getItemType())
            Type.TOGGLE_TRADE_GROUP -> state.toggle(action.getTradeGroup())
            Type.TOGGLE_ITEM -> state.toggleItem(action.getItemKey())
            else -> return
        }
        host.requestContentRebuild()
    }

    private fun handleFilters(action: StockReviewAction) {
        when (action.getType()) {
            Type.OPEN_FILTERS -> modes.enterFilters(state)
            Type.TOGGLE_FILTER_GROUP -> state.toggle(action.getFilterGroup())
            Type.TOGGLE_FILTER -> state.toggleFilter(action.getFilter())
            Type.RESET_FILTERS -> state.clearFilters()
            else -> return
        }
        host.requestContentRebuild()
    }

    private fun handleDebugMode(action: StockReviewAction) {
        when (action.getType()) {
            Type.OPEN_COLOR_DEBUG -> modes.enterColorDebug(state)
            Type.OPEN_SHIP_CATALOG_DEBUG -> modes.enterShipCatalogDebug(state)
            Type.DEBUG_CYCLE_TARGET -> modes.cycleColorDebugTarget(action.getQuantity())
            Type.DEBUG_TOGGLE_PERSISTENCE -> modes.toggleColorDebugPersistence()
            Type.DEBUG_ADJUST_RED -> modes.adjustColorDebugDraft(action.getQuantity(), 0, 0)
            Type.DEBUG_ADJUST_GREEN -> modes.adjustColorDebugDraft(0, action.getQuantity(), 0)
            Type.DEBUG_ADJUST_BLUE -> modes.adjustColorDebugDraft(0, 0, action.getQuantity())
            Type.DEBUG_APPLY -> modes.applyColorDebugDraft()
            Type.DEBUG_CONFIRM -> {
                modes.applyColorDebugDraft()
                modes.leaveColorDebug(state)
            }
            Type.DEBUG_RESTORE -> modes.restoreColorDebugDraft()
            Type.DEBUG_NOOP -> Unit
            else -> return
        }
        host.requestContentRebuild()
    }

    private fun handleGoBack() {
        if (modes.leaveTransientMode(state)) {
            host.requestContentRebuild()
            return
        }
        state.setListScrollOffset(0)
        host.reopen(false)
    }

}
