package weaponsprocurement.stock.market

import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.ui.HintPanelAPI

object StockSubmarketTradeModes {
    @JvmStatic
    fun forSubmarket(submarket: SubmarketAPI?): CampaignUIAPI.CoreUITradeMode {
        return if (submarket?.plugin?.isBlackMarket == true) {
            CampaignUIAPI.CoreUITradeMode.SNEAK
        } else {
            CampaignUIAPI.CoreUITradeMode.OPEN
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
