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
        internal val SORT = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "sort",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.SORT_BUTTON_WIDTH,
            { context -> "Sort: ${context.snapshot.getSortMode().label}" },
            { StockReviewAction.cycleSortMode() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { context -> StockReviewTooltips.sort(context.snapshot.getSortMode()) },
        )

        internal val SOURCE = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "source",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.SOURCE_BUTTON_WIDTH,
            { context -> "Source: ${context.snapshot.getSourceMode().label}" },
            { StockReviewAction.cycleSourceMode() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { context -> StockReviewTooltips.source(context.snapshot.getSourceMode()) },
        )

        internal val BLACK_MARKET = StockReviewButtonDefinition<StockReviewActionRowContext>(
            "black-market",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH,
            { context -> "Black Market: ${StockReviewUiText.onOff(context.snapshot.isIncludeBlackMarket())}" },
            { StockReviewAction.toggleBlackMarket() },
            { context -> context.snapshot.getSourceMode().supportsBlackMarketToggle() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Include black-market stock for Local and Sector Market source modes. Fixer's Market controls its own virtual stock." },
        )

        internal val FILTERS = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "filters",
            StockReviewActionGroup.FILTERS,
            StockReviewStyle.FILTER_BUTTON_WIDTH,
            { context -> "Filters: ${context.state.getActiveFilterCount()}" },
            { StockReviewAction.openFilters() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Open the weapon filter list." },
        )

        internal val COLORS = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
            "colors",
            StockReviewActionGroup.DEBUG_MODE,
            StockReviewStyle.COLOR_BUTTON_WIDTH,
            { "Colors" },
            { StockReviewAction.openColorDebug() },
            { StockReviewStyle.ACTION_BACKGROUND },
            { "Open the color debug menu." },
        )

        internal val SHIPS = StockReviewButtonDefinition.alwaysEnabled<StockReviewActionRowContext>(
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
            kind: StockReviewActionRowKind,
            context: StockReviewActionRowContext,
            factory: StockReviewActionButtonFactory,
        ): List<WimGuiButtonSpec<StockReviewAction>> =
            buttonSet(kind).build(context, factory)

        private fun buttonSet(kind: StockReviewActionRowKind): StockReviewActionRowButtonSet =
            when (kind) {
                StockReviewActionRowKind.NONE -> StockReviewActionRowButtonSet.EMPTY
                StockReviewActionRowKind.TRADE_CONTROLS -> StockReviewActionRowButtonSet.TRADE_CONTROLS
            }
    }
}

class StockReviewActionRowButtonSet private constructor(
    private val definitions: List<StockReviewButtonDefinition<StockReviewActionRowContext>>,
    private val debugDefinitions: List<StockReviewButtonDefinition<StockReviewActionRowContext>>,
) {
    fun build(
        context: StockReviewActionRowContext,
        factory: StockReviewActionButtonFactory,
    ): List<WimGuiButtonSpec<StockReviewAction>> {
        val buttons = ArrayList<WimGuiButtonSpec<StockReviewAction>>()
        for (definition in definitions) {
            buttons.add(definition.build(context, factory))
        }
        if (WeaponsProcurementConfig.isDebugShipCatalogViewEnabled()) {
            for (definition in debugDefinitions) {
                buttons.add(definition.build(context, factory))
            }
        }
        return buttons
    }

    companion object {
        @JvmField val EMPTY = StockReviewActionRowButtonSet(emptyList(), emptyList())
        @JvmField val TRADE_CONTROLS = StockReviewActionRowButtonSet(
            listOf(
                StockReviewActionRowButtons.SORT,
                StockReviewActionRowButtons.SOURCE,
                StockReviewActionRowButtons.BLACK_MARKET,
                StockReviewActionRowButtons.FILTERS,
                StockReviewActionRowButtons.COLORS,
            ),
            listOf(StockReviewActionRowButtons.SHIPS),
        )
    }
}
