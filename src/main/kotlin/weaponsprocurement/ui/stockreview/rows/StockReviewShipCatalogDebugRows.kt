package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.Global
import weaponsprocurement.stock.fixer.ObservedShipStockIndex
import weaponsprocurement.stock.fixer.TheoreticalShipSaleIndex
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle

object StockReviewShipCatalogDebugRows {
    private val observedIndex = ObservedShipStockIndex()
    private val theoreticalIndex = TheoreticalShipSaleIndex()

    @JvmStatic
    fun build(): List<WimGuiListRow<StockReviewAction>> {
        val sector = Global.getSector()
        val observed = observedIndex.collect(sector)
        val theoretical = theoreticalIndex.collect(sector)
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()

        rows.add(
            StockReviewListRow.staticHeading(
                "Theoretical Ship Catalog [Hull Types: ${theoretical.size}]",
                false,
                "Developer diagnostic view for Fixer ship catalog candidates. This does not make ships purchasable.",
            ),
        )
        val candidates = TheoreticalShipSaleIndex.sortedCandidates(theoretical)
        if (candidates.isEmpty()) {
            rows.add(StockReviewListRow.empty("No theoretical ship candidates are currently available."))
        } else {
            for (candidate in candidates) {
                rows.add(candidateRow(candidate))
            }
        }

        val observedOnly = observed.filterKeys { !theoretical.containsKey(it) }.values
            .sortedBy { it.cheapestReferenceSource?.displayName ?: it.cheapestReferenceSource?.hullId ?: "" }
        rows.add(
            StockReviewListRow.staticHeading(
                "Observed-Only Ships [Hull Types: ${observedOnly.size}]",
                true,
                "Ships seen in live cargo that are not in the vanilla-theoretical catalog.",
            ),
        )
        if (observedOnly.isEmpty()) {
            rows.add(StockReviewListRow.empty("No observed-only ships in current market cargo."))
        } else {
            for (ship in observedOnly) {
                val source = ship.cheapestReferenceSource ?: continue
                rows.add(observedRow(source, ship.isOnlyUnsupportedCustomSubmarket))
            }
        }
        return rows
    }

    private fun candidateRow(candidate: TheoreticalShipSaleIndex.Candidate): WimGuiListRow<StockReviewAction> {
        val tooltip = candidate.displayName +
            "\nFaction: ${candidate.factionId ?: "?"}" +
            "\nMarket: ${candidate.marketName ?: "?"}" +
            "\nSubmarket: ${candidate.submarketName ?: "?"}" +
            "\nFrequency: ${candidate.hullFrequency ?: "unknown"}" +
            "\nPriority: ${candidate.priority}" +
            "\nCombat budget estimate: ${String.format(java.util.Locale.US, "%.1f", candidate.combatBudgetEstimate)}"
        return StockReviewListRow.item(
            "${candidate.displayName} (${candidate.hullId})",
            WimGuiRowCell.of(
                StockReviewDebugCellGroup.shipRarity(candidate.rarity.label, StockReviewDebugCellGroup.shipRarityFill(candidate.rarity)),
                StockReviewDebugCellGroup.shipSize("${candidate.hullSizeLabel} / ${candidate.fleetPoints} FP"),
                StockReviewDebugCellGroup.shipFaction(candidate.factionId ?: "?"),
                StockReviewDebugCellGroup.shipMarket(candidate.marketName ?: "?"),
                StockReviewDebugCellGroup.shipSubmarket(candidate.submarketName ?: "?"),
                StockReviewDebugCellGroup.shipPrice(candidate.baseUnitPrice),
            ),
            null,
            tooltip,
            StockReviewStyle.WEAPON_INDENT,
        )
    }

    private fun observedRow(source: ObservedShipStockIndex.ObservedShipSource, unsupportedOnly: Boolean): WimGuiListRow<StockReviewAction> {
        return StockReviewListRow.item(
            "${source.displayName} (${source.hullId})",
            WimGuiRowCell.of(
                StockReviewDebugCellGroup.shipRarity(StockReviewDebugCellGroup.observedShipLabel(unsupportedOnly), StockReviewStyle.PRESET_SCOPE_BUTTON),
                StockReviewDebugCellGroup.shipSize("${source.hullSizeLabel} / ${source.fleetPoints} FP"),
                StockReviewDebugCellGroup.shipFaction("?"),
                StockReviewDebugCellGroup.shipMarket(source.marketName ?: "?"),
                StockReviewDebugCellGroup.shipSubmarket(source.submarketName ?: "?"),
                StockReviewDebugCellGroup.shipPrice(source.baseUnitPrice),
            ),
            null,
            "Observed live ship cargo; no vanilla-theoretical rarity estimate was found.",
            StockReviewStyle.WEAPON_INDENT,
        )
    }
}
