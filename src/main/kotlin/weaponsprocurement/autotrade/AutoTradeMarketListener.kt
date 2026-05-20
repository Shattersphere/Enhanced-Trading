package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import org.apache.log4j.Logger

/**
 * Triggers a single [AutoTradeEngine.run] pass when the player opens a market dialog.
 * Dedupes via a transient marker so reopening the same market in one interaction does
 * not loop. The actual cargo mutation happens through [AutoTradeEngine] / its helpers.
 */
class AutoTradeMarketListener : ColonyInteractionListener {
    @Transient private var lastHandledMarketId: String? = null

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        runOnce(market)
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        runOnce(market)
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        lastHandledMarketId = null
    }

    override fun reportPlayerMarketTransaction(transaction: com.fs.starfarer.api.campaign.PlayerMarketTransaction?) {
        // no-op
    }

    private fun runOnce(market: MarketAPI?) {
        if (market == null) return
        if (market.id != null && market.id == lastHandledMarketId) return
        lastHandledMarketId = market.id
        try {
            // Prime open and black submarkets so their cargo is current.
            val open = market.getSubmarket(Submarkets.SUBMARKET_OPEN)
            val black = market.getSubmarket(Submarkets.SUBMARKET_BLACK)
            AutoTradeSubmarketAccess.prime(open)
            AutoTradeSubmarketAccess.prime(black)

            val playerCargo = Global.getSector()?.playerFleet?.cargo
            AutoTradeRegistry.get().markSeenFromCargo(playerCargo)

            val summary = AutoTradeEngine.run(market) ?: return
            Global.getSector()?.campaignUI?.addMessage(summary)
        } catch (t: Throwable) {
            LOG.warn("Auto-trade listener pass failed", t)
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(AutoTradeMarketListener::class.java)
    }
}
