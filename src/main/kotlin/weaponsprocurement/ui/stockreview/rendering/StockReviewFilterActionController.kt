package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState

class StockReviewFilterActionController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val host: Host,
) {
    interface Host {
        fun requestContentRebuild()
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.FILTERS) { action ->
            when (action.getType()) {
                Type.OPEN_FILTERS -> modes.enterFilters(state)
                Type.TOGGLE_FILTER_GROUP -> state.toggle(action.getFilterGroup())
                Type.TOGGLE_FILTER -> state.toggleFilter(action.getFilter())
                Type.RESET_FILTERS -> state.clearFilters()
                else -> return@group
            }
            host.requestContentRebuild()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
