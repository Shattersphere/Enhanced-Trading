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
import weaponsprocurement.ui.stockreview.rows.StockReviewColorDebugRows
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltipPanel
import java.awt.Color

/**
 * Compact color-tuning overlay for the developer-only UI palette editor. It deliberately
 * bypasses the normal stock list shell because those rows are a small control panel, not content.
 */
object StockReviewColorDebugModalRenderer {
    private const val WIDTH = 570f
    private const val PAD = 14f
    private const val FOOTER_GAP = 12f
    private const val FOOTER_BUTTON_WIDTH = 112f
    private val DIM = Color(0, 0, 0, 191)
    private val TRANSPARENT_BORDER = Color(0, 0, 0, 0)

    @JvmStatic
    fun render(
        root: CustomPanelAPI,
        targetIndex: Int,
        draft: Color?,
        persistent: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
        scrollRowFactory: WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
        extraGapProvider: WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction>,
    ): WimGuiListBounds {
        val rows = StockReviewColorDebugRows.build(targetIndex, draft, persistent)
        val height = modalHeight(rows)
        val left = (StockReviewStyle.MODAL.width - WIDTH) * 0.5f
        val top = (StockReviewStyle.MODAL.height - height) * 0.5f
        val dim = root.createCustomPanel(StockReviewStyle.MODAL.width, StockReviewStyle.MODAL.height, WimGuiPanelPlugin(DIM, null))
        root.addComponent(dim).inTL(0f, 0f)
        val modal = root.createCustomPanel(
            WIDTH,
            height,
            WimGuiPanelPlugin(StockReviewTooltipPanel.ITEM_BACKGROUND, StockReviewTooltipPanel.ITEM_BORDER),
        )
        root.addComponent(modal).inTL(left, top)
        val bounds = renderRows(modal, rows, buttons, scrollRowFactory, extraGapProvider, height)
        renderFooter(modal, buttons, height)
        return bounds.translated(left, top)
    }

    private fun renderRows(
        modal: CustomPanelAPI,
        rows: List<WimGuiListRow<StockReviewAction>>,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
        scrollRowFactory: WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
        extraGapProvider: WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction>,
        height: Float,
    ): WimGuiListBounds {
        val listHeight = footerButtonY(height) - FOOTER_GAP - PAD
        val modalLayout = WimGuiModalLayout(
            WIDTH,
            height,
            PAD,
            PAD,
            PAD,
            StockReviewStyle.FOOTER_HEIGHT,
            StockReviewStyle.ROW_HEIGHT,
            StockReviewStyle.ROW_GAP,
            StockReviewStyle.SMALL_PAD,
        )
        val spec = WimGuiModalListSpec(
            modalLayout,
            PAD,
            PAD,
            WIDTH - 2f * PAD,
            listHeight,
            StockReviewStyle.ROW_HEIGHT,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            StockReviewStyle.ROW_GAP,
            StockReviewStyle.SMALL_PAD,
            StockReviewStyle.BUTTON_GAP,
            120f,
            StockReviewTooltipPanel.ITEM_BACKGROUND,
            TRANSPARENT_BORDER,
            StockReviewStyle.ROW_BORDER,
        )
        return WimGuiModalListRenderer.render(modal, rows, 0, spec, scrollRowFactory, extraGapProvider, buttons).bounds
    }

    private fun renderFooter(
        modal: CustomPanelAPI,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
        height: Float,
    ) {
        val y = footerButtonY(height)
        val gap = StockReviewStyle.BUTTON_GAP
        addButton(modal, PAD, y, "Confirm", StockReviewAction.debugConfirm(), StockReviewStyle.CONFIRM_BUTTON, "Apply the color and return to the trade screen.", buttons)
        addButton(modal, PAD + FOOTER_BUTTON_WIDTH + gap, y, "Apply", StockReviewAction.debugApply(), StockReviewStyle.SAVE_BUTTON, "Apply the color without closing the debug menu.", buttons)
        addButton(modal, PAD + 2f * (FOOTER_BUTTON_WIDTH + gap), y, "Restore", StockReviewAction.debugRestore(), StockReviewStyle.LOAD_BUTTON, "Restore the selected color to its default value.", buttons)
        addButton(modal, WIDTH - PAD - FOOTER_BUTTON_WIDTH, y, "Cancel", StockReviewAction.goBack(), StockReviewStyle.CANCEL_BUTTON, "Return without applying additional changes.", buttons)
    }

    private fun addButton(
        modal: CustomPanelAPI,
        x: Float,
        y: Float,
        label: String,
        action: StockReviewAction,
        fill: Color,
        tooltip: String,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        WimGuiControls.addBoundButton(
            modal,
            x,
            y,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            WimGuiButtonSpec.toggle(
                FOOTER_BUTTON_WIDTH,
                label,
                StockReviewStyle.TEXT,
                action,
                Alignment.MID,
                fill,
                StockReviewStyle.ROW_BORDER,
                tooltip,
                null,
            ),
            buttons,
        )
    }

    private fun modalHeight(rows: List<WimGuiListRow<StockReviewAction>>): Float =
        PAD +
            rows.size * StockReviewStyle.ROW_HEIGHT +
            maxOf(0, rows.size - 1) * StockReviewStyle.ROW_GAP +
            2f * StockReviewStyle.SMALL_PAD +
            FOOTER_GAP +
            StockReviewStyle.ACTION_BUTTON_HEIGHT +
            PAD

    private fun footerButtonY(height: Float): Float =
        height - PAD - StockReviewStyle.ACTION_BUTTON_HEIGHT
}
