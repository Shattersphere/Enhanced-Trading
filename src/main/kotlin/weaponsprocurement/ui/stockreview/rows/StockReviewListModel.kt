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
import weaponsprocurement.stock.item.StockSourceMode
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
            rows.add(StockReviewListRow.empty(emptyStateMessage(snapshot, null)))
            return rows
        }
        var displayed = 0
        displayed += addItemType(rows, snapshot, state, tradeContext, layout, StockItemType.WEAPON, false)
        displayed += addItemType(rows, snapshot, state, tradeContext, layout, StockItemType.WING, true)
        if (displayed == 0) {
            rows.add(StockReviewListRow.empty(emptyStateMessage(snapshot, state)))
        }
        return rows
    }

    private fun emptyStateMessage(snapshot: WeaponStockSnapshot?, state: StockReviewState?): String {
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
        rows.add(
            StockReviewHeadingRows.itemType(itemType, count, expanded, topGap),
        )
        if (!expanded) {
            return count
        }
        var displayed = 0
        displayed += addCategory(rows, snapshot, state, tradeContext, layout, itemType, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false)
        displayed += addCategory(rows, snapshot, state, tradeContext, layout, itemType, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true)
        displayed += addCategory(rows, snapshot, state, tradeContext, layout, itemType, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true)
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
        color: Color,
        topGap: Boolean,
    ): Int {
        val records = filteredRecords(snapshot?.getRecords(itemType, category), state.getActiveFilters())
        val expanded = state.isExpanded(itemType, category)
        rows.add(
            StockReviewHeadingRows.stockCategory(
                categoryHeading(itemType, category, records, tradeContext),
                itemType,
                category,
                color,
                expanded,
                topGap,
            ),
        )
        if (!expanded) {
            return records.size
        }
        if (StockReviewStyle.SHOW_WIDTH_TEST_ROWS && StockItemType.WEAPON == itemType && StockCategory.NO_STOCK == category) {
            StockReviewItemRows.addWorstCaseRow(rows, layout)
        }
        for (record in records) {
            StockReviewItemRows.addTradeRow(rows, record, state, tradeContext, layout)
        }
        return records.size
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
