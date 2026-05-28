package weaponsprocurement.autotrade

/**
 * Transient per-market-visit coordination shared between [AutoTradeMarketListener] (which
 * scans and notifies at planet-hail) and [AutoTradeSubmarketListener] (which executes in the
 * trade screen). Ensures the cross-market sell pass runs once per visit and buys run once per
 * submarket per visit, even though the player may open and reopen submarkets repeatedly.
 *
 * Not persisted: a visit is meaningless across saves, and a stale marker would at worst skip
 * one auto-trade pass until the market is reopened.
 */
object AutoTradeVisitState {
    private var marketId: String? = null
    private var sellsDone = false
    private val buysDone = HashSet<String>()

    @Synchronized
    fun beginVisit(id: String?) {
        if (id != marketId) reset(id)
    }

    @Synchronized
    fun endVisit() {
        reset(null)
    }

    /** Returns true exactly once per visit, for the first caller that should run the sell pass. */
    @Synchronized
    fun claimSells(): Boolean {
        if (sellsDone) return false
        sellsDone = true
        return true
    }

    /** Returns true the first time buys are claimed for a given submarket in this visit. */
    @Synchronized
    fun claimBuys(submarketId: String?): Boolean {
        if (submarketId == null) return false
        return buysDone.add(submarketId)
    }

    private fun reset(id: String?) {
        marketId = id
        sellsDone = false
        buysDone.clear()
    }
}
