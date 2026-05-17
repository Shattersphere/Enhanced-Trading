package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionRef
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import java.awt.Color

object StockReviewRowSpecs {
    @JvmStatic
    fun category(
        label: String?,
        textColor: Color?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
        indent: Float = 0f,
    ): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            buttonFillColor = textColor,
            indent = indent,
            actionRef = action?.let { StockReviewActionRef.rowExpansion(it) },
            alignment = Alignment.LMID,
            topGap = topGap,
            tooltip = tooltip,
        )

    @JvmStatic
    fun filterHeading(
        label: String?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): StockReviewRowSpec =
        groupedHeading(label, StockReviewActionGroup.ROW_EXPANSION, action, topGap, tooltip)

    @JvmStatic
    fun filterControlHeading(
        label: String?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): StockReviewRowSpec =
        groupedHeading(label, StockReviewActionGroup.FILTERS, action, topGap, tooltip)

    @JvmStatic
    fun staticHeading(label: String?, topGap: Boolean, tooltip: String?): StockReviewRowSpec =
        groupedHeading(label, null, null, topGap, tooltip)

    @JvmStatic
    fun nestedHeading(
        label: String?,
        indent: Float,
        rightReserveWidth: Float,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            buttonFillColor = StockReviewStyle.HEADING_BACKGROUND,
            indent = indent,
            actionRef = action?.let { StockReviewActionRef.rowExpansion(it) },
            alignment = Alignment.LMID,
            topGap = topGap,
            rightReserveWidth = rightReserveWidth,
            tooltip = tooltip,
        )

    @JvmStatic
    fun filter(
        label: String?,
        active: Boolean,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): StockReviewRowSpec {
        val fill = if (active) StockReviewStyle.FILTER_ACTIVE else StockReviewStyle.ROW_BACKGROUND
        return StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            fillColor = fill,
            buttonFillColor = fill,
            borderColor = StockReviewStyle.ROW_BORDER,
            indent = if (active) 0f else StockReviewStyle.WEAPON_INDENT,
            actionRef = action?.let { StockReviewActionRef.filters(it) },
            alignment = Alignment.LMID,
            topGap = topGap,
            tooltip = tooltip,
        )
    }

    @JvmStatic
    fun item(
        label: String?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        action: StockReviewActionRef?,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
        indent: Float,
        icon: StockReviewRowIcon?,
    ): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            fillColor = StockReviewStyle.ROW_BACKGROUND,
            buttonFillColor = StockReviewStyle.CELL_BACKGROUND,
            borderColor = StockReviewStyle.ROW_BORDER,
            indent = indent,
            actionRef = action,
            alignment = Alignment.LMID,
            cells = cells,
            tooltip = tooltip,
            tooltipCreator = tooltipCreator,
            icon = icon,
        )

    @JvmStatic
    fun labelText(spec: StockReviewDetailRowSpec): StockReviewRowSpec {
        val componentWidth = maxOf(
            40f,
            spec.layout.listWidth - spec.indent - spec.layout.detailRightReserveWidth - 2f * StockReviewStyle.SMALL_PAD,
        )
        val labelWidth = componentWidth * 0.65f
        val valueWidth = componentWidth - labelWidth
        return StockReviewRowSpec.create(
            label = "",
            textColor = StockReviewStyle.TEXT,
            indent = spec.indent,
            alignment = Alignment.LMID,
            cells =
                WimGuiRowCell.of(
                    WimGuiRowCell.infoWithBorder<StockReviewAction>(
                        spec.label,
                        labelWidth,
                        null,
                        StockReviewStyle.TEXT,
                        Alignment.LMID,
                        StockReviewStyle.ROW_BORDER,
                    ),
                    WimGuiRowCell.infoWithBorder<StockReviewAction>(
                        spec.value,
                        valueWidth,
                        StockReviewStyle.CELL_BACKGROUND,
                        StockReviewStyle.TEXT,
                        Alignment.MID,
                        StockReviewStyle.ROW_BORDER,
                    ),
                ),
            topGap = spec.topGap,
            cellGapOverride = 0f,
            rightReserveWidth = spec.layout.detailRightReserveWidth,
            tooltip = spec.tooltip,
        )
    }

    @JvmStatic
    fun form(label: String?, cells: List<WimGuiRowCell<StockReviewAction>>?): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            fillColor = StockReviewStyle.ROW_BACKGROUND,
            buttonFillColor = StockReviewStyle.ROW_BACKGROUND,
            borderColor = StockReviewStyle.ROW_BORDER,
            alignment = Alignment.LMID,
            cells = cells,
        )

    @JvmStatic
    fun empty(label: String?): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.MUTED,
            alignment = Alignment.LMID,
        )

    @JvmStatic
    fun scroll(label: String?, action: StockReviewAction?): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.SCROLL,
            fillColor = StockReviewStyle.HEADING_BACKGROUND,
            buttonFillColor = StockReviewStyle.HEADING_BACKGROUND,
            actionRef = action?.let { StockReviewActionRef.scroll(it) },
            alignment = Alignment.MID,
            tooltip = "Move the list by one visible page.",
        )

    @JvmStatic
    fun reviewMissing(label: String?): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            fillColor = StockReviewStyle.ROW_BACKGROUND,
            buttonFillColor = StockReviewStyle.ROW_BACKGROUND,
            borderColor = StockReviewStyle.ROW_BORDER,
            indent = StockReviewStyle.WEAPON_INDENT,
            alignment = Alignment.LMID,
        )

    private fun groupedHeading(
        label: String?,
        actionGroup: StockReviewActionGroup?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): StockReviewRowSpec =
        StockReviewRowSpec.create(
            label = label,
            textColor = StockReviewStyle.TEXT,
            fillColor = StockReviewStyle.HEADING_BACKGROUND,
            buttonFillColor = StockReviewStyle.HEADING_BACKGROUND,
            actionRef = action?.let { StockReviewActionRef.of(requireNotNull(actionGroup), it) },
            alignment = Alignment.LMID,
            topGap = topGap,
            tooltip = tooltip,
        )
}
