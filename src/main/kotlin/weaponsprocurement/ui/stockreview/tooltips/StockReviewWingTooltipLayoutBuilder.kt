package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import weaponsprocurement.stock.item.StockDebugItemProfile
import weaponsprocurement.stock.item.StockDebugItemStat
import weaponsprocurement.stock.item.WeaponStockRecord
import java.util.Locale

internal object StockReviewWingTooltipLayoutBuilder {
    fun forRecord(record: WeaponStockRecord, spec: FighterWingSpecAPI): StockReviewWingTooltipLayout {
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

    fun forDebugProfile(profile: StockDebugItemProfile): StockReviewWingTooltipLayout {
        return StockReviewWingTooltipLayout(
            profile.tooltipTitle,
            profile.manufacturer,
            profile.description,
            debugRows(profile.wingTechnicalRows),
            profile.wingSystem,
            profile.wingArmaments,
        )
    }

    private fun addRow(rows: MutableList<StockReviewTooltipStatRow>, label: String, value: String?) {
        if (!hasMeaningful(value)) {
            return
        }
        rows.add(StockReviewTooltipStatRow(label, value ?: ""))
    }

    private fun debugRows(rows: List<StockDebugItemStat>): List<StockReviewTooltipStatRow> =
        rows.map { StockReviewTooltipStatRow(it.label, it.value) }

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
