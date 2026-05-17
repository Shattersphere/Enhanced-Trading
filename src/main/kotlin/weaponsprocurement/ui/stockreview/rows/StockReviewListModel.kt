package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.state.StockReviewFilters
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color

object StockReviewListModel {
    @JvmStatic
    fun build(
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState?,
        tradeContext: StockReviewTradeContext,
    ): List<WimGuiListRow<StockReviewAction>> = build(snapshot, state, tradeContext, StockReviewRowLayout.trade())

    @JvmStatic
    fun build(
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState?,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        if (state == null) {
            rows.add(StockReviewListEmptyRows.main(snapshot, null))
            return rows
        }
        var displayed = 0
        displayed += addItemType(rows, snapshot, state, tradeContext, layout, StockItemType.WEAPON, false)
        displayed += addItemType(rows, snapshot, state, tradeContext, layout, StockItemType.WING, true)
        if (displayed == 0) {
            rows.add(StockReviewListEmptyRows.main(snapshot, state))
        }
        return rows
    }

    private fun addItemType(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
        itemType: StockItemType,
        topGap: Boolean,
    ): Int {
        val count = snapshot?.getCount(itemType) ?: 0
        val expanded = state.isExpanded(itemType)
        StockReviewListSection.addHeading(rows, StockReviewHeadingRows.itemType(itemType, count, expanded, topGap))
        if (!expanded) return count

        var displayed = 0
        displayed += addCategory(rows, snapshot, state, tradeContext, layout, itemType, StockCategory.NO_STOCK, false)
        displayed += addCategory(rows, snapshot, state, tradeContext, layout, itemType, StockCategory.INSUFFICIENT, true)
        displayed += addCategory(rows, snapshot, state, tradeContext, layout, itemType, StockCategory.SUFFICIENT, true)
        return displayed
    }

    private fun addCategory(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
        itemType: StockItemType,
        category: StockCategory,
        topGap: Boolean,
    ): Int {
        val records = filteredRecords(snapshot?.getRecords(itemType, category), state.getActiveFilters())
        val expanded = state.isExpanded(itemType, category)
        return StockReviewListSection.builder(records)
            .expanded(expanded)
            .heading {
                StockReviewHeadingRows.stockCategory(
                    categoryHeading(itemType, category, records, tradeContext),
                    itemType,
                    category,
                    categoryColor(category),
                    expanded,
                    topGap,
                )
            }
            .includeWorstCaseRow(StockItemType.WEAPON == itemType && StockCategory.NO_STOCK == category)
            .itemAppender { targetRows, record -> StockReviewItemRows.addTradeRow(targetRows, record, state, tradeContext, layout) }
            .build()
            .addTo(rows, layout)
    }

    private fun categoryColor(category: StockCategory): Color =
        when (category) {
            StockCategory.NO_STOCK -> StockReviewStyle.NO_STOCK
            StockCategory.INSUFFICIENT -> StockReviewStyle.INSUFFICIENT
            StockCategory.SUFFICIENT -> StockReviewStyle.SUFFICIENT
        }

    private fun filteredRecords(
        records: List<WeaponStockRecord>?,
        activeFilters: Set<StockReviewFilter>?,
    ): List<WeaponStockRecord> {
        if (records == null || records.isEmpty() || StockReviewFilters.count(activeFilters) <= 0) {
            return records ?: emptyList()
        }
        val result = ArrayList<WeaponStockRecord>()
        for (record in records) {
            if (StockReviewFilters.matches(record, activeFilters)) {
                result.add(record)
            }
        }
        return result
    }

    private fun categoryHeading(
        itemType: StockItemType,
        category: StockCategory,
        records: List<WeaponStockRecord>?,
        tradeContext: StockReviewTradeContext?,
    ): String {
        val itemTypes = records?.size ?: 0
        var selling = 0
        var buying = 0
        if (records != null && tradeContext != null) {
            for (record in records) {
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
}
