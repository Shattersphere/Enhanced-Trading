package weaponsprocurement.lifecycle

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import org.apache.log4j.Logger
import weaponsprocurement.stock.fixer.FixerMarketObservedCatalog
import weaponsprocurement.config.WeaponMarketBlacklist
import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.stock.fixer.ShipCatalogDiagnostics

class WeaponsProcurementFixerCatalogUpdater : EveryFrameScript {
    private val catalog = FixerMarketObservedCatalog()
    private val shipDiagnostics = ShipCatalogDiagnostics()
    private var lastSuccessfulScanTimestamp = Long.MIN_VALUE
    private var lastFailedScanTimestamp = Long.MIN_VALUE
    private var lastShipDiagnosticsFailureTimestamp = Long.MIN_VALUE
    private var scanErrorLogged = false
    private var shipDiagnosticsLogged = false
    private var shipDiagnosticsErrorLogged = false
    private var scanLogs = 0

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        val sector = Global.getSector()
        val clock = sector?.clock ?: return
        dumpShipDiagnosticsIfRequested(sector)

        if (!WeaponsProcurementConfig.isFixersMarketEnabled()) {
            return
        }
        if (lastSuccessfulScanTimestamp != Long.MIN_VALUE &&
            clock.getElapsedDaysSince(lastSuccessfulScanTimestamp) < SCAN_INTERVAL_DAYS
        ) {
            return
        }
        if (lastFailedScanTimestamp != Long.MIN_VALUE &&
            clock.getElapsedDaysSince(lastFailedScanTimestamp) < FAILURE_RETRY_INTERVAL_DAYS
        ) {
            return
        }
        try {
            val added = catalog.observeSectorStock(sector, WeaponMarketBlacklist.load())
            if (added > 0 && scanLogs < MAX_SCAN_LOGS) {
                scanLogs++
                LOG.info("WP_FIXER_CATALOG observed new legal items=$added")
            }
            lastSuccessfulScanTimestamp = clock.timestamp
        } catch (t: RuntimeException) {
            lastFailedScanTimestamp = clock.timestamp
            if (!scanErrorLogged) {
                scanErrorLogged = true
                LOG.error("WP_FIXER_CATALOG scan failed", t)
            }
        }
    }

    private fun dumpShipDiagnosticsIfRequested(sector: SectorAPI?) {
        if (shipDiagnosticsLogged) return
        val spec = WeaponsProcurementConfig.debugShipCatalogSpec()
        if (spec.isBlank() || spec.equals("false", ignoreCase = true) || spec.equals("none", ignoreCase = true)) return
        val clock = sector?.clock
        if (clock != null &&
            lastShipDiagnosticsFailureTimestamp != Long.MIN_VALUE &&
            clock.getElapsedDaysSince(lastShipDiagnosticsFailureTimestamp) < FAILURE_RETRY_INTERVAL_DAYS
        ) {
            return
        }
        try {
            shipDiagnostics.dump(sector, spec, LOG)
            shipDiagnosticsLogged = true
        } catch (t: RuntimeException) {
            if (clock != null) {
                lastShipDiagnosticsFailureTimestamp = clock.timestamp
            }
            if (!shipDiagnosticsErrorLogged) {
                shipDiagnosticsErrorLogged = true
                LOG.error("WP_SHIP_CATALOG_DIAG failed; will retry after short backoff", t)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementFixerCatalogUpdater::class.java)
        private const val SCAN_INTERVAL_DAYS = 1f
        private const val FAILURE_RETRY_INTERVAL_DAYS = 0.05f
        private const val MAX_SCAN_LOGS = 10
    }
}
