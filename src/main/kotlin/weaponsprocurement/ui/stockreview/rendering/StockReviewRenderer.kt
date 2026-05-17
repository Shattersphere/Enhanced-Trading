package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiButtonSpecs
import weaponsprocurement.ui.WimGuiColorDebug
import weaponsprocurement.ui.WimGuiListBounds
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiModalActionRow
import weaponsprocurement.ui.WimGuiModalHeader
import weaponsprocurement.ui.WimGuiModalListRenderer
import weaponsprocurement.ui.WimGuiModalListSpec
import weaponsprocurement.ui.WimGuiSemanticButtonFactory
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewColorDebugRows
import weaponsprocurement.ui.stockreview.rows.StockReviewFooterRenderer
import weaponsprocurement.ui.stockreview.rows.StockReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewListRow
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewShipCatalogDebugRows
import weaponsprocurement.ui.stockreview.rows.StockReviewTradeSummaryRenderer
import weaponsprocurement.ui.stockreview.rows.StockReviewScreenMode
import weaponsprocurement.ui.stockreview.state.StockReviewFilterListModel
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color

class StockReviewRenderer :
    WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
    WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction> {
    private val buttonFactory = WimGuiSemanticButtonFactory<StockReviewAction>(StockReviewStyle.ROW_BORDER)
    private var cachedModel: RenderModel? = null

    fun invalidateModelCache() {
        cachedModel = null
    }

    fun render(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        pendingTrades: List<StockReviewPendingTrade>,
        pendingTradeRevision: Int,
        modeRevision: Int,
        reviewMode: Boolean,
        filterMode: Boolean,
        colorDebugMode: Boolean,
        shipCatalogDebugMode: Boolean,
        colorDebugTargetIndex: Int,
        colorDebugDraft: Color?,
        colorDebugPersistent: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val modeSpec = StockReviewModeSpec.resolve(reviewMode, filterMode, colorDebugMode, shipCatalogDebugMode)
        if (modeSpec.showHeader) {
            renderHeader(root, snapshot, state, modeSpec, colorDebugTargetIndex, colorDebugDraft)
        }
        if (modeSpec.showTradeActionRow) {
            renderActionRow(root, snapshot, state, buttons)
        }
        val model = renderModel(
            snapshot,
            state,
            pendingTrades,
            pendingTradeRevision,
            modeRevision,
            modeSpec,
            colorDebugTargetIndex,
            colorDebugDraft,
            colorDebugPersistent,
        )
        val result = renderRows(root, model.rows, state, model.listSpec, buttons)
        if (modeSpec.showSummary) {
            StockReviewTradeSummaryRenderer.render(root, model.tradeContext, state, model.rowLayout)
        }
        StockReviewFooterRenderer.render(root, model.tradeContext, pendingTrades, modeSpec, buttons)
        return result
    }

    private fun renderModel(
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        pendingTrades: List<StockReviewPendingTrade>,
        pendingTradeRevision: Int,
        modeRevision: Int,
        modeSpec: StockReviewModeSpec,
        colorDebugTargetIndex: Int,
        colorDebugDraft: Color?,
        colorDebugPersistent: Boolean,
    ): RenderModel {
        val key = RenderModelKey(
            state.getContentRevision(),
            pendingTradeRevision,
            modeRevision,
            modeSpec.screenMode,
            colorDebugTargetIndex,
            colorKey(colorDebugDraft),
            colorDebugPersistent,
        )
        val current = cachedModel
        if (current != null &&
            current.matches(snapshot, key)
        ) {
            return current
        }

        val tradeContext = StockReviewTradeContext(snapshot, pendingTrades)
        val rowLayout = modeSpec.rowLayout
        val rows: List<WimGuiListRow<StockReviewAction>> = when (modeSpec.screenMode) {
            StockReviewScreenMode.COLOR_DEBUG -> StockReviewColorDebugRows.build(colorDebugTargetIndex, colorDebugDraft, colorDebugPersistent)
            StockReviewScreenMode.SHIP_CATALOG_DEBUG -> StockReviewShipCatalogDebugRows.build()
            StockReviewScreenMode.FILTERS -> StockReviewFilterListModel.build(state)
            StockReviewScreenMode.REVIEW -> StockReviewReviewListModel.build(snapshot, pendingTrades, state, tradeContext, rowLayout)
            StockReviewScreenMode.TRADE -> StockReviewListModel.build(snapshot, state, tradeContext, rowLayout)
        }
        val listSpec = modeSpec.listSpec

        val built = RenderModel(snapshot, key, tradeContext, rowLayout, rows, listSpec)
        cachedModel = built
        return built
    }

    private fun renderHeader(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        modeSpec: StockReviewModeSpec,
        colorDebugTargetIndex: Int,
        colorDebugDraft: Color?,
    ) {
        val title = when (modeSpec.screenMode) {
            StockReviewScreenMode.COLOR_DEBUG -> "Debug Colors"
            StockReviewScreenMode.SHIP_CATALOG_DEBUG -> "Ship Catalog Debug"
            StockReviewScreenMode.FILTERS -> "Filters"
            StockReviewScreenMode.REVIEW -> "Review Trades"
            StockReviewScreenMode.TRADE -> "Make Trades"
        }
        val status = when (modeSpec.screenMode) {
            StockReviewScreenMode.COLOR_DEBUG -> colorStatusLine(colorDebugTargetIndex, colorDebugDraft)
            StockReviewScreenMode.SHIP_CATALOG_DEBUG -> "Developer-only Fixer ship catalog candidate view"
            StockReviewScreenMode.FILTERS -> filterStatusLine(state)
            StockReviewScreenMode.REVIEW,
            StockReviewScreenMode.TRADE -> statusLine(snapshot, state)
        }
        WimGuiModalHeader.addTitleStatusHeader(
            root,
            modeSpec.modal,
            StockReviewStyle.HEADER_HEIGHT,
            title,
            status,
            StockReviewStyle.PANEL_BACKGROUND,
            StockReviewStyle.PANEL_BORDER,
            StockReviewStyle.TEXT,
        )
    }

    private fun renderActionRow(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        WimGuiModalActionRow.add(
            root,
            StockReviewStyle.MODAL,
            0f,
            0f,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            StockReviewStyle.BUTTON_GAP,
            WimGuiButtonSpecs.of(
                buttonFactory.enabledButton(
                    StockReviewStyle.SORT_BUTTON_WIDTH,
                    "Sort: ${snapshot.getSortMode().label}",
                    StockReviewAction.cycleSortMode(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    StockReviewTooltips.sort(snapshot.getSortMode()),
                ),
                buttonFactory.enabledButton(
                    StockReviewStyle.SOURCE_BUTTON_WIDTH,
                    "Source: ${snapshot.getSourceMode().label}",
                    StockReviewAction.cycleSourceMode(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    StockReviewTooltips.source(snapshot.getSourceMode()),
                ),
                buttonFactory.button(
                    StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH,
                    "Black Market: ${onOff(snapshot.isIncludeBlackMarket())}",
                    StockReviewAction.toggleBlackMarket(),
                    snapshot.getSourceMode().supportsBlackMarketToggle(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    "Include black-market stock for Local and Sector Market source modes. Fixer's Market controls its own virtual stock.",
                ),
                buttonFactory.enabledButton(
                    StockReviewStyle.FILTER_BUTTON_WIDTH,
                    "Filters: ${state.getActiveFilterCount()}",
                    StockReviewAction.openFilters(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    "Open the weapon filter list.",
                ),
                buttonFactory.enabledButton(
                    StockReviewStyle.COLOR_BUTTON_WIDTH,
                    "Colors",
                    StockReviewAction.openColorDebug(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    "Open the color debug menu.",
                ),
                if (WeaponsProcurementConfig.isDebugShipCatalogViewEnabled()) {
                    buttonFactory.enabledButton(
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

    private fun renderRows(
        root: CustomPanelAPI,
        rows: List<WimGuiListRow<StockReviewAction>>,
        state: StockReviewState,
        spec: WimGuiModalListSpec,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds = WimGuiModalListRenderer.renderAndStoreOffset(
        root,
        rows,
        state,
        spec,
        this,
        this,
        buttons,
    )

    override fun createScrollRow(label: String, scrollDelta: Int): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.scroll(label, StockReviewAction.scrollList(scrollDelta))

    override fun extraGapBefore(row: WimGuiListRow<StockReviewAction>): Float =
        if (row.hasTopGap()) StockReviewStyle.CATEGORY_TOP_GAP else 0f

    companion object {
        private fun statusLine(snapshot: WeaponStockSnapshot, state: StockReviewState): String =
            "Market: ${snapshot.getMarketName()}" +
                " | Sort: ${snapshot.getSortMode().label}" +
                " | Owned source: ${ownedSourceLabel(snapshot)}" +
                " | Stock source: ${sourceLabel(snapshot.getSourceMode())}" +
                " | Black market: ${onOff(snapshot.isIncludeBlackMarket())}" +
                " | Filters: ${state.getActiveFilterCount()}"

        private fun sourceLabel(sourceMode: StockSourceMode?): String = sourceMode?.label ?: "local"

        private fun filterStatusLine(state: StockReviewState): String =
            "Active filters: ${state.getActiveFilterCount()} | Active filter rows are shown first"

        private fun colorStatusLine(targetIndex: Int, draft: Color?): String {
            val target = WimGuiColorDebug.targetAt(targetIndex)
            val color = draft ?: WimGuiColorDebug.currentColor(target)
            return (target?.label ?: "Unknown") + " | RGB(" +
                color.red + ", " +
                color.green + ", " +
                color.blue + ")"
        }

        private fun ownedSourceLabel(snapshot: WeaponStockSnapshot): String {
            if (snapshot.getOwnedSourcePolicy().name.contains("ACCESSIBLE_STORAGE")) {
                return "fleet + all accessible storage"
            }
            if (snapshot.getOwnedSourcePolicy().name.contains("CURRENT_MARKET_STORAGE")) {
                return "fleet + current market storage"
            }
            return "fleet only"
        }

        private fun onOff(enabled: Boolean): String = if (enabled) "On" else "Off"

        private fun colorKey(color: Color?): Int = color?.rgb ?: 0
    }

    private class RenderModelKey(
        val stateRevision: Int,
        val pendingTradeRevision: Int,
        val modeRevision: Int,
        val screenMode: StockReviewScreenMode,
        val colorDebugTargetIndex: Int,
        val colorDebugDraftRgb: Int,
        val colorDebugPersistent: Boolean,
    ) {
        fun matches(other: RenderModelKey): Boolean =
            stateRevision == other.stateRevision &&
                pendingTradeRevision == other.pendingTradeRevision &&
                modeRevision == other.modeRevision &&
                screenMode == other.screenMode &&
                colorDebugTargetIndex == other.colorDebugTargetIndex &&
                colorDebugDraftRgb == other.colorDebugDraftRgb &&
                colorDebugPersistent == other.colorDebugPersistent
    }

    private class RenderModel(
        private val snapshot: WeaponStockSnapshot,
        private val key: RenderModelKey,
        val tradeContext: StockReviewTradeContext,
        val rowLayout: StockReviewRowLayout,
        val rows: List<WimGuiListRow<StockReviewAction>>,
        val listSpec: WimGuiModalListSpec,
    ) {
        fun matches(
            snapshot: WeaponStockSnapshot,
            key: RenderModelKey,
        ): Boolean =
            this.snapshot === snapshot &&
                this.key.matches(key)
    }
}
