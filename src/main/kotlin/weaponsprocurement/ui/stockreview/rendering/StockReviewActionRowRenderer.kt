package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiButtonSpecs
import weaponsprocurement.ui.WimGuiModalActionRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionButtonFactory
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
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
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
        ) {
            if (modeSpec.actionRowKind != StockReviewActionRowKind.TRADE_CONTROLS) {
                return
            }
            WimGuiModalActionRow.add(
                root,
                modeSpec.modal,
                0f,
                0f,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.BUTTON_GAP,
                WimGuiButtonSpecs.of(
                    BUTTON_FACTORY.enabledButton(
                        StockReviewActionGroup.SOURCE_TRANSITIONS,
                        StockReviewStyle.SORT_BUTTON_WIDTH,
                        "Sort: ${snapshot.getSortMode().label}",
                        StockReviewAction.cycleSortMode(),
                        StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewTooltips.sort(snapshot.getSortMode()),
                    ),
                    BUTTON_FACTORY.enabledButton(
                        StockReviewActionGroup.SOURCE_TRANSITIONS,
                        StockReviewStyle.SOURCE_BUTTON_WIDTH,
                        "Source: ${snapshot.getSourceMode().label}",
                        StockReviewAction.cycleSourceMode(),
                        StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewTooltips.source(snapshot.getSourceMode()),
                    ),
                    BUTTON_FACTORY.button(
                        StockReviewActionGroup.SOURCE_TRANSITIONS,
                        StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH,
                        "Black Market: ${StockReviewUiText.onOff(snapshot.isIncludeBlackMarket())}",
                        StockReviewAction.toggleBlackMarket(),
                        snapshot.getSourceMode().supportsBlackMarketToggle(),
                        StockReviewStyle.ACTION_BACKGROUND,
                        "Include black-market stock for Local and Sector Market source modes. Fixer's Market controls its own virtual stock.",
                    ),
                    BUTTON_FACTORY.enabledButton(
                        StockReviewActionGroup.FILTERS,
                        StockReviewStyle.FILTER_BUTTON_WIDTH,
                        "Filters: ${state.getActiveFilterCount()}",
                        StockReviewAction.openFilters(),
                        StockReviewStyle.ACTION_BACKGROUND,
                        "Open the weapon filter list.",
                    ),
                    BUTTON_FACTORY.enabledButton(
                        StockReviewActionGroup.DEBUG_MODE,
                        StockReviewStyle.COLOR_BUTTON_WIDTH,
                        "Colors",
                        StockReviewAction.openColorDebug(),
                        StockReviewStyle.ACTION_BACKGROUND,
                        "Open the color debug menu.",
                    ),
                    if (WeaponsProcurementConfig.isDebugShipCatalogViewEnabled()) {
                        BUTTON_FACTORY.enabledButton(
                            StockReviewActionGroup.DEBUG_MODE,
                            StockReviewStyle.COLOR_BUTTON_WIDTH,
                            "Ships",
                            StockReviewAction.openShipCatalogDebug(),
                            StockReviewStyle.ACTION_BACKGROUND,
                            "Open the developer-only Fixer ship catalog diagnostic view.",
                        )
                    } else {
                        null
                    },
                ),
                buttons,
            )
        }
    }
}
