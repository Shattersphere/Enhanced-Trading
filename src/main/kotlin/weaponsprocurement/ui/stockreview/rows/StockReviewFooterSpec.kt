package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiModalLayout
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionButtonFactory
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext

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
            { context -> StockReviewFooterButtons.colorDebugLeft(context, BUTTON_FACTORY) },
            { context -> StockReviewFooterButtons.colorDebugRight(context, BUTTON_FACTORY) },
        )

        @JvmStatic
        fun filters(): StockReviewFooterSpec = leftAndRight(
            StockReviewStyle.FILTER_MODAL,
            { context -> StockReviewFooterButtons.filtersLeft(context, BUTTON_FACTORY) },
            { context -> StockReviewFooterButtons.filtersRight(context, BUTTON_FACTORY) },
        )

        @JvmStatic
        fun shipCatalogDebug(): StockReviewFooterSpec = leftRow(
            StockReviewStyle.MODAL,
            { context -> StockReviewFooterButtons.shipCatalogDebugLeft(context, BUTTON_FACTORY) },
        )

        @JvmStatic
        fun review(): StockReviewFooterSpec = leftAndRight(
            StockReviewStyle.REVIEW_MODAL,
            { context -> StockReviewFooterButtons.reviewLeft(context, BUTTON_FACTORY) },
            { context -> StockReviewFooterButtons.reviewRight(context, BUTTON_FACTORY) },
        )

        @JvmStatic
        fun trade(): StockReviewFooterSpec = leftRow(
            StockReviewStyle.MODAL,
            { context -> StockReviewFooterButtons.tradeLeft(context, BUTTON_FACTORY) },
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

    }
}
