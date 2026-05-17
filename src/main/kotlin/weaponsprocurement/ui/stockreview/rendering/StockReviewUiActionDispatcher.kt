package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup

class StockReviewUiActionDispatcher(
    private val rowExpansion: StockReviewRowExpansionController,
    private val sourceTransitions: StockReviewSourceTransitionController,
    private val scroll: StockReviewScrollController,
    private val filters: StockReviewFilterActionController,
    private val debugMode: StockReviewDebugModeController,
    private val navigation: StockReviewNavigationController,
) {
    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.many(
            "row expansion",
            Type.TOGGLE_CATEGORY,
            Type.TOGGLE_ITEM_TYPE,
            Type.TOGGLE_TRADE_GROUP,
            Type.TOGGLE_ITEM,
        ) { action ->
            rowExpansion.handle(action)
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
            scroll.handle(action)
        },
        StockReviewActionHandlerGroup.many(
            "filter controls",
            Type.OPEN_FILTERS,
            Type.TOGGLE_FILTER_GROUP,
            Type.TOGGLE_FILTER,
            Type.RESET_FILTERS,
        ) { action ->
            filters.handle(action)
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
            debugMode.handle(action)
        },
        StockReviewActionHandlerGroup.one("review screen", Type.REVIEW_PURCHASE) {
            navigation.handle(it)
        },
        StockReviewActionHandlerGroup.one("back navigation", Type.GO_BACK) {
            navigation.handle(it)
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
