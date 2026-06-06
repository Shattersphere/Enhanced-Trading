package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import weaponsprocurement.stock.market.StockSubmarketAccess

object StockReviewShipAvailability {
    @JvmStatic
    fun hasLocalTradeOpportunity(
        market: MarketAPI?,
        playerFleet: CampaignFleetAPI?,
        includeBlackMarket: Boolean,
    ): Boolean {
        if (market == null) return false
        return hasBuyCandidate(market, includeBlackMarket) || hasSellCandidate(market, playerFleet, includeBlackMarket)
    }

    private fun hasBuyCandidate(market: MarketAPI, includeBlackMarket: Boolean): Boolean {
        for (submarket in market.submarketsCopy.orEmpty()) {
            if (!StockSubmarketAccess.isTradeEligible(submarket, includeBlackMarket)) continue
            val members = submarket?.cargoNullOk?.mothballedShips?.membersListCopy ?: continue
            for (member in members) {
                if (StockReviewShipEligibility.isTradeableShip(member)) return true
            }
        }
        return false
    }

    private fun hasSellCandidate(
        market: MarketAPI,
        playerFleet: CampaignFleetAPI?,
        includeBlackMarket: Boolean,
    ): Boolean {
        if (sellTarget(market, includeBlackMarket) == null) return false
        val members = playerFleet?.fleetData?.membersListCopy.orEmpty()
        for (member in members) {
            if (StockReviewShipEligibility.isSellableShip(member)) return true
        }
        return false
    }

    private fun sellTarget(market: MarketAPI, includeBlackMarket: Boolean): SubmarketAPI? {
        val submarkets = market.submarketsCopy ?: return null
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
}
