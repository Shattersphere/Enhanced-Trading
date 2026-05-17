package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle

object StockReviewItemRowFrame {
    @JvmStatic
    fun build(
        label: String,
        cells: List<WimGuiRowCell<StockReviewAction>>,
        action: StockReviewActionRef,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
        layout: StockReviewRowLayout,
        icon: StockReviewRowIcon?,
    ): WimGuiListRow<StockReviewAction> = StockReviewListRow.fromSpec(
        StockReviewRowSpec.builder(label)
            .fillColor(StockReviewStyle.ROW_BACKGROUND)
            .buttonFillColor(StockReviewStyle.CELL_BACKGROUND)
            .borderColor(StockReviewStyle.ROW_BORDER)
            .indent(layout.itemIndent)
            .action(action)
            .cells(cells)
            .tooltip(tooltip)
            .tooltipCreator(tooltipCreator)
            .icon(icon)
            .build(),
    )
}
