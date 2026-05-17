package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiListBounds
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiModalListRenderer
import weaponsprocurement.ui.WimGuiModalListSpec
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewFooterRenderer
import weaponsprocurement.ui.stockreview.rows.StockReviewListRow
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewTradeSummaryRenderer
import weaponsprocurement.ui.stockreview.rows.StockReviewScreenMode
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color

class StockReviewRenderer :
    WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
    WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction> {
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
        screenMode: StockReviewScreenMode,
        colorDebugTargetIndex: Int,
        colorDebugDraft: Color?,
        colorDebugPersistent: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val modeSpec = StockReviewModeSpec.forScreenMode(screenMode)
        if (modeSpec.hasHeader()) {
            StockReviewHeaderRenderer.render(root, snapshot, state, modeSpec, colorDebugTargetIndex, colorDebugDraft)
        }
        if (modeSpec.hasTradeActionRow()) {
            StockReviewActionRowRenderer.render(root, snapshot, state, modeSpec, buttons)
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
        if (modeSpec.hasTradeSummary()) {
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
        val rows: List<WimGuiListRow<StockReviewAction>> = modeSpec.listSourceSpec.build(
            StockReviewListSourceContext(
                snapshot,
                state,
                pendingTrades,
                tradeContext,
                rowLayout,
                colorDebugTargetIndex,
                colorDebugDraft,
                colorDebugPersistent,
            ),
        )
        val listSpec = modeSpec.listSpec

        val built = RenderModel(snapshot, key, tradeContext, rowLayout, rows, listSpec)
        cachedModel = built
        return built
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
