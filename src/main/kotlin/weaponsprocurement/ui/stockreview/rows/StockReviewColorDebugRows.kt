package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiColorDebug
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionCells
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import com.fs.starfarer.api.ui.Alignment
import java.awt.Color
import java.util.ArrayList

class StockReviewColorDebugRows private constructor() {
    companion object {
        @JvmStatic
        fun build(targetIndex: Int, draft: Color?, persistent: Boolean): List<WimGuiListRow<StockReviewAction>> {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            val targets = WimGuiColorDebug.targets()
            val target = WimGuiColorDebug.targetAt(targetIndex)
            val color = draft ?: WimGuiColorDebug.currentColor(target)
            val count = "[${Math.max(0, Math.min(targetIndex, targets.size - 1)) + 1}/${targets.size}]"

            rows.add(
                StockReviewListRow.form(
                    "Samples",
                    WimGuiRowCell.of(
                        WimGuiRowCell.info("Container", StockReviewStyle.DEBUG_SAMPLE_WIDTH, color, StockReviewStyle.TEXT, Alignment.MID, "Preview this color as a plain container."),
                        debugCell("Button", StockReviewStyle.DEBUG_SAMPLE_WIDTH, color, StockReviewAction.debugNoop(), "Preview this color as a button."),
                        debugCell("Toggle", StockReviewStyle.DEBUG_SAMPLE_WIDTH, color, StockReviewAction.debugNoop(), "Preview this color as a toggle heading."),
                    ),
                ),
            )
            rows.add(
                StockReviewListRow.form(
                    "Variable",
                    WimGuiRowCell.of(
                        debugCell((target?.label ?: "Unknown") + " " + count, StockReviewStyle.DEBUG_VALUE_WIDTH, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugCycleTarget(1), "Cycle the color variable being edited."),
                    ),
                ),
            )
            rows.add(
                StockReviewListRow.form(
                    "Mode",
                    WimGuiRowCell.of(
                        debugCell(if (persistent) "Permanent" else "Temporary", StockReviewStyle.DEBUG_VALUE_WIDTH, if (persistent) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugTogglePersistence(), "Toggle whether Apply writes the color permanently."),
                    ),
                ),
            )
            rows.add(
                StockReviewListRow.form(
                    "Preview",
                    WimGuiRowCell.of(
                        WimGuiRowCell.info("Color(${color.red}, ${color.green}, ${color.blue})", StockReviewStyle.DEBUG_VALUE_WIDTH, color, StockReviewStyle.TEXT, Alignment.MID, "Current RGB value for the selected color."),
                    ),
                ),
            )
            rows.add(channelRow("Red: ${color.red}", StockReviewAction.debugAdjustRed(-10), StockReviewAction.debugAdjustRed(-1), StockReviewAction.debugAdjustRed(1), StockReviewAction.debugAdjustRed(10)))
            rows.add(channelRow("Green: ${color.green}", StockReviewAction.debugAdjustGreen(-10), StockReviewAction.debugAdjustGreen(-1), StockReviewAction.debugAdjustGreen(1), StockReviewAction.debugAdjustGreen(10)))
            rows.add(channelRow("Blue: ${color.blue}", StockReviewAction.debugAdjustBlue(-10), StockReviewAction.debugAdjustBlue(-1), StockReviewAction.debugAdjustBlue(1), StockReviewAction.debugAdjustBlue(10)))
            return rows
        }

        private fun channelRow(
            label: String,
            minusTen: StockReviewAction,
            minusOne: StockReviewAction,
            plusOne: StockReviewAction,
            plusTen: StockReviewAction,
        ): WimGuiListRow<StockReviewAction> {
            return StockReviewListRow.form(
                label,
                WimGuiRowCell.of(
                    debugCell("-10", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH, StockReviewStyle.CANCEL_BUTTON, minusTen, "Decrease this channel by 10."),
                    debugCell("-1", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH, StockReviewStyle.CANCEL_BUTTON, minusOne, "Decrease this channel by 1."),
                    debugCell("+1", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH, StockReviewStyle.CONFIRM_BUTTON, plusOne, "Increase this channel by 1."),
                    debugCell("+10", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH, StockReviewStyle.CONFIRM_BUTTON, plusTen, "Increase this channel by 10."),
                ),
            )
        }

        private fun debugCell(
            label: String,
            width: Float,
            fill: Color,
            action: StockReviewAction,
            tooltip: String,
        ): WimGuiRowCell<StockReviewAction> =
            StockReviewActionCells.standard(StockReviewActionGroup.DEBUG_MODE, label, width, fill, action, true, tooltip)
    }
}
