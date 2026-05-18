package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.WimGuiStyle
import weaponsprocurement.ui.WimGuiTooltip
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import java.awt.Color
import kotlin.math.roundToInt

class StockReviewShipTooltip(
    private val member: FleetMemberAPI,
) : TooltipMakerAPI.TooltipCreator {
    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = 760f

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        val spec = member.hullSpec
        tooltip.setTitleFontColor(StockReviewStyle.TEXT)
        tooltip.addTitle("${member.shipName}, ${spec.nameWithDesignationWithDashClass}")
        tooltip.setParaFontDefault()
        addDesignType(tooltip)
        addDescription(tooltip)
        addPreview(tooltip)
        addStatusBars(tooltip)
        addDataColumns(tooltip)
        addLoadoutRows(tooltip)
    }

    private fun addDesignType(tooltip: TooltipMakerAPI) {
        val manufacturer = member.hullSpec.manufacturer?.takeIf { it.isNotBlank() } ?: "Unknown"
        tooltip.addPara("Design type: $manufacturer", 8f, StockReviewStyle.TEXT, manufacturer)
    }

    private fun addDescription(tooltip: TooltipMakerAPI) {
        val id = member.hullSpec.descriptionId ?: return
        val description = try {
            Global.getSettings().getDescription(id, Description.Type.SHIP)
        } catch (_: RuntimeException) {
            null
        } ?: return
        for (paragraph in description.text1Paras.orEmpty().take(2)) {
            if (paragraph.isNotBlank()) {
                tooltip.addPara(paragraph, 8f)
            }
        }
    }

    private fun addPreview(tooltip: TooltipMakerAPI) {
        val sprite = member.hullSpec.spriteName
        if (WimGuiTooltip.hasText(sprite)) {
            tooltip.addImage(sprite, 180f, 120f, 10f)
        }
    }

    private fun addStatusBars(tooltip: TooltipMakerAPI) {
        tooltip.addSectionHeading("Current Condition", StockReviewStyle.PANEL_HEADING, StockReviewStyle.ROW_BORDER, Alignment.MID, 10f)
        tooltip.beginGrid(720f, 2)
        tooltip.addToGrid(0, 0, "Combat readiness", percent(member.repairTracker?.cr ?: 0f))
        tooltip.addToGrid(0, 1, "Hull integrity", percent(member.repairTracker?.computeRepairednessFraction() ?: 1f))
        tooltip.addGrid(4f)
    }

    private fun addDataColumns(tooltip: TooltipMakerAPI) {
        tooltip.addSectionHeading("Logistical data", StockReviewStyle.PANEL_HEADING, StockReviewStyle.ROW_BORDER, Alignment.MID, 8f)
        tooltip.beginGrid(720f, 3)
        tooltip.addToGrid(0, 0, "CR per deployment", percent(member.hullSpec.crToDeploy))
        tooltip.addToGrid(0, 1, "Maintenance", oneDecimal(member.hullSpec.suppliesPerMonth))
        tooltip.addToGrid(0, 2, "Hull integrity", member.hullSpec.hitpoints.roundToInt().toString())
        tooltip.addToGrid(1, 0, "Hull size", hullSize(member.hullSpec.hullSize))
        tooltip.addToGrid(1, 1, "Cargo capacity", member.cargoCapacity.roundToInt().toString())
        tooltip.addToGrid(1, 2, "Armor rating", member.hullSpec.armorRating.roundToInt().toString())
        tooltip.addToGrid(2, 0, "Deployment points", member.deploymentPointsCost.roundToInt().toString())
        tooltip.addToGrid(2, 1, "Fuel capacity", member.fuelCapacity.roundToInt().toString())
        tooltip.addToGrid(2, 2, "Top speed", member.stats.maxSpeed.modifiedValue.roundToInt().toString())
        tooltip.addGrid(4f)
    }

    private fun addLoadoutRows(tooltip: TooltipMakerAPI) {
        val system = member.hullSpec.shipSystemId?.takeIf { it.isNotBlank() } ?: "None"
        val mounts = member.hullSpec.allWeaponSlotsCopy
            .filter { it.isWeaponSlot && !it.isHidden && !it.isDecorative }
            .groupBy { "${it.slotSize.name.lowercase().replaceFirstChar(Char::titlecase)} ${it.weaponType.name.lowercase().replaceFirstChar(Char::titlecase)}" }
            .map { "${it.value.size}x ${it.key}" }
            .joinToString(", ")
            .ifBlank { "None" }
        val weapons = member.variant?.fittedWeaponSlots
            ?.mapNotNull { slot -> member.variant.getWeaponSpec(slot)?.weaponName }
            ?.distinct()
            ?.joinToString(", ")
            ?.ifBlank { "None" } ?: "None"
        val hullmods = member.variant?.hullMods
            ?.mapNotNull { id -> Global.getSettings().getHullModSpec(id)?.displayName ?: id }
            ?.joinToString(", ")
            ?.ifBlank { "None" } ?: "None"
        tooltip.addPara("System: $system", 10f, StockReviewStyle.TEXT, system)
        tooltip.addPara("Mounts: $mounts", 4f)
        tooltip.addPara("Armaments: $weapons", 4f)
        tooltip.addPara("Hull mods: $hullmods", 4f)
    }

    private fun percent(value: Float): String = "${(value * 100f).roundToInt()}%"
    private fun oneDecimal(value: Float): String = String.format("%.1f", value)
    private fun hullSize(value: ShipAPI.HullSize?): String =
        value?.name?.lowercase()?.replaceFirstChar(Char::titlecase) ?: "Unknown"
}
