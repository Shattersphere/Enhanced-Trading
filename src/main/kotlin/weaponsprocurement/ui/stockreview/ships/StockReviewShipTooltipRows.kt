package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltipStatRow
import java.util.Locale
import kotlin.math.roundToInt

internal class StockReviewShipTooltipRows(
    private val member: FleetMemberAPI,
    private val debugProfile: StockReviewShipDebugProfile?,
) {
    fun logisticsLeft(): List<StockReviewTooltipStatRow> =
        debugProfile?.logisticsLeft?.map { StockReviewTooltipStatRow(it.label, it.value) } ?: listOf(
            StockReviewTooltipStatRow("CR per deployment", percent(StockReviewShipStats.crPerDeployment(member))),
            StockReviewTooltipStatRow("Recovery rate (per day)", percent(member.repairTracker?.recoveryRate ?: member.stats.repairRatePercentPerDay.modifiedValue / 100f)),
            StockReviewTooltipStatRow("Recovery cost (supplies)", integer(StockReviewShipStats.suppliesToRecover(member))),
            StockReviewTooltipStatRow("Deployment points", integer(member.deploymentPointsCost)),
            StockReviewTooltipStatRow("Peak performance (sec)", integer(StockReviewShipStats.peakPerformanceSeconds(member))),
            StockReviewTooltipStatRow("Crew complement", "${integer(member.crewComposition?.crew ?: 0f)} / ${integer(StockReviewShipStats.maxCrew(member))}"),
            StockReviewTooltipStatRow("Hull size", hullSize(member.hullSpec.hullSize)),
            StockReviewTooltipStatRow("Ordnance points", member.hullSpec.getOrdnancePoints(Global.getSector()?.playerStats).toString()),
        )

    fun logisticsRight(): List<StockReviewTooltipStatRow> =
        debugProfile?.logisticsRight?.map { StockReviewTooltipStatRow(it.label, it.value) } ?: listOf(
            StockReviewTooltipStatRow("Maintenance (supplies/mo)", oneDecimal(StockReviewShipStats.maintenancePerMonth(member))),
            StockReviewTooltipStatRow("Cargo capacity", integer(StockReviewShipStats.cargoCapacity(member))),
            StockReviewTooltipStatRow("Maximum crew", integer(StockReviewShipStats.maxCrew(member))),
            StockReviewTooltipStatRow("Skeleton crew required", integer(StockReviewShipStats.minCrew(member))),
            StockReviewTooltipStatRow("Fuel capacity", integer(StockReviewShipStats.fuelCapacity(member))),
            StockReviewTooltipStatRow("Maximum burn", integer(member.stats.maxBurnLevel.modifiedValue)),
            StockReviewTooltipStatRow("Fuel / light year", oneDecimal(StockReviewShipStats.fuelPerLightYear(member))),
            StockReviewTooltipStatRow("Sensor profile", integer(member.stats.sensorProfile.modifiedValue)),
            StockReviewTooltipStatRow("Sensor strength", integer(member.stats.sensorStrength.modifiedValue)),
        )

    fun combat(): List<StockReviewTooltipStatRow> =
        debugProfile?.combat?.map { StockReviewTooltipStatRow(it.label, it.value) } ?: listOf(
            StockReviewTooltipStatRow("Hull integrity", integer(StockReviewShipStats.hullIntegrity(member))),
            StockReviewTooltipStatRow("Armor rating", integer(StockReviewShipStats.armorRating(member))),
            StockReviewTooltipStatRow("Defense", defenseLabel()),
            StockReviewTooltipStatRow("Shield arc", shieldArcLabel()),
            StockReviewTooltipStatRow("Shield upkeep/sec", shieldUpkeepLabel()),
            StockReviewTooltipStatRow("Shield efficiency", shieldEfficiencyLabel()),
            StockReviewTooltipStatRow("Flux capacity", integer(StockReviewShipStats.fluxCapacity(member))),
            StockReviewTooltipStatRow("Flux dissipation", integer(StockReviewShipStats.fluxDissipation(member))),
            StockReviewTooltipStatRow("Top speed", integer(StockReviewShipStats.topSpeed(member))),
        )

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
        return integer(StockReviewShipStats.shieldArc(member))
    }

    private fun shieldUpkeepLabel(): String {
        val shield = member.hullSpec.shieldSpec ?: return ""
        return integer(StockReviewShipStats.shieldUpkeepPerSecond(member))
    }

    private fun shieldEfficiencyLabel(): String {
        val shield = member.hullSpec.shieldSpec ?: return ""
        return oneDecimalTrim(StockReviewShipStats.shieldFluxPerDamage(member))
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
}
