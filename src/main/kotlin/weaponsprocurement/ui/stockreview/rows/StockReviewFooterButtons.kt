package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionButtonFactory
import weaponsprocurement.ui.stockreview.controls.StockReviewButtonDefinition
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips

class StockReviewFooterButtons private constructor() {
    companion object {
        private val COLOR_CONFIRM = footer(
            "color-confirm",
            StockReviewActionGroup.DEBUG_MODE,
            "Confirm",
            { StockReviewAction.debugConfirm() },
            StockReviewStyle.CONFIRM_BUTTON,
            "Apply the color and return to the trade screen.",
        )

        private val COLOR_APPLY = footer(
            "color-apply",
            StockReviewActionGroup.DEBUG_MODE,
            "Apply",
            { StockReviewAction.debugApply() },
            StockReviewStyle.SAVE_BUTTON,
            "Apply the color without closing the debug menu.",
        )

        private val COLOR_RESTORE = footer(
            "color-restore",
            StockReviewActionGroup.DEBUG_MODE,
            "Restore",
            { StockReviewAction.debugRestore() },
            StockReviewStyle.LOAD_BUTTON,
            "Restore the selected color to its default value.",
        )

        private val COLOR_CANCEL = footer(
            "color-cancel",
            StockReviewActionGroup.NAVIGATION,
            "Cancel",
            { StockReviewAction.goBack() },
            StockReviewStyle.CANCEL_BUTTON,
            "Return without applying additional changes.",
        )

        private val FILTER_CONFIRM = footer(
            "filter-confirm",
            StockReviewActionGroup.NAVIGATION,
            "Confirm",
            { StockReviewAction.goBack() },
            StockReviewStyle.CONFIRM_BUTTON,
            "Return to the trade screen with the current filters.",
        )

        private val FILTER_RESET = footer(
            "filter-reset",
            StockReviewActionGroup.FILTERS,
            "Reset",
            { StockReviewAction.resetFilters() },
            StockReviewStyle.LOAD_BUTTON,
            "Clear every active filter.",
        )

        private val FILTER_CANCEL = footer(
            "filter-cancel",
            StockReviewActionGroup.NAVIGATION,
            "Cancel",
            { StockReviewAction.goBack() },
            StockReviewStyle.CANCEL_BUTTON,
            "Return to the trade screen.",
        )

        private val GO_BACK = footer(
            "go-back",
            StockReviewActionGroup.NAVIGATION,
            "Go Back",
            { StockReviewAction.goBack() },
            StockReviewStyle.CANCEL_BUTTON,
            "Return to the trade screen.",
        )

        private val REVIEW_CONFIRM = StockReviewButtonDefinition(
            "review-confirm",
            StockReviewActionGroup.CONFIRMED_EXECUTION,
            StockReviewStyle.FOOTER_BUTTON_WIDTH,
            { "Confirm Trades" },
            { StockReviewAction.confirmPurchase() },
            { context: StockReviewFooterContext -> context.hasPendingTrades() && context.tradeContext.canConfirm() },
            { StockReviewStyle.CONFIRM_BUTTON },
            { "Execute the queued buys and sells." },
        )

        private val REVIEW_BACK = footer(
            "review-back",
            StockReviewActionGroup.NAVIGATION,
            "Go Back",
            { StockReviewAction.goBack() },
            StockReviewStyle.CANCEL_BUTTON,
            "Return to the trade screen without executing trades.",
        )

        private val TRADE_REVIEW = StockReviewButtonDefinition(
            "trade-review",
            StockReviewActionGroup.NAVIGATION,
            StockReviewStyle.FOOTER_BUTTON_WIDTH,
            { "Review Trades" },
            { StockReviewAction.reviewPurchase() },
            { context: StockReviewFooterContext -> context.hasPendingTrades() },
            { StockReviewStyle.CONFIRM_BUTTON },
            { "Review the queued trades before confirming them." },
        )

        private val PURCHASE_ALL = bulk(
            "purchase-all-until-sufficient",
            StockReviewActionGroup.BULK_SUFFICIENT_PURCHASE,
            "Purchase All Until Sufficient",
            { StockReviewAction.purchaseAllUntilSufficient() },
            StockReviewStyle.BUY_BUTTON,
            { StockReviewTooltips.purchaseAllUntilSufficient() },
        )

        private val SELL_ALL = bulk(
            "sell-all-until-sufficient",
            StockReviewActionGroup.BULK_SUFFICIENT_SALE,
            "Sell All Until Sufficient",
            { StockReviewAction.sellAllUntilSufficient() },
            StockReviewStyle.SELL_BUTTON,
            { StockReviewTooltips.sellAllUntilSufficient() },
        )

        private val RESET_ALL = StockReviewButtonDefinition(
            "reset-all-trades",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.RESET_ALL_BUTTON_WIDTH,
            { "Reset All Trades" },
            { StockReviewAction.resetAllTrades() },
            { context: StockReviewFooterContext -> context.hasPendingTrades() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Clear every queued buy and sell." },
        )

        @JvmStatic
        fun left(
            kind: StockReviewFooterButtonSetKind,
            context: StockReviewFooterContext,
            factory: StockReviewActionButtonFactory,
        ): List<WimGuiButtonSpec<StockReviewAction>> =
            buttonSet(kind).leftButtons(context, factory)

        @JvmStatic
        fun right(
            kind: StockReviewFooterButtonSetKind,
            context: StockReviewFooterContext,
            factory: StockReviewActionButtonFactory,
        ): WimGuiButtonSpec<StockReviewAction>? =
            buttonSet(kind).rightButton(context, factory)

        private fun buttonSet(kind: StockReviewFooterButtonSetKind): StockReviewFooterButtonSet =
            when (kind) {
                StockReviewFooterButtonSetKind.COLOR_DEBUG -> COLOR_DEBUG_SET
                StockReviewFooterButtonSetKind.FILTERS -> FILTERS_SET
                StockReviewFooterButtonSetKind.SHIP_CATALOG_DEBUG -> SHIP_CATALOG_DEBUG_SET
                StockReviewFooterButtonSetKind.REVIEW -> REVIEW_SET
                StockReviewFooterButtonSetKind.TRADE -> TRADE_SET
            }

        private val COLOR_DEBUG_SET = StockReviewFooterButtonSet(COLOR_CANCEL, COLOR_CONFIRM, COLOR_APPLY, COLOR_RESTORE)
        private val FILTERS_SET = StockReviewFooterButtonSet(FILTER_CANCEL, FILTER_CONFIRM, FILTER_RESET)
        private val SHIP_CATALOG_DEBUG_SET = StockReviewFooterButtonSet(null, GO_BACK)
        private val REVIEW_SET = StockReviewFooterButtonSet(REVIEW_BACK, REVIEW_CONFIRM)
        private val TRADE_SET = StockReviewFooterButtonSet(null, TRADE_REVIEW, PURCHASE_ALL, SELL_ALL, RESET_ALL)

        private fun footer(
            id: String,
            group: StockReviewActionGroup,
            label: String,
            action: (StockReviewFooterContext) -> StockReviewAction,
            fill: java.awt.Color,
            tooltip: String,
        ): StockReviewButtonDefinition<StockReviewFooterContext> =
            StockReviewButtonDefinition.alwaysEnabled(
                id,
                group,
                StockReviewStyle.FOOTER_BUTTON_WIDTH,
                { label },
                action,
                { fill },
                { tooltip },
            )

        private fun bulk(
            id: String,
            group: StockReviewActionGroup,
            label: String,
            action: (StockReviewFooterContext) -> StockReviewAction,
            fill: java.awt.Color,
            tooltip: (StockReviewFooterContext) -> String,
        ): StockReviewButtonDefinition<StockReviewFooterContext> =
            StockReviewButtonDefinition.alwaysEnabled(
                id,
                group,
                StockReviewStyle.BULK_BUTTON_WIDTH,
                { label },
                action,
                { fill },
                tooltip,
            )

    }
}

class StockReviewFooterButtonSet(
    private val rightDefinition: StockReviewButtonDefinition<StockReviewFooterContext>?,
    private vararg val leftDefinitions: StockReviewButtonDefinition<StockReviewFooterContext>,
) {
    fun leftButtons(
        context: StockReviewFooterContext,
        factory: StockReviewActionButtonFactory,
    ): List<WimGuiButtonSpec<StockReviewAction>> =
        leftDefinitions.map { it.build(context, factory) }

    fun rightButton(
        context: StockReviewFooterContext,
        factory: StockReviewActionButtonFactory,
    ): WimGuiButtonSpec<StockReviewAction>? =
        rightDefinition?.build(context, factory)
}
