package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.state.StockReviewFilters
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import java.awt.Color
import java.util.Collections

/**
 * Item stock category sections for the main item-trade view. Rows respect active filters,
 * while heading buy/sell aggregates use the full category so hidden queued trades stay visible.
 */
class StockReviewStockCategorySection private constructor(
    @JvmField val category: StockCategory,
    private val fillColor: Color,
    private val topGap: Boolean,
    private val includesWorstCaseRow: Boolean,
) {
    fun addTo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
        itemType: StockItemType,
    ): Int {
        val allRecords = snapshot?.getRecords(itemType, category) ?: emptyList()
        val records = filteredRecords(allRecords, state.getActiveFilters(), state.getItemSearch())
        val expanded = state.isExpanded(itemType, category)
        return StockReviewListSection.add(
            rows,
            layout,
            StockReviewListSectionSpec(
                records,
                expanded,
                StockReviewStockCategoryHeadingRows.stockCategory(
                    categoryHeading(itemType, records, allRecords, tradeContext),
                    itemType,
                    category,
                    fillColor,
                    expanded,
                    topGap,
                ),
                includesWorstCaseRow,
                itemType,
                state,
                StockReviewTradeRecordRowAppender(state, tradeContext, layout),
            ),
        )
    }

    private fun filteredRecords(
        records: List<WeaponStockRecord>?,
        activeFilters: Set<StockReviewFilter>?,
        searchQuery: String?,
    ): List<WeaponStockRecord> {
        if (records == null || records.isEmpty()) {
            return records ?: emptyList()
        }
        val terms = StockReviewFilters.searchTerms(searchQuery)
        if (StockReviewFilters.count(activeFilters) <= 0 && terms.isEmpty()) {
            return records
        }
        val result = ArrayList<WeaponStockRecord>()
        for (record in records) {
            if (StockReviewFilters.matches(record, activeFilters) &&
                StockReviewFilters.matchesSearchTerms(record, terms)
            ) {
                result.add(record)
            }
        }
        return result
    }

    private fun categoryHeading(
        itemType: StockItemType,
        visibleRecords: List<WeaponStockRecord>?,
        aggregateRecords: List<WeaponStockRecord>?,
        tradeContext: StockReviewTradeContext?,
    ): String {
        val itemTypes = visibleRecords?.size ?: 0
        var selling = 0
        var buying = 0
        if (aggregateRecords != null && tradeContext != null) {
            for (record in aggregateRecords) {
                selling += tradeContext.pendingSellQuantityForItem(record.itemKey)
                buying += tradeContext.pendingBuyQuantityForItem(record.itemKey)
            }
        }
        val typeLabel = if (StockItemType.WING == itemType) "Wing Types" else "Weapon Types"
        return category.label +
            " [$typeLabel: $itemTypes, " +
            "Selling: ${maxOf(0, selling)}, " +
            "Buying: ${maxOf(0, buying)}]"
    }

    companion object {
        @JvmStatic
        fun noStock(): StockReviewStockCategorySection =
            StockReviewStockCategorySection(StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false, true)

        @JvmStatic
        fun insufficient(): StockReviewStockCategorySection =
            StockReviewStockCategorySection(StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true, false)

        @JvmStatic
        fun sufficient(): StockReviewStockCategorySection =
            StockReviewStockCategorySection(StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true, false)
    }
}

object StockReviewStockCategorySections {
    @JvmField val ORDERED: List<StockReviewStockCategorySection> = Collections.unmodifiableList(
        listOf(
            StockReviewStockCategorySection.noStock(),
            StockReviewStockCategorySection.insufficient(),
            StockReviewStockCategorySection.sufficient(),
        ),
    )
}
