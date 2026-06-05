package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import weaponsprocurement.stock.item.WeaponStockRecord
import java.util.Locale

internal class StockReviewWeaponTooltipRows(
    private val record: WeaponStockRecord,
) {
    fun primaryRows(spec: WeaponSpecAPI): List<StockReviewTooltipStatRow> {
        val rows = ArrayList<StockReviewTooltipStatRow>()
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

    fun ancillaryRows(spec: WeaponSpecAPI): List<StockReviewTooltipStatRow> {
        val rows = ArrayList<StockReviewTooltipStatRow>()
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

    private fun damageLabel(spec: WeaponSpecAPI): String = if (spec.hasTag("damage_special")) "Special" else "Damage"

    private fun addMountNotes(rows: MutableList<StockReviewTooltipStatRow>, spec: WeaponSpecAPI) {
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

    private fun addRow(rows: MutableList<StockReviewTooltipStatRow>, label: String, value: String?) {
        if (!hasMeaningful(value)) {
            return
        }
        rows.add(StockReviewTooltipStatRow(label, value ?: ""))
    }

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
}
