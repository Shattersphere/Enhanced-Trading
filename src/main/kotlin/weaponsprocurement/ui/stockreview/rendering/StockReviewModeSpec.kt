package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiModalLayout
import weaponsprocurement.ui.WimGuiModalListSpec
import weaponsprocurement.ui.stockreview.rows.StockReviewFooterSpec
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewScreenMode

enum class StockReviewHeaderKind {
    NONE,
    MARKET_STATUS,
    FILTER_STATUS,
    COLOR_DEBUG_STATUS,
    SHIP_CATALOG_DEBUG_STATUS,
}

enum class StockReviewActionRowKind {
    NONE,
    TRADE_CONTROLS,
}

enum class StockReviewSummaryKind {
    NONE,
    TRADE_SUMMARY,
}

class StockReviewLayoutContext private constructor(
    @JvmField val screenMode: StockReviewScreenMode,
    @JvmField val rowLayout: StockReviewRowLayout,
    @JvmField val modal: WimGuiModalLayout,
    @JvmField val listSpec: WimGuiModalListSpec,
    @JvmField val listSourceSpec: StockReviewListSourceSpec,
    @JvmField val headerKind: StockReviewHeaderKind,
    @JvmField val actionRowKind: StockReviewActionRowKind,
    @JvmField val summaryKind: StockReviewSummaryKind,
    @JvmField val footerSpec: StockReviewFooterSpec,
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
                    StockReviewListSourceSpec.review(),
                    StockReviewHeaderKind.NONE,
                    StockReviewActionRowKind.NONE,
                    StockReviewSummaryKind.TRADE_SUMMARY,
                    StockReviewFooterSpec.review(),
                )
                StockReviewScreenMode.FILTERS -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.FILTER_MODAL,
                    StockReviewStyle.FILTER_LIST,
                    StockReviewListSourceSpec.filters(),
                    StockReviewHeaderKind.FILTER_STATUS,
                    StockReviewActionRowKind.NONE,
                    StockReviewSummaryKind.NONE,
                    StockReviewFooterSpec.filters(),
                )
                StockReviewScreenMode.COLOR_DEBUG -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.LIST,
                    StockReviewListSourceSpec.colorDebug(),
                    StockReviewHeaderKind.COLOR_DEBUG_STATUS,
                    StockReviewActionRowKind.NONE,
                    StockReviewSummaryKind.NONE,
                    StockReviewFooterSpec.colorDebug(),
                )
                StockReviewScreenMode.SHIP_CATALOG_DEBUG -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.LIST,
                    StockReviewListSourceSpec.shipCatalogDebug(),
                    StockReviewHeaderKind.SHIP_CATALOG_DEBUG_STATUS,
                    StockReviewActionRowKind.NONE,
                    StockReviewSummaryKind.NONE,
                    StockReviewFooterSpec.shipCatalogDebug(),
                )
                StockReviewScreenMode.TRADE -> StockReviewLayoutContext(
                    screenMode,
                    rowLayout,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.TRADE_LIST,
                    StockReviewListSourceSpec.trade(),
                    StockReviewHeaderKind.NONE,
                    StockReviewActionRowKind.TRADE_CONTROLS,
                    StockReviewSummaryKind.TRADE_SUMMARY,
                    StockReviewFooterSpec.trade(),
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
    @JvmField val listSourceSpec: StockReviewListSourceSpec = layoutContext.listSourceSpec
    @JvmField val headerKind: StockReviewHeaderKind = layoutContext.headerKind
    @JvmField val actionRowKind: StockReviewActionRowKind = layoutContext.actionRowKind
    @JvmField val summaryKind: StockReviewSummaryKind = layoutContext.summaryKind
    @JvmField val footerSpec: StockReviewFooterSpec = layoutContext.footerSpec

    fun hasHeader(): Boolean = headerKind != StockReviewHeaderKind.NONE
    fun hasTradeActionRow(): Boolean = actionRowKind == StockReviewActionRowKind.TRADE_CONTROLS
    fun hasTradeSummary(): Boolean = summaryKind == StockReviewSummaryKind.TRADE_SUMMARY

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
            return forScreenMode(screenMode)
        }

        @JvmStatic
        fun forScreenMode(screenMode: StockReviewScreenMode): StockReviewModeSpec {
            return StockReviewModeSpec(
                StockReviewLayoutContext.forMode(screenMode),
                screenMode == StockReviewScreenMode.REVIEW,
                screenMode == StockReviewScreenMode.FILTERS,
                screenMode == StockReviewScreenMode.COLOR_DEBUG,
                screenMode == StockReviewScreenMode.SHIP_CATALOG_DEBUG,
            )
        }
    }
}
