package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewColorDebugRows
import weaponsprocurement.ui.stockreview.rows.StockReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewShipCatalogDebugRows
import weaponsprocurement.ui.stockreview.state.StockReviewFilterListModel
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
}

class StockReviewListSourceContext(
    @JvmField val snapshot: WeaponStockSnapshot,
    @JvmField val state: StockReviewState,
    @JvmField val pendingTrades: List<StockReviewPendingTrade>,
    @JvmField val tradeContext: StockReviewTradeContext,
    @JvmField val rowLayout: StockReviewRowLayout,
    @JvmField val colorDebugTargetIndex: Int,
    @JvmField val colorDebugDraft: Color?,
    @JvmField val colorDebugPersistent: Boolean,
)

class StockReviewListSourceSpec private constructor(
    @JvmField val kind: StockReviewListSourceKind,
    private val builder: (StockReviewListSourceContext) -> List<WimGuiListRow<StockReviewAction>>,
) {
    fun build(context: StockReviewListSourceContext): List<WimGuiListRow<StockReviewAction>> = builder.invoke(context)

    companion object {
        @JvmStatic
        fun trade(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.TRADE) { context ->
            StockReviewListModel.build(context.snapshot, context.state, context.tradeContext, context.rowLayout)
        }

        @JvmStatic
        fun review(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.REVIEW) { context ->
            StockReviewReviewListModel.build(context.snapshot, context.pendingTrades, context.state, context.tradeContext, context.rowLayout)
        }

        @JvmStatic
        fun filters(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.FILTERS) { context ->
            StockReviewFilterListModel.build(context.state)
        }

        @JvmStatic
        fun colorDebug(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.COLOR_DEBUG) { context ->
            StockReviewColorDebugRows.build(context.colorDebugTargetIndex, context.colorDebugDraft, context.colorDebugPersistent)
        }

        @JvmStatic
        fun shipCatalogDebug(): StockReviewListSourceSpec = StockReviewListSourceSpec(StockReviewListSourceKind.SHIP_CATALOG_DEBUG) {
            StockReviewShipCatalogDebugRows.build()
        }
    }
}
