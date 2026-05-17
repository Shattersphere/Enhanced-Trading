package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import java.awt.Color

class StockReviewRowSpec private constructor(
    @JvmField val label: String?,
    @JvmField val textColor: Color?,
    @JvmField val fillColor: Color?,
    @JvmField val buttonFillColor: Color?,
    @JvmField val borderColor: Color?,
    @JvmField val indent: Float,
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
    class Builder(private val label: String?) {
        private var textColor: Color? = StockReviewStyle.TEXT
        private var fillColor: Color? = null
        private var buttonFillColor: Color? = null
        private var borderColor: Color? = null
        private var indent: Float = 0f
        private var action: StockReviewAction? = null
        private var alignment: Alignment? = Alignment.LMID
        private var cells: List<WimGuiRowCell<StockReviewAction>>? = null
        private var topGap: Boolean = false
        private var cellGapOverride: Float? = null
        private var rightReserveWidth: Float = 0f
        private var tooltip: String? = null
        private var tooltipCreator: TooltipMakerAPI.TooltipCreator? = null
        private var icon: StockReviewRowIcon? = null

        fun textColor(value: Color?) = apply { textColor = value }
        fun fillColor(value: Color?) = apply { fillColor = value }
        fun buttonFillColor(value: Color?) = apply { buttonFillColor = value }
        fun borderColor(value: Color?) = apply { borderColor = value }
        fun indent(value: Float) = apply { indent = value }
        fun action(value: StockReviewAction?) = apply { action = value }
        fun alignment(value: Alignment?) = apply { alignment = value }
        fun cells(value: List<WimGuiRowCell<StockReviewAction>>?) = apply { cells = value }
        fun topGap(value: Boolean) = apply { topGap = value }
        fun cellGapOverride(value: Float?) = apply { cellGapOverride = value }
        fun rightReserveWidth(value: Float) = apply { rightReserveWidth = value }
        fun tooltip(value: String?) = apply { tooltip = value }
        fun tooltipCreator(value: TooltipMakerAPI.TooltipCreator?) = apply { tooltipCreator = value }
        fun icon(value: StockReviewRowIcon?) = apply { icon = value }

        fun build(): StockReviewRowSpec = StockReviewRowSpec(
            label,
            textColor,
            fillColor,
            buttonFillColor,
            borderColor,
            indent,
            action,
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

    companion object {
        @JvmStatic
        fun builder(label: String?): Builder = Builder(label)
    }
}
