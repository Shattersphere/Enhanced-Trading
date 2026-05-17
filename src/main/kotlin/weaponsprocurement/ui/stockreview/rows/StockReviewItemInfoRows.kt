package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.stock.item.WeaponStockRecord

object StockReviewItemInfoRows {
    @JvmStatic
    fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, record: WeaponStockRecord, state: StockReviewState?) {
        add(rows, record, state, StockReviewRowLayout.trade())
    }

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState?,
        layout: StockReviewRowLayout,
    ) {
        val basicExpanded = isInfoSectionExpanded(state, StockReviewHeadingRows.basicInfoSectionKey(record))
        rows.add(StockReviewHeadingRows.basicInfo(record, basicExpanded, layout))
        if (basicExpanded) {
            addBasicInfo(rows, record, layout)
        }
        if (record.isWing()) {
            return
        }
        val advancedExpanded = isInfoSectionExpanded(state, StockReviewHeadingRows.advancedInfoSectionKey(record))
        rows.add(StockReviewHeadingRows.advancedInfo(record, advancedExpanded, layout))
        if (advancedExpanded) {
            addAdvancedInfo(rows, record, layout)
        }
    }

    private fun addBasicInfo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        layout: StockReviewRowLayout,
    ) {
        addRequiredDataRow(rows, "Desired", record.desiredCount.toString(), layout)
        addDataRow(
            rows,
            "Availability",
            record.fixerAvailabilityLabel,
            layout,
            record.fixerAvailabilityDetails,
        )
        addDataRow(rows, "Rarity", record.fixerRarityLabel, layout, record.fixerRarityDetails)
        if (record.isWing()) {
            addDataRow(rows, "Primary Role", record.typeLabel, layout)
            addRequiredDataRow(rows, "Size", "WING", layout)
            addDataRow(rows, "Fighters", record.wingFighterCountLabel, layout)
            addDataRow(rows, "OP", record.wingOpCostLabel, layout)
            addDataRow(rows, "Range", record.rangeLabel, layout)
            addDataRow(rows, "Refit(Sec)", record.wingRefitTimeLabel, layout)
            return
        }
        addDataRow(rows, "Primary Role", record.primaryRoleLabel, layout)
        addDataRow(rows, "Size", record.sizeLabel, layout)
        addDataRow(rows, "Type", record.typeLabel, layout)
        addDataRow(rows, "OP", record.opCostLabel, layout)
        addDataRow(rows, "Range", record.rangeLabel, layout)
        addDataRow(rows, "Refire(Sec)", record.refireSecondsLabel, layout)
        addDataRow(rows, "Damage", record.damageLabel, layout)
        addDataRow(
            rows,
            if (record.hasDifferentSustainedDamagePerSecond()) "Damage/Sec (sustained)" else "Damage/Sec",
            record.sustainedDamagePerSecondLabel,
            layout,
        )
        addDataRow(
            rows,
            if (record.hasDifferentSustainedFluxPerSecond()) "Flux/Sec (sustained)" else "Flux/Sec",
            record.sustainedFluxPerSecondLabel,
            layout,
        )
        addDataRow(rows, "Flux/Damage", record.fluxPerDamageLabel, layout)
        addPositiveDataRow(rows, "EMP", record.empLabel, layout)
        addDataRow(rows, "Max Ammo", record.maxAmmoLabel, layout)
        addDataRow(rows, "Sec / Reload", record.secPerReloadLabel, layout)
        addDataRow(rows, "Ammo Gain", record.ammoGainLabel, layout)
        addDataRow(rows, "Accuracy", record.accuracyLabel, layout)
    }

    private fun addAdvancedInfo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        layout: StockReviewRowLayout,
    ) {
        addPositiveDataRow(
            rows,
            if (record.hasDifferentSustainedEmpPerSecond()) "EMP/Second (sustained)" else "EMP/Second",
            record.sustainedEmpPerSecondLabel,
            layout,
        )
        addPositiveDataRow(rows, "Flux/EMP", record.fluxPerEmpLabel, layout)
        addDataRow(rows, "Beam DPS", record.beamDpsLabel, layout)
        addDataRow(rows, "Charge Up", record.beamChargeUpLabel, layout)
        addDataRow(rows, "Charge Down", record.beamChargeDownLabel, layout)
        addDataRow(rows, "Burst Delay", record.burstDelayLabel, layout)
        addDataRow(rows, "Turn Rate/Second", record.turnRateLabel, layout)
        addDataRow(rows, "Min Spread", record.minSpreadLabel, layout)
        addDataRow(rows, "Max Spread", record.maxSpreadLabel, layout)
        addDataRow(rows, "Spread / Shot", record.spreadPerShotLabel, layout)
        addDataRow(rows, "Spread Decay", record.spreadDecayLabel, layout)
        addDataRow(rows, "Proj. Speed", record.projectileSpeedLabel, layout)
        addDataRow(rows, "Launch Speed", record.launchSpeedLabel, layout)
        addDataRow(rows, "Flight Time", record.flightTimeLabel, layout)
        addDataRow(rows, "Guided", record.guidedLabel, layout)
    }

    private fun isInfoSectionExpanded(state: StockReviewState?, sectionKey: String): Boolean =
        state == null || !state.isItemExpanded(sectionKey)

    private fun addRequiredDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        layout: StockReviewRowLayout,
    ) {
        rows.add(dataRow(label, value, layout))
    }

    private fun addDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        layout: StockReviewRowLayout,
    ) = addDataRow(rows, label, value, layout, null)

    private fun addDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        layout: StockReviewRowLayout,
        tooltip: String?,
    ) {
        if (isMeaningful(value)) {
            rows.add(dataRow(label, value, layout, tooltip))
        }
    }

    private fun addPositiveDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        layout: StockReviewRowLayout,
    ) {
        if (isPositiveValue(value)) {
            rows.add(dataRow(label, value, layout))
        }
    }

    private fun dataRow(
        label: String,
        value: String?,
        layout: StockReviewRowLayout,
    ): WimGuiListRow<StockReviewAction> = dataRow(label, value, layout, null)

    private fun dataRow(
        label: String,
        value: String?,
        layout: StockReviewRowLayout,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = StockReviewListRow.labelTextIndented(
        label,
        value,
        layout.dataIndent,
        false,
        layout.detailRightReserveWidth,
        layout.listWidth,
        tooltip,
    )

    private fun isMeaningful(value: String?): Boolean = value != null && value.trim().isNotEmpty() && value.trim() != "?"

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
