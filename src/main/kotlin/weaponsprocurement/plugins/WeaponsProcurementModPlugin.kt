package weaponsprocurement.plugins

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import weaponsprocurement.lifecycle.StockReviewHotkeyScript
import weaponsprocurement.lifecycle.WeaponsProcurementFixerCatalogUpdater

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
