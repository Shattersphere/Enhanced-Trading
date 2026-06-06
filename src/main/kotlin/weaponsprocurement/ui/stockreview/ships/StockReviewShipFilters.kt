package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.WeaponSlotAPI
import weaponsprocurement.ui.stockreview.state.StockReviewShipFilterField
import weaponsprocurement.ui.stockreview.state.StockReviewState
import java.util.Locale

object StockReviewShipFilters {
    @JvmStatic
    fun filter(records: List<StockReviewShipRecord>, state: StockReviewState): List<StockReviewShipRecord> =
        filterByHullClass(records, state.getShipHullFilter()).filter { matches(it, state) }

    @JvmStatic
    fun matches(record: StockReviewShipRecord, state: StockReviewState): Boolean {
        val activeSizes = state.getActiveShipSizeFilters()
        if (activeSizes.isNotEmpty() && activeSizes.none { it.hullSize == record.member.hullSpec.hullSize }) {
            return false
        }
        if (!matchesMax(record.price.finalCredits, state.getShipFilterInt(StockReviewShipFilterField.MAX_COST))) {
            return false
        }
        if (!matchesMin(ordnancePoints(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_ORDNANCE_POINTS))) {
            return false
        }
        if (!matchesMin(deploymentPoints(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_DEPLOYMENT_POINTS))) {
            return false
        }
        if (!matchesMin(peakPerformance(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_PEAK_PERFORMANCE))) {
            return false
        }
        if (!matchesMin(hullIntegrity(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_HULL_INTEGRITY))) {
            return false
        }
        if (!matchesMin(armorRating(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_ARMOR_RATING))) {
            return false
        }
        if (!matchesMin(fluxCapacity(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_FLUX_CAPACITY))) {
            return false
        }
        if (!matchesMin(fluxDissipation(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_FLUX_DISSIPATION))) {
            return false
        }
        if (!matchesMin(fuelCapacity(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_FUEL_CAPACITY))) {
            return false
        }
        if (!matchesMin(cargoCapacity(record), state.getShipFilterInt(StockReviewShipFilterField.MIN_CARGO_CAPACITY))) {
            return false
        }
        if (!matchesMin(countBySize(record, "SMALL"), state.getShipFilterInt(StockReviewShipFilterField.MIN_SMALL_MOUNTS))) {
            return false
        }
        if (!matchesMin(countBySize(record, "MEDIUM"), state.getShipFilterInt(StockReviewShipFilterField.MIN_MEDIUM_MOUNTS))) {
            return false
        }
        if (!matchesMin(countBySize(record, "LARGE"), state.getShipFilterInt(StockReviewShipFilterField.MIN_LARGE_MOUNTS))) {
            return false
        }
        if (!matchesMin(countByType(record, "ENERGY"), state.getShipFilterInt(StockReviewShipFilterField.MIN_ENERGY_MOUNTS))) {
            return false
        }
        if (!matchesMin(countByType(record, "BALLISTIC"), state.getShipFilterInt(StockReviewShipFilterField.MIN_BALLISTIC_MOUNTS))) {
            return false
        }
        return matchesSystem(record, state.getShipFilterField(StockReviewShipFilterField.SHIP_SYSTEM))
    }

    private fun matchesMax(value: Int, max: Int?): Boolean =
        max == null || value <= max

    private fun matchesMin(value: Int, min: Int?): Boolean =
        min == null || value >= min

    private fun filterByHullClass(records: List<StockReviewShipRecord>, filter: String): List<StockReviewShipRecord> {
        val tokens = filter.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            return records
        }
        return records.filter { record ->
            val searchable = listOfNotNull(
                record.member.hullSpec?.hullName,
                record.member.hullSpec?.nameWithDesignationWithDashClass,
                record.member.hullSpec?.hullId,
                record.member.specId,
            ).joinToString(" ").lowercase(Locale.ROOT)
            tokens.all { searchable.contains(it) }
        }
    }

    private fun ordnancePoints(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 999 else record.member.hullSpec.getOrdnancePoints(Global.getSector()?.playerStats)

    private fun deploymentPoints(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 999 else Math.round(record.member.deploymentPointsCost)

    private fun peakPerformance(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 99_999 else Math.round(StockReviewShipStats.peakPerformanceSeconds(record.member))

    private fun hullIntegrity(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 999_999 else Math.round(StockReviewShipStats.hullIntegrity(record.member))

    private fun armorRating(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 99_999 else Math.round(StockReviewShipStats.armorRating(record.member))

    private fun fluxCapacity(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 999_999 else Math.round(StockReviewShipStats.fluxCapacity(record.member))

    private fun fluxDissipation(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 99_999 else Math.round(StockReviewShipStats.fluxDissipation(record.member))

    private fun fuelCapacity(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 99_999 else Math.round(StockReviewShipStats.fuelCapacity(record.member))

    private fun cargoCapacity(record: StockReviewShipRecord): Int =
        if (record.isDebug()) 99_999 else Math.round(StockReviewShipStats.cargoCapacity(record.member))

    private fun visibleWeaponSlots(record: StockReviewShipRecord): List<WeaponSlotAPI> =
        record.member.hullSpec.allWeaponSlotsCopy.filter { it.isWeaponSlot && !it.isHidden && !it.isDecorative }

    private fun countBySize(record: StockReviewShipRecord, size: String): Int =
        if (record.isDebug()) 99 else visibleWeaponSlots(record).count { it.slotSize.name == size }

    private fun countByType(record: StockReviewShipRecord, type: String): Int =
        if (record.isDebug()) 99 else visibleWeaponSlots(record).count { it.weaponType.name == type }

    private fun matchesSystem(record: StockReviewShipRecord, filter: String): Boolean {
        if (filter.isBlank()) {
            return true
        }
        if (record.debugProfile != null) {
            val haystack = record.debugProfile.system.lowercase(Locale.ROOT)
            return filter.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }.all { haystack.contains(it) }
        }
        val id = record.member.hullSpec.shipSystemId.orEmpty()
        val name = try {
            Global.getSettings().getShipSystemSpec(id)?.name.orEmpty()
        } catch (_: RuntimeException) {
            ""
        }
        val haystack = "$id $name".lowercase(Locale.ROOT)
        return filter.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }.all { haystack.contains(it) }
    }
}
