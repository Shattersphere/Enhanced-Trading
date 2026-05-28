package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener
import org.apache.log4j.Logger

/**
 * Scans for pending auto-trades when the player hails a market and shows an informational
 * message naming where trades will apply. It does not mutate cargo: execution happens in the
 * real trade screen via [AutoTradeSubmarketListener]. Begins/ends the shared
 * [AutoTradeVisitState] so per-visit dedupe is correct.
 */
class AutoTradeMarketListener : ColonyInteractionListener {
    @Transient private var lastNotifiedMarketId: String? = null

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        onMarketOpened(market)
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        onMarketOpened(market)
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        lastNotifiedMarketId = null
        AutoTradeVisitState.endVisit()
    }

    override fun reportPlayerMarketTransaction(transaction: com.fs.starfarer.api.campaign.PlayerMarketTransaction?) {
        // no-op
    }

    private fun onMarketOpened(market: MarketAPI?) {
        if (market == null) return
        AutoTradeVisitState.beginVisit(market.id)
        if (market.id != null && market.id == lastNotifiedMarketId) return
        lastNotifiedMarketId = market.id
        try {
            val playerCargo = Global.getSector()?.playerFleet?.cargo
            AutoTradeRegistry.get().markSeenFromCargo(playerCargo)

            val scan = AutoTradeEngine.scan(market) ?: return
            Global.getSector()?.campaignUI?.addMessage(buildMessage(market, scan))
        } catch (t: Throwable) {
            LOG.warn("Auto-trade scan failed", t)
        }
    }

    private fun buildMessage(market: MarketAPI, scan: AutoTradeEngine.ScanResult): String {
        val parts = ArrayList<String>()
        if (scan.sellCount > 0) parts.add("${scan.sellCount} to sell")
        if (scan.buySubmarketNames.isNotEmpty()) parts.add("buys at ${scan.buySubmarketNames.joinToString(", ")}")
        return "Auto-trade pending @ ${market.name}: ${parts.joinToString("; ")}. Open a market to apply."
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(AutoTradeMarketListener::class.java)
    }
}
