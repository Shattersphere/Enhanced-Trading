package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.fleet.FleetMemberAPI

/**
 * Centralizes ship tooltip stat sourcing so layout code does not guess at FleetMemberAPI
 * stat-modifier semantics.
 */
object StockReviewShipStats {
    @JvmStatic
    fun crPerDeployment(member: FleetMemberAPI): Float =
        member.stats.crPerDeploymentPercent.computeEffective(member.hullSpec.crToDeploy)

    @JvmStatic
    fun suppliesToRecover(member: FleetMemberAPI): Float =
        effectiveMutable(member.hullSpec.suppliesToRecover, member.stats.suppliesToRecover)

    @JvmStatic
    fun peakPerformanceSeconds(member: FleetMemberAPI): Float =
        member.stats.peakCRDuration.computeEffective(member.hullSpec.noCRLossTime)

    @JvmStatic
    fun maintenancePerMonth(member: FleetMemberAPI): Float =
        effectiveMutable(member.hullSpec.suppliesPerMonth, member.stats.suppliesPerMonth)

    @JvmStatic
    fun cargoCapacity(member: FleetMemberAPI): Float =
        member.stats.cargoMod.computeEffective(member.hullSpec.cargo)

    @JvmStatic
    fun maxCrew(member: FleetMemberAPI): Float =
        member.stats.maxCrewMod.computeEffective(member.hullSpec.maxCrew)

    @JvmStatic
    fun minCrew(member: FleetMemberAPI): Float =
        member.stats.minCrewMod.computeEffective(member.hullSpec.minCrew)

    @JvmStatic
    fun fuelCapacity(member: FleetMemberAPI): Float =
        member.stats.fuelMod.computeEffective(member.hullSpec.fuel)

    @JvmStatic
    fun fuelPerLightYear(member: FleetMemberAPI): Float =
        member.stats.fuelUseMod.computeEffective(member.hullSpec.fuelPerLY)

    @JvmStatic
    fun hullIntegrity(member: FleetMemberAPI): Float =
        member.stats.hullBonus.computeEffective(member.hullSpec.hitpoints)

    @JvmStatic
    fun armorRating(member: FleetMemberAPI): Float =
        member.stats.armorBonus.computeEffective(member.hullSpec.armorRating)

    @JvmStatic
    fun shieldArc(member: FleetMemberAPI): Float {
        val shield = member.hullSpec.shieldSpec ?: return 0f
        return member.stats.shieldArcBonus.computeEffective(shield.arc)
    }

    @JvmStatic
    fun shieldUpkeepPerSecond(member: FleetMemberAPI): Float {
        val shield = member.hullSpec.shieldSpec ?: return 0f
        return shield.upkeepCost * positiveMultiplierOrOne(member.stats.shieldUpkeepMult.modifiedValue)
    }

    @JvmStatic
    fun shieldFluxPerDamage(member: FleetMemberAPI): Float {
        val shield = member.hullSpec.shieldSpec ?: return 0f
        return shield.fluxPerDamageAbsorbed * positiveMultiplierOrOne(member.stats.shieldAbsorptionMult.modifiedValue)
    }

    @JvmStatic
    fun fluxCapacity(member: FleetMemberAPI): Float =
        effectiveMutable(member.hullSpec.fluxCapacity, member.stats.fluxCapacity)

    @JvmStatic
    fun fluxDissipation(member: FleetMemberAPI): Float =
        effectiveMutable(member.hullSpec.fluxDissipation, member.stats.fluxDissipation)

    @JvmStatic
    fun topSpeed(member: FleetMemberAPI): Float =
        effectiveMutable(member.hullSpec.engineSpec?.maxSpeed ?: member.stats.maxSpeed.modifiedValue, member.stats.maxSpeed)

    private fun effectiveMutable(base: Float, stat: MutableStat?): Float {
        val copy = stat?.createCopy() ?: return base
        copy.setBaseValue(base)
        return copy.modifiedValue
    }

    private fun positiveMultiplierOrOne(value: Float): Float =
        if (value > 0f) value else 1f
}
