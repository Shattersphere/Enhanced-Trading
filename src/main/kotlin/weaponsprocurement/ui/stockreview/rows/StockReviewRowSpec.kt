package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import java.awt.Color

class StockReviewRowSpec private constructor(
    @JvmField val label: String?,
    @JvmField val textColor: Color?,
    @JvmField val fillColor: Color?,
    @JvmField val buttonFillColor: Color?,
    @JvmField val borderColor: Color?,
    @JvmField val indent: Float,
    @JvmField val actionGroup: StockReviewActionGroup?,
    @JvmField val action: StockReviewAction?,
    @JvmField val alignment: Alignment?,
    @JvmField val cells: List<WimGuiRowCell<StockReviewAction>>?,
    @JvmField val topGap: Boolean,
    @JvmField val cellGapOverride: Float?,
    @JvmField val rightReserveWidth: Float,
    @JvmField val tooltip: String?,
    @JvmField val tooltipCreator: TooltipMakerAPI.TooltipCreator?,
    @JvmField val icon: StockReviewRowIcon?,
) {
    companion object {
        @JvmStatic
        fun create(
            label: String?,
            textColor: Color? = StockReviewStyle.TEXT,
            fillColor: Color? = null,
            buttonFillColor: Color? = null,
            borderColor: Color? = null,
            indent: Float = 0f,
            actionRef: StockReviewActionRef? = null,
            alignment: Alignment? = Alignment.LMID,
            cells: List<WimGuiRowCell<StockReviewAction>>? = null,
            topGap: Boolean = false,
            cellGapOverride: Float? = null,
            rightReserveWidth: Float = 0f,
            tooltip: String? = null,
            tooltipCreator: TooltipMakerAPI.TooltipCreator? = null,
            icon: StockReviewRowIcon? = null,
        ): StockReviewRowSpec = StockReviewRowSpec(
            label,
            textColor,
            fillColor,
            buttonFillColor,
            borderColor,
            indent,
            actionRef?.group,
            actionRef?.action,
            alignment,
            cells,
            topGap,
            cellGapOverride,
            rightReserveWidth,
            tooltip,
            tooltipCreator,
            icon,
        )
    }
}
