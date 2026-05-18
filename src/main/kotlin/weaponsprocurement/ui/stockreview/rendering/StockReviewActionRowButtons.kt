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
import java.awt.Color
import java.util.ArrayList

class StockReviewActionRowContext(
    @JvmField val snapshot: WeaponStockSnapshot,
    @JvmField val state: StockReviewState,
)

object StockReviewActionRowButtonPolicies {
    @JvmStatic
    fun tradeKindLabel(context: StockReviewActionRowContext): String = "Trade: ${context.state.getTradeKind().label}"

    @JvmStatic
    fun sortLabel(context: StockReviewActionRowContext): String = "Sort: ${context.snapshot.getSortMode().label}"

    @JvmStatic
    fun sourceLabel(context: StockReviewActionRowContext): String =
        if (context.state.isShipTrading()) "Source: Local" else "Source: ${context.snapshot.getSourceMode().label}"

    @JvmStatic
    fun blackMarketLabel(context: StockReviewActionRowContext): String =
        "Black Market: ${StockReviewUiText.onOff(context.state.isIncludeBlackMarket())}"

    @JvmStatic
    fun filtersLabel(context: StockReviewActionRowContext): String =
        if (context.state.isShipTrading()) "Filters: ${context.state.getActiveShipFilterCount()}" else "Filters: ${context.state.getActiveFilterCount()}"

    @JvmStatic
    fun alwaysEnabled(context: StockReviewActionRowContext): Boolean = true

    @JvmStatic
    fun blackMarketEnabled(context: StockReviewActionRowContext): Boolean =
        context.state.isShipTrading() || context.snapshot.getSourceMode().supportsBlackMarketToggle()

    @JvmStatic
    fun actionBackground(context: StockReviewActionRowContext): Color = StockReviewStyle.ACTION_BACKGROUND

    @JvmStatic
    fun cycleSortMode(context: StockReviewActionRowContext): StockReviewAction = StockReviewAction.cycleSortMode()

    @JvmStatic
    fun toggleTradeKind(context: StockReviewActionRowContext): StockReviewAction = StockReviewAction.toggleTradeKind()

    @JvmStatic
    fun cycleSourceMode(context: StockReviewActionRowContext): StockReviewAction = StockReviewAction.cycleSourceMode()

    @JvmStatic
    fun toggleBlackMarket(context: StockReviewActionRowContext): StockReviewAction = StockReviewAction.toggleBlackMarket()

    @JvmStatic
    fun openFilters(context: StockReviewActionRowContext): StockReviewAction = StockReviewAction.openFilters()

    @JvmStatic
    fun sortTooltip(context: StockReviewActionRowContext): String = StockReviewTooltips.sort(context.snapshot.getSortMode())

    @JvmStatic
    fun tradeKindTooltip(context: StockReviewActionRowContext): String = "Switch between item trading and local ship trading."

    @JvmStatic
    fun sourceTooltip(context: StockReviewActionRowContext): String =
        if (context.state.isShipTrading()) "Ship trading currently uses only the local market." else StockReviewTooltips.source(context.snapshot.getSourceMode())

    @JvmStatic
    fun blackMarketTooltip(context: StockReviewActionRowContext): String =
        "Include black-market stock for Local and Sector Market source modes. Fixer's Market controls its own virtual stock."

    @JvmStatic
    fun filtersTooltip(context: StockReviewActionRowContext): String =
        if (context.state.isShipTrading()) "Open the ship filter list." else "Open the weapon and fighter filter list."
}

class StockReviewActionRowButtons private constructor() {
    companion object {
        internal val TRADE_KIND = StockReviewButtonDefinition.dynamic(
            "trade-kind",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            140f,
            StockReviewActionRowButtonPolicies::tradeKindLabel,
            StockReviewActionRowButtonPolicies::toggleTradeKind,
            StockReviewActionRowButtonPolicies::alwaysEnabled,
            StockReviewActionRowButtonPolicies::actionBackground,
            StockReviewActionRowButtonPolicies::tradeKindTooltip,
        )

        internal val SORT = StockReviewButtonDefinition.dynamic(
            "sort",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.SORT_BUTTON_WIDTH,
            StockReviewActionRowButtonPolicies::sortLabel,
            StockReviewActionRowButtonPolicies::cycleSortMode,
            StockReviewActionRowButtonPolicies::alwaysEnabled,
            StockReviewActionRowButtonPolicies::actionBackground,
            StockReviewActionRowButtonPolicies::sortTooltip,
        )

        internal val SOURCE = StockReviewButtonDefinition.dynamic(
            "source",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.SOURCE_BUTTON_WIDTH,
            StockReviewActionRowButtonPolicies::sourceLabel,
            StockReviewActionRowButtonPolicies::cycleSourceMode,
            StockReviewActionRowButtonPolicies::alwaysEnabled,
            StockReviewActionRowButtonPolicies::actionBackground,
            StockReviewActionRowButtonPolicies::sourceTooltip,
        )

        internal val BLACK_MARKET = StockReviewButtonDefinition.dynamic(
            "black-market",
            StockReviewActionGroup.SOURCE_TRANSITIONS,
            StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH,
            StockReviewActionRowButtonPolicies::blackMarketLabel,
            StockReviewActionRowButtonPolicies::toggleBlackMarket,
            StockReviewActionRowButtonPolicies::blackMarketEnabled,
            StockReviewActionRowButtonPolicies::actionBackground,
            StockReviewActionRowButtonPolicies::blackMarketTooltip,
        )

        internal val FILTERS = StockReviewButtonDefinition.dynamic(
            "filters",
            StockReviewActionGroup.FILTERS,
            StockReviewStyle.FILTER_BUTTON_WIDTH,
            StockReviewActionRowButtonPolicies::filtersLabel,
            StockReviewActionRowButtonPolicies::openFilters,
            StockReviewActionRowButtonPolicies::alwaysEnabled,
            StockReviewActionRowButtonPolicies::actionBackground,
            StockReviewActionRowButtonPolicies::filtersTooltip,
        )

        internal val COLORS = StockReviewButtonDefinition.static<StockReviewActionRowContext>(
            "colors",
            StockReviewActionGroup.DEBUG_MODE,
            StockReviewStyle.COLOR_BUTTON_WIDTH,
            "Colors",
            StockReviewAction.openColorDebug(),
            StockReviewStyle.ACTION_BACKGROUND,
            "Open the color debug menu.",
        )

        internal val SHIPS = StockReviewButtonDefinition.static<StockReviewActionRowContext>(
            "ships",
            StockReviewActionGroup.DEBUG_MODE,
            StockReviewStyle.COLOR_BUTTON_WIDTH,
            "Ships",
            StockReviewAction.openShipCatalogDebug(),
            StockReviewStyle.ACTION_BACKGROUND,
            "Open the developer-only Fixer ship catalog diagnostic view.",
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
                StockReviewActionRowButtons.TRADE_KIND,
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
