package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.CompatibilityIds
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockDebugItemProfile
import weaponsprocurement.stock.item.StockDebugItemStat
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.WeaponStockRecord
import java.util.Collections

/**
 * Creates debug weapon/LPC records for layout stress testing. These records should remain
 * gated behind debug UI and should exercise normal item row and tooltip paths.
 */
object StockReviewDebugItemRecords {
    const val EMPTY_ICON = CompatibilityIds.Diagnostics.DEBUG_EMPTY_ITEM_ICON

    private const val DEBUG_WEAPON_ID = CompatibilityIds.Diagnostics.DEBUG_WEAPON_ID
    private const val DEBUG_WING_ID = CompatibilityIds.Diagnostics.DEBUG_WING_ID

    private val weaponProfile = StockDebugItemProfile(
        EMPTY_ICON,
        "Debug Worst-Case Suzuki-Clapteryon Thermal Prokector",
        "Debug/Stress Test",
        "This fake diagnostic weapon intentionally uses extreme labels, long text, inflated combat values, ammo data, beam timing, missile timing, and awkward mixed mount metadata. It exists only to prove that the item row, basic info, advanced info, and tooltip can handle worst-case weapon content without clipping important controls or silently hiding fields.",
        "999.9",
        "99,999+",
        listOf(
            stat("Primary role", "Debug direct-fire area suppression siege beam torpedo launcher"),
            stat("Mount type", "Large, Composite"),
            stat("", "Requires a Ballistic, Energy, Missile, or Universal slot"),
            stat("Ordnance points", "999"),
            stat("Range", "99,999"),
            stat("Damage", "99,999x99"),
            stat("EMP damage", "99,999"),
            stat("Damage / second", "99,999 (999,999 burst)"),
            stat("Flux / second", "999,999 (9,999,999 burst)"),
            stat("Flux / shot", "999,999"),
            stat("Flux / damage", "0.01"),
        ),
        listOf(
            stat("Damage type", "High Explosive / Energy / Fragmentation stress sample"),
            stat("", "400% vs armor, 200% vs shields, 25% vs hull"),
            stat("Speed", "99,999"),
            stat("Tracking", "Perfect omnidirectional predictive reacquisition"),
            stat("Accuracy", "Debug absolute worst-case text expansion accuracy descriptor"),
            stat("Turn rate", "9,999 deg/s"),
            stat("Burst size", "99"),
            stat("Refire delay (seconds)", "0.01"),
            stat("Ammo", "9,999"),
            stat("Recharge / second", "99.9"),
            stat("Reload time (seconds)", "999.99"),
            stat("Charge up", "99.99"),
            stat("Charge down", "99.99"),
        ),
        emptyList(),
        "",
        "",
        mapOf(
            "primaryRoleLabel" to "Debug siege suppression beam torpedo",
            "sizeLabel" to "Large",
            "typeLabel" to "Composite",
            "opCostLabel" to "999",
            "rangeLabel" to "99,999",
            "refireSecondsLabel" to "0.01",
            "damageLabel" to "99,999",
            "sustainedDamagePerSecondLabel" to "99,999 (999,999)",
            "sustainedFluxPerSecondLabel" to "999,999 (9,999,999)",
            "fluxPerDamageLabel" to "0.01",
            "empLabel" to "99,999",
            "maxAmmoLabel" to "9,999",
            "secPerReloadLabel" to "999.99",
            "ammoGainLabel" to "99.9",
            "accuracyLabel" to "Debug perfect overlong accuracy",
            "sustainedEmpPerSecondLabel" to "999,999",
            "fluxPerEmpLabel" to "1",
            "beamDpsLabel" to "999,999",
            "beamChargeUpLabel" to "99.99",
            "beamChargeDownLabel" to "99.99",
            "burstDelayLabel" to "99.99",
            "turnRateLabel" to "9,999 deg/s",
            "minSpreadLabel" to "0.0",
            "maxSpreadLabel" to "99.9",
            "spreadPerShotLabel" to "99.9",
            "spreadDecayLabel" to "999.9",
            "projectileSpeedLabel" to "99,999",
            "launchSpeedLabel" to "99,999",
            "flightTimeLabel" to "999.99",
            "guidedLabel" to "TRUE",
        ),
    )

    private val wingProfile = StockDebugItemProfile(
        EMPTY_ICON,
        "Debug Worst-Case Paragon-Drone Supercapital Strike Wing LPC",
        "Debug/Stress Test",
        "This fake diagnostic LPC intentionally combines a long wing name, a long manufacturer, maximum fighter count, extreme OP, range, replacement time, shield, flux, armor, hull, speed, system, and armament values. It also uses an overlong descriptive paragraph with repeated tactical qualifiers, awkward punctuation, and dense field references so the tooltip must decide where to wrap and where to stop after using its allowed height. It is not tradeable and exists only to exercise the normal wing row, tooltip, basic info, and advanced info paths with worst-case field density rather than a special-purpose mock layout.",
        "999.9",
        "99,999+",
        emptyList(),
        emptyList(),
        listOf(
            stat("Primary role", "Debug strike superiority bomber interceptor drone support hybrid"),
            stat("Ordnance points", "999"),
            stat("Crew per fighter", "999"),
            stat("Maximum engagement range", "99,999"),
            stat("Fighters in wing", "99"),
            stat("Base replacement time (seconds)", "999"),
            stat("Hull integrity", "999,999"),
            stat("Armor rating", "99,999"),
            stat("Top speed", "9,999"),
            stat("Flux capacity", "999,999"),
            stat("Flux dissipation", "99,999"),
            stat("Shield efficiency", "0.01"),
            stat("Shield arc", "360"),
        ),
        "Debug Singularity Cascade Fortress Shield Drone Relay With Excessively Long Name",
        "99x Debug Tachyon Lance Drone Emitter, 99x Debug Hellbore Micro-Cannon, 99x Debug Sabot Swarm Rack, 99x Debug Point Defense Laser Cluster, 99x Debug Mining Blaster",
        mapOf(
            "primaryRoleLabel" to "Debug strike superiority bomber interceptor drone support hybrid",
            "typeLabel" to "Debug Drone Bomber Interceptor Support",
            "sizeLabel" to "Wing",
            "wingFighterCountLabel" to "99",
            "wingOpCostLabel" to "999",
            "rangeLabel" to "99,999",
            "wingRefitTimeLabel" to "999s",
            "wingCrewPerFighterLabel" to "999",
            "wingHullIntegrityLabel" to "999,999",
            "wingArmorRatingLabel" to "99,999",
            "wingTopSpeedLabel" to "9,999",
            "wingFluxCapacityLabel" to "999,999",
            "wingFluxDissipationLabel" to "99,999",
            "wingShieldEfficiencyLabel" to "0.01",
            "wingShieldArcLabel" to "360",
        ),
    )

    @JvmStatic
    fun weapon(): WeaponStockRecord =
        debugRecord(StockItemType.WEAPON, DEBUG_WEAPON_ID, StockReviewCellGroup.DEBUG_WORST_CASE_LABEL.removeSuffix(" (+)"), weaponProfile)

    @JvmStatic
    fun wing(): WeaponStockRecord =
        debugRecord(StockItemType.WING, DEBUG_WING_ID, "Debug Worst-Case Paragon-Drone Supercapital Strike Wing LPC", wingProfile)

    @JvmStatic
    fun forItemType(itemType: StockItemType): WeaponStockRecord =
        if (StockItemType.WING == itemType) wing() else weapon()

    private fun debugRecord(
        itemType: StockItemType,
        itemId: String,
        displayName: String,
        profile: StockDebugItemProfile,
    ): WeaponStockRecord =
        WeaponStockRecord(
            itemType,
            itemId,
            displayName,
            99,
            99,
            0,
            99,
            StockCategory.NO_STOCK,
            Collections.emptyList(),
            profile,
        )

    private fun stat(label: String, value: String): StockDebugItemStat = StockDebugItemStat(label, value)
}
