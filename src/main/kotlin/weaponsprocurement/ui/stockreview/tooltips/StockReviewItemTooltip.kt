package weaponsprocurement.ui.stockreview.tooltips

import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterWeaponTooltip
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterWingTooltip
import weaponsprocurement.stock.item.StockDebugItemProfile
import weaponsprocurement.stock.item.StockDebugItemStat
import weaponsprocurement.stock.item.WeaponStockRecord
import java.awt.Color

/**
 * Custom weapon/LPC tooltip approximation for debug and stress records.
 */
class StockReviewItemTooltip private constructor(
    private val record: WeaponStockRecord,
) : TooltipMakerAPI.TooltipCreator {
    private val itemContext = StockReviewItemTooltipContext(record)

    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float =
        if (record.isWing()) StockReviewWingTooltipRenderer.WIDTH else WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        if (record.isWing()) {
            addPaddedWingTooltip(tooltip)
        } else {
            addPaddedWeaponTooltip(tooltip)
        }
    }

    private fun addPaddedWeaponTooltip(tooltip: TooltipMakerAPI) {
        val panelHeight = StockReviewTooltipPanel.maxTooltipHeight()
        val panel = Global.getSettings().createCustom(
            WIDTH,
            panelHeight,
            WimGuiPanelPlugin(StockReviewTooltipPanel.ITEM_BACKGROUND, StockReviewTooltipPanel.ITEM_BORDER),
        )
        val content = panel.createUIElement(CONTENT_WIDTH, TOOLTIP_LAYOUT_HEIGHT, false)
        content.setParaFontDefault()
        content.setParaFontColor(textColor())
        createWeaponTooltip(content)

        val contentHeight = maxOf(1f, content.heightSoFar)
        content.position.setSize(CONTENT_WIDTH, contentHeight)
        panel.addUIElement(content).inTL(OUTER_PAD_X, OUTER_PAD_TOP)
        panel.position.setSize(WIDTH, minOf(panelHeight, contentHeight + OUTER_PAD_TOP + OUTER_PAD_BOTTOM))
        tooltip.addCustom(panel, 0f)
    }

    private fun addPaddedWingTooltip(tooltip: TooltipMakerAPI) {
        val layout = record.debugProfile
            ?.takeIf { record.isWing() }
            ?.let { StockReviewWingTooltipLayoutBuilder.forDebugProfile(it) }
            ?: StockReviewWingTooltipLayoutBuilder.forRecord(record, record.wingSpec ?: return)
        StockReviewWingTooltipRenderer.addTooltip(tooltip, layout, record.ownedCount, itemContext.priceLabel())
    }

    private fun createWeaponTooltip(tooltip: TooltipMakerAPI) {
        val debugProfile = record.debugProfile?.takeIf { !record.isWing() }
        if (debugProfile != null) {
            createDebugWeaponTooltip(tooltip, debugProfile)
            return
        }
        val spec = record.spec ?: return
        tooltip.addTitle(record.displayName, titleColor())
        Misc.addDesignTypePara(tooltip, spec.manufacturer, SMALL_PAD)
        StockReviewWeaponTooltipTextRenderer.addDescription(
            tooltip,
            record.itemId,
            CONTENT_WIDTH,
            SECTION_PAD,
            SMALL_PAD,
        )
        addCargoContext(tooltip)

        addSectionHeading(tooltip, "Primary data", SECTION_PAD)
        StockReviewWeaponTooltipIconGridRenderer.addWeaponGrid(
            tooltip,
            CONTENT_WIDTH,
            StockReviewWeaponIconPlugin.spriteName(spec),
            StockReviewWeaponIconPlugin.motifType(spec),
            weaponRows().primaryRows(spec),
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
            weaponRows().ancillaryRows(spec),
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

    private fun createDebugWeaponTooltip(tooltip: TooltipMakerAPI, profile: StockDebugItemProfile) {
        tooltip.addTitle(profile.tooltipTitle, titleColor())
        Misc.addDesignTypePara(tooltip, profile.manufacturer, SMALL_PAD)
        StockReviewWeaponTooltipTextRenderer.addDebugDescription(
            tooltip,
            profile.description,
            CONTENT_WIDTH,
            SECTION_PAD,
        )
        addCargoContext(tooltip)

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

    private fun addCargoContext(tooltip: TooltipMakerAPI) {
        for (line in itemContext.weaponCargoLines()) {
            addHighlightedPara(tooltip, line.text, line.highlight, SECTION_PAD)
        }
    }

    private fun addHighlightedPara(tooltip: TooltipMakerAPI, text: String, highlight: String?, pad: Float) {
        val label = tooltip.addPara(tooltipFormat(text), pad, textColor(), highlightColor(), highlight)
        label.setHighlight(highlight)
        label.setHighlightColor(highlightColor())
    }

    private fun weaponRows(): StockReviewWeaponTooltipRows = StockReviewWeaponTooltipRows(record)

    companion object {
        private const val VANILLA_TOOLTIP_WIDTH = 400f
        private const val CONTENT_WIDTH = VANILLA_TOOLTIP_WIDTH * 1.25f
        private const val OUTER_PAD_X = 16f
        private const val OUTER_PAD_TOP = 8f
        private const val OUTER_PAD_BOTTOM = OUTER_PAD_X
        private const val WIDTH = CONTENT_WIDTH + 2f * OUTER_PAD_X
        private const val TOOLTIP_LAYOUT_HEIGHT = 1400f
        private const val SECTION_PAD = 9f
        private const val SMALL_PAD = 4f
        private const val SECTION_CONTENT_PAD = 12f
        private const val CUSTOM_TEXT_PAD = 6f
        private const val SECTION_HEADING_HEIGHT = 22f
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun forRecord(record: WeaponStockRecord?, toggleText: String?): TooltipMakerAPI.TooltipCreator? {
            if (record == null) {
                return null
            }
            if (record.isDebug()) {
                return StockReviewItemTooltip(record)
            }
            if (record.isWing() && record.wingSpec == null) {
                return null
            }
            if (!record.isWing() && record.spec == null) {
                return null
            }
            val itemId = record.itemId ?: return null
            val context = StockReviewItemTooltipContext(record)
            if (record.isWing()) {
                val spec = record.wingSpec ?: return null
                return ShatterWingTooltip(
                    wingId = itemId,
                    spec = spec,
                    includeReplacementNotes = false,
                    context = context.shatterContext(),
                )
            }
            val spec = record.spec ?: return null
            return ShatterWeaponTooltip(
                weaponId = itemId,
                spec = spec,
                context = context.shatterContext(),
            )
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
}
