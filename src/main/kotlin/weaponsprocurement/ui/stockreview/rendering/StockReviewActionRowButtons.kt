package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionButtonFactory
import weaponsprocurement.ui.stockreview.controls.StockReviewButtonDefinition
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import java.util.ArrayList

class StockReviewActionRowContext(
    @JvmField val snapshot: WeaponStockSnapshot,
    @JvmField val state: StockReviewState,
)

class StockReviewActionRowButtons private constructor() {
    companion object {
        private val SORT = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "sort",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.SORT_BUTTON_WIDTH,
            { context -> "Sort: ${context.snapshot.getSortMode().label}" },
            { StockReviewAction.cycleSortMode() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { context -> StockReviewTooltips.sort(context.snapshot.getSortMode()) },
        )

        private val SOURCE = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "source",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.SOURCE_BUTTON_WIDTH,
            { context -> "Source: ${context.snapshot.getSourceMode().label}" },
            { StockReviewAction.cycleSourceMode() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { context -> StockReviewTooltips.source(context.snapshot.getSourceMode()) },
        )

        private val BLACK_MARKET = StockReviewButtonDefinition<StockReviewActionRowContext>(
            "black-market",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH,
            { context -> "Black Market: ${StockReviewUiText.onOff(context.snapshot.isIncludeBlackMarket())}" },
            { StockReviewAction.toggleBlackMarket() },
            { context -> context.snapshot.getSourceMode().supportsBlackMarketToggle() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Include black-market stock for Local and Sector Market source modes. Fixer's Market controls its own virtual stock." },
        )

        private val FILTERS = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "filters",
            StockReviewActionGroup.FILTERS,
            StockReviewStyle.FILTER_BUTTON_WIDTH,
            { context -> "Filters: ${context.state.getActiveFilterCount()}" },
            { StockReviewAction.openFilters() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Open the weapon filter list." },
        )

        private val COLORS = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "colors",
            StockReviewActionGroup.DEBUG_MODE,
            StockReviewStyle.COLOR_BUTTON_WIDTH,
            { "Colors" },
            { StockReviewAction.openColorDebug() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Open the color debug menu." },
        )

        private val SHIPS = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "ships",
            StockReviewActionGroup.DEBUG_MODE,
            StockReviewStyle.COLOR_BUTTON_WIDTH,
            { "Ships" },
            { StockReviewAction.openShipCatalogDebug() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Open the developer-only Fixer ship catalog diagnostic view." },
        )

        @JvmStatic
        fun build(
            context: StockReviewActionRowContext,
            factory: StockReviewActionButtonFactory,
        ): List<WimGuiButtonSpec<StockReviewAction>> {
            val buttons = ArrayList<WimGuiButtonSpec<StockReviewAction>>()
            buttons.add(SORT.build(context, factory))
            buttons.add(SOURCE.build(context, factory))
            buttons.add(BLACK_MARKET.build(context, factory))
            buttons.add(FILTERS.build(context, factory))
            buttons.add(COLORS.build(context, factory))
            if (WeaponsProcurementConfig.isDebugShipCatalogViewEnabled()) {
                buttons.add(SHIPS.build(context, factory))
            }
            return buttons
        }
    }
}
