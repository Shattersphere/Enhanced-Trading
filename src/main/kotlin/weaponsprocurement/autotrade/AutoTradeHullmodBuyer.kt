package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.loading.HullModSpecAPI
import org.apache.log4j.Logger

/**
 * Narrow hullmod auto-buy helper. Walks modspec stacks on the configured submarket(s),
 * buys one copy of any unknown / non-blacklisted hullmod, and either adds the modspec to
 * player cargo or consumes-and-learns it (mirroring a manual right-click on the modspec).
 *
 * This does NOT use [weaponsprocurement.trade.execution.StockPurchaseExecutor] because that
 * pipeline is item-cargo (weapons / fighter LPC) shaped. The mutation here is two steps -
 * subtract the stack and subtract credits - and we restore the stack if the credit step
 * throws. No transaction reports are emitted (vanilla modspec right-click does not emit
 * them either).
 */
object AutoTradeHullmodBuyer {
    private val LOG: Logger = Logger.getLogger(AutoTradeHullmodBuyer::class.java)
    private const val MODSPEC = "modspec"

    data class Result(
        @JvmField val bought: Int,
        @JvmField val spent: Long,
        @JvmField val errors: List<String>,
    )

    @JvmStatic
    fun run(
        market: MarketAPI,
        primarySubmarketId: String,
        alsoScanBlack: Boolean,
        cfg: AutoTradeConfig,
        playerCargo: CargoAPI,
    ): Result {
        var bought = 0
        var spent = 0L
        val errors = ArrayList<String>()

        val submarketIds = LinkedHashSet<String>()
        submarketIds.add(primarySubmarketId)
        if (alsoScanBlack) submarketIds.add(com.fs.starfarer.api.impl.campaign.ids.Submarkets.SUBMARKET_BLACK)

        for (submarketId in submarketIds) {
            val submarket = market.getSubmarket(submarketId) ?: continue
            val cargo = submarket.cargoNullOk ?: continue
            // snapshot - we mutate as we go
            val stacks = ArrayList(cargo.stacksCopy ?: continue)
            for (stack in stacks) {
                try {
                    val gained = tryBuyOne(stack, submarket, cfg, playerCargo)
                    if (gained != null) {
                        bought += 1
                        spent += gained
                    }
                } catch (t: Throwable) {
                    LOG.warn("Auto-trade hullmod buy failed", t)
                    errors.add("hullmod: ${t.javaClass.simpleName}")
                }
            }
            cargo.removeEmptyStacks()
        }
        return Result(bought, spent, errors)
    }

    private fun tryBuyOne(
        stack: CargoStackAPI,
        submarket: SubmarketAPI,
        cfg: AutoTradeConfig,
        playerCargo: CargoAPI,
    ): Long? {
        if (!stack.isSpecialStack) return null
        val special = stack.specialDataIfSpecial ?: return null
        if (MODSPEC != special.id) return null
        val hm: HullModSpecAPI = stack.hullModSpecIfHullMod ?: return null
        val hmId = hm.id ?: return null
        val character = Global.getSector()?.characterData ?: return null
        if (character.knowsHullMod(hmId)) return null
        if (cfg.hullmodBlacklist.contains(hmId)) return null
        if (stack.size < 1f) return null

        val baseValue = hm.baseValue
        val tariff = submarket.plugin?.tariff ?: submarket.tariff
        val perUnit = baseValue * (1f + tariff.coerceAtLeast(0f))
        if (perUnit <= 0f) return null
        val credits = playerCargo.credits.get().toLong()
        val budget = credits - cfg.creditFloor
        if (budget < perUnit) return null

        val cost = Math.max(0, Math.round(perUnit))

        // Two-step mutation with manual rollback if credit subtraction fails.
        stack.subtract(1f)
        try {
            playerCargo.credits.subtract(cost.toFloat())
        } catch (t: Throwable) {
            stack.add(1f)
            throw t
        }

        if (cfg.learnHullmodsOnBuy) {
            try { Global.getSoundPlayer()?.playUISound("ui_acquired_hullmod", 1f, 1f) } catch (_: Throwable) {}
            character.addHullMod(hmId)
            try {
                Global.getSector()?.campaignUI?.addMessage("Acquired hull mod: " + hm.displayName)
            } catch (_: Throwable) {}
        } else {
            playerCargo.addHullmods(hmId, 1)
        }
        return cost.toLong()
    }
}
