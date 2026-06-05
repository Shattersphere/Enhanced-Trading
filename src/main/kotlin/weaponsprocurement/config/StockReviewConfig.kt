package weaponsprocurement.config

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import org.apache.log4j.Logger
import org.json.JSONObject
import weaponsprocurement.CompatibilityIds
import weaponsprocurement.stock.item.OwnedSourcePolicy
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.StockSortMode
import java.util.Collections
import java.util.HashMap

class StockReviewConfig private constructor(
    private val smallWeaponDesired: Int,
    private val mediumWeaponDesired: Int,
    private val largeWeaponDesired: Int,
    private val fighterWingDesired: Int,
    private val includeCurrentMarketStorage: Boolean,
    private val includeBlackMarket: Boolean,
    private val sortMode: StockSortMode,
    desiredOverrides: Map<String, Int>,
    ignoredItems: Map<String, Boolean>,
) {
    private val desiredOverrides: Map<String, Int> = Collections.unmodifiableMap(HashMap(desiredOverrides))
    private val ignoredItems: Map<String, Boolean> = Collections.unmodifiableMap(HashMap(ignoredItems))

    fun desiredCount(weaponId: String?, size: WeaponAPI.WeaponSize?): Int {
        val override = desiredOverride(StockItemType.WEAPON, weaponId)
        if (override != null) return override

        if (WeaponAPI.WeaponSize.SMALL == size) return smallWeaponDesired
        if (WeaponAPI.WeaponSize.MEDIUM == size) return mediumWeaponDesired
        if (WeaponAPI.WeaponSize.LARGE == size) return largeWeaponDesired
        return mediumWeaponDesired
    }

    fun isIgnored(itemKeyOrId: String?): Boolean {
        val ignored = ignoredItems[itemKeyOrId]
        return ignored == true
    }

    fun isIgnored(itemType: StockItemType, itemId: String?): Boolean {
        val typedIgnored = ignoredItems[itemType.key(itemId)]
        if (typedIgnored != null) return typedIgnored
        return ignoredItems[itemId] == true
    }

    fun desiredFighterWingCount(wingId: String?): Int {
        val override = desiredOverride(StockItemType.WING, wingId)
        return override ?: fighterWingDesired
    }

    private fun desiredOverride(itemType: StockItemType, itemId: String?): Int? {
        val typedOverride = desiredOverrides[itemType.key(itemId)]
        if (typedOverride != null) return typedOverride
        return desiredOverrides[itemId]
    }

    fun isIncludeCurrentMarketStorage(): Boolean = includeCurrentMarketStorage

    fun isIncludeBlackMarket(): Boolean = includeBlackMarket

    fun getSortMode(): StockSortMode = sortMode

    fun ownedSourcePolicy(includeStorage: Boolean): OwnedSourcePolicy {
        return if (includeStorage) {
            OwnedSourcePolicy.FLEET_AND_ACCESSIBLE_STORAGE
        } else {
            OwnedSourcePolicy.FLEET_ONLY
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockReviewConfig::class.java)
        private const val CONFIG_PATH = CompatibilityIds.ConfigFiles.STOCK_REVIEW

        private const val DEFAULT_SMALL_WEAPON_COUNT = 16
        private const val DEFAULT_MEDIUM_WEAPON_COUNT = 8
        private const val DEFAULT_LARGE_WEAPON_COUNT = 4
        private const val DEFAULT_FIGHTER_WING_COUNT = 4
        private const val DEFAULT_SORT_MODE = "NEED"

        private const val JSON_DESIRED_DEFAULTS = "desiredDefaults"
        private const val JSON_DISPLAY = "display"
        private const val JSON_SOURCES = "sources"
        private const val JSON_PER_WEAPON = "perWeapon"
        private const val JSON_PER_ITEM = "perItem"
        private const val JSON_DEFAULT_SORT = "defaultSort"
        private const val JSON_INCLUDE_CURRENT_MARKET_STORAGE = "includeCurrentMarketStorage"
        private const val JSON_INCLUDE_BLACK_MARKET = "includeBlackMarket"
        private const val JSON_SMALL_WEAPON = "smallWeapon"
        private const val JSON_MEDIUM_WEAPON = "mediumWeapon"
        private const val JSON_LARGE_WEAPON = "largeWeapon"
        private const val JSON_FIGHTER_WING = "fighterWing"
        private const val JSON_DESIRED = "desired"
        private const val JSON_IGNORED = "ignored"

        @JvmStatic
        fun load(): StockReviewConfig {
            return try {
                val json = Global.getSettings().loadJSON(CONFIG_PATH)
                fromJson(json)
            } catch (t: RuntimeException) {
                LOG.warn("WP_STOCK_REVIEW config load failed; using defaults from $CONFIG_PATH", t)
                defaults()
            }
        }

        @JvmStatic
        fun defaults(): StockReviewConfig {
            return StockReviewConfig(
                WeaponsProcurementConfig.desiredSmallWeaponCount(DEFAULT_SMALL_WEAPON_COUNT),
                WeaponsProcurementConfig.desiredMediumWeaponCount(DEFAULT_MEDIUM_WEAPON_COUNT),
                WeaponsProcurementConfig.desiredLargeWeaponCount(DEFAULT_LARGE_WEAPON_COUNT),
                WeaponsProcurementConfig.desiredFighterWingCount(DEFAULT_FIGHTER_WING_COUNT),
                true,
                true,
                StockSortMode.NEED,
                Collections.emptyMap(),
                Collections.emptyMap(),
            )
        }

        private fun fromJson(json: JSONObject): StockReviewConfig {
            val desiredDefaults = json.optJSONObject(JSON_DESIRED_DEFAULTS)
            val small = WeaponsProcurementConfig.desiredSmallWeaponCount(
                clampDesired(optInt(desiredDefaults, JSON_SMALL_WEAPON, DEFAULT_SMALL_WEAPON_COUNT))
            )
            val medium = WeaponsProcurementConfig.desiredMediumWeaponCount(
                clampDesired(optInt(desiredDefaults, JSON_MEDIUM_WEAPON, DEFAULT_MEDIUM_WEAPON_COUNT))
            )
            val large = WeaponsProcurementConfig.desiredLargeWeaponCount(
                clampDesired(optInt(desiredDefaults, JSON_LARGE_WEAPON, DEFAULT_LARGE_WEAPON_COUNT))
            )
            val fighterWing = WeaponsProcurementConfig.desiredFighterWingCount(
                clampDesired(optInt(desiredDefaults, JSON_FIGHTER_WING, DEFAULT_FIGHTER_WING_COUNT))
            )

            val sources = json.optJSONObject(JSON_SOURCES)
            val includeStorage = optBoolean(sources, JSON_INCLUDE_CURRENT_MARKET_STORAGE, true)
            val includeBlackMarket = optBoolean(sources, JSON_INCLUDE_BLACK_MARKET, true)

            val display = json.optJSONObject(JSON_DISPLAY)
            val sortMode = StockSortMode.fromConfig(optString(display, JSON_DEFAULT_SORT, DEFAULT_SORT_MODE))

            val overrides = HashMap<String, Int>()
            val ignored = HashMap<String, Boolean>()
            readPerItemOverrides(json.optJSONObject(JSON_PER_WEAPON), overrides, ignored, medium)
            readPerItemOverrides(json.optJSONObject(JSON_PER_ITEM), overrides, ignored, medium)

            return StockReviewConfig(
                small,
                medium,
                large,
                fighterWing,
                includeStorage,
                includeBlackMarket,
                sortMode,
                overrides,
                ignored,
            )
        }

        private fun optInt(json: JSONObject?, key: String, defaultValue: Int): Int {
            return json?.optInt(key, defaultValue) ?: defaultValue
        }

        private fun optBoolean(json: JSONObject?, key: String, defaultValue: Boolean): Boolean {
            return json?.optBoolean(key, defaultValue) ?: defaultValue
        }

        private fun optString(json: JSONObject?, key: String, defaultValue: String): String {
            return json?.optString(key, defaultValue) ?: defaultValue
        }

        private fun readPerItemOverrides(
            json: JSONObject?,
            desired: MutableMap<String, Int>,
            ignored: MutableMap<String, Boolean>,
            defaultDesired: Int,
        ) {
            if (json == null) return
            val names = JSONObject.getNames(json) ?: return
            for (itemKey in names) {
                val itemConfig = json.optJSONObject(itemKey) ?: continue
                if (itemConfig.has(JSON_DESIRED)) {
                    desired[itemKey] = clampDesired(itemConfig.optInt(JSON_DESIRED, defaultDesired))
                }
                if (itemConfig.has(JSON_IGNORED)) {
                    ignored[itemKey] = itemConfig.optBoolean(JSON_IGNORED, false)
                }
            }
        }

        private fun clampDesired(value: Int): Int {
            if (value < 0) return 0
            if (value > 999) return 999
            return value
        }
    }
}
