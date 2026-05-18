package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiModalFooter
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewModeSpec
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrade
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.state.StockReviewTradeKind
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import com.fs.starfarer.api.ui.CustomPanelAPI

class StockReviewFooterRenderer private constructor() {
    companion object {
        @JvmStatic
        fun render(
            root: CustomPanelAPI,
            tradeContext: StockReviewTradeContext,
            state: StockReviewState,
            pendingTrades: List<StockReviewPendingTrade>?,
            pendingShipTrades: List<StockReviewPendingShipTrade>?,
            modeSpec: StockReviewModeSpec,
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
        ) {
            val footerSpec = modeSpec.footerSpec
            val context = StockReviewFooterContext(tradeContext, pendingTrades, pendingShipTrades, state.getTradeKind())
            when (footerSpec.layoutKind) {
                StockReviewFooterLayoutKind.LEFT_BUTTON_ROW -> WimGuiModalFooter.addLeftButtonRow(
                    root,
                    footerSpec.modal,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    footerSpec.leftButtons(context),
                    buttons,
                )
                StockReviewFooterLayoutKind.LEFT_ROW_AND_RIGHT_BUTTON -> WimGuiModalFooter.addLeftRowAndRightButton(
                    root,
                    footerSpec.modal,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    footerSpec.leftButtons(context),
                    footerSpec.rightButton(context) ?: return,
                    buttons,
                )
            }
        }
    }
}
