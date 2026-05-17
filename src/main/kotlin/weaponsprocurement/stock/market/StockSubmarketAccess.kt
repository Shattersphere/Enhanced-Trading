package weaponsprocurement.stock.market

import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.ui.HintPanelAPI

object StockSubmarketAccess {
    @JvmStatic
    fun isTradeEligible(submarket: SubmarketAPI?, includeBlackMarket: Boolean): Boolean {
        if (submarket == null) return false
        val id = submarket.specId
        if (isNonTradeSubmarket(id)) return false
        if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK == id) return false
        if (submarket.cargoNullOk == null) return false

        val plugin = submarket.plugin ?: return true
        return try {
            !plugin.isHidden && plugin.isEnabled(OpenMarketCoreUi)
        } catch (_: RuntimeException) {
            false
        }
    }

    @JvmStatic
    fun isNonTradeSubmarket(submarketId: String?): Boolean =
        Submarkets.SUBMARKET_STORAGE == submarketId || Submarkets.LOCAL_RESOURCES == submarketId

    private object OpenMarketCoreUi : CoreUIAPI {
        override fun getTradeMode(): CampaignUIAPI.CoreUITradeMode = CampaignUIAPI.CoreUITradeMode.OPEN

        override fun getHintPanel(): HintPanelAPI? = null
    }
}
