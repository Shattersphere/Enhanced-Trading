package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.state.StockReviewFilterGroup
import weaponsprocurement.ui.stockreview.state.StockReviewFilters
import weaponsprocurement.ui.stockreview.state.StockReviewState
import java.util.Collections

class StockReviewFilterGroupSection private constructor(
    @JvmField val group: StockReviewFilterGroup,
) {
    fun shouldShow(state: StockReviewState, activeFilters: Set<StockReviewFilter>): Boolean {
        if (group.weaponOnly) {
            return true
        }
        if (StockSourceMode.FIXERS == state.getSourceMode()) {
            return true
        }
        return StockReviewFilters.activeInGroup(activeFilters, group).isNotEmpty()
    }

    fun addTo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        state: StockReviewState,
        activeFilters: Set<StockReviewFilter>,
        topGap: Boolean,
    ) {
        val expanded = state.isExpanded(group)
        val activeInGroup = StockReviewFilters.activeInGroup(activeFilters, group)
        rows.add(StockReviewFilterHeadingRows.filterGroup(group, activeInGroup.size, expanded, topGap))
        if (!expanded) {
            return
        }
        for (filter in StockReviewFilter.values()) {
            if (group != filter.group || state.isFilterActive(filter)) {
                continue
            }
            rows.add(StockReviewFilterRows.available(filter))
        }
    }

    companion object {
        @JvmStatic
        fun forGroup(group: StockReviewFilterGroup): StockReviewFilterGroupSection =
            StockReviewFilterGroupSection(group)
    }
}

object StockReviewFilterGroupSections {
    @JvmField val ORDERED: List<StockReviewFilterGroupSection> = orderedSections()

    @JvmStatic
    fun addGroups(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        state: StockReviewState,
        activeFilters: Set<StockReviewFilter>,
    ) {
        for (i in ORDERED.indices) {
            val section = ORDERED[i]
            if (!section.shouldShow(state, activeFilters)) {
                continue
            }
            section.addTo(rows, state, activeFilters, activeFilters.isNotEmpty() || i > 0)
        }
    }

    private fun orderedSections(): List<StockReviewFilterGroupSection> {
        val result = ArrayList<StockReviewFilterGroupSection>()
        for (group in StockReviewFilterGroup.values()) {
            result.add(StockReviewFilterGroupSection.forGroup(group))
        }
        return Collections.unmodifiableList(result)
    }
}
