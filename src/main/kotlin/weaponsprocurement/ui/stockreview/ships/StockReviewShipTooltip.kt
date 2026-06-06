package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.WimGuiText
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltipStatRow
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltipPanel
import java.awt.Color
import java.util.Locale
import kotlin.math.max

/**
 * Public-API approximation of the vanilla ship-sale tooltip. It avoids obfuscated UI classes,
 * so layout changes should be checked visually in-game against vanilla ship tooltips.
 */
class StockReviewShipTooltip(
    private val record: StockReviewShipRecord,
) : TooltipMakerAPI.TooltipCreator {
    private val member: FleetMemberAPI = record.member
    private val debugProfile: StockReviewShipDebugProfile? = record.debugProfile
    private val rows = StockReviewShipTooltipRows(member, debugProfile)

    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        val panel = Global.getSettings().createCustom(
            WIDTH,
            StockReviewTooltipPanel.maxTooltipHeight(),
            WimGuiPanelPlugin(StockReviewTooltipPanel.SHIP_BACKGROUND, StockReviewTooltipPanel.SHIP_BORDER),
        )
        val descriptionBottom = addTitleBlock(panel)
        addShipPreview(panel)
        val dataHeadingTop = max(PREVIEW_TOP + PREVIEW_HEIGHT + 12f, descriptionBottom + 22f)
        val loadoutTop = addDataBlock(panel, dataHeadingTop)
        val finalY = addLoadoutBlock(panel, loadoutTop)
        panel.position.setSize(WIDTH, StockReviewTooltipPanel.capHeight(max(MIN_HEIGHT, finalY + PAD)))
        tooltip.addCustom(panel, 0f)
    }

    private fun addTitleBlock(panel: CustomPanelAPI): Float {
        addPanelLabel(panel, titleText(), TITLE_COLOR, PAD, 10f, TOP_TEXT_WIDTH, 28f, Alignment.LMID)
        val manufacturer = debugProfile?.manufacturer ?: member.hullSpec.manufacturer?.takeIf { it.isNotBlank() } ?: "Unknown"
        addRichLine(panel, "Design type: ", manufacturer, PAD, 52f, TOP_TEXT_WIDTH, 24f)
        val lines = addWrappedPanelLabel(
            panel,
            debugProfile?.description ?: descriptionText(),
            TEXT,
            PAD,
            DESCRIPTION_TOP,
            TOP_TEXT_WIDTH - 18f,
            DESCRIPTION_LINE_HEIGHT,
            MAX_DESCRIPTION_LINES,
        )
        return DESCRIPTION_TOP + max(1, lines) * DESCRIPTION_LINE_HEIGHT
    }

    private fun addShipPreview(panel: CustomPanelAPI) {
        val preview = panel.createCustomPanel(
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT,
            StockReviewShipSpritePlugin(member.hullSpec.spriteName, 0.90f, 0.50f, 0.98f),
        )
        panel.addComponent(preview).inTL(COMBAT_X + COMBAT_WIDTH * 0.5f - PREVIEW_WIDTH * 0.5f, PREVIEW_TOP)
    }

    private fun addDataBlock(panel: CustomPanelAPI, dataHeadingTop: Float): Float {
        addSectionHeading(panel, "Logistical data", PAD, dataHeadingTop, COMBAT_X - PAD - 22f)
        addSectionHeading(panel, "Combat performance", COMBAT_X, dataHeadingTop, COMBAT_WIDTH)
        val dataRowsTop = dataHeadingTop + SECTION_HEIGHT + 10f

        val logisticsLeft = rows.logisticsLeft()
        val logisticsRight = rows.logisticsRight()
        val combat = rows.combat()

        addStatRows(panel, logisticsLeft, PAD, dataRowsTop, 380f, 172f, 292f, 70f)
        addStatRows(panel, logisticsRight, 410f, dataRowsTop, 382f, 184f, 296f, 70f)
        addStatRows(panel, combat, COMBAT_X, dataRowsTop, COMBAT_WIDTH, 174f, 286f, 70f)
        val maxRows = max(logisticsLeft.size, max(logisticsRight.size, combat.size))
        return dataRowsTop + STAT_ROW_HEIGHT * maxRows + 24f
    }

    private fun addLoadoutBlock(panel: CustomPanelAPI, loadoutTop: Float): Float {
        var y = loadoutTop
        y = addLoadoutLine(panel, "System:", debugProfile?.system ?: systemLabel(), y, HIGHLIGHT, 2)
        y = addLoadoutLine(panel, "Mounts:", debugProfile?.mounts ?: mountsLabel(), y, HIGHLIGHT, 2)
        y = addLoadoutLine(panel, "Armaments:", debugProfile?.armaments ?: armamentsLabel(), y, HIGHLIGHT, 3)
        return addLoadoutLine(panel, "Hull mods:", debugProfile?.hullMods ?: hullModsLabel(), y, HIGHLIGHT, 2)
    }

    private fun addSectionHeading(panel: CustomPanelAPI, text: String, x: Float, y: Float, width: Float) {
        StockReviewTooltipPanel.addSectionHeading(panel, text, x, y, width, SECTION_HEIGHT, TEXT)
    }

    private fun addStatRows(
        panel: CustomPanelAPI,
        rows: List<StockReviewTooltipStatRow>,
        x: Float,
        y: Float,
        width: Float,
        minLabelWidth: Float,
        maxLabelWidth: Float,
        minValueWidth: Float,
    ) {
        rows.forEachIndexed { index, row ->
            val rowY = y + index * STAT_ROW_HEIGHT
            StockReviewTooltipPanel.addStatRow(
                panel,
                row.label,
                row.value,
                TEXT,
                HIGHLIGHT,
                x,
                rowY,
                width,
                STAT_ROW_HEIGHT,
                minLabelWidth,
                maxLabelWidth,
                minValueWidth,
            )
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
        val usableWidth = StockReviewTooltipPanel.usableTextWidth(width, Alignment.LMID)
        val measure = parent.createUIElement(usableWidth, lineHeight, false)
        measure.setParaFontDefault()
        measure.setParaFontColor(color)
        val lines = WimGuiText.wrapToWidth(text, measure, usableWidth, maxLines)
        for (i in lines.indices) {
            StockReviewTooltipPanel.addLabel(parent, lines[i], color, x, y + i * lineHeight, width, lineHeight, Alignment.LMID)
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
        StockReviewTooltipPanel.addLabel(parent, text, color, x, y, width, height, alignment)
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

    private data class MountKey(
        val size: String,
        val type: String,
        val label: String,
    )

    companion object {
        private const val WIDTH = 1220f
        private const val MIN_HEIGHT = 610f
        private const val PAD = 16f
        private const val TOP_TEXT_WIDTH = 830f
        private const val DESCRIPTION_TOP = 86f
        private const val DESCRIPTION_LINE_HEIGHT = 24f
        private const val MAX_DESCRIPTION_LINES = 12
        private const val PREVIEW_TOP = 36f
        private const val PREVIEW_HEIGHT = 230f
        private const val PREVIEW_WIDTH = 270f
        private const val COMBAT_X = 820f
        private const val COMBAT_WIDTH = 384f
        private const val SECTION_HEIGHT = 22f
        private const val STAT_ROW_HEIGHT = 22f
        private const val LOADOUT_ROW_HEIGHT = 20f
        private val TEXT = StockReviewTooltipPanel.SHIP_TEXT
        private val TITLE_COLOR = StockReviewTooltipPanel.TITLE
        private val HIGHLIGHT = Misc.getHighlightColor()
    }
}
