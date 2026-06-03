package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiModalActionRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionButtonFactory
import weaponsprocurement.ui.stockreview.ships.StockReviewShipHullFilterInput
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.stock.item.WeaponStockSnapshot
import com.fs.starfarer.api.ui.CustomPanelAPI

class StockReviewActionRowRenderer private constructor() {
    companion object {
        private val BUTTON_FACTORY = StockReviewActionButtonFactory(StockReviewStyle.ROW_BORDER)

        @JvmStatic
        fun render(
            root: CustomPanelAPI,
            snapshot: WeaponStockSnapshot,
            state: StockReviewState,
            modeSpec: StockReviewModeSpec,
            itemSearchFocused: Boolean,
            shipHullFilterFocused: Boolean,
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
        ) {
            if (modeSpec.actionRowKind != StockReviewActionRowKind.TRADE_CONTROLS) {
                return
            }
            val actionButtons = StockReviewActionRowButtons.build(
                modeSpec.actionRowKind,
                StockReviewActionRowContext(snapshot, state),
                BUTTON_FACTORY,
            )
            if (actionButtons.isNotEmpty()) {
                WimGuiModalActionRow.add(
                    root,
                    modeSpec.modal,
                    0f,
                    0f,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    actionButtons,
                    buttons,
                )
            }
            if (state.isShipTrading()) {
                StockReviewShipHullFilterInput.render(root, state, shipHullFilterFocused, buttons)
            } else {
                StockReviewItemSearchInput.render(root, state, itemSearchFocused, buttons)
            }
        }
    }
}
