package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.PlayerMarketTransaction
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.apache.log4j.Logger
import weaponsprocurement.stock.market.StockSubmarketAccess
import weaponsprocurement.stock.market.StockSubmarketTradeModes
import weaponsprocurement.trade.plan.TradeMoney
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat

/**
 * Confirms queued exact-member ship trades. The FleetMember id must still exist in the
 * source/player list at confirm time or the trade fails cleanly and leaves the plan visible.
 * Sells run before buys so one queued ship sale can fund queued purchases.
 */
class StockReviewShipExecutionController(
    private val pendingTrades: StockReviewPendingShipTrades,
    private val host: Host,
) {
    interface Host {
        fun market(): MarketAPI?
        fun playerFleet(): CampaignFleetAPI?
        fun includeBlackMarket(): Boolean
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
        val includeBlackMarket = host.includeBlackMarket()
        if (market == null || playerFleet == null) {
            host.postMessage("Ship trades require an active market and player fleet.")
            host.requestContentRebuild()
            return
        }
        val credits = playerFleet.cargo?.credits
        if (credits == null) {
            host.postMessage("Ship trades require accessible player credits.")
            host.requestContentRebuild()
            return
        }
        val orderedTrades = shipExecutionOrder(trades)
        val estimatedCost = netShipCreditCost(orderedTrades)
        if (estimatedCost > TradeMoney.MAX_EXECUTABLE_CREDITS) {
            host.postMessage("Ship order value is too large.")
            host.requestContentRebuild()
            return
        }
        if (estimatedCost > 0L && credits.get() + 0.01f < estimatedCost.toFloat()) {
            host.postMessage("Need ${StockReviewFormat.credits(estimatedCost)} for these ship trades.")
            host.requestContentRebuild()
            return
        }
        val failures = ArrayList<String>()
        val executed = ArrayList<StockReviewPendingShipTrade>()
        for (trade in orderedTrades) {
            val ok = if (trade.isBuy()) {
                executeBuy(market, playerFleet, includeBlackMarket, trade, failures)
            } else {
                executeSell(market, playerFleet, includeBlackMarket, trade, failures)
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

    private fun shipExecutionOrder(trades: List<StockReviewPendingShipTrade>): List<StockReviewPendingShipTrade> {
        val result = ArrayList<StockReviewPendingShipTrade>()
        addMatchingSide(result, trades, false)
        addMatchingSide(result, trades, true)
        return result
    }

    private fun addMatchingSide(
        result: MutableList<StockReviewPendingShipTrade>,
        trades: List<StockReviewPendingShipTrade>,
        buy: Boolean,
    ) {
        for (trade in trades) {
            if (trade.isBuy() == buy) {
                result.add(trade)
            }
        }
    }

    private fun netShipCreditCost(trades: List<StockReviewPendingShipTrade>): Long {
        var total = 0L
        for (trade in trades) {
            val value = trade.unitPrice.toLong()
            total = if (trade.isBuy()) {
                TradeMoney.safeAdd(total, value)
            } else {
                TradeMoney.safeAdd(total, -value)
            }
        }
        return total
    }

    private fun executeBuy(
        market: MarketAPI,
        playerFleet: CampaignFleetAPI,
        includeBlackMarket: Boolean,
        trade: StockReviewPendingShipTrade,
        failures: MutableList<String>,
    ): Boolean {
        val source = findTradeSubmarket(market, trade.submarketId, includeBlackMarket)
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
        return runShipMutation(trade, failures, "purchase") {
            source.cargo.mothballedShips.removeFleetMember(member)
            try {
                playerFleet.fleetData.addFleetMember(member)
            } catch (t: Throwable) {
                source.cargo.mothballedShips.addFleetMember(member)
                throw t
            }
            try {
                credits.subtract(trade.unitPrice.toFloat())
            } catch (t: Throwable) {
                playerFleet.fleetData.removeFleetMember(member)
                source.cargo.mothballedShips.addFleetMember(member)
                throw t
            }
            reportShipTransaction(market, source, member, trade.unitPrice, true)
        }
    }

    private fun executeSell(
        market: MarketAPI,
        playerFleet: CampaignFleetAPI,
        includeBlackMarket: Boolean,
        trade: StockReviewPendingShipTrade,
        failures: MutableList<String>,
    ): Boolean {
        val target = findTradeSubmarket(market, trade.submarketId, includeBlackMarket)
        val member = findMember(playerFleet.fleetData?.membersListCopy, trade.memberId)
        if (target == null || member == null) {
            failures.add("${trade.memberName} is no longer available to sell.")
            return false
        }
        if (!StockReviewShipEligibility.isSellableShip(member)) {
            failures.add("${trade.memberName} is no longer eligible to sell.")
            return false
        }
        val credits = playerFleet.cargo?.credits
        if (credits == null) {
            failures.add("credits are unavailable for ${trade.memberName}.")
            return false
        }
        return runShipMutation(trade, failures, "sale") {
            playerFleet.fleetData.removeFleetMember(member)
            try {
                target.cargo.mothballedShips.addFleetMember(member)
            } catch (t: Throwable) {
                playerFleet.fleetData.addFleetMember(member)
                throw t
            }
            try {
                credits.add(trade.unitPrice.toFloat())
            } catch (t: Throwable) {
                target.cargo.mothballedShips.removeFleetMember(member)
                playerFleet.fleetData.addFleetMember(member)
                throw t
            }
            reportShipTransaction(market, target, member, trade.unitPrice, false)
        }
    }

    private fun runShipMutation(
        trade: StockReviewPendingShipTrade,
        failures: MutableList<String>,
        operation: String,
        mutation: () -> Unit,
    ): Boolean {
        return try {
            mutation()
            true
        } catch (t: Throwable) {
            LOG.warn("WP_STOCK_REVIEW ship $operation failed for ${trade.memberId}", t)
            failures.add("could not complete ship $operation for ${trade.memberName}.")
            false
        }
    }

    private fun findTradeSubmarket(market: MarketAPI?, submarketId: String?, includeBlackMarket: Boolean): SubmarketAPI? {
        if (market == null || submarketId.isNullOrEmpty()) return null
        return market.submarketsCopy
            ?.firstOrNull { it?.specId == submarketId && StockSubmarketAccess.isTradeEligible(it, includeBlackMarket) }
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
            val transaction = PlayerMarketTransaction(market, submarket, StockSubmarketTradeModes.forSubmarket(submarket))
            val saleInfo = PlayerMarketTransaction.ShipSaleInfo(member, unitPrice.toFloat())
            if (bought) {
                transaction.shipsBought.add(saleInfo)
                transaction.creditValue = unitPrice.toFloat()
            } else {
                transaction.shipsSold.add(saleInfo)
                transaction.creditValue = -unitPrice.toFloat()
            }
            plugin.reportPlayerMarketTransaction(transaction)
        } catch (t: Throwable) {
            LOG.warn("WP_STOCK_REVIEW ship transaction report failed for ${member.id} at ${submarket.specId}", t)
        }
    }

    companion object {
        private val LOG: Logger = Global.getLogger(StockReviewShipExecutionController::class.java)
    }
}
