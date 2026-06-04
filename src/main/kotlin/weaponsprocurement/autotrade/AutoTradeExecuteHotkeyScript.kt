package weaponsprocurement.autotrade

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.ui.WimGuiHotkeyLatch

/**
 * Drives confirm-before-trading mode. While a submarket with pending auto-trades is open, the
 * player presses the configurable execute hotkey (default E) to apply the trade. The "press X"
 * prompt itself is posted as a floating message when the submarket opens
 * ([AutoTradeExecutor.promptForOpenSubmarket]); this script only watches for the keypress.
 *
 * Only acts when [AutoTradeConfig.requireConfirm] is set; in immediate mode it does nothing
 * (execution stays in [AutoTradeSubmarketListener]).
 */
class AutoTradeExecuteHotkeyScript : EveryFrameScript {
    private val hotkey = WimGuiHotkeyLatch()

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        val cfg = AutoTradeRegistry.get()
        if (!cfg.enabled || !cfg.requireConfirm) return

        val openMarket = Global.getSector()?.currentlyOpenMarket
        val submarket = AutoTradeVisitState.openSubmarket()
        if (openMarket == null || submarket == null || submarket.market !== openMarket) return

        val keyCode = WeaponsProcurementConfig.autoTradeExecuteHotkeyKeyCode()
        if (hotkey.consumePress(keyCode) && AutoTradeExecutor.hasPending(submarket)) {
            AutoTradeExecutor.executeForOpenSubmarket(submarket)
        }
    }
}
