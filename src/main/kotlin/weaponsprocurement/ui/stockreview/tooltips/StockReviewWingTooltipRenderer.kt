package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.WimGuiPanelPlugin
import java.awt.Color

internal object StockReviewWingTooltipRenderer {
    const val WIDTH: Float = 560f

    private const val PAD_X = 16f
    private const val PAD_TOP = 10f
    private const val PAD_BOTTOM = 16f
    private const val CONTENT_WIDTH = WIDTH - 2f * PAD_X
    private const val LINE_HEIGHT = 23f
    private const val GRID_ROW_HEIGHT = 22f
    private const val MIN_LABEL_WIDTH = 116f
    private const val MAX_LABEL_WIDTH = 318f
    private const val MIN_VALUE_WIDTH = 118f
    private const val LOADOUT_LABEL_WIDTH = 108f
    private const val LOADOUT_VALUE_X = PAD_X + LOADOUT_LABEL_WIDTH
    private const val LOADOUT_VALUE_WIDTH = CONTENT_WIDTH - LOADOUT_LABEL_WIDTH
    private const val LOADOUT_ROW_HEIGHT = 22f
    private const val LOADOUT_MAX_LINES = 4
    private const val MAX_HEIGHT = 860f
    private const val SECTION_HEADING_HEIGHT = 22f
    private const val SMALL_PAD = 4f

    fun addTooltip(
        tooltip: TooltipMakerAPI,
        layout: StockReviewWingTooltipLayout,
        ownedCount: Int,
        priceLabel: String?,
    ) {
        val panel = Global.getSettings().createCustom(
            WIDTH,
            StockReviewTooltipPanel.maxTooltipHeight(LINE_HEIGHT),
            WimGuiPanelPlugin(StockReviewTooltipPanel.ITEM_BACKGROUND, StockReviewTooltipPanel.ITEM_BORDER),
        )
        val systemLines = measuredPanelLines(panel, layout.systemText, LOADOUT_VALUE_WIDTH, LOADOUT_ROW_HEIGHT, LOADOUT_MAX_LINES)
        val armamentLines = measuredPanelLines(panel, layout.armamentsText, LOADOUT_VALUE_WIDTH, LOADOUT_ROW_HEIGHT, LOADOUT_MAX_LINES)
        var y = PAD_TOP
        StockReviewTooltipPanel.addLabel(panel, layout.title, titleColor(), PAD_X, y, CONTENT_WIDTH, 28f, Alignment.LMID)
        y += 34f
        addRichPanelLine(panel, "Design type:", layout.manufacturer, y)
        y += 32f
        y += addWingDescription(panel, layout.descriptionText, y) + 10f
        priceLabel?.let { price ->
            addInlineHighlight(panel, "Sells for:", price, " per unit.", y)
            y += LINE_HEIGHT
        }
        addInlineHighlight(panel, "You own a total of", ownedCount.toString(), " fighter LPCs of this type.", y)
        y += LINE_HEIGHT + 10f

        addSectionHeading(panel, "Technical data", y)
        y += SECTION_HEADING_HEIGHT + 10f
        for (row in layout.technicalRows) {
            addStatRow(panel, row, y)
            y += GRID_ROW_HEIGHT
        }
        y += 10f
        y = addLoadoutLine(panel, "System:", systemLines, y)
        y = addLoadoutLine(panel, "Armaments:", armamentLines, y)
        panel.position.setSize(WIDTH, StockReviewTooltipPanel.capHeight(y + PAD_BOTTOM, LINE_HEIGHT))
        tooltip.addCustom(panel, 0f)
    }

    private fun addSectionHeading(panel: CustomPanelAPI, text: String, y: Float) {
        val heading = StockReviewTooltipPanel.createSectionBand(CONTENT_WIDTH, SECTION_HEADING_HEIGHT)
        StockReviewTooltipPanel.addLabel(heading, text, textColor(), 0f, 0f, CONTENT_WIDTH, SECTION_HEADING_HEIGHT, Alignment.MID)
        panel.addComponent(heading).inTL(PAD_X, y)
    }

    private fun addStatRow(panel: CustomPanelAPI, row: StockReviewTooltipStatRow, y: Float) {
        StockReviewTooltipPanel.addStatRow(
            panel,
            row.label,
            row.value,
            textColor(),
            highlightColor(),
            PAD_X,
            y,
            CONTENT_WIDTH,
            GRID_ROW_HEIGHT,
            MIN_LABEL_WIDTH,
            MAX_LABEL_WIDTH,
            MIN_VALUE_WIDTH,
        )
    }

    private fun addLoadoutLine(panel: CustomPanelAPI, label: String, lines: List<String>, y: Float): Float {
        StockReviewTooltipPanel.addLabel(panel, label, textColor(), PAD_X, y, LOADOUT_LABEL_WIDTH, LOADOUT_ROW_HEIGHT, Alignment.LMID)
        StockReviewTooltipPanel.addLines(
            panel,
            lines,
            highlightColor(),
            LOADOUT_VALUE_X,
            y,
            LOADOUT_VALUE_WIDTH,
            LOADOUT_ROW_HEIGHT,
        )
        return y + maxOf(1, lines.size) * LOADOUT_ROW_HEIGHT
    }

    private fun addWingDescription(panel: CustomPanelAPI, text: String, y: Float): Float {
        if (!hasText(text)) {
            return LINE_HEIGHT
        }
        val content = panel.createUIElement(CONTENT_WIDTH, MAX_HEIGHT, false)
        content.setParaFontDefault()
        content.setParaFontColor(textColor())
        val paragraphs = text.trim()
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        for (index in paragraphs.indices) {
            content.addPara(tooltipFormat(paragraphs[index]), if (index == 0) 0f else SMALL_PAD, textColor())
        }
        val height = maxOf(LINE_HEIGHT, content.heightSoFar)
        content.position.setSize(CONTENT_WIDTH, height)
        panel.addUIElement(content).inTL(PAD_X, y)
        return height
    }

    private fun addRichPanelLine(panel: CustomPanelAPI, prefix: String, value: String, y: Float) {
        StockReviewTooltipPanel.addLabel(panel, prefix, textColor(), PAD_X, y, 126f, LINE_HEIGHT, Alignment.LMID)
        StockReviewTooltipPanel.addLabel(panel, value, highlightColor(), PAD_X + 126f, y, CONTENT_WIDTH - 126f, LINE_HEIGHT, Alignment.LMID)
    }

    private fun addInlineHighlight(panel: CustomPanelAPI, prefix: String, value: String, suffix: String, y: Float) {
        StockReviewTooltipPanel.addLabel(panel, prefix, textColor(), PAD_X, y, 154f, LINE_HEIGHT, Alignment.LMID)
        StockReviewTooltipPanel.addLabel(panel, value, highlightColor(), PAD_X + 154f, y, 80f, LINE_HEIGHT, Alignment.LMID)
        StockReviewTooltipPanel.addLabel(panel, suffix, textColor(), PAD_X + 234f, y, CONTENT_WIDTH - 234f, LINE_HEIGHT, Alignment.LMID)
    }

    private fun measuredPanelLines(
        panel: CustomPanelAPI,
        text: String,
        width: Float,
        lineHeight: Float,
        maxLines: Int,
    ): List<String> = StockReviewTooltipPanel.wrapLines(panel, text, highlightColor(), width, lineHeight, maxLines)

    private fun tooltipFormat(value: String?): String = value?.replace("%", "%%") ?: ""

    private fun hasText(value: String?): Boolean = value != null && value.trim().isNotEmpty()

    private fun titleColor(): Color = Misc.getTooltipTitleAndLightHighlightColor()

    private fun textColor(): Color = StockReviewTooltipPanel.TEXT

    private fun highlightColor(): Color = Misc.getHighlightColor()
}
