package weaponsprocurement.plugins

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import weaponsprocurement.autotrade.AutoTradeExecuteHotkeyScript
import weaponsprocurement.autotrade.AutoTradeMarketListener
import weaponsprocurement.autotrade.AutoTradeSubmarketListener
import weaponsprocurement.lifecycle.StockReviewHotkeyScript
import weaponsprocurement.lifecycle.WeaponsProcurementFixerCatalogUpdater

/**
 * Registers the campaign scripts that own popup opening and Fixer catalog observation.
 * Keep feature logic in those scripts/controllers so repeated game loads stay idempotent.
 */
class WeaponsProcurementModPlugin : BaseModPlugin() {
    override fun onGameLoad(newGame: Boolean) {
        val sector = Global.getSector()
        if (sector == null) {
            LOG.warn("WP_PLUGIN registration skipped: sector is null")
            return
        }
        if (!hasScript(sector.transientScripts, StockReviewHotkeyScript::class.java) &&
            !hasScript(sector.scripts, StockReviewHotkeyScript::class.java)
        ) {
            sector.addTransientScript(StockReviewHotkeyScript())
            LOG.info("WP_STOCK_REVIEW hotkey registered")
        }
        if (!hasScript(sector.transientScripts, WeaponsProcurementFixerCatalogUpdater::class.java) &&
            !hasScript(sector.scripts, WeaponsProcurementFixerCatalogUpdater::class.java)
        ) {
            sector.addTransientScript(WeaponsProcurementFixerCatalogUpdater())
            LOG.info("WP_FIXER_CATALOG updater registered")
        }
        if (!hasScript(sector.transientScripts, AutoTradeExecuteHotkeyScript::class.java) &&
            !hasScript(sector.scripts, AutoTradeExecuteHotkeyScript::class.java)
        ) {
            sector.addTransientScript(AutoTradeExecuteHotkeyScript())
            LOG.info("WP_AUTOTRADE execute-hotkey script registered")
        }
        try {
            sector.listenerManager.removeListenerOfClass(AutoTradeMarketListener::class.java)
            sector.listenerManager.addListener(AutoTradeMarketListener())
            sector.listenerManager.removeListenerOfClass(AutoTradeSubmarketListener::class.java)
            sector.listenerManager.addListener(AutoTradeSubmarketListener())
            LOG.info("WP_AUTOTRADE listeners registered")
        } catch (t: Throwable) {
            LOG.warn("WP_AUTOTRADE listener registration failed", t)
        }
    }

    private fun hasScript(scripts: List<EveryFrameScript>?, scriptClass: Class<out EveryFrameScript>): Boolean {
        if (scripts == null) {
            return false
        }
        for (script in scripts) {
            if (scriptClass.isInstance(script)) {
                return true
            }
        }
        return false
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementModPlugin::class.java)
    }
}
