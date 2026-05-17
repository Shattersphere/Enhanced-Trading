package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState

object StockReviewListEmptyRows {
    private const val REVIEW_EMPTY = "No trades are planned."

    @JvmStatic
    fun main(snapshot: WeaponStockSnapshot?, state: StockReviewState?): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(StockReviewRowSpecs.empty(mainMessage(snapshot, state)))

    @JvmStatic
    fun review(): WimGuiListRow<StockReviewAction> = StockReviewListRow.fromSpec(StockReviewRowSpecs.empty(REVIEW_EMPTY))

    private fun mainMessage(snapshot: WeaponStockSnapshot?, state: StockReviewState?): String {
        if (snapshot != null && snapshot.getTotalRecords() > 0 && state != null && state.getActiveFilterCount() > 0) {
            return "All rows are hidden by the active filters."
        }
        val sourceMode = snapshot?.getSourceMode() ?: StockSourceMode.LOCAL
        if (StockSourceMode.SECTOR == sourceMode) {
            return "No Sector Market weapon or wing stock is currently available."
        }
        if (StockSourceMode.FIXERS == sourceMode) {
            return "Fixer's Market has no eligible theoretical or observed stock, or all eligible stock is blacklisted."
        }
        return "No local weapon or wing stock is buyable here, and no player-cargo weapons or wings are available to sell."
    }
}
