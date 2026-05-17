package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction

private enum class StockReviewItemInfoVisibility {
    REQUIRED,
    MEANINGFUL,
    POSITIVE,
}

class StockReviewItemInfoField private constructor(
    private val label: (WeaponStockRecord) -> String,
    private val value: (WeaponStockRecord) -> String?,
    private val tooltip: (WeaponStockRecord) -> String?,
    private val visibility: StockReviewItemInfoVisibility,
) {
    fun row(record: WeaponStockRecord, layout: StockReviewRowLayout): WimGuiListRow<StockReviewAction>? {
        val value = value.invoke(record)
        if (!shouldShow(value)) {
            return null
        }
        return StockReviewDetailRows.itemInfo(
            label.invoke(record),
            value,
            layout,
            tooltip.invoke(record),
        )
    }

    private fun shouldShow(value: String?): Boolean =
        when (visibility) {
            StockReviewItemInfoVisibility.REQUIRED -> true
            StockReviewItemInfoVisibility.MEANINGFUL -> isMeaningful(value)
            StockReviewItemInfoVisibility.POSITIVE -> isPositiveValue(value)
        }

    companion object {
        @JvmStatic
        fun required(label: String, value: (WeaponStockRecord) -> String?): StockReviewItemInfoField =
            field({ label }, value, { null }, StockReviewItemInfoVisibility.REQUIRED)

        @JvmStatic
        fun meaningful(label: String, value: (WeaponStockRecord) -> String?): StockReviewItemInfoField =
            field({ label }, value, { null }, StockReviewItemInfoVisibility.MEANINGFUL)

        @JvmStatic
        fun meaningful(
            label: String,
            value: (WeaponStockRecord) -> String?,
            tooltip: (WeaponStockRecord) -> String?,
        ): StockReviewItemInfoField = field({ label }, value, tooltip, StockReviewItemInfoVisibility.MEANINGFUL)

        @JvmStatic
        fun meaningful(
            label: (WeaponStockRecord) -> String,
            value: (WeaponStockRecord) -> String?,
        ): StockReviewItemInfoField = field(label, value, { null }, StockReviewItemInfoVisibility.MEANINGFUL)

        @JvmStatic
        fun positive(label: String, value: (WeaponStockRecord) -> String?): StockReviewItemInfoField =
            field({ label }, value, { null }, StockReviewItemInfoVisibility.POSITIVE)

        @JvmStatic
        fun positive(
            label: (WeaponStockRecord) -> String,
            value: (WeaponStockRecord) -> String?,
        ): StockReviewItemInfoField = field(label, value, { null }, StockReviewItemInfoVisibility.POSITIVE)

        private fun field(
            label: (WeaponStockRecord) -> String,
            value: (WeaponStockRecord) -> String?,
            tooltip: (WeaponStockRecord) -> String?,
            visibility: StockReviewItemInfoVisibility,
        ): StockReviewItemInfoField = StockReviewItemInfoField(label, value, tooltip, visibility)

        private fun isMeaningful(value: String?): Boolean =
            value != null && value.trim().isNotEmpty() && value.trim() != "?"

        private fun isPositiveValue(value: String?): Boolean {
            if (!isMeaningful(value)) {
                return false
            }
            val normalized = value?.replace("\u00b0/s", "")?.trim() ?: return false
            return try {
                normalized.toFloat() > 0f
            } catch (ex: NumberFormatException) {
                true
            }
        }
    }
}

object StockReviewItemInfoFields {
    private val FIXER_FIELDS = listOf(
        StockReviewItemInfoField.required("Desired") { it.desiredCount.toString() },
        StockReviewItemInfoField.meaningful("Availability", { it.fixerAvailabilityLabel }, { it.fixerAvailabilityDetails }),
        StockReviewItemInfoField.meaningful("Rarity", { it.fixerRarityLabel }, { it.fixerRarityDetails }),
    )

    private val WING_BASIC_FIELDS = listOf(
        StockReviewItemInfoField.meaningful("Primary Role") { it.typeLabel },
        StockReviewItemInfoField.required("Size") { "WING" },
        StockReviewItemInfoField.meaningful("Fighters") { it.wingFighterCountLabel },
        StockReviewItemInfoField.meaningful("OP") { it.wingOpCostLabel },
        StockReviewItemInfoField.meaningful("Range") { it.rangeLabel },
        StockReviewItemInfoField.meaningful("Refit(Sec)") { it.wingRefitTimeLabel },
    )

    private val WEAPON_BASIC_FIELDS = listOf(
        StockReviewItemInfoField.meaningful("Primary Role") { it.primaryRoleLabel },
        StockReviewItemInfoField.meaningful("Size") { it.sizeLabel },
        StockReviewItemInfoField.meaningful("Type") { it.typeLabel },
        StockReviewItemInfoField.meaningful("OP") { it.opCostLabel },
        StockReviewItemInfoField.meaningful("Range") { it.rangeLabel },
        StockReviewItemInfoField.meaningful("Refire(Sec)") { it.refireSecondsLabel },
        StockReviewItemInfoField.meaningful("Damage") { it.damageLabel },
        StockReviewItemInfoField.meaningful(
            { if (it.hasDifferentSustainedDamagePerSecond()) "Damage/Sec (sustained)" else "Damage/Sec" },
            { it.sustainedDamagePerSecondLabel },
        ),
        StockReviewItemInfoField.meaningful(
            { if (it.hasDifferentSustainedFluxPerSecond()) "Flux/Sec (sustained)" else "Flux/Sec" },
            { it.sustainedFluxPerSecondLabel },
        ),
        StockReviewItemInfoField.meaningful("Flux/Damage") { it.fluxPerDamageLabel },
        StockReviewItemInfoField.positive("EMP") { it.empLabel },
        StockReviewItemInfoField.meaningful("Max Ammo") { it.maxAmmoLabel },
        StockReviewItemInfoField.meaningful("Sec / Reload") { it.secPerReloadLabel },
        StockReviewItemInfoField.meaningful("Ammo Gain") { it.ammoGainLabel },
        StockReviewItemInfoField.meaningful("Accuracy") { it.accuracyLabel },
    )

    private val WEAPON_ADVANCED_FIELDS = listOf(
        StockReviewItemInfoField.positive(
            { if (it.hasDifferentSustainedEmpPerSecond()) "EMP/Second (sustained)" else "EMP/Second" },
            { it.sustainedEmpPerSecondLabel },
        ),
        StockReviewItemInfoField.positive("Flux/EMP") { it.fluxPerEmpLabel },
        StockReviewItemInfoField.meaningful("Beam DPS") { it.beamDpsLabel },
        StockReviewItemInfoField.meaningful("Charge Up") { it.beamChargeUpLabel },
        StockReviewItemInfoField.meaningful("Charge Down") { it.beamChargeDownLabel },
        StockReviewItemInfoField.meaningful("Burst Delay") { it.burstDelayLabel },
        StockReviewItemInfoField.meaningful("Turn Rate/Second") { it.turnRateLabel },
        StockReviewItemInfoField.meaningful("Min Spread") { it.minSpreadLabel },
        StockReviewItemInfoField.meaningful("Max Spread") { it.maxSpreadLabel },
        StockReviewItemInfoField.meaningful("Spread / Shot") { it.spreadPerShotLabel },
        StockReviewItemInfoField.meaningful("Spread Decay") { it.spreadDecayLabel },
        StockReviewItemInfoField.meaningful("Proj. Speed") { it.projectileSpeedLabel },
        StockReviewItemInfoField.meaningful("Launch Speed") { it.launchSpeedLabel },
        StockReviewItemInfoField.meaningful("Flight Time") { it.flightTimeLabel },
        StockReviewItemInfoField.meaningful("Guided") { it.guidedLabel },
    )

    @JvmStatic
    fun basic(record: WeaponStockRecord): List<StockReviewItemInfoField> =
        if (record.isWing()) FIXER_FIELDS + WING_BASIC_FIELDS else FIXER_FIELDS + WEAPON_BASIC_FIELDS

    @JvmStatic
    fun advanced(record: WeaponStockRecord): List<StockReviewItemInfoField> =
        if (record.isWing()) emptyList() else WEAPON_ADVANCED_FIELDS
}
