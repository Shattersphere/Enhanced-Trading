package weaponsprocurement.ui.stockreview.rendering

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiControls
import weaponsprocurement.ui.WimGuiListBounds
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiModalLayout
import weaponsprocurement.ui.WimGuiModalListRenderer
import weaponsprocurement.ui.WimGuiModalListSpec
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewFilterGroupSections
import weaponsprocurement.ui.stockreview.rows.StockReviewFilterRows
import weaponsprocurement.ui.stockreview.ships.StockReviewShipFilterModal
import weaponsprocurement.ui.stockreview.state.StockReviewShipFilterField
import weaponsprocurement.ui.stockreview.state.StockReviewState
import java.awt.Color

class StockReviewFilterModalRenderer private constructor() {
    companion object {
        private const val ITEM_WIDTH = 390f
        private const val SHIP_WIDTH = 650f
        private const val PAD = 14f
        private const val FOOTER_GAP = 12f
        private const val FOOTER_BUTTON_WIDTH = 112f
        private const val RESET_BUTTON_WIDTH = 96f
        private const val MAX_ITEM_LIST_HEIGHT = 430f
        private val DIM = Color(0, 0, 0, 150)

        @JvmStatic
        fun render(
            root: CustomPanelAPI,
            state: StockReviewState,
            focusedShipFilterField: StockReviewShipFilterField?,
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
            scrollRowFactory: WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
            extraGapProvider: WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction>,
        ): WimGuiListBounds {
            val rows = if (state.isShipTrading()) null else itemRows(state)
            val width = if (state.isShipTrading()) SHIP_WIDTH else ITEM_WIDTH
            val height = if (state.isShipTrading()) {
                StockReviewShipFilterModal.preferredHeight(PAD, FOOTER_GAP, StockReviewStyle.ACTION_BUTTON_HEIGHT)
            } else {
                itemHeight(rows.orEmpty())
            }
            addDim(root)
            val modalLeft = (StockReviewStyle.MODAL.width - width) * 0.5f
            val modalTop = (StockReviewStyle.MODAL.height - height) * 0.5f
            val modal = root.createCustomPanel(width, height, WimGuiPanelPlugin(StockReviewStyle.PANEL_BACKGROUND, StockReviewStyle.PANEL_BORDER))
            root.addComponent(modal).inTL(modalLeft, modalTop)
            val bounds = if (state.isShipTrading()) {
                StockReviewShipFilterModal.render(modal, state, focusedShipFilterField, buttons)
                WimGuiListBounds(0, modalLeft, modalTop, width, height)
            } else {
                renderItemFilters(modal, state, rows.orEmpty(), buttons, scrollRowFactory, extraGapProvider, width, height).translated(modalLeft, modalTop)
            }
            renderFooter(modal, state, buttons, width, height)
            return bounds
        }

        private fun addDim(root: CustomPanelAPI) {
            val dim = root.createCustomPanel(StockReviewStyle.MODAL.width, StockReviewStyle.MODAL.height, WimGuiPanelPlugin(DIM, null))
            root.addComponent(dim).inTL(0f, 0f)
        }

        private fun renderItemFilters(
            panel: CustomPanelAPI,
            state: StockReviewState,
            rows: List<WimGuiListRow<StockReviewAction>>,
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
            scrollRowFactory: WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
            extraGapProvider: WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction>,
            width: Float,
            height: Float,
        ): WimGuiListBounds {
            val footerTop = footerButtonY(height)
            val listTop = PAD
            val listHeight = maxOf(StockReviewStyle.ROW_HEIGHT, footerTop - FOOTER_GAP - listTop)
            val modalLayout = WimGuiModalLayout(width, height, PAD, PAD, listTop, 48f, StockReviewStyle.ROW_HEIGHT, StockReviewStyle.ROW_GAP, StockReviewStyle.SMALL_PAD)
            val spec = WimGuiModalListSpec(
                modalLayout,
                PAD,
                listTop,
                width - 2f * PAD,
                listHeight,
                StockReviewStyle.ROW_HEIGHT,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.ROW_GAP,
                StockReviewStyle.SMALL_PAD,
                StockReviewStyle.BUTTON_GAP,
                120f,
                StockReviewStyle.PANEL_BACKGROUND,
                StockReviewStyle.PANEL_BORDER,
                StockReviewStyle.ROW_BORDER,
            )
            return WimGuiModalListRenderer.renderAndStoreOffset(panel, rows, state, spec, scrollRowFactory, extraGapProvider, buttons)
        }

        private fun renderFooter(panel: CustomPanelAPI, state: StockReviewState, buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>, width: Float, height: Float) {
            val y = footerButtonY(height)
            val gap = StockReviewStyle.BUTTON_GAP
            WimGuiControls.addBoundButton(
                panel,
                PAD,
                y,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                WimGuiButtonSpec.toggle(
                    FOOTER_BUTTON_WIDTH,
                    "Confirm",
                    StockReviewStyle.TEXT,
                    StockReviewAction.goBack(),
                    Alignment.MID,
                    StockReviewStyle.CONFIRM_BUTTON,
                    StockReviewStyle.ROW_BORDER,
                    "Return to the trade screen with the current filters.",
                    null,
                ),
                buttons,
            )
            WimGuiControls.addBoundButton(
                panel,
                PAD + FOOTER_BUTTON_WIDTH + gap,
                y,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                WimGuiButtonSpec.toggle(
                    RESET_BUTTON_WIDTH,
                    "Reset",
                    StockReviewStyle.TEXT,
                    StockReviewAction.resetFilters(),
                    Alignment.MID,
                    StockReviewStyle.LOAD_BUTTON,
                    StockReviewStyle.ROW_BORDER,
                    if (state.isShipTrading()) "Clear every active ship filter." else "Clear every active item filter.",
                    null,
                ),
                buttons,
            )
            WimGuiControls.addBoundButton(
                panel,
                width - PAD - FOOTER_BUTTON_WIDTH,
                y,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                WimGuiButtonSpec.toggle(
                    FOOTER_BUTTON_WIDTH,
                    "Cancel",
                    StockReviewStyle.TEXT,
                    StockReviewAction.goBack(),
                    Alignment.MID,
                    StockReviewStyle.CANCEL_BUTTON,
                    StockReviewStyle.ROW_BORDER,
                    "Return to the trade screen.",
                    null,
                ),
                buttons,
            )
        }

        @JvmStatic
        fun modalLeft(state: StockReviewState): Float =
            (StockReviewStyle.MODAL.width - if (state.isShipTrading()) SHIP_WIDTH else ITEM_WIDTH) * 0.5f

        @JvmStatic
        fun modalTop(state: StockReviewState): Float =
            (StockReviewStyle.MODAL.height - if (state.isShipTrading()) {
                StockReviewShipFilterModal.preferredHeight(PAD, FOOTER_GAP, StockReviewStyle.ACTION_BUTTON_HEIGHT)
            } else {
                itemHeight(itemRows(state))
            }) * 0.5f

        private fun itemRows(state: StockReviewState): List<WimGuiListRow<StockReviewAction>> {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            val active = state.getActiveFilters()
            StockReviewFilterRows.addActive(rows, active)
            StockReviewFilterGroupSections.addGroups(rows, state, active)
            return rows
        }

        private fun itemHeight(rows: List<WimGuiListRow<StockReviewAction>>): Float {
            val contentHeight = rows.sumOf {
                (StockReviewStyle.ROW_HEIGHT + StockReviewStyle.ROW_GAP + if (it.hasTopGap()) StockReviewStyle.CATEGORY_TOP_GAP else 0f).toDouble()
            }.toFloat() + 2f * StockReviewStyle.SMALL_PAD
            val listHeight = contentHeight.coerceAtMost(MAX_ITEM_LIST_HEIGHT)
            return PAD + listHeight + FOOTER_GAP + StockReviewStyle.ACTION_BUTTON_HEIGHT + PAD
        }

        private fun footerButtonY(height: Float): Float =
            height - PAD - StockReviewStyle.ACTION_BUTTON_HEIGHT
    }
}
