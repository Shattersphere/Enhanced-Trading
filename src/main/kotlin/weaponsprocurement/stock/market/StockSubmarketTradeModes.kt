package weaponsprocurement.stock.market

import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.ui.HintPanelAPI

object StockSubmarketTradeModes {
    @JvmStatic
    fun forSubmarket(submarket: SubmarketAPI?): CampaignUIAPI.CoreUITradeMode {
        return if (isBlackMarket(submarket)) {
            CampaignUIAPI.CoreUITradeMode.SNEAK
        } else {
            CampaignUIAPI.CoreUITradeMode.OPEN
        }
    }

    @JvmStatic
    fun isBlackMarket(submarket: SubmarketAPI?): Boolean {
        if (submarket == null) return false
        if (Submarkets.SUBMARKET_BLACK == submarket.specId) return true
        val plugin = submarket.plugin ?: return false
        return try {
            plugin.isBlackMarket
        } catch (_: RuntimeException) {
            false
        }
    }

    @JvmStatic
    fun coreUiFor(submarket: SubmarketAPI?): CoreUIAPI = TradeModeCoreUi(forSubmarket(submarket))

    private class TradeModeCoreUi(
        private val tradeMode: CampaignUIAPI.CoreUITradeMode,
    ) : CoreUIAPI {
        override fun getTradeMode(): CampaignUIAPI.CoreUITradeMode = tradeMode

        override fun getHintPanel(): HintPanelAPI? = null
    }
}
