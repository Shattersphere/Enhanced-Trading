package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewColorDebugRows
import weaponsprocurement.ui.stockreview.rows.StockReviewAutoRulesRows
import weaponsprocurement.ui.stockreview.rows.StockReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewShipCatalogDebugRows
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrade
import weaponsprocurement.ui.stockreview.ships.StockReviewShipReviewListModel
import weaponsprocurement.ui.stockreview.state.StockReviewFilterListModel
import weaponsprocurement.ui.stockreview.state.StockReviewAutoRulesController
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color

enum class StockReviewListSourceKind {
    TRADE,
    REVIEW,
    FILTERS,
    COLOR_DEBUG,
    SHIP_CATALOG_DEBUG,
    AUTO_RULES,
}

class StockReviewListSourceContext(
    @JvmField val snapshot: WeaponStockSnapshot,
    @JvmField val state: StockReviewState,
    @JvmField val pendingTrades: List<StockReviewPendingTrade>,
    @JvmField val pendingShipTrades: List<StockReviewPendingShipTrade>,
    @JvmField val tradeContext: StockReviewTradeContext,
    @JvmField val rowLayout: StockReviewRowLayout,
    @JvmField val colorDebugTargetIndex: Int,
    @JvmField val colorDebugDraft: Color?,
    @JvmField val colorDebugPersistent: Boolean,
    @JvmField val autoRulesController: StockReviewAutoRulesController?,
)

class StockReviewListSourceSpec private constructor(
    @JvmField val kind: StockReviewListSourceKind,
) {
    fun build(context: StockReviewListSourceContext): List<WimGuiListRow<StockReviewAction>> =
        when (kind) {
            StockReviewListSourceKind.TRADE ->
                StockReviewListModel.build(context.snapshot, context.state, context.tradeContext, context.rowLayout)
            StockReviewListSourceKind.REVIEW ->
                if (context.state.isShipTrading()) {
                    StockReviewShipReviewListModel.build(context.pendingShipTrades, context.rowLayout)
                } else {
                    StockReviewReviewListModel.build(context.snapshot, context.pendingTrades, context.state, context.tradeContext, context.rowLayout)
                }
            StockReviewListSourceKind.FILTERS ->
                StockReviewFilterListModel.build(context.state)
            StockReviewListSourceKind.COLOR_DEBUG ->
                StockReviewColorDebugRows.build(context.colorDebugTargetIndex, context.colorDebugDraft, context.colorDebugPersistent)
            StockReviewListSourceKind.SHIP_CATALOG_DEBUG ->
                StockReviewShipCatalogDebugRows.build()
            StockReviewListSourceKind.AUTO_RULES ->
                StockReviewAutoRulesRows.build(context.rowLayout, context.autoRulesController)
        }

    companion object {
        @JvmStatic
        fun trade(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.TRADE)

        @JvmStatic
        fun review(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.REVIEW)

        @JvmStatic
        fun filters(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.FILTERS)

        @JvmStatic
        fun colorDebug(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.COLOR_DEBUG)

        @JvmStatic
        fun shipCatalogDebug(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.SHIP_CATALOG_DEBUG)

        @JvmStatic
        fun autoRules(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.AUTO_RULES)
    }
}
