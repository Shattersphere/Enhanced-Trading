package weaponsprocurement

/**
 * Shipped identifiers that Starsector, LunaLib, user config, saves, or validators
 * can observe. Move implementation freely, but do not change these values without
 * an explicit compatibility gate.
 */
object CompatibilityIds {
    const val MOD_ID: String = "enhanced_trading"

    object Luna {
        const val UPDATE_INTERVAL_SECONDS: String = "wp_update_interval_seconds"
        const val ENABLE_DIALOG_OPTION: String = "wp_enable_dialog_option"
        const val ENABLE_SECTOR_MARKET: String = "wp_enable_sector_market"
        const val ENABLE_FIXERS_MARKET: String = "wp_enable_fixers_market"
        const val ENABLE_FIXERS_MARKET_TAG_INFERENCE: String = "wp_enable_fixers_market_tag_inference"
        const val SECTOR_MARKET_PRICE_MULTIPLIER: String = "wp_sector_market_price_multiplier"
        const val FIXERS_MARKET_PRICE_MULTIPLIER: String = "wp_fixers_market_price_multiplier"
        const val DESIRED_SMALL_WEAPON_COUNT: String = "wp_desired_small_weapon_count"
        const val DESIRED_MEDIUM_WEAPON_COUNT: String = "wp_desired_medium_weapon_count"
        const val DESIRED_LARGE_WEAPON_COUNT: String = "wp_desired_large_weapon_count"
        const val DESIRED_FIGHTER_WING_COUNT: String = "wp_desired_fighter_wing_count"
        const val TRADE_HOTKEY: String = "wp_trade_hotkey"
        const val ENABLE_DEBUG_UI: String = "wp_enable_debug_ui"
    }

    object SystemProperties {
        const val UPDATE_INTERVAL_SECONDS: String = "wp.config.updateIntervalSeconds"
        const val DIALOG_OPTION_ENABLED: String = "wp.config.dialogOptionEnabled"
        const val SECTOR_MARKET_ENABLED: String = "wp.config.sectorMarketEnabled"
        const val FIXERS_MARKET_ENABLED: String = "wp.config.fixersMarketEnabled"
        const val FIXERS_MARKET_TAG_INFERENCE_ENABLED: String = "wp.config.fixersMarketTagInferenceEnabled"
        const val SECTOR_MARKET_PRICE_MULTIPLIER: String = "wp.config.sectorMarketPriceMultiplier"
        const val FIXERS_MARKET_PRICE_MULTIPLIER: String = "wp.config.fixersMarketPriceMultiplier"
        const val DESIRED_SMALL_WEAPON_COUNT: String = "wp.config.desiredSmallWeaponCount"
        const val DESIRED_MEDIUM_WEAPON_COUNT: String = "wp.config.desiredMediumWeaponCount"
        const val DESIRED_LARGE_WEAPON_COUNT: String = "wp.config.desiredLargeWeaponCount"
        const val DESIRED_FIGHTER_WING_COUNT: String = "wp.config.desiredFighterWingCount"
        const val TRADE_HOTKEY: String = "wp.config.tradeHotkey"
        const val DEBUG_UI_ENABLED: String = "wp.config.debugUiEnabled"
    }

    object Diagnostics {
        const val TRADE_FAILURE_STEP: String = "wp.debug.failTradeStep"
        const val SHIP_CATALOG: String = "wp.debug.shipCatalog"
        const val SHIP_CATALOG_VIEW: String = "wp.debug.shipCatalogView"
        const val DEBUG_EMPTY_ITEM_ICON: String = "graphics/ui/wp_debug_empty_item.png"
        const val DEBUG_WEAPON_ID: String = "wp_debug_worst_case_weapon"
        const val DEBUG_WING_ID: String = "wp_debug_worst_case_wing"
    }

    object ConfigFiles {
        const val STOCK_REVIEW: String = "data/config/enhanced_trading_stock.json"
        const val MARKET_BLACKLIST: String = "data/config/enhanced_trading_market_blacklist.json"
    }

    object StockItemKeys {
        const val WEAPON_PREFIX: String = "W:"
        const val WING_PREFIX: String = "F:"
    }

    object MarketBlacklist {
        const val SECTOR_KEY: String = "BANNED_FROM_SECTOR_MARKET"
        const val FIXERS_KEY: String = "BANNED_FROM_FIXERS_MARKET"
    }

    object Persistence {
        const val FIXER_OBSERVED_CATALOG_KEY: String = "weaponsProcurement.fixerObservedCatalog.v1"
        const val FIXER_OBSERVED_CATALOG_VALUE_SEPARATOR: String = "|"
    }

    object Markets {
        const val FIXERS_MARKET_SUBMARKET_ID: String = "wp_fixers_market"
    }
}
