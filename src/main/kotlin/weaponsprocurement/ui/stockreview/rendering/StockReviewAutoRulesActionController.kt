package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatch
import weaponsprocurement.ui.stockreview.actions.StockReviewActionDispatcher
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.actions.StockReviewActionHandlerGroup
import weaponsprocurement.ui.stockreview.state.StockReviewAutoRulesController

class StockReviewAutoRulesActionController(
    private val controller: StockReviewAutoRulesController,
    private val host: Host,
) {
    interface Host {
        fun requestContentRebuild()
    }

    private val dispatcher: StockReviewActionDispatcher = StockReviewActionDispatch.of(
        StockReviewActionHandlerGroup.group(StockReviewActionGroup.AUTO_RULES) { action ->
            when (action.getType()) {
                Type.AUTO_RULES_TOGGLE_ENABLED -> controller.handleToggleEnabled()
                Type.AUTO_RULES_TOGGLE_SELL_BLACK -> controller.handleToggleSellBlack()
                Type.AUTO_RULES_TOGGLE_BUY_BLACK -> controller.handleToggleBuyBlack()
                Type.AUTO_RULES_TOGGLE_HULLMODS_FROM_BLACK -> controller.handleToggleHullmodsFromBlack()
                Type.AUTO_RULES_TOGGLE_BUY_UNKNOWN_HULLMODS -> controller.handleToggleBuyUnknownHullmods()
                Type.AUTO_RULES_TOGGLE_LEARN_HULLMODS -> controller.handleToggleLearnHullmods()
                Type.AUTO_RULES_ADJUST_CREDIT_FLOOR -> controller.handleAdjustCreditFloor(action.getQuantity())
                Type.AUTO_RULES_CYCLE_TAB -> controller.cycleTab(action.getQuantity())
                Type.AUTO_RULES_CYCLE_HELD_FILTER -> controller.cycleHeldFilter(action.getQuantity())
                Type.AUTO_RULES_CYCLE_RULE_FILTER -> controller.cycleRuleFilter(action.getQuantity())
                Type.AUTO_RULES_CYCLE_NEW_FILTER -> controller.cycleNewFilter(action.getQuantity())
                Type.AUTO_RULES_CYCLE_SIZE_FILTER -> controller.cycleSizeFilter(action.getQuantity())
                Type.AUTO_RULES_CYCLE_DAMAGE_FILTER -> controller.cycleDamageFilter(action.getQuantity())
                Type.AUTO_RULES_CYCLE_DESIGN_TYPE_FILTER -> controller.cycleDesignTypeFilter(action.getQuantity())
                Type.AUTO_RULES_COMMIT_NAME_QUERY -> {} // commit is handled by text field callback; this just triggers rebuild
                Type.AUTO_RULES_ADJUST_SELL_ABOVE -> controller.handleAdjustSellAbove(action.getItemKey(), action.getQuantity())
                Type.AUTO_RULES_ADJUST_BUY_BELOW -> controller.handleAdjustBuyBelow(action.getItemKey(), action.getQuantity())
                Type.AUTO_RULES_CLEAR_RULE -> controller.handleClearRule(action.getItemKey())
                Type.AUTO_RULES_TOGGLE_SELECT_ITEM -> controller.handleToggleSelectItem(action.getItemKey())
                Type.AUTO_RULES_TOGGLE_SELECT_ALL_VISIBLE -> controller.handleToggleSelectAllVisible()
                Type.AUTO_RULES_APPLY_BULK -> controller.handleApplyBulk()
                Type.AUTO_RULES_CLEAR_SELECTED -> controller.handleClearSelected()
                Type.AUTO_RULES_TOGGLE_HULLMOD_BLACKLIST -> controller.handleToggleHullmodBlacklist(action.getItemKey())
                else -> return@group
            }
            host.requestContentRebuild()
        },
    )

    fun handle(action: StockReviewAction?): Boolean = dispatcher.handle(action)
}
