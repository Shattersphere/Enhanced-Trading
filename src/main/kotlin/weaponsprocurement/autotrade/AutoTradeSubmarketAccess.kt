package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.ui.HintPanelAPI
import weaponsprocurement.stock.market.StockSubmarketAccess

/**
 * Generic submarket accessibility for the auto-trade hail scan.
 *
 * Rather than hard-coding open/black rules, this delegates to each submarket plugin's own
 * `isEnabled(CoreUIAPI)` - the same decision the game UI uses - so it works for military
 * markets (relationship-gated), storage, and arbitrary modded submarkets. The plugin's only
 * `CoreUIAPI` dependency is the trade mode, which we derive from transponder state: a fleet
 * running with its transponder off is in SNEAK mode (open/military markets disabled, only the
 * black market reachable), otherwise OPEN.
 */
object AutoTradeSubmarketAccess {
    @JvmStatic
    fun isAccessible(submarket: SubmarketAPI?): Boolean {
        if (submarket == null) return false
        if (StockSubmarketAccess.isNonTradeSubmarket(submarket.specId)) return false
        val plugin = submarket.plugin ?: return false
        if (plugin.isHidden) return false
        val transponderOn = Global.getSector()?.playerFleet?.isTransponderOn == true
        val mode = if (transponderOn) CoreUITradeMode.OPEN else CoreUITradeMode.SNEAK
        return try {
            plugin.isEnabled(StubCoreUi(mode))
        } catch (_: RuntimeException) {
            false
        }
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

    private class StubCoreUi(private val mode: CoreUITradeMode) : CoreUIAPI {
        override fun getTradeMode(): CoreUITradeMode = mode
        override fun getHintPanel(): HintPanelAPI? = null
    }
}
