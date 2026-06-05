package weaponsprocurement.ui.stockreview.tooltips

import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.FighterWingSpecAPI
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
import java.util.Locale

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
            ?.let { debugWingLayout(it) }
            ?: wingLayout(record, record.wingSpec ?: return)
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

        private fun addRow(rows: MutableList<StockReviewTooltipStatRow>, label: String, value: String?) {
            if (!hasMeaningful(value)) {
                return
            }
            rows.add(StockReviewTooltipStatRow(label, value ?: ""))
        }

        private fun debugRows(rows: List<StockDebugItemStat>): List<StockReviewTooltipStatRow> =
            rows.map { StockReviewTooltipStatRow(it.label, it.value) }

        private fun wingLayout(record: WeaponStockRecord, spec: FighterWingSpecAPI): StockReviewWingTooltipLayout {
            val variant = spec.variant
            val hull = variant?.hullSpec
            val manufacturer = hull?.manufacturer?.takeIf { hasText(it) } ?: "Unknown"
            val description = wingDescription(hull?.descriptionId)
            val rows = ArrayList<StockReviewTooltipStatRow>()
            addRow(rows, "Primary role", spec.roleDesc?.takeIf { hasText(it) } ?: format(spec.role))
            addRow(rows, "Ordnance points", record.wingOpCostLabel)
            addRow(rows, "Crew per fighter", integer(hull?.minCrew))
            addRow(rows, "Maximum engagement range", record.rangeLabel)
            addRow(rows, "Fighters in wing", spec.numFighters.toString())
            addRow(rows, "Base replacement time (seconds)", integer(spec.refitTime))
            addRow(rows, "Hull integrity", integer(hull?.hitpoints))
            addRow(rows, "Armor rating", integer(hull?.armorRating))
            addRow(rows, "Top speed", integer(hull?.engineSpec?.maxSpeed))
            addRow(rows, "Flux capacity", integer(hull?.fluxCapacity))
            addRow(rows, "Flux dissipation", integer(hull?.fluxDissipation))
            addRow(rows, "Shield efficiency", shieldEfficiency(hull?.shieldSpec?.fluxPerDamageAbsorbed))
            addRow(rows, "Shield arc", shieldArc(hull?.shieldSpec?.arc))
            val system = wingSystemLabel(hull?.shipSystemId)
            val armaments = wingArmamentsLabel(spec)
            return StockReviewWingTooltipLayout(wingTitle(spec), manufacturer, description, rows, system, armaments)
        }

        private fun debugWingLayout(profile: StockDebugItemProfile): StockReviewWingTooltipLayout {
            return StockReviewWingTooltipLayout(
                profile.tooltipTitle,
                profile.manufacturer,
                profile.description,
                debugRows(profile.wingTechnicalRows),
                profile.wingSystem,
                profile.wingArmaments,
            )
        }

        private fun wingTitle(spec: FighterWingSpecAPI): String {
            val name = spec.wingName?.takeIf { hasText(it) } ?: spec.id
            return if (name.endsWith("LPC", ignoreCase = true)) name else "$name LPC"
        }

        private fun wingDescription(descriptionId: String?): String {
            val id = descriptionId?.takeIf { hasText(it) } ?: return ""
            val description = try {
                Global.getSettings().getDescription(id, Description.Type.SHIP)
            } catch (_: RuntimeException) {
                null
            } ?: return ""
            val paragraphs = description.text1Paras.orEmpty().filter { it.isNotBlank() }
            return paragraphs.joinToString("\n\n") { it.trim() }
        }

        private fun wingSystemLabel(systemId: String?): String {
            val id = systemId?.takeIf { hasText(it) } ?: return "None"
            return try {
                Global.getSettings().getShipSystemSpec(id)?.name ?: id
            } catch (_: RuntimeException) {
                id
            }
        }

        private fun wingArmamentsLabel(spec: FighterWingSpecAPI): String {
            val variant = spec.variant
            val names = ArrayList<String>()
            if (variant != null) {
                for (slot in variant.fittedWeaponSlots.orEmpty()) {
                    val name = try {
                        variant.getWeaponSpec(slot)?.weaponName
                    } catch (_: RuntimeException) {
                        null
                    }
                    if (hasText(name)) {
                        names.add(name!!)
                    }
                }
                if (names.isEmpty()) {
                    for (weaponId in variant.hullSpec?.builtInWeapons?.values.orEmpty()) {
                        if (!hasText(weaponId)) continue
                        try {
                            val name = Global.getSettings().getWeaponSpec(weaponId!!)?.weaponName
                            if (hasText(name)) {
                                names.add(name!!)
                            }
                        } catch (_: RuntimeException) {
                            continue
                        }
                    }
                }
            }
            if (names.isEmpty()) {
                return "None"
            }
            return names.groupingBy { it }.eachCount()
                .entries
                .sortedBy { it.key }
                .joinToString(", ") { "${it.value}x ${it.key}" }
        }

        private fun integer(value: Float?): String? {
            if (value == null || !validNumber(value)) {
                return null
            }
            return Math.round(value).toString()
        }

        private fun shieldEfficiency(value: Float?): String? {
            if (value == null || !validNumber(value) || value <= 0f) {
                return null
            }
            return formatOneDecimalTrim(value)
        }

        private fun shieldArc(value: Float?): String? {
            if (value == null || !validNumber(value) || value <= 0f) {
                return null
            }
            return Math.round(value).toString()
        }

        private fun titleColor(): Color = Misc.getTooltipTitleAndLightHighlightColor()

        private fun textColor(): Color = StockReviewTooltipPanel.TEXT

        private fun highlightColor(): Color = Misc.getHighlightColor()

        private fun tooltipFormat(value: String?): String = value?.replace("%", "%%") ?: ""

        private fun hasText(value: String?): Boolean = value != null && value.trim().isNotEmpty()

        private fun hasMeaningful(value: String?): Boolean {
            if (!hasText(value)) {
                return false
            }
            val trimmed = value?.trim() ?: return false
            return trimmed != "?" && trimmed != "---" && !trimmed.equals("None", ignoreCase = true)
        }

        private fun validNumber(value: Float): Boolean = !value.isNaN() && !value.isInfinite()

        private fun format(value: Any?): String {
            if (value == null) {
                return "?"
            }
            var text = value.toString().replace('_', ' ').trim()
            if (text.isEmpty() || text == "?") {
                return "?"
            }
            text = text.lowercase(Locale.US)
            val result = StringBuilder(text.length)
            var capitalize = true
            for (c in text) {
                if (Character.isWhitespace(c) || c == '/' || c == '-') {
                    capitalize = true
                    result.append(c)
                } else if (capitalize) {
                    result.append(c.uppercaseChar())
                    capitalize = false
                } else {
                    result.append(c)
                }
            }
            return result.toString()
        }

        private fun formatOneDecimalTrim(value: Float): String {
            if (!validNumber(value)) {
                return "?"
            }
            val rounded = Math.round(value)
            if (Math.abs(value - rounded) < 0.05f) {
                return rounded.toString()
            }
            return String.format(Locale.US, "%.1f", value)
        }
    }
}
