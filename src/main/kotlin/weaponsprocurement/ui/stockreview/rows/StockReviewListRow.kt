package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction

object StockReviewListRow {
    @JvmStatic
    fun fromSpec(spec: StockReviewRowSpec): WimGuiListRow<StockReviewAction> = WimGuiListRow(
        spec.label,
        spec.textColor,
        spec.fillColor,
        spec.buttonFillColor,
        spec.borderColor,
        spec.indent,
        spec.action,
        spec.alignment,
        spec.cells,
        spec.topGap,
        spec.cellGapOverride,
        spec.rightReserveWidth,
        spec.tooltip,
        spec.tooltipCreator,
        spec.icon,
    )
}
