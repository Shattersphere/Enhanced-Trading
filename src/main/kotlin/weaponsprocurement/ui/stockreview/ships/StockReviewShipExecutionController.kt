package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.PlayerMarketTransaction
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.apache.log4j.Logger

/**
 * Confirms queued exact-member ship trades. The FleetMember id must still exist in the
 * source/player list at confirm time or the trade fails cleanly and leaves the plan visible.
 */
class StockReviewShipExecutionController(
    private val pendingTrades: StockReviewPendingShipTrades,
    private val host: Host,
) {
    interface Host {
        fun market(): MarketAPI?
        fun playerFleet(): CampaignFleetAPI?
        fun rebuildSnapshot()
        fun requestContentRebuild()
        fun refreshVanillaCargoScreen()
        fun postMessage(message: String?)
        fun exitReviewMode()
        fun reopen(review: Boolean)
    }

    fun confirmPendingShipTrades() {
        val trades = pendingTrades.asList()
        if (trades.isEmpty()) {
            host.exitReviewMode()
            host.reopen(false)
            return
        }
        val market = host.market()
        val playerFleet = host.playerFleet()
        if (market == null || playerFleet == null) {
            host.postMessage("Ship trades require an active market and player fleet.")
            host.requestContentRebuild()
            return
        }
        val failures = ArrayList<String>()
        val executed = ArrayList<StockReviewPendingShipTrade>()
        for (trade in trades) {
            val ok = if (trade.isBuy()) {
                executeBuy(market, playerFleet, trade, failures)
            } else {
                executeSell(market, playerFleet, trade, failures)
            }
            if (ok) {
                executed.add(trade)
            }
        }
        for (trade in executed) {
            pendingTrades.reset(trade.recordKey)
        }
        host.refreshVanillaCargoScreen()
        host.rebuildSnapshot()
        if (failures.isEmpty()) {
            host.postMessage("Ship trades completed.")
            host.exitReviewMode()
            host.reopen(false)
            return
        }
        host.postMessage("Some ship trades could not be completed: ${failures.first()}")
        host.requestContentRebuild()
    }

    private fun executeBuy(
        market: MarketAPI,
        playerFleet: CampaignFleetAPI,
        trade: StockReviewPendingShipTrade,
        failures: MutableList<String>,
    ): Boolean {
        val source = findSubmarket(market, trade.submarketId)
        val member = findMember(source?.cargoNullOk?.mothballedShips?.membersListCopy, trade.memberId)
        if (source == null || member == null) {
            failures.add("${trade.memberName} is no longer for sale.")
            return false
        }
        val credits = playerFleet.cargo?.credits
        if (credits == null || credits.get() < trade.unitPrice) {
            failures.add("not enough credits for ${trade.memberName}.")
            return false
        }
        source.cargo.mothballedShips.removeFleetMember(member)
        playerFleet.fleetData.addFleetMember(member)
        credits.subtract(trade.unitPrice.toFloat())
        reportShipTransaction(market, source, member, trade.unitPrice, true)
        return true
    }

    private fun executeSell(
        market: MarketAPI,
        playerFleet: CampaignFleetAPI,
        trade: StockReviewPendingShipTrade,
        failures: MutableList<String>,
    ): Boolean {
        val target = findSubmarket(market, trade.submarketId)
        val member = findMember(playerFleet.fleetData?.membersListCopy, trade.memberId)
        if (target == null || member == null) {
            failures.add("${trade.memberName} is no longer available to sell.")
            return false
        }
        playerFleet.fleetData.removeFleetMember(member)
        target.cargo.mothballedShips.addFleetMember(member)
        playerFleet.cargo.credits.add(trade.unitPrice.toFloat())
        reportShipTransaction(market, target, member, trade.unitPrice, false)
        return true
    }

    private fun findSubmarket(market: MarketAPI?, submarketId: String?): SubmarketAPI? {
        if (market == null || submarketId.isNullOrEmpty()) return null
        return market.submarketsCopy?.firstOrNull { it?.specId == submarketId }
    }

    private fun findMember(members: List<FleetMemberAPI>?, memberId: String): FleetMemberAPI? =
        members?.firstOrNull { it?.id == memberId }

    private fun reportShipTransaction(
        market: MarketAPI,
        submarket: SubmarketAPI,
        member: FleetMemberAPI,
        unitPrice: Int,
        bought: Boolean,
    ) {
        val plugin = submarket.plugin ?: return
        try {
            val transaction = PlayerMarketTransaction(market, submarket, tradeMode(submarket))
            val saleInfo = PlayerMarketTransaction.ShipSaleInfo(member, unitPrice.toFloat())
            if (bought) {
                transaction.shipsBought.add(saleInfo)
                transaction.creditValue = unitPrice.toFloat()
            } else {
                transaction.shipsSold.add(saleInfo)
                transaction.creditValue = -unitPrice.toFloat()
            }
            plugin.reportPlayerMarketTransaction(transaction)
        } catch (t: RuntimeException) {
            LOG.warn("WP_STOCK_REVIEW ship transaction report failed for ${member.id} at ${submarket.specId}", t)
        }
    }

    private fun tradeMode(submarket: SubmarketAPI): CampaignUIAPI.CoreUITradeMode =
        if (submarket.plugin?.isBlackMarket == true) CampaignUIAPI.CoreUITradeMode.SNEAK else CampaignUIAPI.CoreUITradeMode.OPEN

    companion object {
        private val LOG: Logger = Global.getLogger(StockReviewShipExecutionController::class.java)
    }
}
