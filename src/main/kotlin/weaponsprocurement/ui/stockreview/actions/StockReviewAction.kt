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
        OPEN_AUTO_RULES,
        AUTO_RULES_TOGGLE_ENABLED,
        AUTO_RULES_TOGGLE_REQUIRE_CONFIRM,
        AUTO_RULES_TOGGLE_SUSPICION_SELL,
        AUTO_RULES_TOGGLE_SUSPICION_BUY,
        AUTO_RULES_TOGGLE_BUY_UNKNOWN_HULLMODS,
        AUTO_RULES_TOGGLE_LEARN_HULLMODS,
        AUTO_RULES_ADJUST_CREDIT_FLOOR,
        AUTO_RULES_CYCLE_TAB,
        AUTO_RULES_CYCLE_HELD_FILTER,
        AUTO_RULES_CYCLE_RULE_FILTER,
        AUTO_RULES_CYCLE_NEW_FILTER,
        AUTO_RULES_CYCLE_SIZE_FILTER,
        AUTO_RULES_CYCLE_DAMAGE_FILTER,
        AUTO_RULES_CYCLE_DESIGN_TYPE_FILTER,
        AUTO_RULES_COMMIT_NAME_QUERY,
        AUTO_RULES_ADJUST_SELL_ABOVE,
        AUTO_RULES_ADJUST_BUY_BELOW,
        AUTO_RULES_CLEAR_RULE,
        AUTO_RULES_TOGGLE_SELECT_ITEM,
        AUTO_RULES_TOGGLE_SELECT_ALL_VISIBLE,
        AUTO_RULES_APPLY_BULK,
        AUTO_RULES_CLEAR_SELECTED,
        AUTO_RULES_TOGGLE_HULLMOD_BLACKLIST,
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
        @JvmStatic
        fun toggle(category: StockCategory?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_CATEGORY, category, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(itemType: StockItemType?, category: StockCategory?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_CATEGORY, category, itemType, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(itemType: StockItemType?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_ITEM_TYPE, null, itemType, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(tradeGroup: StockReviewTradeGroup?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_TRADE_GROUP, null, null, tradeGroup, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(reviewItemGroup: StockReviewReviewItemGroup?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_REVIEW_ITEM_GROUP, null, null, null, reviewItemGroup, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(filterGroup: StockReviewFilterGroup?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_FILTER_GROUP, null, null, null, null, filterGroup, null, null, null, null, 0)

        @JvmStatic
        fun toggleShipSizeFilter(size: StockReviewShipSizeFilter?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_SHIP_SIZE_FILTER, null, null, null, null, null, null, size, null, null, 0)

        @JvmStatic
        fun toggleItem(itemKey: String?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_ITEM, null, null, null, null, null, null, null, itemKey, null, 0)

        @JvmStatic
        fun toggleTradeKind(): StockReviewAction =
            StockReviewAction(Type.TOGGLE_TRADE_KIND, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun buyBest(itemKey: String?, quantity: Int): StockReviewAction = adjustPlan(itemKey, quantity)

        @JvmStatic
        fun adjustPlan(itemKey: String?, delta: Int): StockReviewAction =
            StockReviewAction(Type.ADJUST_PLAN, null, null, null, null, null, null, null, itemKey, null, delta)

        @JvmStatic
        fun toggleShipPlan(recordKey: String?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_SHIP_PLAN, null, null, null, null, null, null, null, recordKey, null, 0)

        @JvmStatic
        fun resetPlan(itemKey: String?): StockReviewAction =
            StockReviewAction(Type.RESET_PLAN, null, null, null, null, null, null, null, itemKey, null, 0)

        @JvmStatic
        fun resetShipPlan(recordKey: String?): StockReviewAction =
            StockReviewAction(Type.RESET_SHIP_PLAN, null, null, null, null, null, null, null, recordKey, null, 0)

        @JvmStatic
        fun adjustToSufficient(itemKey: String?, delta: Int): StockReviewAction =
            StockReviewAction(Type.ADJUST_TO_SUFFICIENT, null, null, null, null, null, null, null, itemKey, null, delta)

        @JvmStatic
        fun cycleSortMode(): StockReviewAction =
            StockReviewAction(Type.CYCLE_SORT_MODE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggleBlackMarket(): StockReviewAction =
            StockReviewAction(Type.TOGGLE_BLACK_MARKET, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun cycleSourceMode(): StockReviewAction =
            StockReviewAction(Type.CYCLE_SOURCE_MODE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun scrollList(delta: Int): StockReviewAction =
            StockReviewAction(Type.SCROLL_LIST, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun reviewPurchase(): StockReviewAction =
            StockReviewAction(Type.REVIEW_PURCHASE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun purchaseAllUntilSufficient(): StockReviewAction =
            StockReviewAction(Type.PURCHASE_ALL_UNTIL_SUFFICIENT, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun sellAllUntilSufficient(): StockReviewAction =
            StockReviewAction(Type.SELL_ALL_UNTIL_SUFFICIENT, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun resetAllTrades(): StockReviewAction =
            StockReviewAction(Type.RESET_ALL_TRADES, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun openFilters(): StockReviewAction =
            StockReviewAction(Type.OPEN_FILTERS, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggleFilter(filter: StockReviewFilter?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_FILTER, null, null, null, null, null, filter, null, null, null, 0)

        @JvmStatic
        fun resetFilters(): StockReviewAction =
            StockReviewAction(Type.RESET_FILTERS, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun openColorDebug(): StockReviewAction =
            StockReviewAction(Type.OPEN_COLOR_DEBUG, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun openShipCatalogDebug(): StockReviewAction =
            StockReviewAction(Type.OPEN_SHIP_CATALOG_DEBUG, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugCycleTarget(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_CYCLE_TARGET, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugTogglePersistence(): StockReviewAction =
            StockReviewAction(Type.DEBUG_TOGGLE_PERSISTENCE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugAdjustRed(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_ADJUST_RED, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugAdjustGreen(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_ADJUST_GREEN, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugAdjustBlue(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_ADJUST_BLUE, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugApply(): StockReviewAction =
            StockReviewAction(Type.DEBUG_APPLY, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugConfirm(): StockReviewAction =
            StockReviewAction(Type.DEBUG_CONFIRM, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugRestore(): StockReviewAction =
            StockReviewAction(Type.DEBUG_RESTORE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugNoop(): StockReviewAction =
            StockReviewAction(Type.DEBUG_NOOP, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun confirmPurchase(): StockReviewAction =
            StockReviewAction(Type.CONFIRM_PURCHASE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun goBack(): StockReviewAction =
            StockReviewAction(Type.GO_BACK, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun openAutoRules(): StockReviewAction =
            StockReviewAction(Type.OPEN_AUTO_RULES, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleEnabled(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_ENABLED, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleRequireConfirm(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_REQUIRE_CONFIRM, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleSuspicionSelling(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_SUSPICION_SELL, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleSuspicionBuying(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_SUSPICION_BUY, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleBuyUnknownHullmods(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_BUY_UNKNOWN_HULLMODS, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleLearnHullmods(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_LEARN_HULLMODS, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesAdjustCreditFloor(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_ADJUST_CREDIT_FLOOR, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleTab(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_TAB, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleHeldFilter(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_HELD_FILTER, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleRuleFilter(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_RULE_FILTER, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleNewFilter(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_NEW_FILTER, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleSizeFilter(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_SIZE_FILTER, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleDamageFilter(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_DAMAGE_FILTER, null, null, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun autoRulesCycleDesignTypeFilter(delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CYCLE_DESIGN_TYPE_FILTER, null, null, null, null, null, null, null, null, null, delta)

        /** Dispatched purely as a notice that a name-query commit happened; the actual
         *  string is delivered via the text field callback, not the action payload. */
        @JvmStatic
        fun autoRulesCommitNameQuery(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_COMMIT_NAME_QUERY, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesAdjustSellAbove(itemKey: String?, delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_ADJUST_SELL_ABOVE, null, null, null, null, null, null, null, itemKey, null, delta)

        @JvmStatic
        fun autoRulesAdjustBuyBelow(itemKey: String?, delta: Int): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_ADJUST_BUY_BELOW, null, null, null, null, null, null, null, itemKey, null, delta)

        @JvmStatic
        fun autoRulesClearRule(itemKey: String?): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CLEAR_RULE, null, null, null, null, null, null, null, itemKey, null, 0)

        @JvmStatic
        fun autoRulesToggleSelectItem(itemKey: String?): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_SELECT_ITEM, null, null, null, null, null, null, null, itemKey, null, 0)

        @JvmStatic
        fun autoRulesToggleSelectAllVisible(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_SELECT_ALL_VISIBLE, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesApplyBulk(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_APPLY_BULK, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesClearSelected(): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_CLEAR_SELECTED, null, null, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun autoRulesToggleHullmodBlacklist(hullmodId: String?): StockReviewAction =
            StockReviewAction(Type.AUTO_RULES_TOGGLE_HULLMOD_BLACKLIST, null, null, null, null, null, null, null, hullmodId, null, 0)
    }
}
