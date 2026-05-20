package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Submarkets

/**
 * Submarket accessibility helpers ported from AutoTrader. We delegate to the vanilla
 * submarket plugin's `isEnabled(null)` check whenever it tolerates a null UI; if it
 * insists on a real CoreUIAPI we fall back to a conservative manual heuristic.
 */
object AutoTradeSubmarketAccess {
    @JvmStatic
    fun isAccessible(market: MarketAPI?, submarketId: String): Boolean {
        if (market == null) return false
        val submarket = market.getSubmarket(submarketId) ?: return false
        val plugin = submarket.plugin
        if (plugin != null) {
            try {
                return plugin.isEnabled(null)
            } catch (_: Throwable) {
                // Plugin needed a real CoreUIAPI - fall through to the manual heuristic.
            }
        }
        return manualFallback(market, submarketId)
    }

    @JvmStatic
    fun prime(submarket: SubmarketAPI?) {
        val plugin = submarket?.plugin ?: return
        try {
            plugin.updateCargoPrePlayerInteraction()
        } catch (_: Throwable) {
            // Some submarkets refuse off-screen updates; skip silently.
        }
    }

    private fun manualFallback(market: MarketAPI, submarketId: String): Boolean {
        if (market.isPlayerOwned) return true
        val freePort = market.isFreePort
        val hostile = market.faction != null && market.faction.isHostileTo(Factions.PLAYER)
        if (hostile && !freePort) return false
        if (Submarkets.SUBMARKET_BLACK == submarketId) return true
        if (freePort) return true
        val playerFleet = Global.getSector()?.playerFleet
        return playerFleet != null && playerFleet.isTransponderOn
    }
}
