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
            // Two campaign-feed messages on arrival: a compact totals line, then the list of
            // items available to auto-buy. The feed is the only surface that shows on arrival and
            // wraps at a narrow width, so the totals line stays short while the buy list spells
            // out the items.
            AutoTradeNotify.postCampaign(buildFeedSummary(market, scan))
            buildBuyList(scan)?.let { AutoTradeNotify.postCampaign(it) }
        } catch (t: Throwable) {
            LOG.warn("Auto-trade scan failed", t)
        }
    }

    /** Compact totals line for the campaign feed: "Auto-trade @ Market: sell N, buy M." */
    private fun buildFeedSummary(market: MarketAPI, scan: AutoTradeEngine.ScanResult): String {
        val sellCount = scan.sellLabels.size
        val buyCount = scan.buysBySubmarket.values.sumOf { it.size }
        val parts = ArrayList<String>()
        if (sellCount > 0) parts.add("sell $sellCount")
        if (buyCount > 0) parts.add("buy $buyCount")
        return "Auto-trade @ ${market.name}: ${parts.joinToString(", ")}."
    }

    /** Distinct list of items available to auto-buy across all submarkets, or null if none. */
    private fun buildBuyList(scan: AutoTradeEngine.ScanResult): String? {
        val items = scan.buysBySubmarket.values.flatten().distinct()
        if (items.isEmpty()) return null
        return "Buy: ${items.joinToString(", ")}."
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(AutoTradeMarketListener::class.java)
    }
}
