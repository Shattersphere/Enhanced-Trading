package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiModalLayout
import weaponsprocurement.ui.WimGuiModalListSpec
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewScreenMode

class StockReviewLayoutContext private constructor(
    @JvmField val screenMode: StockReviewScreenMode,
    @JvmField val rowLayout: StockReviewRowLayout,
    @JvmField val modal: WimGuiModalLayout,
    @JvmField val listSpec: WimGuiModalListSpec,
    @JvmField val showHeader: Boolean,
    @JvmField val showTradeActionRow: Boolean,
    @JvmField val showSummary: Boolean,
) {
    companion object {
        @JvmStatic
        fun forMode(screenMode: StockReviewScreenMode): StockReviewLayoutContext {
            val rowLayout = StockReviewRowLayout.forScreenMode(screenMode)
            return when (screenMode) {
                StockReviewScreenMode.REVIEW -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.REVIEW_MODAL,
                    StockReviewStyle.REVIEW_LIST,
                    false,
                    false,
                    true,
                )
                StockReviewScreenMode.FILTERS -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.FILTER_MODAL,
                    StockReviewStyle.FILTER_LIST,
                    true,
                    false,
                    false,
                )
                StockReviewScreenMode.COLOR_DEBUG -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.LIST,
                    true,
                    false,
                    false,
                )
                StockReviewScreenMode.SHIP_CATALOG_DEBUG -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.LIST,
                    true,
                    false,
                    false,
                )
                StockReviewScreenMode.TRADE -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.TRADE_LIST,
                    false,
                    true,
                    true,
                )
            }
        }
    }
}

class StockReviewModeSpec private constructor(
    @JvmField val layoutContext: StockReviewLayoutContext,
    @JvmField val reviewMode: Boolean,
    @JvmField val filterMode: Boolean,
    @JvmField val colorDebugMode: Boolean,
    @JvmField val shipCatalogDebugMode: Boolean,
) {
    @JvmField val screenMode: StockReviewScreenMode = layoutContext.screenMode
    @JvmField val rowLayout: StockReviewRowLayout = layoutContext.rowLayout
    @JvmField val modal: WimGuiModalLayout = layoutContext.modal
    @JvmField val listSpec: WimGuiModalListSpec = layoutContext.listSpec
    @JvmField val showHeader: Boolean = layoutContext.showHeader
    @JvmField val showTradeActionRow: Boolean = layoutContext.showTradeActionRow
    @JvmField val showSummary: Boolean = layoutContext.showSummary

    companion object {
        @JvmStatic
        fun resolve(
            reviewMode: Boolean,
            filterMode: Boolean,
            colorDebugMode: Boolean,
            shipCatalogDebugMode: Boolean,
        ): StockReviewModeSpec {
            val screenMode = when {
                colorDebugMode -> StockReviewScreenMode.COLOR_DEBUG
                shipCatalogDebugMode -> StockReviewScreenMode.SHIP_CATALOG_DEBUG
                filterMode -> StockReviewScreenMode.FILTERS
                reviewMode -> StockReviewScreenMode.REVIEW
                else -> StockReviewScreenMode.TRADE
            }
            return StockReviewModeSpec(
                StockReviewLayoutContext.forMode(screenMode),
                reviewMode,
                filterMode,
                colorDebugMode,
                shipCatalogDebugMode,
            )
        }
    }
}
