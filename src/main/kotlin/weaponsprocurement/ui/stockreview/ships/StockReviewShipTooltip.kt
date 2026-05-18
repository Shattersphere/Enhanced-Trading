package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.WimGuiStyle
import weaponsprocurement.ui.WimGuiText
import java.awt.Color
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class StockReviewShipTooltip(
    private val member: FleetMemberAPI,
) : TooltipMakerAPI.TooltipCreator {
    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        val panel = Global.getSettings().createCustom(WIDTH, HEIGHT, WimGuiPanelPlugin(BACKGROUND, BORDER))
        addTitleBlock(panel)
        addShipPreview(panel)
        addConditionBars(panel)
        addDataBlock(panel)
        addLoadoutBlock(panel)
        tooltip.addCustom(panel, 0f)
    }

    private fun addTitleBlock(panel: CustomPanelAPI) {
        addPanelLabel(panel, "${member.shipName}, ${member.hullSpec.nameWithDesignationWithDashClass}", TITLE_COLOR, PAD, 8f, 735f, 28f, Alignment.LMID)
        val manufacturer = member.hullSpec.manufacturer?.takeIf { it.isNotBlank() } ?: "Unknown"
        addRichLine(panel, "Design type: ", manufacturer, PAD, 46f, 735f, 24f)
        addWrappedPanelLabel(panel, descriptionText(), TEXT, PAD, 78f, 710f, 22f, 4)
    }

    private fun addShipPreview(panel: CustomPanelAPI) {
        val preview = panel.createCustomPanel(
            270f,
            245f,
            StockReviewShipSpritePlugin(member.hullSpec.spriteName, 0.93f, 0.52f, 0.98f),
        )
        panel.addComponent(preview).inTL(778f, 48f)
    }

    private fun addConditionBars(panel: CustomPanelAPI) {
        addPanelLabel(panel, "Combat readiness", TEXT, PAD, 292f, 185f, 22f, Alignment.LMID)
        addStatusBar(panel, 176f, 296f, 310f, member.repairTracker?.cr ?: 0f, percent(member.repairTracker?.cr ?: 0f))
        addPanelLabel(panel, "Hull integrity", TEXT, 500f, 292f, 165f, 22f, Alignment.LMID)
        val repaired = member.repairTracker?.computeRepairednessFraction() ?: 1f
        addStatusBar(panel, 642f, 296f, 310f, repaired, percent(repaired))
    }

    private fun addDataBlock(panel: CustomPanelAPI) {
        addSectionHeading(panel, "Logistical data", PAD, 348f, 650f)
        addSectionHeading(panel, "Combat performance", 705f, 348f, 343f)

        val logisticsLeft = listOf(
            StatRow("CR per deployment", percent(member.hullSpec.crToDeploy)),
            StatRow("Recovery rate (per day)", percent(member.repairTracker?.recoveryRate ?: member.stats.repairRatePercentPerDay.modifiedValue / 100f)),
            StatRow("Recovery cost (supplies)", integer(member.stats.suppliesToRecover.modifiedValue)),
            StatRow("Deployment points", integer(member.deploymentPointsCost)),
            StatRow("Peak performance (sec)", integer(member.hullSpec.noCRLossTime)),
            StatRow("Crew complement", "${integer(member.crewComposition?.crew ?: 0f)} / ${integer(member.maxCrew)}"),
            StatRow("Hull size", hullSize(member.hullSpec.hullSize)),
            StatRow("Ordnance points", member.hullSpec.getOrdnancePoints(Global.getSector()?.playerStats).toString()),
        )
        val logisticsRight = listOf(
            StatRow("Maintenance (supplies/mo)", oneDecimal(member.stats.suppliesPerMonth.modifiedValue)),
            StatRow("Cargo capacity", integer(member.cargoCapacity)),
            StatRow("Maximum crew", integer(member.maxCrew)),
            StatRow("Skeleton crew required", integer(member.minCrew)),
            StatRow("Fuel capacity", integer(member.fuelCapacity)),
            StatRow("Maximum burn", integer(member.stats.maxBurnLevel.modifiedValue)),
            StatRow("Fuel / light year", oneDecimal(member.stats.fuelUseMod.computeEffective(member.hullSpec.fuelPerLY))),
            StatRow("Sensor profile", integer(member.stats.sensorProfile.modifiedValue)),
            StatRow("Sensor strength", integer(member.stats.sensorStrength.modifiedValue)),
        )
        val combat = listOf(
            StatRow("Hull integrity", integer(member.hullSpec.hitpoints)),
            StatRow("Armor rating", integer(member.hullSpec.armorRating)),
            StatRow("Defense", defenseLabel()),
            StatRow("Shield arc", shieldArcLabel()),
            StatRow("Shield upkeep/sec", shieldUpkeepLabel()),
            StatRow("Shield flux/damage", shieldEfficiencyLabel()),
            StatRow("Flux capacity", integer(member.stats.fluxCapacity.modifiedValue)),
            StatRow("Flux dissipation", integer(member.stats.fluxDissipation.modifiedValue)),
            StatRow("Top speed", integer(member.stats.maxSpeed.modifiedValue)),
        )

        addStatRows(panel, logisticsLeft, PAD, 380f, 330f, 116f)
        addStatRows(panel, logisticsRight, 355f, 380f, 330f, 128f)
        addStatRows(panel, combat, 715f, 380f, 323f, 170f)
    }

    private fun addLoadoutBlock(panel: CustomPanelAPI) {
        var y = 592f
        y = addLoadoutLine(panel, "System:", systemLabel(), y, HIGHLIGHT)
        y = addLoadoutLine(panel, "Mounts:", mountsLabel(), y, HIGHLIGHT)
        y = addLoadoutLine(panel, "Armaments:", armamentsLabel(), y, HIGHLIGHT)
        addLoadoutLine(panel, "Hull mods:", hullModsLabel(), y, HIGHLIGHT)
    }

    private fun addStatusBar(panel: CustomPanelAPI, x: Float, y: Float, width: Float, fraction: Float, label: String) {
        val bar = panel.createCustomPanel(width, 18f, BarPlugin(fraction.coerceIn(0f, 1f)))
        panel.addComponent(bar).inTL(x, y)
        addPanelLabel(panel, label, HIGHLIGHT, x + width + 8f, y - 2f, 52f, 22f, Alignment.LMID)
    }

    private fun addSectionHeading(panel: CustomPanelAPI, text: String, x: Float, y: Float, width: Float) {
        val heading = panel.createCustomPanel(width, SECTION_HEIGHT, SectionHeadingPlugin())
        panel.addComponent(heading).inTL(x, y)
        addPanelLabel(panel, text, TEXT, x, y - 1f, width, SECTION_HEIGHT, Alignment.MID)
    }

    private fun addStatRows(panel: CustomPanelAPI, rows: List<StatRow>, x: Float, y: Float, width: Float, labelWidth: Float) {
        rows.forEachIndexed { index, row ->
            val rowY = y + index * STAT_ROW_HEIGHT
            addPanelLabel(panel, row.label, TEXT, x, rowY, labelWidth, STAT_ROW_HEIGHT, Alignment.LMID)
            addPanelLabel(panel, row.value, row.color, x + labelWidth, rowY, width - labelWidth, STAT_ROW_HEIGHT, Alignment.RMID)
        }
    }

    private fun addLoadoutLine(panel: CustomPanelAPI, label: String, value: String, y: Float, valueColor: Color): Float {
        addPanelLabel(panel, label, TEXT, PAD, y, 116f, LOADOUT_ROW_HEIGHT, Alignment.LMID)
        val lines = addWrappedPanelLabel(panel, value, valueColor, 132f, y, 900f, LOADOUT_ROW_HEIGHT, 2)
        return y + max(1, lines) * LOADOUT_ROW_HEIGHT
    }

    private fun addRichLine(panel: CustomPanelAPI, prefix: String, value: String, x: Float, y: Float, width: Float, height: Float) {
        addPanelLabel(panel, prefix, TEXT, x, y, 116f, height, Alignment.LMID)
        addPanelLabel(panel, value, HIGHLIGHT, x + 116f, y, width - 116f, height, Alignment.LMID)
    }

    private fun addWrappedPanelLabel(
        parent: CustomPanelAPI,
        text: String?,
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        lineHeight: Float,
        maxLines: Int,
    ): Int {
        val lines = WimGuiText.wrap(text, WimGuiText.estimatedChars(width - 8f), maxLines)
        for (i in lines.indices) {
            addPanelLabel(parent, lines[i], color, x, y + i * lineHeight, width, lineHeight, Alignment.LMID)
        }
        return lines.size
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
        var labelX = x
        var labelWidth = width
        if (Alignment.LMID == alignment) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD
            labelWidth = max(8f, width - WimGuiStyle.TEXT_LEFT_PAD)
        }
        val element = parent.createUIElement(labelWidth, height, false)
        element.setParaFontDefault()
        element.setParaFontColor(color)
        val line: LabelAPI = element.addPara(WimGuiText.paraFormat(WimGuiText.fitToWidth(text, element, labelWidth)), 0f, color)
        line.setAlignment(alignment)
        parent.addUIElement(element).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD)
    }

    private fun descriptionText(): String {
        val id = member.hullSpec.descriptionId ?: return ""
        val description = try {
            Global.getSettings().getDescription(id, Description.Type.SHIP)
        } catch (_: RuntimeException) {
            null
        } ?: return ""
        val paragraphs = description.text1Paras.orEmpty().filter { it.isNotBlank() }
        return paragraphs.take(2).joinToString("\n\n") { it.trim() }
    }

    private fun systemLabel(): String {
        val id = member.hullSpec.shipSystemId?.takeIf { it.isNotBlank() } ?: return "None"
        return try {
            Global.getSettings().getShipSystemSpec(id)?.name ?: id
        } catch (_: RuntimeException) {
            id
        }
    }

    private fun mountsLabel(): String {
        val slots = member.hullSpec.allWeaponSlotsCopy
            .filter { it.isWeaponSlot && !it.isHidden && !it.isDecorative }
        if (slots.isEmpty()) {
            return "None"
        }
        return slots
            .groupBy { mountKey(it) }
            .entries
            .sortedWith(compareBy({ slotSizeOrder(it.key.size) }, { it.key.type }))
            .joinToString(", ") { "${it.value.size}x ${it.key.label}" }
    }

    private fun armamentsLabel(): String {
        val variant = member.variant ?: return "None"
        val names = variant.fittedWeaponSlots
            .mapNotNull { slot -> variant.getWeaponSpec(slot)?.weaponName }
            .filter { it.isNotBlank() }
        if (names.isEmpty()) {
            return "None"
        }
        return names.groupingBy { it }.eachCount()
            .entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.value}x ${it.key}" }
    }

    private fun hullModsLabel(): String {
        val variant = member.variant ?: return "None"
        val names = variant.sortedMods
            .mapNotNull { id ->
                try {
                    Global.getSettings().getHullModSpec(id)?.displayName ?: id
                } catch (_: RuntimeException) {
                    id
                }
            }
            .filter { it.isNotBlank() }
        return names.joinToString(", ").ifBlank { "None" }
    }

    private fun defenseLabel(): String {
        val type = member.hullSpec.defenseType ?: return "None"
        return when (type.name) {
            "OMNI" -> "Omni Shield"
            "FRONT" -> "Front Shield"
            "PHASE" -> "Phase Cloak"
            "NONE" -> "None"
            else -> prettyEnum(type.name)
        }
    }

    private fun shieldArcLabel(): String {
        val shield = member.hullSpec.shieldSpec ?: return ""
        return integer(shield.arc)
    }

    private fun shieldUpkeepLabel(): String {
        val shield = member.hullSpec.shieldSpec ?: return ""
        return integer(shield.upkeepCost)
    }

    private fun shieldEfficiencyLabel(): String {
        val shield = member.hullSpec.shieldSpec ?: return ""
        return oneDecimalTrim(shield.fluxPerDamageAbsorbed)
    }

    private fun percent(value: Float): String = "${(value * 100f).roundToInt()}%"
    private fun integer(value: Float): String = value.roundToInt().toString()
    private fun oneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)
    private fun oneDecimalTrim(value: Float): String {
        val formatted = oneDecimal(value)
        return if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
    }

    private fun hullSize(value: ShipAPI.HullSize?): String =
        value?.name?.lowercase(Locale.ROOT)?.replaceFirstChar(Char::titlecase) ?: "Unknown"

    private fun prettyEnum(value: String): String =
        value.lowercase(Locale.ROOT).split('_').joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

    private fun mountKey(slot: WeaponSlotAPI): MountKey =
        MountKey(slot.slotSize.name, slot.weaponType.name, "${prettyEnum(slot.slotSize.name)} ${prettyEnum(slot.weaponType.name)}")

    private fun slotSizeOrder(size: String): Int = when (size) {
        "SMALL" -> 0
        "MEDIUM" -> 1
        "LARGE" -> 2
        else -> 3
    }

    private data class StatRow(
        val label: String,
        val value: String,
        val color: Color = HIGHLIGHT,
    )

    private data class MountKey(
        val size: String,
        val type: String,
        val label: String,
    )

    private class SectionHeadingPlugin : BaseCustomUIPanelPlugin() {
        private var position: PositionAPI? = null

        override fun positionChanged(position: PositionAPI?) {
            this.position = position
        }

        override fun renderBelow(alphaMult: Float) {
            val current = position ?: return
            Misc.renderQuadAlpha(current.x, current.y, current.width, current.height, SECTION, alphaMult)
        }
    }

    private class BarPlugin(private val fraction: Float) : BaseCustomUIPanelPlugin() {
        private var position: PositionAPI? = null

        override fun positionChanged(position: PositionAPI?) {
            this.position = position
        }

        override fun renderBelow(alphaMult: Float) {
            val current = position ?: return
            Misc.renderQuadAlpha(current.x, current.y, current.width, current.height, BAR_BACKGROUND, alphaMult)
            Misc.renderQuadAlpha(current.x + 2f, current.y + 3f, max(1f, (current.width - 4f) * fraction), current.height - 6f, BAR_FILL, alphaMult)
        }

        override fun render(alphaMult: Float) {
            val current = position ?: return
            Misc.renderQuadAlpha(current.x, current.y, current.width, 1f, BORDER, alphaMult)
            Misc.renderQuadAlpha(current.x, current.y + current.height - 1f, current.width, 1f, BORDER, alphaMult)
            Misc.renderQuadAlpha(current.x, current.y, 1f, current.height, BORDER, alphaMult)
            Misc.renderQuadAlpha(current.x + current.width - 1f, current.y, 1f, current.height, BORDER, alphaMult)
        }
    }

    companion object {
        private const val WIDTH = 1080f
        private const val HEIGHT = 696f
        private const val PAD = 16f
        private const val SECTION_HEIGHT = 22f
        private const val STAT_ROW_HEIGHT = 22f
        private const val LOADOUT_ROW_HEIGHT = 22f
        private val BACKGROUND = Color(0, 0, 0, 245)
        private val BORDER = Color(100, 185, 200, 255)
        private val SECTION = Color(9, 78, 88, 225)
        private val TEXT = Color(218, 226, 228, 255)
        private val TITLE_COLOR = Color(205, 245, 255, 255)
        private val HIGHLIGHT = Misc.getHighlightColor()
        private val BAR_BACKGROUND = Color(5, 28, 32, 210)
        private val BAR_FILL = Color(208, 255, 238, 230)
    }
}
