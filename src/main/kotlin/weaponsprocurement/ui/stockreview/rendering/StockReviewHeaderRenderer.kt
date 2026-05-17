package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiColorDebug
import weaponsprocurement.ui.WimGuiModalHeader
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.stock.item.WeaponStockSnapshot
import com.fs.starfarer.api.ui.CustomPanelAPI
import java.awt.Color

class StockReviewHeaderRenderer private constructor() {
    companion object {
        @JvmStatic
        fun render(
            root: CustomPanelAPI,
            snapshot: WeaponStockSnapshot,
            state: StockReviewState,
            modeSpec: StockReviewModeSpec,
            colorDebugTargetIndex: Int,
            colorDebugDraft: Color?,
        ) {
            val header = header(modeSpec.headerKind, snapshot, state, colorDebugTargetIndex, colorDebugDraft) ?: return
            WimGuiModalHeader.addTitleStatusHeader(
                root,
                modeSpec.modal,
                StockReviewStyle.HEADER_HEIGHT,
                header.title,
                header.status,
                StockReviewStyle.PANEL_BACKGROUND,
                StockReviewStyle.PANEL_BORDER,
                StockReviewStyle.TEXT,
            )
        }

        private fun header(
            kind: StockReviewHeaderKind,
            snapshot: WeaponStockSnapshot,
            state: StockReviewState,
            colorDebugTargetIndex: Int,
            colorDebugDraft: Color?,
        ): HeaderText? =
            when (kind) {
                StockReviewHeaderKind.NONE -> null
                StockReviewHeaderKind.MARKET_STATUS -> HeaderText("Make Trades", statusLine(snapshot, state))
                StockReviewHeaderKind.FILTER_STATUS -> HeaderText("Filters", filterStatusLine(state))
                StockReviewHeaderKind.COLOR_DEBUG_STATUS -> HeaderText("Debug Colors", colorStatusLine(colorDebugTargetIndex, colorDebugDraft))
                StockReviewHeaderKind.SHIP_CATALOG_DEBUG_STATUS -> HeaderText("Ship Catalog Debug", "Developer-only Fixer ship catalog candidate view")
            }

        private fun statusLine(snapshot: WeaponStockSnapshot, state: StockReviewState): String =
            "Market: ${snapshot.getMarketName()}" +
                " | Sort: ${snapshot.getSortMode().label}" +
                " | Owned source: ${ownedSourceLabel(snapshot)}" +
                " | Stock source: ${sourceLabel(snapshot.getSourceMode())}" +
                " | Black market: ${StockReviewUiText.onOff(snapshot.isIncludeBlackMarket())}" +
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
    }

    private class HeaderText(
        val title: String,
        val status: String,
    )
}
