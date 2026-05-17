package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiButtonSpecs
import weaponsprocurement.ui.WimGuiModalLayout
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionButtonFactory
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import java.awt.Color

enum class StockReviewFooterLayoutKind {
    LEFT_BUTTON_ROW,
    LEFT_ROW_AND_RIGHT_BUTTON,
}

class StockReviewFooterContext(
    @JvmField val tradeContext: StockReviewTradeContext,
    @JvmField val pendingTrades: List<StockReviewPendingTrade>?,
) {
    fun hasPendingTrades(): Boolean = !pendingTrades.isNullOrEmpty()
}

class StockReviewFooterSpec private constructor(
    @JvmField val modal: WimGuiModalLayout,
    @JvmField val layoutKind: StockReviewFooterLayoutKind,
    private val leftButtons: (StockReviewFooterContext) -> List<WimGuiButtonSpec<StockReviewAction>>,
    private val rightButton: ((StockReviewFooterContext) -> WimGuiButtonSpec<StockReviewAction>)?,
) {
    fun leftButtons(context: StockReviewFooterContext): List<WimGuiButtonSpec<StockReviewAction>> = leftButtons.invoke(context)

    fun rightButton(context: StockReviewFooterContext): WimGuiButtonSpec<StockReviewAction>? = rightButton?.invoke(context)

    companion object {
        private val BUTTON_FACTORY = StockReviewActionButtonFactory(StockReviewStyle.ROW_BORDER)

        @JvmStatic
        fun colorDebug(): StockReviewFooterSpec = leftAndRight(
            StockReviewStyle.MODAL,
            {
                WimGuiButtonSpecs.of(
                    footerButton(StockReviewActionGroup.DEBUG_MODE, "Confirm", StockReviewAction.debugConfirm(), true, StockReviewStyle.CONFIRM_BUTTON, "Apply the color and return to the trade screen."),
                    footerButton(StockReviewActionGroup.DEBUG_MODE, "Apply", StockReviewAction.debugApply(), true, StockReviewStyle.SAVE_BUTTON, "Apply the color without closing the debug menu."),
                    footerButton(StockReviewActionGroup.DEBUG_MODE, "Restore", StockReviewAction.debugRestore(), true, StockReviewStyle.LOAD_BUTTON, "Restore the selected color to its default value."),
                )
            },
            {
                footerButton(StockReviewActionGroup.NAVIGATION, "Cancel", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON, "Return without applying additional changes.")
            },
        )

        @JvmStatic
        fun filters(): StockReviewFooterSpec = leftAndRight(
            StockReviewStyle.FILTER_MODAL,
            {
                WimGuiButtonSpecs.of(
                    footerButton(StockReviewActionGroup.NAVIGATION, "Confirm", StockReviewAction.goBack(), true, StockReviewStyle.CONFIRM_BUTTON, "Return to the trade screen with the current filters."),
                    footerButton(StockReviewActionGroup.FILTERS, "Reset", StockReviewAction.resetFilters(), true, StockReviewStyle.LOAD_BUTTON, "Clear every active filter."),
                )
            },
            {
                footerButton(StockReviewActionGroup.NAVIGATION, "Cancel", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON, "Return to the trade screen.")
            },
        )

        @JvmStatic
        fun shipCatalogDebug(): StockReviewFooterSpec = leftRow(
            StockReviewStyle.MODAL,
            {
                WimGuiButtonSpecs.of(
                    footerButton(StockReviewActionGroup.NAVIGATION, "Go Back", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON, "Return to the trade screen."),
                )
            },
        )

        @JvmStatic
        fun review(): StockReviewFooterSpec = leftAndRight(
            StockReviewStyle.REVIEW_MODAL,
            { context ->
                WimGuiButtonSpecs.of(
                    footerButton(
                        StockReviewActionGroup.CONFIRMED_EXECUTION,
                        "Confirm Trades",
                        StockReviewAction.confirmPurchase(),
                        context.hasPendingTrades() && context.tradeContext.canConfirm(),
                        StockReviewStyle.CONFIRM_BUTTON,
                        "Execute the queued buys and sells.",
                    ),
                )
            },
            {
                footerButton(StockReviewActionGroup.NAVIGATION, "Go Back", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON, "Return to the trade screen without executing trades.")
            },
        )

        @JvmStatic
        fun trade(): StockReviewFooterSpec = leftRow(
            StockReviewStyle.MODAL,
            { context ->
                WimGuiButtonSpecs.of(
                    footerButton(StockReviewActionGroup.NAVIGATION, "Review Trades", StockReviewAction.reviewPurchase(), context.hasPendingTrades(), StockReviewStyle.CONFIRM_BUTTON, "Review the queued trades before confirming them."),
                    bulkButton(StockReviewActionGroup.BULK_SUFFICIENT_PURCHASE, "Purchase All Until Sufficient", StockReviewAction.purchaseAllUntilSufficient(), true, StockReviewStyle.BUY_BUTTON, StockReviewTooltips.purchaseAllUntilSufficient()),
                    bulkButton(StockReviewActionGroup.BULK_SUFFICIENT_SALE, "Sell All Until Sufficient", StockReviewAction.sellAllUntilSufficient(), true, StockReviewStyle.SELL_BUTTON, StockReviewTooltips.sellAllUntilSufficient()),
                    BUTTON_FACTORY.button(StockReviewActionGroup.SOURCE_TRANSITIONS, StockReviewStyle.RESET_ALL_BUTTON_WIDTH, "Reset All Trades", StockReviewAction.resetAllTrades(), context.hasPendingTrades(), StockReviewStyle.ACTION_BACKGROUND, "Clear every queued buy and sell."),
                )
            },
        )

        private fun leftRow(
            modal: WimGuiModalLayout,
            leftButtons: (StockReviewFooterContext) -> List<WimGuiButtonSpec<StockReviewAction>>,
        ): StockReviewFooterSpec = StockReviewFooterSpec(
            modal,
            StockReviewFooterLayoutKind.LEFT_BUTTON_ROW,
            leftButtons,
            null,
        )

        private fun leftAndRight(
            modal: WimGuiModalLayout,
            leftButtons: (StockReviewFooterContext) -> List<WimGuiButtonSpec<StockReviewAction>>,
            rightButton: (StockReviewFooterContext) -> WimGuiButtonSpec<StockReviewAction>,
        ): StockReviewFooterSpec = StockReviewFooterSpec(
            modal,
            StockReviewFooterLayoutKind.LEFT_ROW_AND_RIGHT_BUTTON,
            leftButtons,
            rightButton,
        )

        private fun footerButton(group: StockReviewActionGroup, label: String, action: StockReviewAction, enabled: Boolean, fill: Color, tooltip: String): WimGuiButtonSpec<StockReviewAction> =
            BUTTON_FACTORY.button(group, StockReviewStyle.FOOTER_BUTTON_WIDTH, label, action, enabled, fill, tooltip)

        private fun bulkButton(group: StockReviewActionGroup, label: String, action: StockReviewAction, enabled: Boolean, fill: Color, tooltip: String): WimGuiButtonSpec<StockReviewAction> =
            BUTTON_FACTORY.button(group, StockReviewStyle.BULK_BUTTON_WIDTH, label, action, enabled, fill, tooltip)
    }
}
