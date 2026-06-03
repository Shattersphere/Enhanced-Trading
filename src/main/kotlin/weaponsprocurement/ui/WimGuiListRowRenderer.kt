package weaponsprocurement.ui

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.shattersphere.shatterlib.starsector.ui.StarsectorPanelTableCellSpec
import com.shattersphere.shatterlib.starsector.ui.StarsectorPanelTableRenderSpec
import com.shattersphere.shatterlib.starsector.ui.StarsectorPanelTableRowIconSpec
import com.shattersphere.shatterlib.starsector.ui.StarsectorPanelTableRowSpec
import com.shattersphere.shatterlib.starsector.ui.StarsectorPanelTableRows
import weaponsprocurement.ui.stockreview.rendering.StockReviewIconLayout
import java.awt.Color

class WimGuiListRowRenderer private constructor() {
    companion object {
        @JvmStatic
        fun <A> renderRow(
            parent: CustomPanelAPI,
            row: WimGuiListRow<A>,
            y: Float,
            rowHeight: Float,
            actionHeight: Float,
            horizontalPad: Float,
            buttonGap: Float,
            minLabelWidth: Float,
            defaultBorder: Color,
            buttons: MutableList<WimGuiButtonBinding<A>>,
        ) {
            val interactive = !WimGuiControls.interactionsSuppressed()
            val result = StarsectorPanelTableRows.render(
                StarsectorPanelTableRenderSpec(
                    parent = parent,
                    row = StarsectorPanelTableRowSpec(
                        label = row.getLabel(),
                        fillColor = row.getFillColor(),
                        textColor = row.getTextColor() ?: WimGuiStyle.DEFAULT_TEXT,
                        borderColor = if (row.getIndent() > 0f) null else row.getBorderColor(),
                        labelBorderColor = if (row.getMainAction() != null) defaultBorder else null,
                        buttonFillColor = row.getButtonFillColor() ?: WimGuiStyle.UNCOLOURED_BUTTON,
                        buttonTextColor = row.getTextColor() ?: WimGuiStyle.DEFAULT_TEXT,
                        indent = row.getIndent(),
                        action = row.getMainAction(),
                        actionCoversRow = false,
                        alignment = row.getMainAlignment(),
                        cells = row.getCells().map { cell ->
                            StarsectorPanelTableCellSpec(
                                label = cell.getLabel(),
                                width = cell.getWidth(),
                                fillColor = cellFill(cell),
                                textColor = cell.getTextColor() ?: WimGuiStyle.DEFAULT_TEXT,
                                borderColor = cell.borderColor(defaultBorder),
                                action = cell.getAction(),
                                enabled = cell.isEnabled(),
                                alignment = cell.getAlignment(),
                                tooltipText = cell.getTooltip(),
                            )
                        },
                        rightReserveWidth = row.rightReserveWidth(),
                        tooltipText = row.getTooltip(),
                        tooltipCreator = row.getTooltipCreator(),
                        icon = rowIconSpec(row),
                    ),
                    y = y,
                    rowHeight = rowHeight,
                    actionHeight = actionHeight,
                    horizontalPad = horizontalPad,
                    cellGap = row.cellGap(buttonGap),
                    minLabelWidth = minLabelWidth,
                    labelLeftPad = WimGuiStyle.TEXT_LEFT_PAD,
                    defaultBorderColor = defaultBorder,
                    textTopPad = WimGuiStyle.TEXT_TOP_PAD,
                    preferredTextLineHeight = rowHeight,
                    interactive = interactive,
                ),
            )
            if (interactive) {
                for (binding in result.actionBindings) {
                    buttons.add(WimGuiButtonBinding(binding.panel, null, binding.action))
                }
            }
        }

        private fun <A> rowIconSpec(row: WimGuiListRow<A>): StarsectorPanelTableRowIconSpec? {
            val icon = row.getIcon() ?: return null
            return StarsectorPanelTableRowIconSpec(
                spriteName = icon.spriteName,
                visualCenterRatio = StockReviewIconLayout.relativeVisualCenterX(1f),
            )
        }

        private fun <A> cellFill(cell: WimGuiRowCell<A>): Color? =
            if (cell.isAction() && !cell.isEnabled()) {
                WimGuiStyle.DISABLED_BACKGROUND
            } else {
                cell.getFillColor()
            }
    }
}
