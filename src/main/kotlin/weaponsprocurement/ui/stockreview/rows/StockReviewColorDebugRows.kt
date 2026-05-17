package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiColorDebug
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
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
                StockReviewListRow.fromSpec(
                    StockReviewRowSpecs.form(
                        "Samples",
                        WimGuiRowCell.of(
                            StockReviewDebugCellGroup.colorSampleInfo("Container", color, "Preview this color as a plain container."),
                            StockReviewDebugCellGroup.colorSampleButton("Button", color, "Preview this color as a button."),
                            StockReviewDebugCellGroup.colorSampleButton("Toggle", color, "Preview this color as a toggle heading."),
                        ),
                    ),
                ),
            )
            rows.add(
                StockReviewListRow.fromSpec(
                    StockReviewRowSpecs.form(
                        "Variable",
                        WimGuiRowCell.of(
                            StockReviewDebugCellGroup.colorValueButton((target?.label ?: "Unknown") + " " + count, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugCycleTarget(1), "Cycle the color variable being edited."),
                        ),
                    ),
                ),
            )
            rows.add(
                StockReviewListRow.fromSpec(
                    StockReviewRowSpecs.form(
                        "Mode",
                        WimGuiRowCell.of(
                            StockReviewDebugCellGroup.colorValueButton(if (persistent) "Permanent" else "Temporary", if (persistent) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugTogglePersistence(), "Toggle whether Apply writes the color permanently."),
                        ),
                    ),
                ),
            )
            rows.add(
                StockReviewListRow.fromSpec(
                    StockReviewRowSpecs.form(
                        "Preview",
                        WimGuiRowCell.of(
                            StockReviewDebugCellGroup.colorPreview(color),
                        ),
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
            return StockReviewListRow.fromSpec(
                StockReviewRowSpecs.form(
                    label,
                    WimGuiRowCell.of(
                        StockReviewDebugCellGroup.colorDelta("-10", StockReviewStyle.CANCEL_BUTTON, minusTen, "Decrease this channel by 10."),
                        StockReviewDebugCellGroup.colorDelta("-1", StockReviewStyle.CANCEL_BUTTON, minusOne, "Decrease this channel by 1."),
                        StockReviewDebugCellGroup.colorDelta("+1", StockReviewStyle.CONFIRM_BUTTON, plusOne, "Increase this channel by 1."),
                        StockReviewDebugCellGroup.colorDelta("+10", StockReviewStyle.CONFIRM_BUTTON, plusTen, "Increase this channel by 10."),
                    ),
                ),
            )
        }
    }
}
