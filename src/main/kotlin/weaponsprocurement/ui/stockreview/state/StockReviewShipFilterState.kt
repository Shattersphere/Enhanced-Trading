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
    MIN_DEPLOYMENT_POINTS("Min deployment points", true),
    MIN_PEAK_PERFORMANCE("Min peak performance", true),
    MIN_HULL_INTEGRITY("Min hull integrity", true),
    MIN_ARMOR_RATING("Min armor rating", true),
    MIN_FLUX_CAPACITY("Min flux capacity", true),
    MIN_FLUX_DISSIPATION("Min flux dissipation", true),
    MIN_FUEL_CAPACITY("Min fuel capacity", true),
    MIN_CARGO_CAPACITY("Min cargo capacity", true),
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

    private fun normalize(field: StockReviewShipFilterField, value: String?): String {
        val normalized = value.orEmpty()
            .filter { isAllowed(field, it) }
            .take(MAX_FIELD_LENGTH)
            .trimStart()
        if (!field.numeric) return normalized
        return normalizeNumeric(normalized)
    }

    private fun normalizeNumeric(value: String): String {
        val significant = value.trimStart('0')
        if (significant.isEmpty()) return if (value.isEmpty()) "" else "0"
        if (significant.length > MAX_INT_TEXT.length ||
            (significant.length == MAX_INT_TEXT.length && significant > MAX_INT_TEXT)
        ) {
            return MAX_INT_TEXT
        }
        return significant
    }

    private fun isAllowed(field: StockReviewShipFilterField, char: Char): Boolean =
        if (field.numeric) {
            char.isDigit()
        } else {
            char.isLetterOrDigit() || char == ' ' || char == '-' || char == '_' || char == '\'' || char == '.'
        }

    companion object {
        private const val MAX_FIELD_LENGTH = 32
        private const val MAX_INT_TEXT = "2147483647"
    }
}
