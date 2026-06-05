package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import org.apache.log4j.Logger
import weaponsprocurement.CompatibilityIds
import weaponsprocurement.config.WeaponMarketBlacklist
import weaponsprocurement.stock.item.SubmarketWeaponStock
import weaponsprocurement.stock.market.MarketStockService
import java.util.Collections
import java.util.HashMap

class FixerMarketObservedCatalog {
    private val marketStockService = MarketStockService()

    fun observeSectorStock(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Int {
        val catalog = rawCatalog(sector) ?: return 0
        val markets: List<MarketAPI> = sector?.economy?.marketsCopy ?: return 0

        pruneInvalidPersistentEntries(catalog)
        var added = 0
        for (market in markets) {
            val stock = marketStockService.collectCurrentMarketItemStock(market, true)
            for (itemKey in stock.itemKeys()) {
                if (!FixerCatalogPolicy.isEligibleObservedItem(itemKey, blacklist)) continue
                val source = cheapestReferenceSource(stock.getSubmarketStocks(itemKey)) ?: continue
                if (!catalog.containsKey(itemKey)) added++
                catalog[itemKey] = encode(source.baseUnitPrice, source.unitCargoSpace)
            }
        }
        return added
    }

    fun observedItems(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Map<String, ObservedItem> {
        val catalog = rawCatalog(sector)
        if (catalog == null || catalog.isEmpty()) return Collections.emptyMap()

        val result = HashMap<String, ObservedItem>()
        var pruned = 0
        val iterator = catalog.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val itemKey = entry.key
            val item = decodePersistentItem(itemKey, entry.value)
            if (item == null) {
                iterator.remove()
                pruned++
                continue
            }
            if (!FixerCatalogPolicy.isBanned(blacklist, itemKey)) {
                result[itemKey] = item
            }
        }
        logPrunedEntries(pruned)
        return Collections.unmodifiableMap(result)
    }

    fun cacheKey(sector: SectorAPI?): String {
        val catalog = rawCatalog(sector)
        if (catalog == null) return "observed=none"
        pruneInvalidPersistentEntries(catalog)
        return "observed=${catalog.size}:${catalog.hashCode()}"
    }

    private fun pruneInvalidPersistentEntries(catalog: MutableMap<String, String>) {
        if (catalog.isEmpty()) return
        var pruned = 0
        val iterator = catalog.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (decodePersistentItem(entry.key, entry.value) == null) {
                iterator.remove()
                pruned++
            }
        }
        logPrunedEntries(pruned)
    }

    class ObservedItem private constructor(
        @get:JvmName("getBaseUnitPrice")
        val baseUnitPrice: Int,
        @get:JvmName("getUnitCargoSpace")
        val unitCargoSpace: Float,
    ) {
        companion object {
            fun create(baseUnitPrice: Int, unitCargoSpace: Float): ObservedItem {
                return ObservedItem(baseUnitPrice, unitCargoSpace)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(FixerMarketObservedCatalog::class.java)
        private const val PERSISTENT_KEY = CompatibilityIds.Persistence.FIXER_OBSERVED_CATALOG_KEY
        private const val VALUE_SEPARATOR = CompatibilityIds.Persistence.FIXER_OBSERVED_CATALOG_VALUE_SEPARATOR

        private var migrationLogged = false
        private var pruneLogged = false

        @JvmStatic
        fun isSafeFixerItem(itemKey: String?): Boolean {
            return FixerCatalogPolicy.isSafeItem(itemKey)
        }

        private fun cheapestReferenceSource(sources: List<SubmarketWeaponStock>?): SubmarketWeaponStock? {
            var best: SubmarketWeaponStock? = null
            if (sources == null) return null
            for (source in sources) {
                if (source.count <= 0 || !source.isPurchasable()) continue
                val currentBest = best
                if (currentBest == null || compareReferenceSource(source, currentBest) < 0) {
                    best = source
                }
            }
            return best
        }

        private fun rawCatalog(sector: SectorAPI?): MutableMap<String, String>? {
            val persistentData = sector?.persistentData ?: return null
            val existing = persistentData[PERSISTENT_KEY]
            if (existing is MutableMap<*, *> && containsOnlyStringEntries(existing)) {
                @Suppress("UNCHECKED_CAST")
                return existing as MutableMap<String, String>
            }
            if (existing is Map<*, *>) {
                return sanitizedCatalog(sector, existing)
            }
            val catalog = HashMap<String, String>()
            persistentData[PERSISTENT_KEY] = catalog
            return catalog
        }

        private fun containsOnlyStringEntries(existing: Map<*, *>): Boolean {
            for ((key, value) in existing) {
                if (key !is String || value !is String) return false
            }
            return true
        }

        private fun sanitizedCatalog(sector: SectorAPI, existing: Map<*, *>): MutableMap<String, String> {
            val catalog = HashMap<String, String>()
            var discarded = 0
            for ((key, value) in existing) {
                if (key is String && value is String) {
                    catalog[key] = value
                } else {
                    discarded++
                }
            }
            if (discarded > 0 && !migrationLogged) {
                migrationLogged = true
                LOG.warn("WP_FIXER_CATALOG discarded $discarded malformed persistent entries.")
            }
            sector.persistentData[PERSISTENT_KEY] = catalog
            return catalog
        }

        private fun encode(baseUnitPrice: Int, unitCargoSpace: Float): String {
            return "${Math.max(0, baseUnitPrice)}$VALUE_SEPARATOR${sanitizeUnitCargoSpace(unitCargoSpace)}"
        }

        private fun decode(value: String?): ObservedItem? {
            if (value == null) return null
            val parts = value.split(VALUE_SEPARATOR, limit = 2)
            return try {
                val baseUnitPrice = if (parts.isNotEmpty()) parts[0].trim().toInt() else 0
                val unitCargoSpace = if (parts.size > 1) parts[1].trim().toFloat() else 1f
                if (!isFinite(unitCargoSpace)) return null
                ObservedItem.create(Math.max(0, baseUnitPrice), sanitizeUnitCargoSpace(unitCargoSpace))
            } catch (_: RuntimeException) {
                null
            }
        }

        private fun decodePersistentItem(itemKey: String?, encoded: String?): ObservedItem? {
            if (!FixerCatalogPolicy.isSafeItem(itemKey)) return null
            return decode(encoded)
        }

        private fun sanitizeUnitCargoSpace(unitCargoSpace: Float): Float {
            return if (isFinite(unitCargoSpace)) Math.max(0.01f, unitCargoSpace) else 1f
        }

        private fun isFinite(value: Float): Boolean {
            return !value.isNaN() && !value.isInfinite()
        }

        private fun logPrunedEntries(pruned: Int) {
            if (pruned > 0 && !pruneLogged) {
                pruneLogged = true
                LOG.warn("WP_FIXER_CATALOG pruned $pruned invalid or unsafe persistent entries.")
            }
        }

        private fun compareReferenceSource(left: SubmarketWeaponStock, right: SubmarketWeaponStock): Int {
            val result = left.baseUnitPrice.compareTo(right.baseUnitPrice)
            return if (result != 0) {
                result
            } else {
                left.displaySourceName.orEmpty().compareTo(right.displaySourceName.orEmpty(), ignoreCase = true)
            }
        }

    }
}
