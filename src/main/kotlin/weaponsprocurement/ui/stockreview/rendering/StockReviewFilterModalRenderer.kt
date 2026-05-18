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
import weaponsprocurement.ui.stockreview.state.StockReviewState
import java.awt.Color

class StockReviewFilterModalRenderer private constructor() {
    companion object {
        private const val WIDTH = 720f
        private const val ITEM_HEIGHT = 560f
        private const val PAD = 20f
        private const val TITLE_HEIGHT = 26f
        private const val STATUS_HEIGHT = 24f
        private const val FOOTER_BUTTON_WIDTH = 210f
        private val DIM = Color(0, 0, 0, 150)

        @JvmStatic
        fun render(
            root: CustomPanelAPI,
            state: StockReviewState,
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
            scrollRowFactory: WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
            extraGapProvider: WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction>,
        ): WimGuiListBounds {
            val height = if (state.isShipTrading()) StockReviewShipFilterModal.preferredHeight() else ITEM_HEIGHT
            addDim(root)
            val modalLeft = (StockReviewStyle.MODAL.width - WIDTH) * 0.5f
            val modalTop = (StockReviewStyle.MODAL.height - height) * 0.5f
            val modal = root.createCustomPanel(WIDTH, height, WimGuiPanelPlugin(StockReviewStyle.PANEL_BACKGROUND, StockReviewStyle.PANEL_BORDER))
            root.addComponent(modal).inTL(modalLeft, modalTop)
            renderHeader(modal, state)
            val bounds = if (state.isShipTrading()) {
                StockReviewShipFilterModal.render(modal, state, buttons)
                WimGuiListBounds(0, modalLeft, modalTop, WIDTH, height)
            } else {
                renderItemFilters(modal, state, buttons, scrollRowFactory, extraGapProvider).translated(modalLeft, modalTop)
            }
            renderFooter(modal, state, buttons, height)
            return bounds
        }

        private fun addDim(root: CustomPanelAPI) {
            val dim = root.createCustomPanel(StockReviewStyle.MODAL.width, StockReviewStyle.MODAL.height, WimGuiPanelPlugin(DIM, null))
            root.addComponent(dim).inTL(0f, 0f)
        }

        private fun renderHeader(panel: CustomPanelAPI, state: StockReviewState) {
            WimGuiControls.addLabel(panel, "Filters", StockReviewStyle.TEXT, PAD, 14f, WIDTH - 2f * PAD, TITLE_HEIGHT, Alignment.LMID)
            val status = if (state.isShipTrading()) {
                "Active ship filters: ${state.getActiveShipFilterCount()}"
            } else {
                "Active item filters: ${state.getActiveFilterCount()} | Active rows are shown first"
            }
            WimGuiControls.addLabel(panel, status, StockReviewStyle.TEXT, PAD, 44f, WIDTH - 2f * PAD, STATUS_HEIGHT, Alignment.LMID)
        }

        private fun renderItemFilters(
            panel: CustomPanelAPI,
            state: StockReviewState,
            buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
            scrollRowFactory: WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
            extraGapProvider: WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction>,
        ): WimGuiListBounds {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            val active = state.getActiveFilters()
            StockReviewFilterRows.addActive(rows, active)
            StockReviewFilterGroupSections.addGroups(rows, state, active)
            val modalLayout = WimGuiModalLayout(WIDTH, ITEM_HEIGHT, PAD, PAD, 78f, 52f, StockReviewStyle.ROW_HEIGHT, StockReviewStyle.ROW_GAP, StockReviewStyle.SMALL_PAD)
            val listTop = 82f
            val listHeight = ITEM_HEIGHT - listTop - 72f
            val spec = WimGuiModalListSpec(
                modalLayout,
                PAD,
                listTop,
                WIDTH - 2f * PAD,
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

        private fun renderFooter(panel: CustomPanelAPI, state: StockReviewState, buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>, height: Float) {
            val y = height - PAD - StockReviewStyle.ACTION_BUTTON_HEIGHT
            val gap = StockReviewStyle.BUTTON_GAP
            val resetWidth = 160f
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
                    resetWidth,
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
                WIDTH - PAD - FOOTER_BUTTON_WIDTH,
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
        fun modalLeft(): Float = (StockReviewStyle.MODAL.width - WIDTH) * 0.5f

        @JvmStatic
        fun modalTop(state: StockReviewState): Float =
            (StockReviewStyle.MODAL.height - if (state.isShipTrading()) StockReviewShipFilterModal.preferredHeight() else ITEM_HEIGHT) * 0.5f
    }
}
