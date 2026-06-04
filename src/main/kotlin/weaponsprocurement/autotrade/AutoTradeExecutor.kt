package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import org.apache.log4j.Logger
import org.lwjgl.input.Keyboard
import weaponsprocurement.config.WeaponsProcurementConfig

/**
 * Shared execution entry point for both auto-trade modes. Immediate mode calls this from
 * [AutoTradeSubmarketListener] as soon as a submarket opens; confirm mode calls it from
 * [AutoTradeExecuteHotkeyScript] when the player presses the execute hotkey.
 *
 * Dedupe is coordinated through [AutoTradeVisitState] so reopening a submarket or pressing the
 * hotkey twice does not re-trade. The result summary is posted through the floating message
 * display (not the campaign log) so it is visible in the trade screen itself.
 */
object AutoTradeExecutor {
    private val LOG: Logger = Logger.getLogger(AutoTradeExecutor::class.java)

    /**
     * Run the pending auto-trades for the opened submarket: buys scoped to it, plus the
     * cross-market sell pass once per visit. Returns true if anything was traded.
     */
    fun executeForOpenSubmarket(submarket: SubmarketAPI?): Boolean {
        if (submarket == null) return false
        val market = submarket.market ?: return false
        return try {
            AutoTradeVisitState.beginVisit(market.id)
            val includeSells = AutoTradeVisitState.claimSells()
            val runBuys = AutoTradeVisitState.claimBuys(submarket.specId)
            if (!includeSells && !runBuys) return false

            val summary = AutoTradeEngine.executeForSubmarket(submarket, includeSells)
            if (summary != null) {
                AutoTradeNotify.post(summary)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            LOG.warn("Auto-trade execution failed", t)
            false
        }
    }

    /**
     * Confirm-mode prompt: if this newly-opened submarket has pending trades, post a one-time
     * (per submarket per visit) floating "press X to confirm" message. The detailed list of what
     * would trade is shown earlier, on the campaign screen at planet-hail
     * ([AutoTradeMarketListener]); here we only need to tell the player which key applies it.
     */
    fun promptForOpenSubmarket(submarket: SubmarketAPI?) {
        if (submarket == null) return
        if (!hasPending(submarket)) return
        if (!AutoTradeVisitState.claimPrompt(submarket.specId)) return

        val keyCode = WeaponsProcurementConfig.autoTradeExecuteHotkeyKeyCode()
        val keyName = Keyboard.getKeyName(keyCode)?.takeIf { it.isNotBlank() } ?: keyCode.toString()
        AutoTradeNotify.post("Press $keyName to confirm auto-trade.")
    }

    /**
     * Whether pressing the execute hotkey at this submarket would currently do anything: there
     * are unclaimed sells with live candidates, or unclaimed buys with live candidates here.
     */
    fun hasPending(submarket: SubmarketAPI?): Boolean {
        if (submarket == null) return false
        return try {
            val sellsPending = AutoTradeVisitState.sellsPending() && AutoTradeEngine.hasSellCandidates()
            val buysPending = AutoTradeVisitState.buysPending(submarket.specId) &&
                AutoTradeEngine.submarketHasBuyCandidates(submarket)
            sellsPending || buysPending
        } catch (t: Throwable) {
            LOG.warn("Auto-trade pending check failed", t)
            false
        }
    }
}
