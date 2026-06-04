package weaponsprocurement.autotrade

import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.listeners.SubmarketInteractionListener
import org.apache.log4j.Logger

/**
 * Reacts when the player opens a submarket in the trade screen, where stock is live and
 * vanilla-primed.
 *
 * In immediate mode trades run right away via [AutoTradeExecutor]. In confirm mode
 * ([AutoTradeConfig.requireConfirm]) this only records the opened submarket so the marker /
 * hint can show; [AutoTradeExecuteHotkeyScript] runs the trade when the player presses the
 * execute hotkey.
 */
class AutoTradeSubmarketListener : SubmarketInteractionListener {
    override fun reportPlayerOpenedSubmarket(
        submarket: SubmarketAPI?,
        type: SubmarketInteractionListener.SubmarketInteractionType?,
    ) {
        if (submarket == null) return
        val market = submarket.market ?: return
        try {
            AutoTradeVisitState.beginVisit(market.id)
            AutoTradeVisitState.setOpenSubmarket(submarket)
            val cfg = AutoTradeRegistry.get()
            if (!cfg.enabled) return
            if (cfg.requireConfirm) {
                // Wait for the execute hotkey; prompt the player that trades are pending here.
                AutoTradeExecutor.promptForOpenSubmarket(submarket)
                return
            }
            AutoTradeExecutor.executeForOpenSubmarket(submarket)
        } catch (t: Throwable) {
            LOG.warn("Auto-trade submarket open handling failed", t)
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(AutoTradeSubmarketListener::class.java)
    }
}
