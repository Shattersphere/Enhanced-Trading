package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.stock.item.StockDebugItemProfile
import weaponsprocurement.stock.item.StockDebugItemStat
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import java.awt.Color

internal object StockReviewWeaponTooltipRenderer {
    private const val VANILLA_TOOLTIP_WIDTH = 400f
    private const val CONTENT_WIDTH = VANILLA_TOOLTIP_WIDTH * 1.25f
    private const val OUTER_PAD_X = 16f
    private const val OUTER_PAD_TOP = 8f
    private const val OUTER_PAD_BOTTOM = OUTER_PAD_X
    const val WIDTH: Float = CONTENT_WIDTH + 2f * OUTER_PAD_X
    private const val TOOLTIP_LAYOUT_HEIGHT = 1400f
    private const val SECTION_PAD = 9f
    private const val SMALL_PAD = 4f
    private const val SECTION_CONTENT_PAD = 12f
    private const val CUSTOM_TEXT_PAD = 6f
    private const val SECTION_HEADING_HEIGHT = 22f

    fun addTooltip(
        tooltip: TooltipMakerAPI,
        record: WeaponStockRecord,
        itemContext: StockReviewItemTooltipContext,
    ) {
        val panelHeight = StockReviewTooltipPanel.maxTooltipHeight()
        val panel = Global.getSettings().createCustom(
            WIDTH,
            panelHeight,
            WimGuiPanelPlugin(StockReviewTooltipPanel.ITEM_BACKGROUND, StockReviewTooltipPanel.ITEM_BORDER),
        )
        val content = panel.createUIElement(CONTENT_WIDTH, TOOLTIP_LAYOUT_HEIGHT, false)
        content.setParaFontDefault()
        content.setParaFontColor(textColor())
        addContent(content, record, itemContext)

        val contentHeight = maxOf(1f, content.heightSoFar)
        content.position.setSize(CONTENT_WIDTH, contentHeight)
        panel.addUIElement(content).inTL(OUTER_PAD_X, OUTER_PAD_TOP)
        panel.position.setSize(WIDTH, minOf(panelHeight, contentHeight + OUTER_PAD_TOP + OUTER_PAD_BOTTOM))
        tooltip.addCustom(panel, 0f)
    }

    private fun addContent(
        tooltip: TooltipMakerAPI,
        record: WeaponStockRecord,
        itemContext: StockReviewItemTooltipContext,
    ) {
        val debugProfile = record.debugProfile?.takeIf { !record.isWing() }
        if (debugProfile != null) {
            addDebugContent(tooltip, debugProfile, itemContext)
            return
        }
        val spec = record.spec ?: return
        val rows = StockReviewWeaponTooltipRows(record)
        tooltip.addTitle(record.displayName, titleColor())
        Misc.addDesignTypePara(tooltip, spec.manufacturer, SMALL_PAD)
        StockReviewWeaponTooltipTextRenderer.addDescription(
            tooltip,
            record.itemId,
            CONTENT_WIDTH,
            SECTION_PAD,
            SMALL_PAD,
        )
        addCargoContext(tooltip, itemContext)

        addSectionHeading(tooltip, "Primary data", SECTION_PAD)
        StockReviewWeaponTooltipIconGridRenderer.addWeaponGrid(
            tooltip,
            CONTENT_WIDTH,
            StockReviewWeaponIconPlugin.spriteName(spec),
            StockReviewWeaponIconPlugin.motifType(spec),
            rows.primaryRows(spec),
            SECTION_CONTENT_PAD,
        )
        StockReviewWeaponTooltipTextRenderer.addCustomSpecPara(
            tooltip,
            spec.customPrimary,
            spec.customPrimaryHL,
            spec,
            CONTENT_WIDTH,
            CUSTOM_TEXT_PAD,
            SMALL_PAD,
        )

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD)
        StockReviewWeaponTooltipIconGridRenderer.addDamageTypeGrid(
            tooltip,
            CONTENT_WIDTH,
            spec.damageType,
            rows.ancillaryRows(spec),
            SECTION_CONTENT_PAD,
        )
        StockReviewWeaponTooltipTextRenderer.addCustomSpecPara(
            tooltip,
            spec.customAncillary,
            spec.customAncillaryHL,
            spec,
            CONTENT_WIDTH,
            CUSTOM_TEXT_PAD,
            SMALL_PAD,
        )
    }

    private fun addDebugContent(
        tooltip: TooltipMakerAPI,
        profile: StockDebugItemProfile,
        itemContext: StockReviewItemTooltipContext,
    ) {
        tooltip.addTitle(profile.tooltipTitle, titleColor())
        Misc.addDesignTypePara(tooltip, profile.manufacturer, SMALL_PAD)
        StockReviewWeaponTooltipTextRenderer.addDebugDescription(
            tooltip,
            profile.description,
            CONTENT_WIDTH,
            SECTION_PAD,
        )
        addCargoContext(tooltip, itemContext)

        addSectionHeading(tooltip, "Primary data", SECTION_PAD)
        StockReviewWeaponTooltipIconGridRenderer.addSpriteGrid(
            tooltip,
            CONTENT_WIDTH,
            profile.iconSpriteName,
            debugRows(profile.primaryRows),
            SECTION_CONTENT_PAD,
        )

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD)
        StockReviewWeaponTooltipIconGridRenderer.addSpriteGrid(
            tooltip,
            CONTENT_WIDTH,
            profile.iconSpriteName,
            debugRows(profile.ancillaryRows),
            SECTION_CONTENT_PAD,
        )
    }

    private fun addCargoContext(tooltip: TooltipMakerAPI, itemContext: StockReviewItemTooltipContext) {
        for (line in itemContext.weaponCargoLines()) {
            addHighlightedPara(tooltip, line.text, line.highlight, SECTION_PAD)
        }
    }

    private fun addHighlightedPara(tooltip: TooltipMakerAPI, text: String, highlight: String?, pad: Float) {
        val label = tooltip.addPara(tooltipFormat(text), pad, textColor(), highlightColor(), highlight)
        label.setHighlight(highlight)
        label.setHighlightColor(highlightColor())
    }

    private fun addSectionHeading(tooltip: TooltipMakerAPI, text: String, pad: Float) {
        val panel = StockReviewTooltipPanel.createSectionBand(CONTENT_WIDTH, SECTION_HEADING_HEIGHT)
        addPanelLabel(panel, text, textColor(), 0f, 0f, CONTENT_WIDTH, SECTION_HEADING_HEIGHT, Alignment.MID)
        tooltip.addCustom(panel, pad)
    }

    private fun addPanelLabel(
        parent: CustomPanelAPI,
        text: String?,
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alignment: Alignment,
    ) {
        StockReviewTooltipPanel.addLabel(parent, text, color, x, y, width, height, alignment)
    }

    private fun debugRows(rows: List<StockDebugItemStat>): List<StockReviewTooltipStatRow> =
        rows.map { StockReviewTooltipStatRow(it.label, it.value) }

    private fun titleColor(): Color = Misc.getTooltipTitleAndLightHighlightColor()

    private fun textColor(): Color = StockReviewTooltipPanel.TEXT

    private fun highlightColor(): Color = Misc.getHighlightColor()

    private fun tooltipFormat(value: String?): String = value?.replace("%", "%%") ?: ""
}
