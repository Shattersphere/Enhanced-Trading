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

enum class StockReviewFooterButtonSetKind {
    COLOR_DEBUG,
    FILTERS,
    SHIP_CATALOG_DEBUG,
    REVIEW,
    TRADE,
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
    @JvmField val buttonSetKind: StockReviewFooterButtonSetKind,
) {
    fun leftButtons(context: StockReviewFooterContext): List<WimGuiButtonSpec<StockReviewAction>> =
        StockReviewFooterButtons.left(buttonSetKind, context, BUTTON_FACTORY)

    fun rightButton(context: StockReviewFooterContext): WimGuiButtonSpec<StockReviewAction>? =
        StockReviewFooterButtons.right(buttonSetKind, context, BUTTON_FACTORY)

    companion object {
        private val BUTTON_FACTORY = StockReviewActionButtonFactory(StockReviewStyle.ROW_BORDER)

        @JvmStatic
        fun colorDebug(): StockReviewFooterSpec =
            leftAndRight(StockReviewStyle.MODAL, StockReviewFooterButtonSetKind.COLOR_DEBUG)

        @JvmStatic
        fun filters(): StockReviewFooterSpec =
            leftAndRight(StockReviewStyle.FILTER_MODAL, StockReviewFooterButtonSetKind.FILTERS)

        @JvmStatic
        fun shipCatalogDebug(): StockReviewFooterSpec =
            leftRow(StockReviewStyle.MODAL, StockReviewFooterButtonSetKind.SHIP_CATALOG_DEBUG)

        @JvmStatic
        fun review(): StockReviewFooterSpec =
            leftAndRight(StockReviewStyle.REVIEW_MODAL, StockReviewFooterButtonSetKind.REVIEW)

        @JvmStatic
        fun trade(): StockReviewFooterSpec =
            leftRow(StockReviewStyle.MODAL, StockReviewFooterButtonSetKind.TRADE)

        private fun leftRow(
            modal: WimGuiModalLayout,
            buttonSetKind: StockReviewFooterButtonSetKind,
        ): StockReviewFooterSpec = StockReviewFooterSpec(
            modal,
            StockReviewFooterLayoutKind.LEFT_BUTTON_ROW,
            buttonSetKind,
        )

        private fun leftAndRight(
            modal: WimGuiModalLayout,
            buttonSetKind: StockReviewFooterButtonSetKind,
        ): StockReviewFooterSpec = StockReviewFooterSpec(
            modal,
            StockReviewFooterLayoutKind.LEFT_ROW_AND_RIGHT_BUTTON,
            buttonSetKind,
        )

    }
}
