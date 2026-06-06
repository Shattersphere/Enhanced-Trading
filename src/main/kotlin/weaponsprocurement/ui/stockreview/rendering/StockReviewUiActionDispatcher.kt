package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup

class StockReviewUiActionDispatcher(
    private val rowExpansion: StockReviewRowExpansionController,
    private val sourceTransitions: StockReviewSourceTransitionController,
    private val scroll: StockReviewScrollController,
    private val filters: StockReviewFilterActionController,
    private val debugMode: StockReviewDebugModeController,
    private val navigation: StockReviewNavigationController,
) {
    fun handle(action: StockReviewAction?): Boolean {
        if (action == null) return false
        return when (action.getGroup()) {
            StockReviewActionGroup.ROW_EXPANSION -> rowExpansion.handle(action)
            StockReviewActionGroup.SOURCE_TRANSITIONS -> sourceTransitions.handle(action)
            StockReviewActionGroup.SCROLL -> scroll.handle(action)
            StockReviewActionGroup.FILTERS -> filters.handle(action)
            StockReviewActionGroup.DEBUG_MODE -> debugMode.handle(action)
            StockReviewActionGroup.NAVIGATION -> navigation.handle(action)
            else -> false
        }
    }
}
