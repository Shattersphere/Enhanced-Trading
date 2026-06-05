param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

function Assert-JsonValue {
    param(
        [object]$Json,
        [string]$Path,
        [object]$Expected
    )
    $value = $Json
    foreach ($part in $Path.Split(".")) {
        if ($null -eq $value -or -not $value.PSObject.Properties.Name.Contains($part)) {
            Add-Failure "mod_info.json missing $Path"
            return
        }
        $value = $value.$part
    }
    if ($value -eq $Expected) {
        Add-Pass "mod_info.json $Path is $Expected"
    } else {
        Add-Failure "mod_info.json $Path expected $Expected but was $value"
    }
}

$modInfoText = Read-Text "mod_info.json"
if (-not [string]::IsNullOrWhiteSpace($modInfoText)) {
    $modInfo = $modInfoText | ConvertFrom-Json
    Assert-JsonValue -Json $modInfo -Path "id" -Expected "enhanced_trading"
    Assert-JsonValue -Json $modInfo -Path "gameVersion" -Expected "0.98a"
    Assert-JsonValue -Json $modInfo -Path "modPlugin" -Expected "weaponsprocurement.plugins.WeaponsProcurementModPlugin"
    if ($modInfo.jars -contains "jars/enhanced-trading.jar") {
        Add-Pass "mod_info.json declares jars/enhanced-trading.jar"
    } else {
        Add-Failure "mod_info.json must declare jars/enhanced-trading.jar"
    }
    foreach ($depId in @("lw_lazylib", "lunalib", "shatter_lib")) {
        if (@($modInfo.dependencies | ForEach-Object { $_.id }) -contains $depId) {
            Add-Pass "mod_info.json declares dependency $depId"
        } else {
            Add-Failure "mod_info.json must declare dependency $depId"
        }
    }
}

$buildGradle = Read-Text "build.gradle.kts"
Assert-Contains "build.gradle.kts" $buildGradle 'archiveFileName.set("enhanced-trading.jar")'
Assert-Contains "build.gradle.kts" $buildGradle 'kotlin("jvm") version "2.1.20"'
Assert-Contains "build.gradle.kts" $buildGradle 'modIds = listOf("shatter_lib")'

$buildScript = Read-Text "build.ps1"
Assert-Contains "build.ps1" $buildScript 'jars\enhanced-trading.jar'

$rules = Read-Text "data/campaign/rules.csv"
foreach ($needle in @("wp_marketOpenWeaponProcurement", "wp_openWeaponProcurement", "WP_OpenDialog canShow", "WP_OpenDialog open")) {
    Assert-Contains "data/campaign/rules.csv" $rules $needle
}

$luna = Read-Text "data/config/LunaSettings.csv"
foreach ($key in @(
    "wp_update_interval_seconds",
    "wp_trade_hotkey",
    "wp_enable_dialog_option",
    "wp_enable_sector_market",
    "wp_enable_fixers_market",
    "wp_enable_fixers_market_tag_inference",
    "wp_sector_market_price_multiplier",
    "wp_fixers_market_price_multiplier",
    "wp_desired_small_weapon_count",
    "wp_desired_medium_weapon_count",
    "wp_desired_large_weapon_count",
    "wp_desired_fighter_wing_count",
    "wp_enable_debug_ui"
)) {
    Assert-Contains "data/config/LunaSettings.csv" $luna $key
}

$compatibilityIds = Read-Text "src/main/kotlin/weaponsprocurement/CompatibilityIds.kt"
foreach ($needle in @(
    'const val MOD_ID: String = "enhanced_trading"',
    'const val TRADE_HOTKEY: String = "wp_trade_hotkey"',
    'const val UPDATE_INTERVAL_SECONDS: String = "wp.config.updateIntervalSeconds"',
    'const val DIALOG_OPTION_ENABLED: String = "wp.config.dialogOptionEnabled"',
    'const val SECTOR_MARKET_ENABLED: String = "wp.config.sectorMarketEnabled"',
    'const val FIXERS_MARKET_ENABLED: String = "wp.config.fixersMarketEnabled"',
    'const val FIXERS_MARKET_TAG_INFERENCE_ENABLED: String = "wp.config.fixersMarketTagInferenceEnabled"',
    'const val SECTOR_MARKET_PRICE_MULTIPLIER: String = "wp.config.sectorMarketPriceMultiplier"',
    'const val FIXERS_MARKET_PRICE_MULTIPLIER: String = "wp.config.fixersMarketPriceMultiplier"',
    'const val DESIRED_SMALL_WEAPON_COUNT: String = "wp.config.desiredSmallWeaponCount"',
    'const val DESIRED_MEDIUM_WEAPON_COUNT: String = "wp.config.desiredMediumWeaponCount"',
    'const val DESIRED_LARGE_WEAPON_COUNT: String = "wp.config.desiredLargeWeaponCount"',
    'const val DESIRED_FIGHTER_WING_COUNT: String = "wp.config.desiredFighterWingCount"',
    'const val TRADE_HOTKEY: String = "wp.config.tradeHotkey"',
    'const val DEBUG_UI_ENABLED: String = "wp.config.debugUiEnabled"',
    'const val TRADE_FAILURE_STEP: String = "wp.debug.failTradeStep"',
    'const val SHIP_CATALOG: String = "wp.debug.shipCatalog"',
    'const val SHIP_CATALOG_VIEW: String = "wp.debug.shipCatalogView"',
    'const val DEBUG_WEAPON_ID: String = "wp_debug_worst_case_weapon"',
    'const val DEBUG_WING_ID: String = "wp_debug_worst_case_wing"',
    'const val STOCK_REVIEW: String = "data/config/enhanced_trading_stock.json"',
    'const val MARKET_BLACKLIST: String = "data/config/enhanced_trading_market_blacklist.json"',
    'const val WEAPON_PREFIX: String = "W:"',
    'const val WING_PREFIX: String = "F:"',
    'const val SECTOR_KEY: String = "BANNED_FROM_SECTOR_MARKET"',
    'const val FIXERS_KEY: String = "BANNED_FROM_FIXERS_MARKET"',
    'const val FIXER_OBSERVED_CATALOG_KEY: String = "weaponsProcurement.fixerObservedCatalog.v1"',
    'const val FIXER_OBSERVED_CATALOG_VALUE_SEPARATOR: String = "|"',
    'const val FIXERS_MARKET_SUBMARKET_ID: String = "wp_fixers_market"',
    'const val DEBUG_EMPTY_ITEM_ICON: String = "graphics/ui/wp_debug_empty_item.png"'
)) {
    Assert-Contains "CompatibilityIds.kt" $compatibilityIds $needle
}

$settingsConfig = Read-Text "src/main/kotlin/weaponsprocurement/config/WeaponsProcurementConfig.kt"
foreach ($needle in @(
    'MOD_ID = CompatibilityIds.MOD_ID',
    'SETTING_TRADE_HOTKEY = CompatibilityIds.Luna.TRADE_HOTKEY',
    'KEY_DEBUG_TRADE_FAILURE_STEP: String = CompatibilityIds.Diagnostics.TRADE_FAILURE_STEP',
    'KEY_DEBUG_SHIP_CATALOG: String = CompatibilityIds.Diagnostics.SHIP_CATALOG',
    'KEY_DEBUG_SHIP_CATALOG_VIEW: String = CompatibilityIds.Diagnostics.SHIP_CATALOG_VIEW'
)) {
    Assert-Contains "WeaponsProcurementConfig.kt" $settingsConfig $needle
}

$debugItems = Read-Text "src/main/kotlin/weaponsprocurement/ui/stockreview/rows/StockReviewDebugItemRecords.kt"
foreach ($needle in @(
    'EMPTY_ICON = CompatibilityIds.Diagnostics.DEBUG_EMPTY_ITEM_ICON',
    'DEBUG_WEAPON_ID = CompatibilityIds.Diagnostics.DEBUG_WEAPON_ID',
    'DEBUG_WING_ID = CompatibilityIds.Diagnostics.DEBUG_WING_ID'
)) {
    Assert-Contains "StockReviewDebugItemRecords.kt diagnostic id bridge" $debugItems $needle
}

foreach ($needle in @(
    'KEY_UPDATE_INTERVAL = CompatibilityIds.SystemProperties.UPDATE_INTERVAL_SECONDS',
    'KEY_DIALOG_OPTION_ENABLED = CompatibilityIds.SystemProperties.DIALOG_OPTION_ENABLED',
    'KEY_SECTOR_MARKET_ENABLED = CompatibilityIds.SystemProperties.SECTOR_MARKET_ENABLED',
    'KEY_FIXERS_MARKET_ENABLED = CompatibilityIds.SystemProperties.FIXERS_MARKET_ENABLED',
    'KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED = CompatibilityIds.SystemProperties.FIXERS_MARKET_TAG_INFERENCE_ENABLED',
    'KEY_SECTOR_MARKET_PRICE_MULTIPLIER = CompatibilityIds.SystemProperties.SECTOR_MARKET_PRICE_MULTIPLIER',
    'KEY_FIXERS_MARKET_PRICE_MULTIPLIER = CompatibilityIds.SystemProperties.FIXERS_MARKET_PRICE_MULTIPLIER',
    'KEY_DESIRED_SMALL_WEAPON_COUNT = CompatibilityIds.SystemProperties.DESIRED_SMALL_WEAPON_COUNT',
    'KEY_DESIRED_MEDIUM_WEAPON_COUNT = CompatibilityIds.SystemProperties.DESIRED_MEDIUM_WEAPON_COUNT',
    'KEY_DESIRED_LARGE_WEAPON_COUNT = CompatibilityIds.SystemProperties.DESIRED_LARGE_WEAPON_COUNT',
    'KEY_DESIRED_FIGHTER_WING_COUNT = CompatibilityIds.SystemProperties.DESIRED_FIGHTER_WING_COUNT',
    'KEY_TRADE_HOTKEY = CompatibilityIds.SystemProperties.TRADE_HOTKEY',
    'KEY_DEBUG_UI_ENABLED = CompatibilityIds.SystemProperties.DEBUG_UI_ENABLED'
)) {
    Assert-Contains "WeaponsProcurementConfig.kt system-property bridge" $settingsConfig $needle
}

$stockConfig = Read-Text "src/main/kotlin/weaponsprocurement/config/StockReviewConfig.kt"
foreach ($needle in @(
    'CONFIG_PATH = CompatibilityIds.ConfigFiles.STOCK_REVIEW',
    'private const val JSON_DESIRED_DEFAULTS = "desiredDefaults"',
    'private const val JSON_DISPLAY = "display"',
    'private const val JSON_SOURCES = "sources"',
    'private const val JSON_PER_WEAPON = "perWeapon"',
    'private const val JSON_PER_ITEM = "perItem"',
    'private const val JSON_INCLUDE_CURRENT_MARKET_STORAGE = "includeCurrentMarketStorage"',
    'private const val JSON_INCLUDE_BLACK_MARKET = "includeBlackMarket"',
    'json.optJSONObject(JSON_DESIRED_DEFAULTS)',
    'json.optJSONObject(JSON_PER_WEAPON)',
    'json.optJSONObject(JSON_PER_ITEM)',
    'optBoolean(sources, JSON_INCLUDE_CURRENT_MARKET_STORAGE, true)',
    'optBoolean(sources, JSON_INCLUDE_BLACK_MARKET, true)'
)) {
    Assert-Contains "StockReviewConfig.kt" $stockConfig $needle
}

$blacklistConfig = Read-Text "src/main/kotlin/weaponsprocurement/config/WeaponMarketBlacklist.kt"
foreach ($needle in @(
    'CONFIG_PATH = CompatibilityIds.ConfigFiles.MARKET_BLACKLIST',
    'SECTOR_KEY = CompatibilityIds.MarketBlacklist.SECTOR_KEY',
    'FIXERS_KEY = CompatibilityIds.MarketBlacklist.FIXERS_KEY'
)) {
    Assert-Contains "WeaponMarketBlacklist.kt" $blacklistConfig $needle
}

$stockItemType = Read-Text "src/main/kotlin/weaponsprocurement/stock/item/StockItemType.kt"
Assert-Contains "StockItemType.kt" $stockItemType 'WEAPON("Weapons", "Weapon", CompatibilityIds.StockItemKeys.WEAPON_PREFIX)'
Assert-Contains "StockItemType.kt" $stockItemType 'WING("Wings", "Wing", CompatibilityIds.StockItemKeys.WING_PREFIX)'

$fixerCatalog = Read-Text "src/main/kotlin/weaponsprocurement/stock/fixer/FixerMarketObservedCatalog.kt"
Assert-Contains "FixerMarketObservedCatalog.kt" $fixerCatalog 'PERSISTENT_KEY = CompatibilityIds.Persistence.FIXER_OBSERVED_CATALOG_KEY'
Assert-Contains "FixerMarketObservedCatalog.kt" $fixerCatalog 'VALUE_SEPARATOR = CompatibilityIds.Persistence.FIXER_OBSERVED_CATALOG_VALUE_SEPARATOR'

$globalMarket = Read-Text "src/main/kotlin/weaponsprocurement/stock/market/GlobalWeaponMarketService.kt"
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'VIRTUAL_SUBMARKET_ID: String = CompatibilityIds.Markets.FIXERS_MARKET_SUBMARKET_ID'

$submarketAccess = Read-Text "src/main/kotlin/weaponsprocurement/stock/market/StockSubmarketAccess.kt"
Assert-Contains "StockSubmarketAccess.kt" $submarketAccess 'Submarkets.SUBMARKET_STORAGE == submarketId'
Assert-Contains "StockSubmarketAccess.kt" $submarketAccess 'Submarkets.LOCAL_RESOURCES == submarketId'

$sanity = Read-Text ".github/workflows/sanity.yml"
Assert-Contains ".github/workflows/sanity.yml" $sanity 'push:'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'pull_request:'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'workflow_dispatch:'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'branches: [ main ]'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-jar-classes.ps1 -JarPath .\jars\enhanced-trading.jar -Label Repo'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-compatibility-surfaces.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-validation-assertions.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-config-contracts.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-fixer-persistence-contracts.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-trade-rollback-contracts.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-source-semantics-contracts.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-ship-trading-contracts.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-runtime-evidence-contracts.ps1'
if ($sanity.Contains("weapons-procurement.jar")) {
    Add-Failure ".github/workflows/sanity.yml must not reference weapons-procurement.jar"
} else {
    Add-Pass ".github/workflows/sanity.yml does not reference weapons-procurement.jar"
}

foreach ($path in @("data/hulls", "data/weapons", "data/variants", "data/wings", "data/ships", "data/factions", "data/world")) {
    if (Test-Path -LiteralPath (Join-Path $repoRoot $path)) {
        Add-Failure "$path exists; update the modernization plan before adding Starsector ID families"
    } else {
        Add-Pass "$path absent"
    }
}

if ($failures.Count -gt 0) {
    throw "Compatibility surface validation failed with $($failures.Count) failure(s)."
}

Write-Host "Compatibility surface validation passed."
