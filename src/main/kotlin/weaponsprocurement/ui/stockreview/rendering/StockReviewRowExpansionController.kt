package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewState

class StockReviewRowExpansionController(
    private val state: StockReviewState,
    private val host: Host,
) {
    interface Host {
        fun requestContentRebuild()
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.ROW_EXPANSION) { action ->
            when (action.getType()) {
                Type.TOGGLE_CATEGORY -> state.toggle(action.getItemType(), action.getCategory())
                Type.TOGGLE_ITEM_TYPE -> state.toggle(action.getItemType())
                Type.TOGGLE_TRADE_GROUP -> state.toggle(action.getTradeGroup())
                Type.TOGGLE_ITEM -> state.toggleItem(action.getItemKey())
                else -> return@group
            }
            host.requestContentRebuild()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
