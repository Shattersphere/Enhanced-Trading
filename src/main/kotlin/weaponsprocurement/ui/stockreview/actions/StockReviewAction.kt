package weaponsprocurement.ui.stockreview.actions

import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.state.StockReviewFilterGroup
import weaponsprocurement.ui.stockreview.state.StockReviewShipSizeFilter
import weaponsprocurement.ui.stockreview.trade.StockReviewReviewItemGroup
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType

class StockReviewAction private constructor(
    private val type: Type,
    private val category: StockCategory?,
    private val itemType: StockItemType?,
    private val tradeGroup: StockReviewTradeGroup?,
    private val reviewItemGroup: StockReviewReviewItemGroup?,
    private val filterGroup: StockReviewFilterGroup?,
    private val filter: StockReviewFilter?,
    private val shipSizeFilter: StockReviewShipSizeFilter?,
    private val itemKey: String?,
    private val submarketId: String?,
    private val quantity: Int,
) {
    enum class Type {
        TOGGLE_CATEGORY,
        TOGGLE_ITEM_TYPE,
        TOGGLE_TRADE_GROUP,
        TOGGLE_REVIEW_ITEM_GROUP,
        TOGGLE_ITEM,
        TOGGLE_TRADE_KIND,
        ADJUST_PLAN,
        TOGGLE_SHIP_PLAN,
        ADJUST_TO_SUFFICIENT,
        RESET_PLAN,
        RESET_SHIP_PLAN,
        CYCLE_SORT_MODE,
        CYCLE_SOURCE_MODE,
        TOGGLE_BLACK_MARKET,
        SCROLL_LIST,
        PURCHASE_ALL_UNTIL_SUFFICIENT,
        SELL_ALL_UNTIL_SUFFICIENT,
        RESET_ALL_TRADES,
        OPEN_FILTERS,
        TOGGLE_FILTER_GROUP,
        TOGGLE_FILTER,
        TOGGLE_SHIP_SIZE_FILTER,
        CLEAR_ITEM_SEARCH,
        CLEAR_SHIP_HULL_FILTER,
        RESET_FILTERS,
        OPEN_COLOR_DEBUG,
        OPEN_SHIP_CATALOG_DEBUG,
        DEBUG_CYCLE_TARGET,
        DEBUG_TOGGLE_PERSISTENCE,
        DEBUG_ADJUST_RED,
        DEBUG_ADJUST_GREEN,
        DEBUG_ADJUST_BLUE,
        DEBUG_APPLY,
        DEBUG_CONFIRM,
        DEBUG_RESTORE,
        DEBUG_NOOP,
        REVIEW_PURCHASE,
        CONFIRM_PURCHASE,
        GO_BACK,
    }

    fun getType(): Type = type
    fun getCategory(): StockCategory? = category
    fun getItemType(): StockItemType? = itemType
    fun getTradeGroup(): StockReviewTradeGroup? = tradeGroup
    fun getReviewItemGroup(): StockReviewReviewItemGroup? = reviewItemGroup
    fun getFilterGroup(): StockReviewFilterGroup? = filterGroup
    fun getFilter(): StockReviewFilter? = filter
    fun getShipSizeFilter(): StockReviewShipSizeFilter? = shipSizeFilter
    fun getItemKey(): String? = itemKey
    fun getSubmarketId(): String? = submarketId
    fun getQuantity(): Int = quantity
    fun getGroup(): StockReviewActionGroup = StockReviewActionGroup.forType(type)

    companion object {
        private fun action(
            type: Type,
            category: StockCategory? = null,
            itemType: StockItemType? = null,
            tradeGroup: StockReviewTradeGroup? = null,
            reviewItemGroup: StockReviewReviewItemGroup? = null,
            filterGroup: StockReviewFilterGroup? = null,
            filter: StockReviewFilter? = null,
            shipSizeFilter: StockReviewShipSizeFilter? = null,
            itemKey: String? = null,
            submarketId: String? = null,
            quantity: Int = 0,
        ): StockReviewAction =
            StockReviewAction(
                type,
                category,
                itemType,
                tradeGroup,
                reviewItemGroup,
                filterGroup,
                filter,
                shipSizeFilter,
                itemKey,
                submarketId,
                quantity,
            )

        @JvmStatic
        fun toggle(category: StockCategory?): StockReviewAction =
            action(Type.TOGGLE_CATEGORY, category = category)

        @JvmStatic
        fun toggle(itemType: StockItemType?, category: StockCategory?): StockReviewAction =
            action(Type.TOGGLE_CATEGORY, category = category, itemType = itemType)

        @JvmStatic
        fun toggle(itemType: StockItemType?): StockReviewAction =
            action(Type.TOGGLE_ITEM_TYPE, itemType = itemType)

        @JvmStatic
        fun toggle(tradeGroup: StockReviewTradeGroup?): StockReviewAction =
            action(Type.TOGGLE_TRADE_GROUP, tradeGroup = tradeGroup)

        @JvmStatic
        fun toggle(reviewItemGroup: StockReviewReviewItemGroup?): StockReviewAction =
            action(Type.TOGGLE_REVIEW_ITEM_GROUP, reviewItemGroup = reviewItemGroup)

        @JvmStatic
        fun toggle(filterGroup: StockReviewFilterGroup?): StockReviewAction =
            action(Type.TOGGLE_FILTER_GROUP, filterGroup = filterGroup)

        @JvmStatic
        fun toggleShipSizeFilter(size: StockReviewShipSizeFilter?): StockReviewAction =
            action(Type.TOGGLE_SHIP_SIZE_FILTER, shipSizeFilter = size)

        @JvmStatic
        fun toggleItem(itemKey: String?): StockReviewAction =
            action(Type.TOGGLE_ITEM, itemKey = itemKey)

        @JvmStatic
        fun toggleTradeKind(): StockReviewAction =
            action(Type.TOGGLE_TRADE_KIND)

        @JvmStatic
        fun buyBest(itemKey: String?, quantity: Int): StockReviewAction = adjustPlan(itemKey, quantity)

        @JvmStatic
        fun adjustPlan(itemKey: String?, delta: Int): StockReviewAction =
            action(Type.ADJUST_PLAN, itemKey = itemKey, quantity = delta)

        @JvmStatic
        fun toggleShipPlan(recordKey: String?): StockReviewAction =
            action(Type.TOGGLE_SHIP_PLAN, itemKey = recordKey)

        @JvmStatic
        fun resetPlan(itemKey: String?): StockReviewAction =
            action(Type.RESET_PLAN, itemKey = itemKey)

        @JvmStatic
        fun resetShipPlan(recordKey: String?): StockReviewAction =
            action(Type.RESET_SHIP_PLAN, itemKey = recordKey)

        @JvmStatic
        fun adjustToSufficient(itemKey: String?, delta: Int): StockReviewAction =
            action(Type.ADJUST_TO_SUFFICIENT, itemKey = itemKey, quantity = delta)

        @JvmStatic
        fun cycleSortMode(): StockReviewAction =
            action(Type.CYCLE_SORT_MODE)

        @JvmStatic
        fun toggleBlackMarket(): StockReviewAction =
            action(Type.TOGGLE_BLACK_MARKET)

        @JvmStatic
        fun cycleSourceMode(): StockReviewAction =
            action(Type.CYCLE_SOURCE_MODE)

        @JvmStatic
        fun scrollList(delta: Int): StockReviewAction =
            action(Type.SCROLL_LIST, quantity = delta)

        @JvmStatic
        fun reviewPurchase(): StockReviewAction =
            action(Type.REVIEW_PURCHASE)

        @JvmStatic
        fun purchaseAllUntilSufficient(): StockReviewAction =
            action(Type.PURCHASE_ALL_UNTIL_SUFFICIENT)

        @JvmStatic
        fun sellAllUntilSufficient(): StockReviewAction =
            action(Type.SELL_ALL_UNTIL_SUFFICIENT)

        @JvmStatic
        fun resetAllTrades(): StockReviewAction =
            action(Type.RESET_ALL_TRADES)

        @JvmStatic
        fun openFilters(): StockReviewAction =
            action(Type.OPEN_FILTERS)

        @JvmStatic
        fun toggleFilter(filter: StockReviewFilter?): StockReviewAction =
            action(Type.TOGGLE_FILTER, filter = filter)

        @JvmStatic
        fun resetFilters(): StockReviewAction =
            action(Type.RESET_FILTERS)

        @JvmStatic
        fun clearItemSearch(): StockReviewAction =
            action(Type.CLEAR_ITEM_SEARCH)

        @JvmStatic
        fun clearShipHullFilter(): StockReviewAction =
            action(Type.CLEAR_SHIP_HULL_FILTER)

        @JvmStatic
        fun openColorDebug(): StockReviewAction =
            action(Type.OPEN_COLOR_DEBUG)

        @JvmStatic
        fun openShipCatalogDebug(): StockReviewAction =
            action(Type.OPEN_SHIP_CATALOG_DEBUG)

        @JvmStatic
        fun debugCycleTarget(delta: Int): StockReviewAction =
            action(Type.DEBUG_CYCLE_TARGET, quantity = delta)

        @JvmStatic
        fun debugTogglePersistence(): StockReviewAction =
            action(Type.DEBUG_TOGGLE_PERSISTENCE)

        @JvmStatic
        fun debugAdjustRed(delta: Int): StockReviewAction =
            action(Type.DEBUG_ADJUST_RED, quantity = delta)

        @JvmStatic
        fun debugAdjustGreen(delta: Int): StockReviewAction =
            action(Type.DEBUG_ADJUST_GREEN, quantity = delta)

        @JvmStatic
        fun debugAdjustBlue(delta: Int): StockReviewAction =
            action(Type.DEBUG_ADJUST_BLUE, quantity = delta)

        @JvmStatic
        fun debugApply(): StockReviewAction =
            action(Type.DEBUG_APPLY)

        @JvmStatic
        fun debugConfirm(): StockReviewAction =
            action(Type.DEBUG_CONFIRM)

        @JvmStatic
        fun debugRestore(): StockReviewAction =
            action(Type.DEBUG_RESTORE)

        @JvmStatic
        fun debugNoop(): StockReviewAction =
            action(Type.DEBUG_NOOP)

        @JvmStatic
        fun confirmPurchase(): StockReviewAction =
            action(Type.CONFIRM_PURCHASE)

        @JvmStatic
        fun goBack(): StockReviewAction =
            action(Type.GO_BACK)
    }
}
