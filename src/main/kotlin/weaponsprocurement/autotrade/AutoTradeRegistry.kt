package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import weaponsprocurement.config.WeaponsProcurementConfig

/**
 * Singleton accessor for the persistent [AutoTradeConfig]. The config is stored in
 * `sector.persistentData` under [KEY] and survives saves.
 *
 * First access on a sector with no stored config seeds the boolean toggles and credit
 * floor from current LunaSettings values; per-item rules always start empty.
 */
object AutoTradeRegistry {
    const val KEY: String = "wp.autotrade.config"

    private val LOG: Logger = Logger.getLogger(AutoTradeRegistry::class.java)

    @JvmStatic
    fun get(): AutoTradeConfig {
        val sector = Global.getSector()
        if (sector == null) {
            // No sector yet (e.g. during early bootstrap). Return a transient throwaway
            // so callers don't NPE; it will be discarded once the sector exists.
            val transient = AutoTradeConfig()
            transient.ensureSets()
            return transient
        }
        val data = sector.persistentData
        val existing = data[KEY] as? AutoTradeConfig
        if (existing != null) {
            existing.ensureSets()
            return existing
        }
        val created = AutoTradeConfig()
        created.ensureSets()
        WeaponsProcurementConfig.applyAutoTradeDefaults(created)
        data[KEY] = created
        LOG.info("WP_AUTOTRADE seeded persistent config from defaults")
        return created
    }
}
