package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewState

class StockReviewDebugModeController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val host: Host,
) {
    interface Host {
        fun requestContentRebuild()
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.DEBUG_MODE) { action ->
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
                else -> return@group
            }
            host.requestContentRebuild()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
