package weaponsprocurement.ui.stockreview.ships

import weaponsprocurement.ui.stockreview.actions.StockReviewAction

class StockReviewShipTradeController(
    private val pendingTrades: StockReviewPendingShipTrades,
    private val host: Host,
) {
    interface Host {
        fun shipSnapshot(): StockReviewShipSnapshot
        fun requestContentRebuild()
        fun postMessage(message: String?)
    }

    fun toggleShipPlan(action: StockReviewAction) {
        val record = host.shipSnapshot().getRecord(action.getItemKey())
        if (record == null) {
            host.postMessage("That ship is no longer available.")
            host.requestContentRebuild()
            return
        }
        if (pendingTrades.toggle(record)) {
            host.requestContentRebuild()
        }
    }

    fun resetShipPlan(action: StockReviewAction) {
        if (pendingTrades.reset(action.getItemKey())) {
            host.requestContentRebuild()
        }
    }
}
