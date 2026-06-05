package weaponsprocurement.ui.stockreview.tooltips

import weaponsprocurement.ui.WimGuiText
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
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
        addDescription(tooltip)
        addCargoContext(tooltip)

        addSectionHeading(tooltip, "Primary data", SECTION_PAD)
        addIconGrid(
            tooltip,
            StockReviewWeaponIconPlugin.spriteName(spec),
            weaponRows().primaryRows(spec),
            true,
            StockReviewWeaponIconPlugin.motifType(spec),
            SECTION_CONTENT_PAD,
        )
        addSpecPara(tooltip, spec.customPrimary, spec.customPrimaryHL, CUSTOM_TEXT_PAD, spec)

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD)
        addIconGrid(tooltip, damageIconSpriteName(spec.damageType), weaponRows().ancillaryRows(spec), false, null, SECTION_CONTENT_PAD)
        addSpecPara(tooltip, spec.customAncillary, spec.customAncillaryHL, CUSTOM_TEXT_PAD, spec)
    }

    private fun createDebugWeaponTooltip(tooltip: TooltipMakerAPI, profile: StockDebugItemProfile) {
        tooltip.addTitle(profile.tooltipTitle, titleColor())
        Misc.addDesignTypePara(tooltip, profile.manufacturer, SMALL_PAD)
        tooltip.addPara(tooltipFormat(truncateForTooltipLines(profile.description, DESCRIPTION_MAX_LINES + 2, CONTENT_WIDTH, tooltip)), SECTION_PAD, textColor())
        addCargoContext(tooltip)

        addSectionHeading(tooltip, "Primary data", SECTION_PAD)
        addIconGrid(tooltip, profile.iconSpriteName, debugRows(profile.primaryRows), false, null, SECTION_CONTENT_PAD)

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD)
        addIconGrid(tooltip, profile.iconSpriteName, debugRows(profile.ancillaryRows), false, null, SECTION_CONTENT_PAD)
    }

    private fun addDescription(tooltip: TooltipMakerAPI) {
        val description: Description = try {
            Global.getSettings().getDescription(record.itemId, Description.Type.WEAPON)
        } catch (_: RuntimeException) {
            null
        } ?: return
        val firstPara = description.text1FirstPara
        if (hasText(firstPara)) {
            val label = tooltip.addPara(tooltipFormat(truncateForTooltipLines(firstPara.trim(), DESCRIPTION_MAX_LINES, CONTENT_WIDTH, tooltip)), SECTION_PAD)
            if (hasText(description.text2) && description.text2.trim().startsWith("-")) {
                label.italicize()
            }
        }
        if (hasText(description.text2) && description.text2.trim().startsWith("-")) {
            val label = tooltip.addPara(tooltipFormat(description.text2.trim()), SMALL_PAD, mutedColor())
            label.italicize()
        }
    }

    private fun addCargoContext(tooltip: TooltipMakerAPI) {
        for (line in itemContext.weaponCargoLines()) {
            addHighlightedPara(tooltip, line.text, line.highlight, SECTION_PAD)
        }
    }

    private fun addIconGrid(
        tooltip: TooltipMakerAPI,
        spriteName: String?,
        rows: List<StockReviewTooltipStatRow>,
        weaponTile: Boolean,
        motifType: WeaponAPI.WeaponType?,
        pad: Float,
    ) {
        if (rows.isEmpty()) {
            return
        }
        val visibleRows = cappedRows(rows, MAX_ICON_GRID_ROWS)
        val height = maxOf(ICON_SIZE + ICON_TOP, visibleRows.size * GRID_ROW_HEIGHT)
        val panel = Global.getSettings().createCustom(CONTENT_WIDTH, height, BaseCustomUIPanelPlugin())
        val icon = panel.createCustomPanel(
            ICON_SIZE,
            ICON_SIZE,
            if (weaponTile) StockReviewWeaponIconPlugin(spriteName, motifType) else StockReviewTooltipIconPanelPlugin(spriteName, ICON_INSET),
        )
        panel.addComponent(icon).inTL(ICON_LEFT, minOf(ICON_TOP, maxOf(0f, height - ICON_SIZE)))

        for (i in visibleRows.indices) {
            val row = visibleRows[i]
            addStatRow(panel, ICON_LEFT + ICON_SIZE + ICON_GRID_GAP, i * GRID_ROW_HEIGHT, GRID_WIDTH, GRID_ROW_HEIGHT, row)
        }
        tooltip.addCustom(panel, pad)
        tooltip.addSpacer(GRID_BOTTOM_PAD)
    }

    private fun cappedRows(rows: List<StockReviewTooltipStatRow>, maxRows: Int): List<StockReviewTooltipStatRow> {
        if (rows.size <= maxRows) {
            return rows
        }
        val capped = ArrayList(rows.subList(0, maxOf(1, maxRows)))
        capped[capped.size - 1] = StockReviewTooltipStatRow("", "...")
        return capped
    }

    private fun addSpecPara(tooltip: TooltipMakerAPI, text: String?, highlight: String?, pad: Float, spec: WeaponSpecAPI) {
        if (!hasText(text)) {
            return
        }
        tooltip.addSpacer(SMALL_PAD)
        val rawHighlights = splitHighlights(highlight)
        val substitutedText = substituteFormatSpecifiers(text, rawHighlights, spec)
        val displayText = truncateForTooltipLines(substitutedText, CUSTOM_TEXT_MAX_LINES, CONTENT_WIDTH, tooltip)
        val highlights = visibleHighlights(displayText, rawHighlights)
        if (highlights.isNotEmpty()) {
            val label = tooltip.addPara(tooltipFormat(displayText), pad, textColor(), highlightColor(), *highlights)
            label.setHighlight(*highlights)
            label.setHighlightColor(highlightColor())
            tooltip.addSpacer(SMALL_PAD)
            return
        }
        tooltip.addPara(tooltipFormat(displayText), pad, textColor())
        tooltip.addSpacer(SMALL_PAD)
    }

    private fun truncateForTooltipLines(text: String?, maxLines: Int, width: Float, tooltip: TooltipMakerAPI): String {
        val source = text?.takeIf { hasText(it) } ?: return text ?: ""
        val normalized = source.trim().replace(Regex("\\s+"), " ")
        return WimGuiText.wrapToWidth(normalized, tooltip, width, maxLines).joinToString("\n")
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
        private const val GRID_BOTTOM_PAD = 8f
        private const val GRID_ROW_HEIGHT = 24f
        private const val SECTION_HEADING_HEIGHT = 22f
        private const val ICON_SIZE = 92f
        private const val ICON_LEFT = 28f
        private const val ICON_TOP = 12f
        private const val ICON_INSET = 2f
        private const val ICON_GRID_GAP = 28f
        private const val GRID_WIDTH = CONTENT_WIDTH - ICON_LEFT - ICON_SIZE - ICON_GRID_GAP - 8f
        private const val GRID_MIN_LABEL_WIDTH = 108f
        private const val GRID_MAX_LABEL_WIDTH = 252f
        private const val GRID_MIN_VALUE_WIDTH = 86f
        private const val MAX_ICON_GRID_ROWS = 10
        private const val DESCRIPTION_MAX_LINES = 4
        private const val CUSTOM_TEXT_MAX_LINES = 3
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

        private fun addStatRow(panel: CustomPanelAPI, x: Float, y: Float, width: Float, height: Float, row: StockReviewTooltipStatRow?) {
            if (row == null) {
                return
            }
            if (!hasText(row.label)) {
                addPanelLabel(panel, row.value, highlightColor(), x, y, width, height, Alignment.RMID)
                return
            }
            StockReviewTooltipPanel.addStatRow(
                panel,
                row.label,
                row.value,
                textColor(),
                highlightColor(),
                x,
                y,
                width,
                height,
                GRID_MIN_LABEL_WIDTH,
                GRID_MAX_LABEL_WIDTH,
                GRID_MIN_VALUE_WIDTH,
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

        private fun damageIconSpriteName(type: DamageType?): String? {
            var key = "icon_other"
            if (DamageType.KINETIC == type) {
                key = "icon_kinetic"
            } else if (DamageType.HIGH_EXPLOSIVE == type) {
                key = "icon_high_explosive"
            } else if (DamageType.FRAGMENTATION == type) {
                key = "icon_fragmentation"
            } else if (DamageType.ENERGY == type) {
                key = "icon_energy"
            }
            return try {
                Global.getSettings().getSpriteName("ui", key)
            } catch (_: RuntimeException) {
                null
            }
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

        private fun splitHighlights(highlight: String?): Array<String> {
            val source = highlight?.takeIf { hasText(it) } ?: return emptyArray()
            val result = ArrayList<String>()
            for (raw in source.split("|")) {
                val trimmed = raw.trim()
                if (trimmed.isNotEmpty()) {
                    result.add(trimmed)
                }
            }
            return result.toTypedArray()
        }

        private fun titleColor(): Color = Misc.getTooltipTitleAndLightHighlightColor()

        private fun textColor(): Color = StockReviewTooltipPanel.TEXT

        private fun mutedColor(): Color = StockReviewTooltipPanel.MUTED

        private fun highlightColor(): Color = Misc.getHighlightColor()

        private fun visibleHighlights(text: String?, highlights: Array<String>?): Array<String> {
            if (!hasText(text) || highlights == null || highlights.isEmpty()) {
                return emptyArray()
            }
            val result = ArrayList<String>()
            val source = text ?: return emptyArray()
            for (highlight in highlights) {
                if (hasText(highlight) && source.contains(highlight)) {
                    result.add(highlight)
                }
            }
            return result.toTypedArray()
        }

        private fun substituteFormatSpecifiers(text: String?, highlights: Array<String>, spec: WeaponSpecAPI): String {
            if (!hasText(text)) {
                return text ?: ""
            }
            val source = text ?: return ""
            val result = StringBuilder(source.length)
            var highlightIndex = 0
            var i = 0
            while (i < source.length) {
                val c = source[i]
                if (c != '%' || i + 1 >= source.length) {
                    result.append(c)
                    i++
                    continue
                }
                val next = source[i + 1]
                if (next == '%') {
                    result.append('%')
                    i += 2
                    continue
                }
                if (next == 's' || next == 'd' || next == 'f') {
                    result.append(formatHighlightValue(highlights, highlightIndex, spec))
                    highlightIndex++
                    i += 2
                    continue
                }
                result.append(c)
                i++
            }
            return result.toString()
        }

        private fun formatHighlightValue(highlights: Array<String>?, index: Int, spec: WeaponSpecAPI?): String {
            if (highlights != null && index >= 0 && index < highlights.size && hasText(highlights[index])) {
                return highlights[index].trim()
            }
            if (index == 0 && spec != null && spec.derivedStats != null) {
                val value = if (spec.isBeam) spec.derivedStats.dps else spec.derivedStats.damagePerShot
                if (validNumber(value) && value > 0f) {
                    return formatOneDecimalTrim(value)
                }
            }
            return "?"
        }

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
