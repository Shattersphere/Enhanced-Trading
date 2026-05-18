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
    private val record: StockReviewShipRecord,
) : TooltipMakerAPI.TooltipCreator {
    private val member: FleetMemberAPI = record.member
    private val debugProfile: StockReviewShipDebugProfile? = record.debugProfile

    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        val layout = TooltipLayout.from(member, debugProfile)
        val panel = Global.getSettings().createCustom(WIDTH, layout.height, WimGuiPanelPlugin(BACKGROUND, null))
        addTitleBlock(panel, layout)
        addShipPreview(panel, layout)
        addDataBlock(panel, layout)
        addLoadoutBlock(panel, layout)
        tooltip.addCustom(panel, 0f)
    }

    private fun addTitleBlock(panel: CustomPanelAPI, layout: TooltipLayout) {
        addPanelLabel(panel, titleText(), TITLE_COLOR, PAD, 10f, TOP_TEXT_WIDTH, 28f, Alignment.LMID)
        val manufacturer = debugProfile?.manufacturer ?: member.hullSpec.manufacturer?.takeIf { it.isNotBlank() } ?: "Unknown"
        addRichLine(panel, "Design type: ", manufacturer, PAD, 52f, TOP_TEXT_WIDTH, 24f)
        addWrappedPanelLabel(panel, layout.descriptionText, TEXT, PAD, DESCRIPTION_TOP, TOP_TEXT_WIDTH - 18f, DESCRIPTION_LINE_HEIGHT, MAX_DESCRIPTION_LINES)
    }

    private fun addShipPreview(panel: CustomPanelAPI, layout: TooltipLayout) {
        val preview = panel.createCustomPanel(
            PREVIEW_WIDTH,
            layout.previewHeight,
            StockReviewShipSpritePlugin(member.hullSpec.spriteName, 0.90f, 0.50f, 0.98f),
        )
        panel.addComponent(preview).inTL(COMBAT_X + COMBAT_WIDTH * 0.5f - PREVIEW_WIDTH * 0.5f, PREVIEW_TOP)
    }

    private fun addDataBlock(panel: CustomPanelAPI, layout: TooltipLayout) {
        addSectionHeading(panel, "Logistical data", PAD, layout.dataHeadingTop, 690f)
        addSectionHeading(panel, "Combat performance", COMBAT_X, layout.dataHeadingTop, COMBAT_WIDTH)

        val logisticsLeft = debugProfile?.logisticsLeft?.map { StatRow(it.label, it.value) } ?: listOf(
            StatRow("CR per deployment", percent(member.hullSpec.crToDeploy)),
            StatRow("Recovery rate (per day)", percent(member.repairTracker?.recoveryRate ?: member.stats.repairRatePercentPerDay.modifiedValue / 100f)),
            StatRow("Recovery cost (supplies)", integer(member.stats.suppliesToRecover.modifiedValue)),
            StatRow("Deployment points", integer(member.deploymentPointsCost)),
            StatRow("Peak performance (sec)", integer(member.hullSpec.noCRLossTime)),
            StatRow("Crew complement", "${integer(member.crewComposition?.crew ?: 0f)} / ${integer(member.maxCrew)}"),
            StatRow("Hull size", hullSize(member.hullSpec.hullSize)),
            StatRow("Ordnance points", member.hullSpec.getOrdnancePoints(Global.getSector()?.playerStats).toString()),
        )
        val logisticsRight = debugProfile?.logisticsRight?.map { StatRow(it.label, it.value) } ?: listOf(
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
        val combat = debugProfile?.combat?.map { StatRow(it.label, it.value) } ?: listOf(
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

        addStatRows(panel, logisticsLeft, PAD, layout.dataRowsTop, 326f, 222f)
        addStatRows(panel, logisticsRight, 356f, layout.dataRowsTop, 340f, 224f)
        addStatRows(panel, combat, 744f, layout.dataRowsTop, 346f, 218f)
    }

    private fun addLoadoutBlock(panel: CustomPanelAPI, layout: TooltipLayout) {
        var y = layout.loadoutTop
        y = addLoadoutLine(panel, "System:", debugProfile?.system ?: systemLabel(), y, HIGHLIGHT, layout.systemLines)
        y = addLoadoutLine(panel, "Mounts:", debugProfile?.mounts ?: mountsLabel(), y, HIGHLIGHT, layout.mountLines)
        y = addLoadoutLine(panel, "Armaments:", debugProfile?.armaments ?: armamentsLabel(), y, HIGHLIGHT, layout.armamentLines)
        addLoadoutLine(panel, "Hull mods:", debugProfile?.hullMods ?: hullModsLabel(), y, HIGHLIGHT, layout.hullModLines)
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

    private fun addLoadoutLine(panel: CustomPanelAPI, label: String, value: String, y: Float, valueColor: Color, maxLines: Int): Float {
        addPanelLabel(panel, label, TEXT, PAD, y, 116f, LOADOUT_ROW_HEIGHT, Alignment.LMID)
        val lines = addWrappedPanelLabel(panel, value, valueColor, 132f, y, WIDTH - 132f - PAD, LOADOUT_ROW_HEIGHT, maxLines)
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
        return paragraphs.joinToString("\n\n") { it.trim() }
    }

    private fun titleText(): String =
        debugProfile?.tooltipTitle ?: "${member.shipName}, ${member.hullSpec.nameWithDesignationWithDashClass}"

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

    private data class TooltipLayout(
        val descriptionText: String,
        val dataHeadingTop: Float,
        val dataRowsTop: Float,
        val loadoutTop: Float,
        val height: Float,
        val previewHeight: Float,
        val systemLines: Int,
        val mountLines: Int,
        val armamentLines: Int,
        val hullModLines: Int,
    ) {
        companion object {
            fun from(member: FleetMemberAPI): TooltipLayout {
                return from(member, null)
            }

            fun from(member: FleetMemberAPI, debugProfile: StockReviewShipDebugProfile?): TooltipLayout {
                val description = debugProfile?.description ?: descriptionText(member)
                val descriptionLineCount = WimGuiText.wrap(
                    description,
                    WimGuiText.estimatedChars(TOP_TEXT_WIDTH - 26f),
                    MAX_DESCRIPTION_LINES,
                ).size
                val descriptionBottom = DESCRIPTION_TOP + descriptionLineCount * DESCRIPTION_LINE_HEIGHT
                val dataHeadingTop = max(PREVIEW_TOP + PREVIEW_HEIGHT + 12f, descriptionBottom + 22f)
                val dataRowsTop = dataHeadingTop + SECTION_HEIGHT + 10f
                val loadoutTop = dataRowsTop + STAT_ROW_HEIGHT * 9f + 24f
                val systemLines = lineCount(debugProfile?.system ?: systemLabel(member), 2)
                val mountLines = lineCount(debugProfile?.mounts ?: mountsLabel(member), 2)
                val armamentLines = lineCount(debugProfile?.armaments ?: armamentsLabel(member), 3)
                val hullModLines = lineCount(debugProfile?.hullMods ?: hullModsLabel(member), 2)
                val loadoutHeight = (systemLines + mountLines + armamentLines + hullModLines) * LOADOUT_ROW_HEIGHT
                val height = (loadoutTop + loadoutHeight + PAD).coerceIn(MIN_HEIGHT, MAX_HEIGHT)
                return TooltipLayout(
                    description,
                    dataHeadingTop,
                    dataRowsTop,
                    loadoutTop,
                    height,
                    PREVIEW_HEIGHT,
                    systemLines,
                    mountLines,
                    armamentLines,
                    hullModLines,
                )
            }

            private fun lineCount(text: String, maxLines: Int): Int =
                WimGuiText.wrap(text, WimGuiText.estimatedChars(WIDTH - 132f - PAD - 8f), maxLines).size

            private fun descriptionText(member: FleetMemberAPI): String {
                val id = member.hullSpec.descriptionId ?: return ""
                val description = try {
                    Global.getSettings().getDescription(id, Description.Type.SHIP)
                } catch (_: RuntimeException) {
                    null
                } ?: return ""
                val paragraphs = description.text1Paras.orEmpty().filter { it.isNotBlank() }
                return paragraphs.joinToString("\n\n") { it.trim() }
            }

            private fun systemLabel(member: FleetMemberAPI): String {
                val id = member.hullSpec.shipSystemId?.takeIf { it.isNotBlank() } ?: return "None"
                return try {
                    Global.getSettings().getShipSystemSpec(id)?.name ?: id
                } catch (_: RuntimeException) {
                    id
                }
            }

            private fun mountsLabel(member: FleetMemberAPI): String {
                val slots = member.hullSpec.allWeaponSlotsCopy
                    .filter { it.isWeaponSlot && !it.isHidden && !it.isDecorative }
                if (slots.isEmpty()) return "None"
                return slots
                    .groupBy { MountKey(it.slotSize.name, it.weaponType.name, "${prettyEnum(it.slotSize.name)} ${prettyEnum(it.weaponType.name)}") }
                    .entries
                    .sortedWith(compareBy({ slotSizeOrder(it.key.size) }, { it.key.type }))
                    .joinToString(", ") { "${it.value.size}x ${it.key.label}" }
            }

            private fun armamentsLabel(member: FleetMemberAPI): String {
                val variant = member.variant ?: return "None"
                val names = variant.fittedWeaponSlots
                    .mapNotNull { slot -> variant.getWeaponSpec(slot)?.weaponName }
                    .filter { it.isNotBlank() }
                if (names.isEmpty()) return "None"
                return names.groupingBy { it }.eachCount()
                    .entries
                    .sortedBy { it.key }
                    .joinToString(", ") { "${it.value}x ${it.key}" }
            }

            private fun hullModsLabel(member: FleetMemberAPI): String {
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

            private fun prettyEnum(value: String): String =
                value.lowercase(Locale.ROOT).split('_').joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

            private fun slotSizeOrder(size: String): Int = when (size) {
                "SMALL" -> 0
                "MEDIUM" -> 1
                "LARGE" -> 2
                else -> 3
            }
        }
    }

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

    companion object {
        private const val WIDTH = 1120f
        private const val MIN_HEIGHT = 610f
        private const val MAX_HEIGHT = 880f
        private const val PAD = 16f
        private const val TOP_TEXT_WIDTH = 790f
        private const val DESCRIPTION_TOP = 86f
        private const val DESCRIPTION_LINE_HEIGHT = 24f
        private const val MAX_DESCRIPTION_LINES = 9
        private const val PREVIEW_TOP = 36f
        private const val PREVIEW_HEIGHT = 230f
        private const val PREVIEW_WIDTH = 270f
        private const val COMBAT_X = 732f
        private const val COMBAT_WIDTH = 372f
        private const val SECTION_HEIGHT = 22f
        private const val STAT_ROW_HEIGHT = 22f
        private const val LOADOUT_ROW_HEIGHT = 20f
        private val BACKGROUND = Color(0, 0, 0, 245)
        private val BORDER = Color(100, 185, 200, 255)
        private val SECTION = Color(9, 78, 88, 225)
        private val TEXT = Color(218, 226, 228, 255)
        private val TITLE_COLOR = Color(205, 245, 255, 255)
        private val HIGHLIGHT = Misc.getHighlightColor()
    }
}
