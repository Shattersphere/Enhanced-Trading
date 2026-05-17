package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewFilterGroupSections
import weaponsprocurement.ui.stockreview.rows.StockReviewFilterRows
import java.util.ArrayList

class StockReviewFilterListModel private constructor() {
    companion object {
        @JvmStatic
        fun build(state: StockReviewState): List<WimGuiListRow<StockReviewAction>> {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            val active = state.getActiveFilters()
            StockReviewFilterRows.addActive(rows, active)
            StockReviewFilterGroupSections.addGroups(rows, state, active)
            return rows
        }
    }
}
