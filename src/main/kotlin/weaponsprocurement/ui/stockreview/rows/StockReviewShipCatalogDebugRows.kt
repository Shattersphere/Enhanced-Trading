package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.stock.fixer.ObservedShipStockIndex
import weaponsprocurement.stock.fixer.ShipCatalogRarity
import weaponsprocurement.stock.fixer.TheoreticalShipSaleIndex
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
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
                info(candidate.rarity.label, RARITY_WIDTH, rarityFill(candidate.rarity), Alignment.MID),
                info("${candidate.hullSizeLabel} / ${candidate.fleetPoints} FP", SIZE_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.MID),
                info(candidate.factionId ?: "?", FACTION_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID),
                info(candidate.marketName ?: "?", MARKET_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID),
                info(candidate.submarketName ?: "?", SUBMARKET_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID),
                info(StockReviewFormat.credits(candidate.baseUnitPrice.toLong()), PRICE_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.RMID),
            ),
            null,
            tooltip,
            StockReviewStyle.WEAPON_INDENT,
        )
    }

    private fun observedRow(source: ObservedShipStockIndex.ObservedShipSource, unsupportedOnly: Boolean): WimGuiListRow<StockReviewAction> {
        val sourceLabel = if (unsupportedOnly) "Custom only" else "Observed"
        return StockReviewListRow.item(
            "${source.displayName} (${source.hullId})",
            WimGuiRowCell.of(
                info(sourceLabel, RARITY_WIDTH, StockReviewStyle.PRESET_SCOPE_BUTTON, Alignment.MID),
                info("${source.hullSizeLabel} / ${source.fleetPoints} FP", SIZE_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.MID),
                info("?", FACTION_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID),
                info(source.marketName ?: "?", MARKET_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID),
                info(source.submarketName ?: "?", SUBMARKET_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID),
                info(StockReviewFormat.credits(source.baseUnitPrice.toLong()), PRICE_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.RMID),
            ),
            null,
            "Observed live ship cargo; no vanilla-theoretical rarity estimate was found.",
            StockReviewStyle.WEAPON_INDENT,
        )
    }

    private fun info(label: String?, width: Float, fill: java.awt.Color?, alignment: Alignment): WimGuiRowCell<StockReviewAction> =
        WimGuiRowCell.info(label, width, fill, StockReviewStyle.TEXT, alignment)

    private fun rarityFill(rarity: ShipCatalogRarity): java.awt.Color =
        when (rarity) {
            ShipCatalogRarity.COMMON -> StockReviewStyle.CONFIRM_BUTTON
            ShipCatalogRarity.UNCOMMON -> StockReviewStyle.CELL_BACKGROUND
            ShipCatalogRarity.RARE -> StockReviewStyle.LOAD_BUTTON
            ShipCatalogRarity.VERY_RARE -> StockReviewStyle.CANCEL_BUTTON
            ShipCatalogRarity.UNKNOWN_CUSTOM_SUBMARKET -> StockReviewStyle.PRESET_SCOPE_BUTTON
        }

    private const val RARITY_WIDTH = 104f
    private const val SIZE_WIDTH = 112f
    private const val FACTION_WIDTH = 132f
    private const val MARKET_WIDTH = 190f
    private const val SUBMARKET_WIDTH = 150f
    private const val PRICE_WIDTH = 112f
}
