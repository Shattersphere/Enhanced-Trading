package weaponsprocurement.ui.stockreview.tooltips

import weaponsprocurement.ui.WimGuiText
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.rendering.StockReviewIconLayout
import weaponsprocurement.ui.stockreview.rendering.StockReviewSpriteRenderer
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.trade.quote.CreditFormat
import weaponsprocurement.stock.item.StockDebugItemProfile
import weaponsprocurement.stock.item.StockDebugItemStat
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.stock.item.SubmarketWeaponStock
import weaponsprocurement.stock.item.WeaponStockRecord
import java.awt.Color
import java.util.Locale

/**
 * Custom weapon/LPC tooltip approximation. Debug records and real specs share this path so
 * stress samples exercise the same production layout as normal rows.
 */
class StockReviewItemTooltip private constructor(
    private val record: WeaponStockRecord,
) : TooltipMakerAPI.TooltipCreator {
    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = if (record.isWing()) WING_WIDTH else WIDTH

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
        val panel = Global.getSettings().createCustom(
            WING_WIDTH,
            StockReviewTooltipPanel.maxTooltipHeight(WING_LINE_HEIGHT),
            WimGuiPanelPlugin(StockReviewTooltipPanel.ITEM_BACKGROUND, StockReviewTooltipPanel.ITEM_BORDER),
        )
        val descriptionLines = measuredPanelLines(panel, layout.descriptionText, WING_CONTENT_WIDTH, WING_LINE_HEIGHT, WING_DESCRIPTION_MAX_LINES)
        val systemLines = measuredPanelLines(panel, layout.systemText, WING_LOADOUT_VALUE_WIDTH, WING_LOADOUT_ROW_HEIGHT, WING_LOADOUT_MAX_LINES)
        val armamentLines = measuredPanelLines(panel, layout.armamentsText, WING_LOADOUT_VALUE_WIDTH, WING_LOADOUT_ROW_HEIGHT, WING_LOADOUT_MAX_LINES)
        var y = WING_PAD_TOP
        addPanelLabel(panel, layout.title, titleColor(), WING_PAD_X, y, WING_CONTENT_WIDTH, 28f, Alignment.LMID)
        y += 34f
        addRichPanelLine(panel, "Design type:", layout.manufacturer, y)
        y += 32f
        addPanelLines(
            panel,
            descriptionLines,
            textColor(),
            WING_PAD_X,
            y,
            WING_CONTENT_WIDTH,
            WING_LINE_HEIGHT,
        )
        y += maxOf(1, descriptionLines.size) * WING_LINE_HEIGHT + 10f
        priceLabel()?.let { price ->
            addInlineHighlight(panel, "Sells for:", price, " per unit.", y)
            y += WING_LINE_HEIGHT
        }
        addInlineHighlight(panel, "You own a total of", record.ownedCount.toString(), " fighter LPCs of this type.", y)
        y += WING_LINE_HEIGHT + 10f

        addWingSectionHeading(panel, "Technical data", y)
        y += SECTION_HEADING_HEIGHT + 10f
        for (row in layout.technicalRows) {
            addWingStatRow(panel, row, y)
            y += WING_GRID_ROW_HEIGHT
        }
        y += 10f
        y = addWingLoadoutLine(panel, "System:", systemLines, y)
        y = addWingLoadoutLine(panel, "Armaments:", armamentLines, y)
        panel.position.setSize(WING_WIDTH, StockReviewTooltipPanel.capHeight(y + WING_PAD_BOTTOM, WING_LINE_HEIGHT))
        tooltip.addCustom(panel, 0f)
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
            primaryRows(spec),
            true,
            StockReviewWeaponIconPlugin.motifType(spec),
            SECTION_CONTENT_PAD,
        )
        addSpecPara(tooltip, spec.customPrimary, spec.customPrimaryHL, CUSTOM_TEXT_PAD, spec)

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD)
        addIconGrid(tooltip, damageIconSpriteName(spec.damageType), ancillaryRows(spec), false, null, SECTION_CONTENT_PAD)
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
        val cargoSpace = cargoSpaceLabel()
        if (hasText(cargoSpace)) {
            addHighlightedPara(tooltip, "Cargo space: $cargoSpace per unit.", cargoSpace, SECTION_PAD)
        }

        val price = priceLabel()
        if (hasText(price)) {
            addHighlightedPara(tooltip, "Price: $price per unit.", price, SECTION_PAD)
        }

        val count = record.ownedCount.toString()
        val plural = if (record.ownedCount == 1) "weapon" else "weapons"
        addHighlightedPara(tooltip, "You own a total of $count $plural of this type.", count, SECTION_PAD)
    }

    private fun primaryRows(spec: WeaponSpecAPI): List<StatRow> {
        val rows = ArrayList<StatRow>()
        addRow(rows, "Primary role", format(spec.primaryRoleStr))
        addRow(rows, "Mount type", format(spec.size) + ", " + format(spec.mountType))
        addMountNotes(rows, spec)
        addRow(rows, "Ordnance points", record.opCostLabel)
        addRow(rows, "Range", record.rangeLabel)
        addRow(rows, damageLabel(spec), damageValue(spec))
        if (hasMeaningful(record.empLabel) && record.empLabel != "0") {
            addRow(rows, "EMP damage", record.empLabel)
        }
        if (!spec.isNoDPSInTooltip) {
            addRow(rows, "Damage / second", record.sustainedDamagePerSecondLabel)
        }
        addRow(rows, "Flux / second", record.sustainedFluxPerSecondLabel)
        addRow(rows, "Flux / shot", fluxPerShotLabel(spec))
        addRow(rows, "Flux / damage", record.fluxPerDamageLabel)
        return rows
    }

    private fun ancillaryRows(spec: WeaponSpecAPI): List<StatRow> {
        val rows = ArrayList<StatRow>()
        val damageType = spec.damageType
        addRow(rows, "Damage type", damageType?.displayName ?: "?")
        addRow(rows, "", damageMultiplierLabel(damageType))
        addRow(rows, "Speed", format(spec.speedStr))
        addRow(rows, "Tracking", format(spec.trackingStr))
        addRow(rows, "Accuracy", format(spec.accuracyStr))
        addRow(rows, "Turn rate", format(spec.turnRateStr))
        if (spec.burstSize > 1) {
            addRow(rows, "Burst size", spec.burstSize.toString())
        }
        addRow(rows, "Refire delay (seconds)", record.refireSecondsLabel)
        if (spec.usesAmmo()) {
            addRow(rows, "Ammo", record.maxAmmoLabel)
            addRow(rows, "Recharge / second", record.ammoGainLabel)
            addRow(rows, "Reload time (seconds)", record.secPerReloadLabel)
        }
        if (spec.isBeam) {
            addRow(rows, "Charge up", record.beamChargeUpLabel)
            addRow(rows, "Charge down", record.beamChargeDownLabel)
        }
        return rows
    }

    private fun addIconGrid(
        tooltip: TooltipMakerAPI,
        spriteName: String?,
        rows: List<StatRow>,
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
            if (weaponTile) StockReviewWeaponIconPlugin(spriteName, motifType) else IconPanelPlugin(spriteName),
        )
        panel.addComponent(icon).inTL(ICON_LEFT, minOf(ICON_TOP, maxOf(0f, height - ICON_SIZE)))

        for (i in visibleRows.indices) {
            val row = visibleRows[i]
            addStatRow(panel, ICON_LEFT + ICON_SIZE + ICON_GRID_GAP, i * GRID_ROW_HEIGHT, GRID_WIDTH, GRID_ROW_HEIGHT, row)
        }
        tooltip.addCustom(panel, pad)
        tooltip.addSpacer(GRID_BOTTOM_PAD)
    }

    private fun cappedRows(rows: List<StatRow>, maxRows: Int): List<StatRow> {
        if (rows.size <= maxRows) {
            return rows
        }
        val capped = ArrayList(rows.subList(0, maxOf(1, maxRows)))
        capped[capped.size - 1] = StatRow("", "...")
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

    private fun cargoSpaceLabel(): String? {
        record.debugProfile?.cargoSpaceLabel?.let { return it }
        val cargoSpace = unitCargoSpace()
        return if (validNumber(cargoSpace)) formatOneDecimalTrim(cargoSpace) else null
    }

    private fun unitCargoSpace(): Float {
        val stocks: List<SubmarketWeaponStock> = record.submarketStocks
        for (stock in stocks) {
            val value = stock.unitCargoSpace
            if (validNumber(value) && value > 0f) {
                return value
            }
        }
        val reference = StockItemStacks.referenceUnitCargoSpace(record.itemType, record.itemId)
        if (validNumber(reference) && reference > 0f) {
            return reference
        }
        return Float.NaN
    }

    private fun priceLabel(): String? {
        record.debugProfile?.priceLabel?.let { return it }
        var price = record.cheapestPurchasableUnitPrice
        if (price == Int.MAX_VALUE) {
            price = StockItemStacks.referenceBaseUnitPrice(record.itemType, record.itemId)
            if (price <= 0) {
                price = Math.round(maxOf(0f, record.spec?.baseValue ?: 0f))
            }
        }
        return if (price <= 0) null else CreditFormat.credits(price)
    }

    private fun damageValue(spec: WeaponSpecAPI): String? {
        if (spec.hasTag("damage_special")) {
            return "Special"
        }
        val damage = record.damageLabel
        if (!hasMeaningful(damage)) {
            return damage
        }
        val burstSize = spec.burstSize
        if (!spec.isBeam && burstSize > 1) {
            return damage + "x" + burstSize
        }
        return damage
    }

    private fun fluxPerShotLabel(spec: WeaponSpecAPI): String {
        val projectile = projectileSpec(spec) ?: return "?"
        val energy = projectile.energyPerShot
        return if (validNumber(energy) && energy > 0f) Math.round(energy).toString() else "0"
    }

    private data class StatRow(
        val label: String = "",
        val value: String = "",
    ) {
        fun isSpacer(): Boolean = label.isEmpty() && value.isEmpty()
    }

    private data class WingTooltipLayout(
        val title: String,
        val manufacturer: String,
        val descriptionText: String,
        val descriptionLines: List<String>,
        val technicalRows: List<StatRow>,
        val systemText: String,
        val systemLines: List<String>,
        val armamentsText: String,
        val armamentLines: List<String>,
        val height: Float,
    )

    private class IconPanelPlugin(private val spriteName: String?) : BaseCustomUIPanelPlugin() {
        private var position: PositionAPI? = null

        override fun positionChanged(position: PositionAPI?) {
            this.position = position
        }

        override fun render(alphaMult: Float) {
            val currentPosition = position ?: return
            val x = currentPosition.x
            val y = currentPosition.y
            val width = currentPosition.width
            val height = currentPosition.height
            renderSprite(x, y, width, height, ICON_INSET, alphaMult)
        }

        private fun renderSprite(x: Float, y: Float, width: Float, height: Float, inset: Float, alphaMult: Float) {
            val maxWidth = maxOf(1f, width - 2f * inset)
            val maxHeight = maxOf(1f, height - 2f * inset)
            StockReviewSpriteRenderer.renderFittedSprite(
                spriteName,
                Color.WHITE,
                StockReviewIconLayout.visualCenterX(x, width),
                y + height * 0.5f,
                maxWidth,
                maxHeight,
                alphaMult,
            )
        }

    }

    companion object {
        private const val VANILLA_TOOLTIP_WIDTH = 400f
        private const val CONTENT_WIDTH = VANILLA_TOOLTIP_WIDTH * 1.25f
        private const val WING_WIDTH = 560f
        private const val WING_PAD_X = 16f
        private const val WING_PAD_TOP = 10f
        private const val WING_PAD_BOTTOM = 16f
        private const val WING_CONTENT_WIDTH = WING_WIDTH - 2f * WING_PAD_X
        private const val WING_LINE_HEIGHT = 23f
        private const val WING_GRID_ROW_HEIGHT = 22f
        private const val WING_MIN_LABEL_WIDTH = 116f
        private const val WING_MAX_LABEL_WIDTH = 318f
        private const val WING_MIN_VALUE_WIDTH = 118f
        private const val WING_LOADOUT_LABEL_WIDTH = 108f
        private const val WING_LOADOUT_VALUE_X = WING_PAD_X + WING_LOADOUT_LABEL_WIDTH
        private const val WING_LOADOUT_VALUE_WIDTH = WING_CONTENT_WIDTH - WING_LOADOUT_LABEL_WIDTH
        private const val WING_LOADOUT_ROW_HEIGHT = 22f
        private const val WING_DESCRIPTION_MAX_LINES = 9
        private const val WING_LOADOUT_MAX_LINES = 4
        private const val WING_MAX_HEIGHT = 860f
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
        private const val ESTIMATED_DESCRIPTION_CHAR_WIDTH = 8f
        private const val WING_DESCRIPTION_CHAR_WIDTH = 7.4f
        private const val WING_LOADOUT_CHAR_WIDTH = 6.9f

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
            return StockReviewItemTooltip(record)
        }

        private fun addStatRow(panel: CustomPanelAPI, x: Float, y: Float, width: Float, height: Float, row: StatRow?) {
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

        private fun damageLabel(spec: WeaponSpecAPI): String = if (spec.hasTag("damage_special")) "Special" else "Damage"

        private fun addMountNotes(rows: MutableList<StatRow>, spec: WeaponSpecAPI) {
            val required = requiredMountSlots(spec)
            if (hasText(required)) {
                addRow(rows, "", required)
            }
            if (spec.type != null && spec.mountType != null && spec.type != spec.mountType) {
                addRow(rows, "", "Counts as ${format(spec.type)} for stat modifiers")
            }
        }

        private fun requiredMountSlots(spec: WeaponSpecAPI): String? {
            if (spec.mountType == null || spec.type == null || spec.mountType == spec.type) {
                return null
            }
            return when (spec.mountType) {
                WeaponAPI.WeaponType.COMPOSITE -> "Requires a Ballistic, Missile, or Composite slot"
                WeaponAPI.WeaponType.HYBRID -> "Requires a Ballistic, Energy, or Hybrid slot"
                WeaponAPI.WeaponType.SYNERGY -> "Requires an Energy, Missile, or Synergy slot"
                WeaponAPI.WeaponType.UNIVERSAL -> "Requires a Ballistic, Energy, Missile, or Universal slot"
                else -> null
            }
        }

        private fun projectileSpec(spec: WeaponSpecAPI): ProjectileWeaponSpecAPI? =
            if (spec is ProjectileWeaponSpecAPI) spec else null

        private fun damageMultiplierLabel(damageType: DamageType?): String {
            if (damageType == null) {
                return "?"
            }
            val shield = Math.round(damageType.shieldMult * 100f)
            val armor = Math.round(damageType.armorMult * 100f)
            val hull = Math.round(damageType.hullMult * 100f)
            if (DamageType.KINETIC == damageType) {
                return "$shield% vs shields, $armor% vs armor"
            }
            if (DamageType.HIGH_EXPLOSIVE == damageType) {
                return "$armor% vs armor, $shield% vs shields"
            }
            if (DamageType.FRAGMENTATION == damageType) {
                return if (shield == armor) {
                    "$shield% vs shields and armor, $hull% vs hull"
                } else {
                    "$shield% vs shields, $armor% vs armor, $hull% vs hull"
                }
            }
            if (shield == 100 && armor == 100 && hull == 100) {
                return damageType.description
            }
            val parts = ArrayList<String>()
            parts.add("$shield% vs shields")
            parts.add("$armor% vs armor")
            parts.add("$hull% vs hull")
            return parts.joinToString(", ")
        }

        private fun addRow(rows: MutableList<StatRow>, label: String, value: String?) {
            if (!hasMeaningful(value)) {
                return
            }
            rows.add(StatRow(label, value ?: ""))
        }

        private fun debugRows(rows: List<StockDebugItemStat>): List<StatRow> =
            rows.map { StatRow(it.label, it.value) }

        private fun wingLayout(record: WeaponStockRecord, spec: FighterWingSpecAPI): WingTooltipLayout {
            val variant = spec.variant
            val hull = variant?.hullSpec
            val manufacturer = hull?.manufacturer?.takeIf { hasText(it) } ?: "Unknown"
            val description = wingDescription(hull?.descriptionId)
            val rows = ArrayList<StatRow>()
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
            return buildWingTooltipLayout(wingTitle(spec), manufacturer, description, rows, system, armaments)
        }

        private fun debugWingLayout(profile: StockDebugItemProfile): WingTooltipLayout {
            return buildWingTooltipLayout(
                profile.tooltipTitle,
                profile.manufacturer,
                profile.description,
                debugRows(profile.wingTechnicalRows),
                profile.wingSystem,
                profile.wingArmaments,
            )
        }

        private fun buildWingTooltipLayout(
            title: String,
            manufacturer: String,
            description: String,
            technicalRows: List<StatRow>,
            system: String,
            armaments: String,
        ): WingTooltipLayout {
            val descriptionLines = wrapWingDescription(description)
            val systemLines = wrapWingLoadout(system)
            val armamentLines = wrapWingLoadout(armaments)
            val height = (
                WING_PAD_TOP +
                    34f +
                    32f +
                    maxOf(1, descriptionLines.size) * WING_LINE_HEIGHT +
                    10f +
                    WING_LINE_HEIGHT * 2f +
                    10f +
                    SECTION_HEADING_HEIGHT +
                    10f +
                    technicalRows.size * WING_GRID_ROW_HEIGHT +
                    10f +
                    maxOf(1, systemLines.size) * WING_LOADOUT_ROW_HEIGHT +
                    maxOf(1, armamentLines.size) * WING_LOADOUT_ROW_HEIGHT +
                    WING_PAD_BOTTOM
                ).coerceAtMost(StockReviewTooltipPanel.capHeight(WING_MAX_HEIGHT))
            return WingTooltipLayout(title, manufacturer, description, descriptionLines, technicalRows, system, systemLines, armaments, armamentLines, height)
        }

        private fun addWingSectionHeading(panel: CustomPanelAPI, text: String, y: Float) {
            val heading = StockReviewTooltipPanel.createSectionBand(WING_CONTENT_WIDTH, SECTION_HEADING_HEIGHT)
            addPanelLabel(heading, text, textColor(), 0f, 0f, WING_CONTENT_WIDTH, SECTION_HEADING_HEIGHT, Alignment.MID)
            panel.addComponent(heading).inTL(WING_PAD_X, y)
        }

        private fun addWingStatRow(panel: CustomPanelAPI, row: StatRow, y: Float) {
            StockReviewTooltipPanel.addStatRow(
                panel,
                row.label,
                row.value,
                textColor(),
                highlightColor(),
                WING_PAD_X,
                y,
                WING_CONTENT_WIDTH,
                WING_GRID_ROW_HEIGHT,
                WING_MIN_LABEL_WIDTH,
                WING_MAX_LABEL_WIDTH,
                WING_MIN_VALUE_WIDTH,
            )
        }

        private fun addWingLoadoutLine(panel: CustomPanelAPI, label: String, lines: List<String>, y: Float): Float {
            addPanelLabel(panel, label, textColor(), WING_PAD_X, y, WING_LOADOUT_LABEL_WIDTH, WING_LOADOUT_ROW_HEIGHT, Alignment.LMID)
            addPanelLines(
                panel,
                lines,
                highlightColor(),
                WING_LOADOUT_VALUE_X,
                y,
                WING_LOADOUT_VALUE_WIDTH,
                WING_LOADOUT_ROW_HEIGHT,
            )
            return y + maxOf(1, lines.size) * WING_LOADOUT_ROW_HEIGHT
        }

        private fun addRichPanelLine(panel: CustomPanelAPI, prefix: String, value: String, y: Float) {
            addPanelLabel(panel, prefix, textColor(), WING_PAD_X, y, 126f, WING_LINE_HEIGHT, Alignment.LMID)
            addPanelLabel(panel, value, highlightColor(), WING_PAD_X + 126f, y, WING_CONTENT_WIDTH - 126f, WING_LINE_HEIGHT, Alignment.LMID)
        }

        private fun addInlineHighlight(panel: CustomPanelAPI, prefix: String, value: String, suffix: String, y: Float) {
            addPanelLabel(panel, prefix, textColor(), WING_PAD_X, y, 154f, WING_LINE_HEIGHT, Alignment.LMID)
            addPanelLabel(panel, value, highlightColor(), WING_PAD_X + 154f, y, 80f, WING_LINE_HEIGHT, Alignment.LMID)
            addPanelLabel(panel, suffix, textColor(), WING_PAD_X + 234f, y, WING_CONTENT_WIDTH - 234f, WING_LINE_HEIGHT, Alignment.LMID)
        }

        private fun addPanelLines(
            parent: CustomPanelAPI,
            lines: List<String>,
            color: Color,
            x: Float,
            y: Float,
            width: Float,
            lineHeight: Float,
        ) {
            StockReviewTooltipPanel.addLines(parent, lines, color, x, y, width, lineHeight)
        }

        private fun wrapWingDescription(text: String): List<String> =
            WimGuiText.wrap(text, WimGuiText.estimatedChars(WING_CONTENT_WIDTH - 8f, 0f, WING_DESCRIPTION_CHAR_WIDTH), WING_DESCRIPTION_MAX_LINES)

        private fun wrapWingLoadout(text: String): List<String> =
            WimGuiText.wrap(text, WimGuiText.estimatedChars(WING_LOADOUT_VALUE_WIDTH - 8f, 0f, WING_LOADOUT_CHAR_WIDTH), WING_LOADOUT_MAX_LINES)

        private fun measuredPanelLines(
            panel: CustomPanelAPI,
            text: String,
            width: Float,
            lineHeight: Float,
            maxLines: Int,
        ): List<String> = StockReviewTooltipPanel.wrapLines(panel, text, highlightColor(), width, lineHeight, maxLines)

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

        @Suppress("unused")
        private fun addSpacer(rows: MutableList<StatRow>) {
            if (rows.isNotEmpty() && !rows[rows.size - 1].isSpacer()) {
                rows.add(StatRow("", ""))
            }
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

        private fun truncateDescription(text: String?): String? {
            if (!hasText(text)) {
                return text
            }
            return truncateForLines(text, DESCRIPTION_MAX_LINES, CONTENT_WIDTH)
        }

        private fun truncateForLines(text: String?, maxLines: Int, width: Float): String {
            val source = text?.takeIf { hasText(it) } ?: return text ?: ""
            val normalized = source.trim().replace(Regex("\\s+"), " ")
            if (maxLines <= 0) {
                return normalized
            }
            var charsPerLine = maxOf(32, Math.floor((CONTENT_WIDTH / ESTIMATED_DESCRIPTION_CHAR_WIDTH).toDouble()).toInt())
            if (validNumber(width) && width > 0f) {
                charsPerLine = maxOf(32, Math.floor((width / ESTIMATED_DESCRIPTION_CHAR_WIDTH).toDouble()).toInt())
            }
            val words = normalized.split(" ")
            val result = StringBuilder(normalized.length)
            var line = 1
            var lineChars = 0
            var truncated = false
            for (word in words) {
                if (word.isEmpty()) {
                    continue
                }
                var addedChars = if (lineChars <= 0) word.length else word.length + 1
                if (lineChars > 0 && lineChars + addedChars > charsPerLine) {
                    line++
                    lineChars = 0
                    addedChars = word.length
                }
                if (line > maxLines) {
                    truncated = true
                    break
                }
                if (result.isNotEmpty()) {
                    result.append(' ')
                }
                result.append(word)
                lineChars += addedChars
            }
            if (!truncated && result.length == normalized.length) {
                return normalized
            }
            return trimForEllipsis(result.toString()) + "..."
        }

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

        private fun trimForEllipsis(value: String?): String {
            if (value == null) {
                return ""
            }
            var trimmed = value.trim()
            while (trimmed.endsWith(",") || trimmed.endsWith(";") || trimmed.endsWith(":") || trimmed.endsWith(".")) {
                trimmed = trimmed.substring(0, trimmed.length - 1).trim()
            }
            return trimmed
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
