package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import org.apache.log4j.Logger
import weaponsprocurement.stock.item.StockItemCargo
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.trade.execution.StockPurchaseService
import weaponsprocurement.trade.plan.TradeMoney

/**
 * Single-market auto-trade pass. Routes weapon / fighter LPC sells and buys through
 * [StockPurchaseService] (rollback-aware), and performs a narrow hullmod auto-buy via
 * [AutoTradeHullmodBuyer]. Returns a human-readable summary suitable for a campaign
 * message.
 */
object AutoTradeEngine {
    private val LOG: Logger = Logger.getLogger(AutoTradeEngine::class.java)
    private val PURCHASE_SERVICE = StockPurchaseService()

    @JvmStatic
    fun run(market: MarketAPI?): String? {
        if (market == null) return null
        val cfg = AutoTradeRegistry.get()
        if (!cfg.enabled) return null
        val sector = Global.getSector() ?: return null
        val playerCargo = sector.playerFleet?.cargo ?: return null

        val openOk = AutoTradeSubmarketAccess.isAccessible(market, Submarkets.SUBMARKET_OPEN)
        val blackOk = AutoTradeSubmarketAccess.isAccessible(market, Submarkets.SUBMARKET_BLACK)
        if (!openOk && !blackOk) return null

        val transponderOff = sector.playerFleet?.isTransponderOn == false
        // The "through black market" toggles enable black-market routing in addition to (not
        // instead of) the open market. The black market is preferred when both are usable.
        // Black-market routing is forced whenever the transponder is off, since there is no
        // reason to limit ourselves to the open market in that state.
        val includeBlackForSells = (cfg.sellThroughBlack || transponderOff) && blackOk
        val includeBlackForBuys = (cfg.buyThroughBlack || transponderOff) && blackOk

        // Buy sources, in preference order. Black first (when applicable), then open. Falls
        // back to whichever submarket is actually accessible if the preferred ordering is
        // empty.
        val buySources = LinkedHashSet<String>()
        if (includeBlackForBuys) buySources.add(Submarkets.SUBMARKET_BLACK)
        if (openOk) buySources.add(Submarkets.SUBMARKET_OPEN)
        if (buySources.isEmpty() && blackOk) buySources.add(Submarkets.SUBMARKET_BLACK)
        if (buySources.isEmpty()) return null

        var totalSold = 0
        var totalBought = 0
        var creditsGained = 0L
        var creditsSpent = 0L
        val errors = ArrayList<String>()

        // Sells: weapons then fighters. StockPurchaseService.sellItemToMarket() picks the best
        // buyer between open and black when includeBlackMarket is true, falling back to legal
        // if the black market refuses the item.
        for ((itemId, rule) in snapshot(cfg.weapons)) {
            applySellRule(StockItemType.WEAPON, itemId, rule, market, includeBlackForSells, playerCargo,
                onSold = { qty, credits -> totalSold += qty; creditsGained += credits },
                onError = { errors.add(it) })
        }
        for ((itemId, rule) in snapshot(cfg.fighters)) {
            applySellRule(StockItemType.WING, itemId, rule, market, includeBlackForSells, playerCargo,
                onSold = { qty, credits -> totalSold += qty; creditsGained += credits },
                onError = { errors.add(it) })
        }

        // Buys: weapons then fighters. Iterate each buy source until the rule's target qty is
        // reached or both submarkets are exhausted.
        for ((itemId, rule) in snapshot(cfg.weapons)) {
            applyBuyRule(StockItemType.WEAPON, itemId, rule, market, buySources, includeBlackForBuys,
                playerCargo, cfg.creditFloor,
                onBought = { qty, credits -> totalBought += qty; creditsSpent += credits },
                onError = { errors.add(it) })
        }
        for ((itemId, rule) in snapshot(cfg.fighters)) {
            applyBuyRule(StockItemType.WING, itemId, rule, market, buySources, includeBlackForBuys,
                playerCargo, cfg.creditFloor,
                onBought = { qty, credits -> totalBought += qty; creditsSpent += credits },
                onError = { errors.add(it) })
        }

        // Hullmods. Primary scan = open if accessible (else black). Optional secondary scan of
        // the black market when the user opted in, or when the transponder is off.
        var hullmodBought = 0
        var hullmodSpent = 0L
        if (cfg.buyUnknownHullmods) {
            val primarySubmarketId = buySources.first()
            val alsoScanBlack = blackOk && primarySubmarketId != Submarkets.SUBMARKET_BLACK &&
                (cfg.buyHullmodsFromBlack || transponderOff)
            val result = AutoTradeHullmodBuyer.run(
                market = market,
                primarySubmarketId = primarySubmarketId,
                alsoScanBlack = alsoScanBlack,
                cfg = cfg,
                playerCargo = playerCargo,
            )
            hullmodBought = result.bought
            hullmodSpent = result.spent
            errors.addAll(result.errors)
        }

        if (totalSold == 0 && totalBought == 0 && hullmodBought == 0 && errors.isEmpty()) return null

        val sb = StringBuilder()
        sb.append("Auto-trade @ ").append(market.name).append(": ")
        val parts = ArrayList<String>()
        if (totalSold > 0) parts.add("sold $totalSold (+${formatCredits(creditsGained)})")
        if (totalBought > 0) parts.add("bought $totalBought (-${formatCredits(creditsSpent)})")
        if (hullmodBought > 0) parts.add("hullmods $hullmodBought (-${formatCredits(hullmodSpent)})")
        if (parts.isEmpty()) parts.add("no trades")
        sb.append(parts.joinToString(", "))
        if (errors.isNotEmpty()) sb.append(" [errors: ").append(errors.size).append("]")
        return sb.toString()
    }

    private fun applySellRule(
        itemType: StockItemType,
        itemId: String,
        rule: AutoTradeItemRule,
        market: MarketAPI,
        sellThroughBlack: Boolean,
        playerCargo: CargoAPI,
        onSold: (Int, Long) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (rule.sellAbove < 0) return
        val owned = StockItemCargo.itemCount(playerCargo, itemType, itemId)
        if (owned <= rule.sellAbove) return
        val qty = owned - rule.sellAbove
        try {
            val result = PURCHASE_SERVICE.sellItemToMarket(
                Global.getSector(), market, itemType, itemId, qty, sellThroughBlack)
            if (result.isSuccess()) {
                onSold(result.quantity, -result.credits)
            } else if (result.message != null) {
                // Suppress noisy "no buyer" failures when nothing happens - only report
                // unusual conditions.
                if (!result.message.contains("buyer", ignoreCase = true)) {
                    onError(result.message)
                }
            }
        } catch (t: Throwable) {
            LOG.warn("Auto-trade sell failed for $itemId", t)
            onError("sell $itemId: ${t.javaClass.simpleName}")
        }
    }

    private fun applyBuyRule(
        itemType: StockItemType,
        itemId: String,
        rule: AutoTradeItemRule,
        market: MarketAPI,
        submarketIds: Collection<String>,
        includeBlackMarket: Boolean,
        playerCargo: CargoAPI,
        creditFloor: Int,
        onBought: (Int, Long) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (rule.buyBelow <= 0) return
        for (submarketId in submarketIds) {
            val owned = StockItemCargo.itemCount(playerCargo, itemType, itemId)
            if (owned >= rule.buyBelow) return
            val want = rule.buyBelow - owned
            val submarket = market.getSubmarket(submarketId) ?: continue
            val cargo = submarket.cargoNullOk ?: continue
            val stack = StockItemCargo.itemStack(cargo, itemType, itemId) ?: continue
            val available = Math.round(stack.size)
            if (available <= 0) continue
            val unitPrice = StockItemStacks.unitPrice(submarket, stack)
            if (unitPrice <= 0) continue
            val credits = playerCargo.credits.get().toLong()
            val spendable = credits - creditFloor
            if (spendable < unitPrice) return
            val affordable = (spendable / unitPrice).toInt()
            val qty = minOf(want, available, affordable)
            if (qty <= 0) continue
            try {
                val result = PURCHASE_SERVICE.buyItemFromLocalSource(
                    Global.getSector(), market, itemType, itemId, submarketId, qty, includeBlackMarket)
                if (result.isSuccess()) {
                    onBought(result.quantity, result.credits)
                } else if (result.message != null) {
                    if (!result.message.contains("space", ignoreCase = true) &&
                        !result.message.contains("credits", ignoreCase = true) &&
                        !result.message.contains("available", ignoreCase = true)) {
                        onError(result.message)
                    }
                }
            } catch (t: Throwable) {
                LOG.warn("Auto-trade buy failed for $itemId from $submarketId", t)
                onError("buy $itemId: ${t.javaClass.simpleName}")
            }
        }
    }

    private fun snapshot(map: Map<String, AutoTradeItemRule>): List<Pair<String, AutoTradeItemRule>> {
        val out = ArrayList<Pair<String, AutoTradeItemRule>>(map.size)
        for ((k, v) in map) {
            if (k.isNullOrBlank() || v == null) continue
            if (v.isEmpty()) continue
            out.add(k to v)
        }
        return out
    }

    private fun formatCredits(amount: Long): String {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount) + " cr"
    }
}
