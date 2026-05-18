package weaponsprocurement.ui.stockreview.state

import com.fs.starfarer.api.combat.ShipAPI
import java.util.EnumMap
import java.util.EnumSet

enum class StockReviewShipSizeFilter(
    val label: String,
    val hullSize: ShipAPI.HullSize,
) {
    FRIGATE("Frigate", ShipAPI.HullSize.FRIGATE),
    DESTROYER("Destroyer", ShipAPI.HullSize.DESTROYER),
    CRUISER("Cruiser", ShipAPI.HullSize.CRUISER),
    CAPITAL("Capital", ShipAPI.HullSize.CAPITAL_SHIP),
}

enum class StockReviewShipFilterField(
    val label: String,
    val numeric: Boolean,
) {
    MAX_COST("Max cost", true),
    MIN_ORDNANCE_POINTS("Min ordnance points", true),
    MIN_SMALL_MOUNTS("Min small mounts", true),
    MIN_MEDIUM_MOUNTS("Min medium mounts", true),
    MIN_LARGE_MOUNTS("Min large mounts", true),
    MIN_ENERGY_MOUNTS("Min energy mounts", true),
    MIN_BALLISTIC_MOUNTS("Min ballistic mounts", true),
    SHIP_SYSTEM("Ship system", false),
}

class StockReviewShipFilterState {
    private val activeSizes: EnumSet<StockReviewShipSizeFilter> = EnumSet.noneOf(StockReviewShipSizeFilter::class.java)
    private val fields: EnumMap<StockReviewShipFilterField, String> = EnumMap(StockReviewShipFilterField::class.java)

    constructor()

    constructor(source: StockReviewShipFilterState) {
        activeSizes.addAll(source.activeSizes)
        fields.putAll(source.fields)
    }

    fun isSizeActive(size: StockReviewShipSizeFilter?): Boolean = activeSizes.contains(size)

    fun toggleSize(size: StockReviewShipSizeFilter?): Boolean {
        if (size == null) return false
        if (activeSizes.contains(size)) {
            activeSizes.remove(size)
        } else {
            activeSizes.add(size)
        }
        return true
    }

    fun getActiveSizes(): Set<StockReviewShipSizeFilter> = EnumSet.copyOf(activeSizes)

    fun getField(field: StockReviewShipFilterField?): String =
        if (field == null) "" else fields[field].orEmpty()

    fun setField(field: StockReviewShipFilterField?, value: String?): Boolean {
        if (field == null) return false
        val normalized = normalize(field, value)
        if (normalized.isBlank()) {
            val hadValue = fields.remove(field) != null
            return hadValue
        }
        if (fields[field] == normalized) {
            return false
        }
        fields[field] = normalized
        return true
    }

    fun appendField(field: StockReviewShipFilterField?, char: Char): Boolean {
        if (field == null) return false
        val current = getField(field)
        if (current.length >= MAX_FIELD_LENGTH || !isAllowed(field, char)) {
            return false
        }
        return setField(field, current + char)
    }

    fun backspaceField(field: StockReviewShipFilterField?): Boolean {
        val current = getField(field)
        if (current.isEmpty()) {
            return false
        }
        return setField(field, current.substring(0, current.length - 1))
    }

    fun intValue(field: StockReviewShipFilterField): Int? =
        getField(field).toIntOrNull()

    fun clear(): Boolean {
        val changed = activeSizes.isNotEmpty() || fields.isNotEmpty()
        activeSizes.clear()
        fields.clear()
        return changed
    }

    fun activeCount(): Int = activeSizes.size + fields.values.count { it.isNotBlank() }

    private fun normalize(field: StockReviewShipFilterField, value: String?): String =
        value.orEmpty()
            .filter { isAllowed(field, it) }
            .take(MAX_FIELD_LENGTH)
            .trimStart()

    private fun isAllowed(field: StockReviewShipFilterField, char: Char): Boolean =
        if (field.numeric) {
            char.isDigit()
        } else {
            char.isLetterOrDigit() || char == ' ' || char == '-' || char == '_' || char == '\'' || char == '.'
        }

    companion object {
        private const val MAX_FIELD_LENGTH = 32
    }
}
