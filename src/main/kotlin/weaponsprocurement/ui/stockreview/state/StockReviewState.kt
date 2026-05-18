package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.WimGuiScroll
import weaponsprocurement.ui.WimGuiScrollableListState
import weaponsprocurement.ui.stockreview.trade.StockReviewReviewItemGroup
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.item.StockSortMode
import weaponsprocurement.stock.item.StockSourceMode

class StockReviewState : WimGuiScrollableListState {
    private val expansion: StockReviewExpansionState
    private val filters: StockReviewFilterState
    private val shipFilters: StockReviewShipFilterState
    private val source: StockReviewSourceState
    private var listScrollOffset = 0
    private var tradeWarning = "None"
    private var initialCredits = -1f
    private var initialCargoCapacity = -1f
    private var contentRevision = 0
    private var tradeKind = StockReviewTradeKind.ITEMS
    private var shipHullFilter = ""

    constructor(config: StockReviewConfig) {
        expansion = StockReviewExpansionState()
        filters = StockReviewFilterState()
        shipFilters = StockReviewShipFilterState()
        source = StockReviewSourceState(config)
    }

    constructor(source: StockReviewState) {
        expansion = StockReviewExpansionState(source.expansion)
        filters = StockReviewFilterState(source.filters)
        shipFilters = StockReviewShipFilterState(source.shipFilters)
        this.source = StockReviewSourceState(source.source)
        listScrollOffset = source.listScrollOffset
        tradeWarning = source.tradeWarning
        initialCredits = source.initialCredits
        initialCargoCapacity = source.initialCargoCapacity
        contentRevision = source.contentRevision
        tradeKind = source.tradeKind
        shipHullFilter = source.shipHullFilter
    }

    fun isExpanded(category: StockCategory?): Boolean = expansion.isExpanded(category)
    fun toggle(category: StockCategory?) {
        markContentChangedIf(expansion.toggle(category))
    }
    fun isExpanded(itemType: StockItemType?, category: StockCategory?): Boolean = expansion.isExpanded(itemType, category)
    fun toggle(itemType: StockItemType?, category: StockCategory?) {
        markContentChangedIf(expansion.toggle(itemType, category))
    }
    fun isExpanded(itemType: StockItemType?): Boolean = expansion.isExpanded(itemType)
    fun toggle(itemType: StockItemType?) {
        markContentChangedIf(expansion.toggle(itemType))
    }
    fun isExpanded(tradeGroup: StockReviewTradeGroup?): Boolean = expansion.isExpanded(tradeGroup)
    fun toggle(tradeGroup: StockReviewTradeGroup?) {
        markContentChangedIf(expansion.toggle(tradeGroup))
    }
    fun setExpanded(tradeGroup: StockReviewTradeGroup?, value: Boolean) {
        markContentChangedIf(expansion.setExpanded(tradeGroup, value))
    }
    fun isExpanded(reviewItemGroup: StockReviewReviewItemGroup?): Boolean = expansion.isExpanded(reviewItemGroup)
    fun toggle(reviewItemGroup: StockReviewReviewItemGroup?) {
        markContentChangedIf(expansion.toggle(reviewItemGroup))
    }
    fun isItemExpanded(itemKey: String?): Boolean = expansion.isItemExpanded(itemKey)
    fun toggleItem(itemKey: String?) {
        markContentChangedIf(expansion.toggleItem(itemKey))
    }

    fun isFilterActive(filter: StockReviewFilter?): Boolean = filters.isFilterActive(filter)

    fun toggleFilter(filter: StockReviewFilter?) {
        if (filters.toggleFilter(filter)) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun getActiveFilters(): Set<StockReviewFilter> = filters.getActiveFilters()
    fun getActiveFilterCount(): Int = filters.getActiveFilterCount()

    fun clearFilters() {
        if (filters.clearFilters()) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun isShipSizeFilterActive(size: StockReviewShipSizeFilter?): Boolean = shipFilters.isSizeActive(size)

    fun toggleShipSizeFilter(size: StockReviewShipSizeFilter?) {
        if (shipFilters.toggleSize(size)) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun getActiveShipSizeFilters(): Set<StockReviewShipSizeFilter> = shipFilters.getActiveSizes()

    fun getShipFilterField(field: StockReviewShipFilterField?): String = shipFilters.getField(field)

    fun setShipFilterField(field: StockReviewShipFilterField?, value: String?) {
        if (shipFilters.setField(field, value)) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun appendShipFilterField(field: StockReviewShipFilterField?, char: Char) {
        if (shipFilters.appendField(field, char)) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun backspaceShipFilterField(field: StockReviewShipFilterField?) {
        if (shipFilters.backspaceField(field)) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun getShipFilterInt(field: StockReviewShipFilterField): Int? = shipFilters.intValue(field)

    fun clearShipFilters() {
        if (shipFilters.clear()) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun getActiveShipFilterCount(): Int =
        shipFilters.activeCount() + if (shipHullFilter.isBlank()) 0 else 1

    fun isExpanded(group: StockReviewFilterGroup?): Boolean = filters.isExpanded(group)
    fun toggle(group: StockReviewFilterGroup?) {
        markContentChangedIf(filters.toggle(group))
    }
    fun getSortMode(): StockSortMode = source.getSortMode()
    fun cycleSortMode() {
        markContentChangedIf(source.cycleSortMode())
    }
    fun isIncludeCurrentMarketStorage(): Boolean = source.isIncludeCurrentMarketStorage()
    fun isIncludeBlackMarket(): Boolean = source.isIncludeBlackMarket()
    fun toggleBlackMarket() {
        markContentChangedIf(source.toggleBlackMarket())
    }
    fun getSourceMode(): StockSourceMode = source.getSourceMode()
    fun cycleSourceMode() {
        markContentChangedIf(source.cycleSourceMode())
    }

    fun normalizeSourceMode() {
        markContentChangedIf(source.normalizeSourceMode())
    }

    fun getTradeKind(): StockReviewTradeKind = tradeKind

    fun isShipTrading(): Boolean = tradeKind == StockReviewTradeKind.SHIPS

    fun cycleTradeKind() {
        tradeKind = tradeKind.next()
        listScrollOffset = 0
        markContentChanged()
    }

    fun setTradeKind(tradeKind: StockReviewTradeKind) {
        if (this.tradeKind == tradeKind) {
            return
        }
        this.tradeKind = tradeKind
        listScrollOffset = 0
        markContentChanged()
    }

    fun getShipHullFilter(): String = shipHullFilter

    fun setShipHullFilter(value: String?) {
        val normalized = normalizeShipHullFilter(value)
        if (shipHullFilter == normalized) {
            return
        }
        shipHullFilter = normalized
        listScrollOffset = 0
        markContentChanged()
    }

    fun appendShipHullFilter(char: Char) {
        if (shipHullFilter.length >= MAX_SHIP_HULL_FILTER_LENGTH || !isShipHullFilterChar(char)) {
            return
        }
        setShipHullFilter(shipHullFilter + char)
    }

    fun backspaceShipHullFilter() {
        if (shipHullFilter.isEmpty()) {
            return
        }
        setShipHullFilter(shipHullFilter.substring(0, shipHullFilter.length - 1))
    }

    fun getContentRevision(): Int = contentRevision

    override fun getListScrollOffset(): Int = listScrollOffset

    override fun setListScrollOffset(listScrollOffset: Int) {
        this.listScrollOffset = Math.max(0, listScrollOffset)
    }

    fun adjustListScrollOffset(delta: Int, maxOffset: Int) {
        listScrollOffset = WimGuiScroll.usefulOffsetByDelta(listScrollOffset, delta, Math.max(0, maxOffset))
    }

    fun getTradeWarning(): String = tradeWarning

    fun setTradeWarning(tradeWarning: String?) {
        this.tradeWarning = if (tradeWarning.isNullOrEmpty()) "None" else tradeWarning
    }

    fun getInitialCredits(): Float = initialCredits

    fun setInitialCreditsIfUnset(initialCredits: Float) {
        if (this.initialCredits < 0f) {
            this.initialCredits = Math.max(0f, initialCredits)
        }
    }

    fun getInitialCargoCapacity(): Float = initialCargoCapacity

    fun setInitialCargoCapacityIfUnset(initialCargoCapacity: Float) {
        if (this.initialCargoCapacity < 0f) {
            this.initialCargoCapacity = Math.max(0f, initialCargoCapacity)
        }
    }

    private fun markContentChanged() {
        contentRevision++
    }

    private fun markContentChangedIf(changed: Boolean) {
        if (changed) markContentChanged()
    }

    private fun normalizeShipHullFilter(value: String?): String =
        (value ?: "")
            .filter(::isShipHullFilterChar)
            .take(MAX_SHIP_HULL_FILTER_LENGTH)

    private fun isShipHullFilterChar(char: Char): Boolean =
        char.isLetterOrDigit() || char == ' ' || char == '-' || char == '_' || char == '\'' || char == '.'

    companion object {
        private const val MAX_SHIP_HULL_FILTER_LENGTH = 40
    }
}
