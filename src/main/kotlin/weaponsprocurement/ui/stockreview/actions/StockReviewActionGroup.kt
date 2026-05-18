package weaponsprocurement.ui.stockreview.actions

import java.util.EnumMap
import java.util.EnumSet

enum class StockReviewActionGroup(
    @JvmField val label: String,
    vararg actionTypes: StockReviewAction.Type,
) {
    ROW_EXPANSION(
        "row expansion",
        StockReviewAction.Type.TOGGLE_CATEGORY,
        StockReviewAction.Type.TOGGLE_ITEM_TYPE,
        StockReviewAction.Type.TOGGLE_TRADE_GROUP,
        StockReviewAction.Type.TOGGLE_REVIEW_ITEM_GROUP,
        StockReviewAction.Type.TOGGLE_ITEM,
    ),
    PLAN_ADJUSTMENT(
        "item plan adjustment",
        StockReviewAction.Type.ADJUST_PLAN,
        StockReviewAction.Type.ADJUST_TO_SUFFICIENT,
    ),
    PLAN_RESET(
        "item plan reset",
        StockReviewAction.Type.RESET_PLAN,
    ),
    BULK_SUFFICIENT_PURCHASE(
        "bulk sufficient purchase",
        StockReviewAction.Type.PURCHASE_ALL_UNTIL_SUFFICIENT,
    ),
    BULK_SUFFICIENT_SALE(
        "bulk sufficient sale",
        StockReviewAction.Type.SELL_ALL_UNTIL_SUFFICIENT,
    ),
    CONFIRMED_EXECUTION(
        "confirmed execution",
        StockReviewAction.Type.CONFIRM_PURCHASE,
    ),
    SOURCE_TRANSITIONS(
        "source and snapshot transitions",
        StockReviewAction.Type.CYCLE_SORT_MODE,
        StockReviewAction.Type.CYCLE_SOURCE_MODE,
        StockReviewAction.Type.TOGGLE_BLACK_MARKET,
        StockReviewAction.Type.RESET_ALL_TRADES,
    ),
    SCROLL(
        "list scroll",
        StockReviewAction.Type.SCROLL_LIST,
    ),
    FILTERS(
        "filter controls",
        StockReviewAction.Type.OPEN_FILTERS,
        StockReviewAction.Type.TOGGLE_FILTER_GROUP,
        StockReviewAction.Type.TOGGLE_FILTER,
        StockReviewAction.Type.RESET_FILTERS,
    ),
    DEBUG_MODE(
        "debug mode controls",
        StockReviewAction.Type.OPEN_COLOR_DEBUG,
        StockReviewAction.Type.OPEN_SHIP_CATALOG_DEBUG,
        StockReviewAction.Type.DEBUG_CYCLE_TARGET,
        StockReviewAction.Type.DEBUG_TOGGLE_PERSISTENCE,
        StockReviewAction.Type.DEBUG_ADJUST_RED,
        StockReviewAction.Type.DEBUG_ADJUST_GREEN,
        StockReviewAction.Type.DEBUG_ADJUST_BLUE,
        StockReviewAction.Type.DEBUG_APPLY,
        StockReviewAction.Type.DEBUG_CONFIRM,
        StockReviewAction.Type.DEBUG_RESTORE,
        StockReviewAction.Type.DEBUG_NOOP,
    ),
    NAVIGATION(
        "navigation",
        StockReviewAction.Type.REVIEW_PURCHASE,
        StockReviewAction.Type.GO_BACK,
    ),
    ;

    @JvmField val types: Set<StockReviewAction.Type> = EnumSet.copyOf(actionTypes.toList())

    fun contains(type: StockReviewAction.Type): Boolean = types.contains(type)

    companion object {
        private val GROUP_BY_TYPE: Map<StockReviewAction.Type, StockReviewActionGroup> = buildGroupMap()

        @JvmStatic
        fun forType(type: StockReviewAction.Type): StockReviewActionGroup =
            GROUP_BY_TYPE[type] ?: throw IllegalArgumentException("Unhandled stock-review action type: $type")

        private fun buildGroupMap(): Map<StockReviewAction.Type, StockReviewActionGroup> {
            val result = EnumMap<StockReviewAction.Type, StockReviewActionGroup>(StockReviewAction.Type::class.java)
            for (group in values()) {
                for (type in group.types) {
                    val previous = result.put(type, group)
                    if (previous != null) {
                        throw IllegalStateException("Stock-review action type $type is assigned to both ${previous.name} and ${group.name}.")
                    }
                }
            }
            for (type in StockReviewAction.Type.values()) {
                if (!result.containsKey(type)) {
                    throw IllegalStateException("Stock-review action type $type has no action group.")
                }
            }
            return result
        }
    }
}
