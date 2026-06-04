package weaponsprocurement.autotrade

import com.fs.starfarer.api.campaign.econ.SubmarketAPI

/**
 * Transient per-market-visit coordination shared between [AutoTradeMarketListener] (which
 * scans and notifies at planet-hail), [AutoTradeSubmarketListener] (which records or executes
 * on submarket open) and [AutoTradeExecuteHotkeyScript] (which executes on the hotkey in
 * confirm mode). Ensures the cross-market sell pass runs once per visit and buys run once per
 * submarket per visit, even though the player may open and reopen submarkets repeatedly.
 *
 * Not persisted: a visit is meaningless across saves, and a stale marker would at worst skip
 * one auto-trade pass until the market is reopened.
 */
object AutoTradeVisitState {
    private var marketId: String? = null
    private var sellsDone = false
    private val buysDone = HashSet<String>()
    private val prompted = HashSet<String>()
    @Transient private var openSubmarket: SubmarketAPI? = null

    @Synchronized
    fun beginVisit(id: String?) {
        if (id != marketId) reset(id)
    }

    @Synchronized
    fun endVisit() {
        reset(null)
    }

    /** Records the submarket the player currently has open in the trade screen (confirm mode). */
    @Synchronized
    fun setOpenSubmarket(submarket: SubmarketAPI?) {
        openSubmarket = submarket
    }

    @Synchronized
    fun openSubmarket(): SubmarketAPI? = openSubmarket

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

    /** Returns true the first time a confirm-mode prompt is shown for a submarket this visit. */
    @Synchronized
    fun claimPrompt(submarketId: String?): Boolean {
        if (submarketId == null) return false
        return prompted.add(submarketId)
    }

    /** Non-mutating: whether the sell pass has not yet run this visit. */
    @Synchronized
    fun sellsPending(): Boolean = !sellsDone

    /** Non-mutating: whether buys for this submarket have not yet run this visit. */
    @Synchronized
    fun buysPending(submarketId: String?): Boolean = submarketId != null && !buysDone.contains(submarketId)

    private fun reset(id: String?) {
        marketId = id
        sellsDone = false
        buysDone.clear()
        prompted.clear()
        openSubmarket = null
    }
}
