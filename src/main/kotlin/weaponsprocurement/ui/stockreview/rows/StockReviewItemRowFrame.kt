package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef

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
        StockReviewRowSpecs.item(label, cells, action, tooltip, tooltipCreator, layout.itemIndent, icon),
    )
}
