package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import weaponsprocurement.stock.market.StockSubmarketAccess

class StockReviewShipSnapshotBuilder {
    fun build(market: MarketAPI?, playerFleet: CampaignFleetAPI?, includeBlackMarket: Boolean): StockReviewShipSnapshot {
        if (market == null) {
            return StockReviewShipSnapshot.EMPTY
        }
        val buyRecords = ArrayList<StockReviewShipRecord>()
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
