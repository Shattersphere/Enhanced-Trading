package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.WimGuiStyle
import weaponsprocurement.ui.WimGuiText
import weaponsprocurement.ui.WimGuiTooltip
import java.awt.Color

/**
 * Shared custom tooltip drawing helpers and height cap. Weapon, wing, and ship tooltips
 * use this instead of vanilla obfuscated tooltip classes.
 */
object StockReviewTooltipPanel {
    val SECTION: Color = Color(9, 78, 88, 225)
    val TEXT: Color = Color(215, 215, 215, 255)
    val SHIP_TEXT: Color = Color(218, 226, 228, 255)
    val TITLE: Color = Color(205, 245, 255, 255)
    val MUTED: Color = Color(175, 175, 175, 255)
    val ITEM_BACKGROUND: Color = Color(0, 0, 0, 255)
    val ITEM_BORDER: Color = Color(115, 145, 150, 255)
    val SHIP_BACKGROUND: Color = Color(0, 0, 0, 245)
    val SHIP_BORDER: Color = Color(100, 185, 200, 255)

    @JvmStatic
    fun maxTooltipHeight(lineHeight: Float = WimGuiStyle.TEXT_LINE_HEIGHT): Float = WimGuiTooltip.maxTooltipHeight(lineHeight)

    @JvmStatic
    fun capHeight(height: Float, lineHeight: Float = WimGuiStyle.TEXT_LINE_HEIGHT): Float = WimGuiTooltip.capHeight(height, lineHeight)

    @JvmStatic
    fun addLabel(
        parent: CustomPanelAPI,
        text: String?,
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alignment: Alignment,
    ) {
        var labelX = x
        var labelWidth = width
        if (Alignment.LMID == alignment) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD
            labelWidth = usableTextWidth(width, alignment)
        }
        val element = parent.createUIElement(labelWidth, height, false)
        element.setParaFontDefault()
        element.setParaFontColor(color)
        val line: LabelAPI = element.addPara(WimGuiText.paraFormat(WimGuiText.fitToWidth(text, element, labelWidth)), 0f, color)
        line.setAlignment(alignment)
        parent.addUIElement(element).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD)
    }

    @JvmStatic
    fun wrapLines(
        parent: CustomPanelAPI,
        text: String?,
        color: Color,
        width: Float,
        lineHeight: Float,
        maxLines: Int,
        alignment: Alignment = Alignment.LMID,
    ): List<String> {
        val usableWidth = usableTextWidth(width, alignment)
        val measure = parent.createUIElement(usableWidth, lineHeight, false)
        measure.setParaFontDefault()
        measure.setParaFontColor(color)
        return WimGuiText.wrapToWidth(text, measure, usableWidth, maxLines)
    }

    @JvmStatic
    fun usableTextWidth(width: Float, alignment: Alignment = Alignment.LMID): Float {
        var usable = maxOf(8f, width)
        if (Alignment.LMID == alignment) {
            usable -= WimGuiStyle.TEXT_LEFT_PAD + WimGuiStyle.TEXT_RIGHT_PAD + 2f
        } else if (Alignment.RMID == alignment) {
            usable -= WimGuiStyle.TEXT_RIGHT_PAD + 2f
        }
        return maxOf(8f, usable)
    }

    @JvmStatic
    fun addStatRow(
        parent: CustomPanelAPI,
        label: String?,
        value: String?,
        labelColor: Color,
        valueColor: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        minLabelWidth: Float,
        maxLabelWidth: Float,
        minValueWidth: Float,
    ) {
        if (label.isNullOrBlank()) {
            addLabel(parent, value, valueColor, x, y, width, height, Alignment.RMID)
            return
        }

        val safeWidth = maxOf(32f, width)
        val maxAllowedLabel = maxOf(8f, minOf(maxOf(minLabelWidth, maxLabelWidth), safeWidth - minValueWidth))
        val minAllowedLabel = minOf(minLabelWidth, maxAllowedLabel)
        val labelTarget = measuredTextWidth(parent, label, labelColor, height) +
            WimGuiStyle.TEXT_LEFT_PAD +
            WimGuiStyle.TEXT_RIGHT_PAD +
            8f
        val valueTarget = measuredTextWidth(parent, value, valueColor, height) +
            WimGuiStyle.TEXT_RIGHT_PAD +
            8f

        var labelWidth = labelTarget.coerceIn(minAllowedLabel, maxAllowedLabel)
        if (safeWidth - labelWidth < valueTarget && labelWidth > minAllowedLabel) {
            labelWidth = maxOf(minAllowedLabel, safeWidth - valueTarget)
        }

        val valueWidth = maxOf(8f, safeWidth - labelWidth)
        addLabel(parent, label, labelColor, x, y, labelWidth, height, Alignment.LMID)
        addLabel(parent, value, valueColor, x + labelWidth, y, valueWidth, height, Alignment.RMID)
    }

    @JvmStatic
    fun addLines(
        parent: CustomPanelAPI,
        lines: List<String>,
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        lineHeight: Float,
    ) {
        val safeLines = if (lines.isEmpty()) listOf("") else lines
        for (i in safeLines.indices) {
            addLabel(parent, safeLines[i], color, x, y + i * lineHeight, width, lineHeight, Alignment.LMID)
        }
    }

    @JvmStatic
    fun createSectionBand(width: Float, height: Float, color: Color = SECTION): CustomPanelAPI =
        Global.getSettings().createCustom(width, height, SectionBandPlugin(color))

    @JvmStatic
    fun addSectionHeading(
        parent: CustomPanelAPI,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        textColor: Color,
        bandColor: Color = SECTION,
    ) {
        val heading = createSectionBand(width, height, bandColor)
        parent.addComponent(heading).inTL(x, y)
        addLabel(parent, text, textColor, x, y - 1f, width, height, Alignment.MID)
    }

    private fun measuredTextWidth(parent: CustomPanelAPI, text: String?, color: Color, height: Float): Float {
        val safeText = text ?: ""
        if (safeText.isEmpty()) {
            return 0f
        }
        return try {
            val measure = parent.createUIElement(1f, height, false)
            measure.setParaFontDefault()
            measure.setParaFontColor(color)
            measure.computeStringWidth(safeText)
        } catch (_: RuntimeException) {
            safeText.length * WimGuiStyle.TEXT_APPROX_CHAR_WIDTH
        }
    }

    private class SectionBandPlugin(private val color: Color) : BaseCustomUIPanelPlugin() {
        private var position: PositionAPI? = null

        override fun positionChanged(position: PositionAPI?) {
            this.position = position
        }

        override fun renderBelow(alphaMult: Float) {
            val current = position ?: return
            Misc.renderQuadAlpha(current.x, current.y, current.width, current.height, color, alphaMult)
        }
    }
}
