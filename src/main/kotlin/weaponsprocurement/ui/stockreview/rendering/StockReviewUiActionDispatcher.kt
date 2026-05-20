package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup

class StockReviewUiActionDispatcher(
    private val rowExpansion: StockReviewRowExpansionController,
    private val sourceTransitions: StockReviewSourceTransitionController,
    private val scroll: StockReviewScrollController,
    private val filters: StockReviewFilterActionController,
    private val debugMode: StockReviewDebugModeController,
    private val navigation: StockReviewNavigationController,
    private val autoRules: StockReviewAutoRulesActionController,
) {
    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.ROW_EXPANSION) { action ->
            rowExpansion.handle(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.SOURCE_TRANSITIONS) { action ->
            sourceTransitions.handle(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.SCROLL) { action ->
            scroll.handle(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.FILTERS) { action ->
            filters.handle(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.DEBUG_MODE) { action ->
            debugMode.handle(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.NAVIGATION) { action ->
            navigation.handle(action)
        },
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.AUTO_RULES) { action ->
            autoRules.handle(action)
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
