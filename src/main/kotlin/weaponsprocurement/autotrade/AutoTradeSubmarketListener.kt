package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.listeners.SubmarketInteractionListener
import org.apache.log4j.Logger

/**
 * Executes auto-trades when the player actually opens a submarket in the trade screen, where
 * stock is live and vanilla-primed. Buys are scoped to the opened submarket; the cross-market
 * sell pass runs once per visit (on the first submarket opened). Dedupe is coordinated through
 * [AutoTradeVisitState] so reopening a submarket or switching tabs does not re-trade.
 */
class AutoTradeSubmarketListener : SubmarketInteractionListener {
    override fun reportPlayerOpenedSubmarket(
        submarket: SubmarketAPI?,
        type: SubmarketInteractionListener.SubmarketInteractionType?,
    ) {
        if (submarket == null) return
        val market = submarket.market ?: return
        try {
            AutoTradeVisitState.beginVisit(market.id)
            val includeSells = AutoTradeVisitState.claimSells()
            val runBuys = AutoTradeVisitState.claimBuys(submarket.specId)
            if (!includeSells && !runBuys) return

            val summary = AutoTradeEngine.executeForSubmarket(submarket, includeSells) ?: return
            Global.getSector()?.campaignUI?.addMessage(summary)
        } catch (t: Throwable) {
            LOG.warn("Auto-trade execution failed", t)
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(AutoTradeSubmarketListener::class.java)
    }
}
