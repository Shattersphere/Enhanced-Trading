package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import org.apache.log4j.Logger
import weaponsprocurement.stock.item.StockItemCargo
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.market.StockSubmarketAccess
import weaponsprocurement.trade.execution.StockPurchaseMarketSources
import weaponsprocurement.trade.execution.StockPurchaseService

/**
 * Auto-trade engine for the notify-then-execute flow.
 *
 * [scan] is read-only and produces the planet-hail preview (sell summary plus which
 * submarkets currently have executable buys). [executeForSubmarket] runs in the real trade
 * screen against the submarket the player actually opened, so buys read live, vanilla-primed
 * stock and no off-UI accessibility probing is needed.
 *
 * Black-market routing follows transponder state and the per-item suspicion toggles:
 * when the transponder is off the player is anonymous (black market is the only market and
 * is always used); when it is on, the black market is only used if the relevant
 * `allowSuspicionWhen*` flag is set, since those transactions are attributable to the player.
 */
object AutoTradeEngine {
    private val LOG: Logger = Logger.getLogger(AutoTradeEngine::class.java)
    private val PURCHASE_SERVICE = StockPurchaseService()

    /** Read-only preview of what a market visit would do. */
    class ScanResult(
        @JvmField val sellCount: Int,
        @JvmField val buySubmarketNames: List<String>,
    ) {
        fun isEmpty(): Boolean = sellCount == 0 && buySubmarketNames.isEmpty()
    }

    @JvmStatic
    fun scan(market: MarketAPI?): ScanResult? {
        if (market == null) return null
        val cfg = AutoTradeRegistry.get()
        if (!cfg.enabled) return null
        val sector = Global.getSector() ?: return null
        val playerCargo = sector.playerFleet?.cargo ?: return null
        val transponderOff = sector.playerFleet?.isTransponderOn == false

        var sellCount = 0
        for ((itemId, rule) in snapshot(cfg.weapons)) sellCount += sellableQty(StockItemType.WEAPON, itemId, rule, playerCargo)
        for ((itemId, rule) in snapshot(cfg.fighters)) sellCount += sellableQty(StockItemType.WING, itemId, rule, playerCargo)

        val buyNames = ArrayList<String>()
        for (submarket in market.submarketsCopy ?: emptyList()) {
            if (submarket == null) continue
            if (!AutoTradeSubmarketAccess.isAccessible(submarket)) continue
            if (!buysAllowedAt(submarket, cfg, transponderOff)) continue
            if (submarketHasBuys(submarket, cfg, playerCargo)) buyNames.add(submarket.name ?: submarket.specId)
        }

        val result = ScanResult(sellCount, buyNames)
        return if (result.isEmpty()) null else result
    }

    /**
     * Execute auto-trades for the submarket the player just opened. Buys are scoped to this
     * submarket (live stock). When [includeSells] is true (the first cargo submarket opened
     * in a visit) the cross-market sell pass also runs. Returns a campaign-message summary,
     * or null if nothing happened.
     */
    @JvmStatic
    fun executeForSubmarket(submarket: SubmarketAPI?, includeSells: Boolean): String? {
        if (submarket == null) return null
        val market = submarket.market ?: return null
        val cfg = AutoTradeRegistry.get()
        if (!cfg.enabled) return null
        val sector = Global.getSector() ?: return null
        val playerCargo = sector.playerFleet?.cargo ?: return null
        val transponderOff = sector.playerFleet?.isTransponderOn == false

        // Sells are tracked per venue (open vs black) so the summary can name where each
        // sale landed - the sell pass routes cross-market and prefers the black market.
        val soldQtyByVenue = LinkedHashMap<String, Int>()
        val soldCreditsByVenue = LinkedHashMap<String, Long>()
        var totalBought = 0
        var creditsSpent = 0L
        var hullmodBought = 0
        var hullmodSpent = 0L
        val errors = ArrayList<String>()

        // Sells: cross-market pass, routed by suspicion-when-selling + transponder state.
        if (includeSells) {
            val includeBlackForSells = transponderOff || cfg.allowSuspicionWhenSelling
            val onSold: (String, Int, Long) -> Unit = { venue, qty, credits ->
                soldQtyByVenue[venue] = (soldQtyByVenue[venue] ?: 0) + qty
                soldCreditsByVenue[venue] = (soldCreditsByVenue[venue] ?: 0L) + credits
            }
            for ((itemId, rule) in snapshot(cfg.weapons)) {
                applySellRule(StockItemType.WEAPON, itemId, rule, market, includeBlackForSells, playerCargo, onSold) { errors.add(it) }
            }
            for ((itemId, rule) in snapshot(cfg.fighters)) {
                applySellRule(StockItemType.WING, itemId, rule, market, includeBlackForSells, playerCargo, onSold) { errors.add(it) }
            }
        }

        // Buys: scoped to the opened submarket only, if buying here is allowed.
        if (buysAllowedAt(submarket, cfg, transponderOff)) {
            val submarketId = submarket.specId
            val isBlack = submarket.plugin?.isBlackMarket == true
            val sources = listOf(submarketId)
            for ((itemId, rule) in snapshot(cfg.weapons)) {
                applyBuyRule(StockItemType.WEAPON, itemId, rule, market, sources, isBlack, playerCargo, cfg.creditFloor,
                    onBought = { qty, credits -> totalBought += qty; creditsSpent += credits },
                    onError = { errors.add(it) })
            }
            for ((itemId, rule) in snapshot(cfg.fighters)) {
                applyBuyRule(StockItemType.WING, itemId, rule, market, sources, isBlack, playerCargo, cfg.creditFloor,
                    onBought = { qty, credits -> totalBought += qty; creditsSpent += credits },
                    onError = { errors.add(it) })
            }
            if (cfg.buyUnknownHullmods) {
                val result = AutoTradeHullmodBuyer.run(submarket, cfg, playerCargo)
                hullmodBought = result.bought
                hullmodSpent = result.spent
                errors.addAll(result.errors)
            }
        }

        val totalSold = soldQtyByVenue.values.sum()
        if (totalSold == 0 && totalBought == 0 && hullmodBought == 0 && errors.isEmpty()) return null

        val sb = StringBuilder()
        sb.append("Auto-trade @ ").append(market.name).append(": ")
        val parts = ArrayList<String>()
        for ((venue, qty) in soldQtyByVenue) {
            if (qty > 0) parts.add("sold $qty on $venue (+${formatCredits(soldCreditsByVenue[venue] ?: 0L)})")
        }
        if (totalBought > 0) parts.add("bought $totalBought (-${formatCredits(creditsSpent)})")
        if (hullmodBought > 0) parts.add("hullmods $hullmodBought (-${formatCredits(hullmodSpent)})")
        if (parts.isEmpty()) parts.add("no trades")
        sb.append(parts.joinToString(", "))
        if (errors.isNotEmpty()) sb.append(" [errors: ").append(errors.size).append("]")
        return sb.toString()
    }

    /**
     * Whether auto-buys at this submarket are permitted. The player opening a submarket already
     * proves it is reachable, so any real (non-storage, credits-based) trade market qualifies -
     * including military and modded markets. The black market is the one special case: it is
     * only used while the transponder is on if the player opted into incurring suspicion.
     */
    private fun buysAllowedAt(submarket: SubmarketAPI, cfg: AutoTradeConfig, transponderOff: Boolean): Boolean {
        val plugin = submarket.plugin ?: return false
        if (StockSubmarketAccess.isNonTradeSubmarket(submarket.specId)) return false
        if (plugin.isFreeTransfer) return false
        if (plugin.isBlackMarket) return transponderOff || cfg.allowSuspicionWhenBuying
        return true
    }

    private fun sellableQty(itemType: StockItemType, itemId: String, rule: AutoTradeItemRule, playerCargo: CargoAPI): Int {
        if (rule.sellAbove < 0) return 0
        val owned = StockItemCargo.itemCount(playerCargo, itemType, itemId)
        return if (owned > rule.sellAbove) owned - rule.sellAbove else 0
    }

    private fun submarketHasBuys(submarket: SubmarketAPI, cfg: AutoTradeConfig, playerCargo: CargoAPI): Boolean {
        AutoTradeSubmarketAccess.prime(submarket)
        val cargo = submarket.cargoNullOk ?: return false
        for ((itemId, rule) in snapshot(cfg.weapons)) {
            if (hasBuyStock(StockItemType.WEAPON, itemId, rule, cargo, playerCargo)) return true
        }
        for ((itemId, rule) in snapshot(cfg.fighters)) {
            if (hasBuyStock(StockItemType.WING, itemId, rule, cargo, playerCargo)) return true
        }
        if (cfg.buyUnknownHullmods && AutoTradeHullmodBuyer.hasBuyableHullmod(submarket, cfg)) return true
        return false
    }

    private fun hasBuyStock(
        itemType: StockItemType,
        itemId: String,
        rule: AutoTradeItemRule,
        submarketCargo: CargoAPI,
        playerCargo: CargoAPI,
    ): Boolean {
        if (rule.buyBelow <= 0) return false
        val owned = StockItemCargo.itemCount(playerCargo, itemType, itemId)
        if (owned >= rule.buyBelow) return false
        val stack = StockItemCargo.itemStack(submarketCargo, itemType, itemId) ?: return false
        return Math.round(stack.size) > 0
    }

    private fun applySellRule(
        itemType: StockItemType,
        itemId: String,
        rule: AutoTradeItemRule,
        market: MarketAPI,
        includeBlack: Boolean,
        playerCargo: CargoAPI,
        onSold: (String, Int, Long) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (rule.sellAbove < 0) return
        val owned = StockItemCargo.itemCount(playerCargo, itemType, itemId)
        if (owned <= rule.sellAbove) return
        val qty = owned - rule.sellAbove
        try {
            // Determine the venue the service will route to (same sellTarget call it uses
            // internally) so the summary can name it.
            val playerStack = StockItemCargo.itemStack(playerCargo, itemType, itemId)
            val venue = StockPurchaseMarketSources.sellTarget(market, playerStack, includeBlack)?.submarket?.name ?: "market"
            val result = PURCHASE_SERVICE.sellItemToMarket(
                Global.getSector(), market, itemType, itemId, qty, includeBlack)
            if (result.isSuccess()) {
                onSold(venue, result.quantity, -result.credits)
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
