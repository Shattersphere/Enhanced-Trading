package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import weaponsprocurement.stock.market.StockSubmarketAccess
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle

/**
 * Builds the local-only ship trade snapshot from market mothballed ships plus player fleet
 * sell candidates. Remote ship trading needs separate source semantics before being added.
 */
class StockReviewShipSnapshotBuilder {
    fun build(market: MarketAPI?, playerFleet: CampaignFleetAPI?, includeBlackMarket: Boolean): StockReviewShipSnapshot {
        if (market == null) {
            return StockReviewShipSnapshot.EMPTY
        }
        val buyRecords = ArrayList<StockReviewShipRecord>()
        addDebugRecord(buyRecords)
        for (submarket in market.submarketsCopy.orEmpty()) {
            if (!StockSubmarketAccess.isTradeEligible(submarket, includeBlackMarket)) continue
            val members = submarket?.cargoNullOk?.mothballedShips?.membersListCopy ?: continue
            for (member in members) {
                addBuyRecord(buyRecords, member, submarket)
            }
        }

        val sellRecords = ArrayList<StockReviewShipRecord>()
        val sellTarget = sellTarget(market, includeBlackMarket)
        if (sellTarget != null) {
            val members = playerFleet?.fleetData?.membersListCopy.orEmpty()
            for (member in members) {
                addSellRecord(sellRecords, member, sellTarget)
            }
        }
        return StockReviewShipSnapshot(buyRecords, sellRecords)
    }

    private fun addBuyRecord(
        records: MutableList<StockReviewShipRecord>,
        member: FleetMemberAPI?,
        submarket: SubmarketAPI,
    ) {
        if (!isTradeableShip(member)) return
        val id = member?.id?.takeIf { it.isNotBlank() } ?: return
        val quote = StockReviewShipPricing.buyQuote(member, submarket)
        records.add(
            StockReviewShipRecord(
                "B|${submarket.specId}|$id",
                StockReviewShipTradeSide.BUY,
                member,
                submarket,
                submarket.specId,
                submarket.nameOneLine,
                quote,
            ),
        )
    }

    private fun addSellRecord(
        records: MutableList<StockReviewShipRecord>,
        member: FleetMemberAPI?,
        submarket: SubmarketAPI,
    ) {
        if (!isTradeableShip(member)) return
        if (member?.isFlagship == true) return
        val id = member?.id?.takeIf { it.isNotBlank() } ?: return
        val quote = StockReviewShipPricing.sellQuote(member, submarket)
        records.add(
            StockReviewShipRecord(
                "S|$id",
                StockReviewShipTradeSide.SELL,
                member,
                submarket,
                submarket.specId,
                submarket.nameOneLine,
                quote,
            ),
        )
    }

    private fun addDebugRecord(records: MutableList<StockReviewShipRecord>) {
        if (!StockReviewStyle.showDebugUi()) return
        val member = debugMember() ?: return
        // Stress record is gated by debug UI but still flows through the normal grid/tooltip path.
        member.shipName = "HSS Debug Worst-Case Layout Regression Fortress of Excessively Long Field Values"
        records.add(
            StockReviewShipRecord(
                "D|debug-worst-case-ship",
                StockReviewShipTradeSide.BUY,
                member,
                null,
                null,
                "Debug sample",
                StockReviewShipPriceQuote(9_999_999, 9_999_999, 99_999_999, 0.99f),
                debugProfile(),
            ),
        )
    }

    private fun debugMember(): FleetMemberAPI? {
        val factory = Global.getFactory() ?: return null
        val candidates = listOf("paragon_Elite", "paragon_Overdriven", "paragon_Hull", "paragon")
        for (variantOrHullId in candidates) {
            try {
                return factory.createFleetMember(FleetMemberType.SHIP, variantOrHullId)
            } catch (_: RuntimeException) {
                continue
            }
        }
        return null
    }

    private fun debugProfile(): StockReviewShipDebugProfile =
        StockReviewShipDebugProfile(
            "Debug Worst-Case Supercapital Siege Carrier Logistics Battleship Mk. XIV-D",
            "Capital",
            "HSS Debug Worst-Case Layout Regression Fortress, Debug Worst-Case Supercapital Siege Carrier Logistics Battleship Mk. XIV-D",
            "Debug/Stress Test",
            "This fake entry intentionally uses extreme values, very long field labels, long system text, dense weapon mounts, and a crowded armament list. It exists only to prove that the ship grid and tooltip can survive worst-case local UI content without wrapping into controls, clipping important text, or creating unusable empty space. The description deliberately keeps going with extra clauses about implausible refit history, overlong campaign provenance, overloaded carrier logistics, and stress-test notes so the tooltip has to expand before it decides to truncate. If a real modded hull ships with this much descriptive copy, the popup should preserve the useful top-level details, avoid colliding with the preview sprite, and cut off the tail gracefully instead of pushing data rows off-screen. This second diagnostic paragraph keeps piling on deliberately awkward content: contradictory doctrine notes, excessive prototype lineage, nested battlefield caveats, refit warnings, salvage-office caveats, officer folklore, support-fleet logistics, and long prose that should use the available tooltip height before it is clipped. The intent is to prove that the title, art, description, data tables, and loadout rows all remain readable when a modded capital ship supplies far more lore text than a vanilla hull ever would.",
            listOf(
                StockReviewShipDebugStat("CR per deployment", "9999%"),
                StockReviewShipDebugStat("Recovery rate (per day)", "999% (-999)"),
                StockReviewShipDebugStat("Recovery cost (supplies)", "99999"),
                StockReviewShipDebugStat("Deployment points", "999"),
                StockReviewShipDebugStat("Peak performance (sec)", "99999"),
                StockReviewShipDebugStat("Crew complement", "99999 / 99999"),
                StockReviewShipDebugStat("Hull size", "Capital"),
                StockReviewShipDebugStat("Ordnance points", "999"),
            ),
            listOf(
                StockReviewShipDebugStat("Maintenance (supplies/mo)", "9999.9"),
                StockReviewShipDebugStat("Cargo capacity", "99999"),
                StockReviewShipDebugStat("Maximum crew", "99999"),
                StockReviewShipDebugStat("Skeleton crew required", "99999"),
                StockReviewShipDebugStat("Fuel capacity", "99999"),
                StockReviewShipDebugStat("Maximum burn", "99"),
                StockReviewShipDebugStat("Fuel / light year", "999.9"),
                StockReviewShipDebugStat("Sensor profile", "99999"),
                StockReviewShipDebugStat("Sensor strength", "99999"),
            ),
            listOf(
                StockReviewShipDebugStat("Hull integrity", "999999"),
                StockReviewShipDebugStat("Armor rating", "99999"),
                StockReviewShipDebugStat("Defense", "Omni Phase Fortress Shield"),
                StockReviewShipDebugStat("Shield arc", "360"),
                StockReviewShipDebugStat("Shield upkeep/sec", "9999"),
                StockReviewShipDebugStat("Shield efficiency", "0.01"),
                StockReviewShipDebugStat("Flux capacity", "999999"),
                StockReviewShipDebugStat("Flux dissipation", "99999"),
                StockReviewShipDebugStat("Top speed", "999"),
            ),
            "Debug Singularity Cascade Fortress Shield With Excessively Long Name",
            "12x Large Energy, 12x Large Ballistic, 12x Large Missile, 18x Medium Hybrid, 18x Medium Synergy, 24x Small Universal, 24x Small Composite",
            "8x Tachyon Lance, 8x Hellbore Cannon, 8x Squall MLRS, 12x Heavy Needler, 12x Hypervelocity Driver, 16x Burst PD Laser, 16x Sabot SRM, 16x Atropos-class Torpedo, 24x Tactical Laser",
            "Advanced Targeting Core, Integrated Targeting Unit, Expanded Missile Racks, Resistant Flux Conduits, Heavy Armor, Hardened Shields, Solar Shielding, Augmented Drive Field, Operations Center",
        )

    private fun sellTarget(market: MarketAPI?, includeBlackMarket: Boolean): SubmarketAPI? {
        val submarkets = market?.submarketsCopy ?: return null
        if (includeBlackMarket) {
            submarkets.firstOrNull { it?.specId == Submarkets.SUBMARKET_BLACK && StockSubmarketAccess.isTradeEligible(it, true) }
                ?.let { return it }
        }
        submarkets.firstOrNull { it?.specId == Submarkets.SUBMARKET_OPEN && StockSubmarketAccess.isTradeEligible(it, includeBlackMarket) }
            ?.let { return it }
        submarkets.firstOrNull { it?.specId == Submarkets.GENERIC_MILITARY && StockSubmarketAccess.isTradeEligible(it, includeBlackMarket) }
            ?.let { return it }
        return submarkets.firstOrNull { StockSubmarketAccess.isTradeEligible(it, includeBlackMarket) }
    }

    private fun isTradeableShip(member: FleetMemberAPI?): Boolean {
        if (member == null) return false
        if (member.isFighterWing) return false
        if (member.isStation) return false
        return member.type != null
    }
}
