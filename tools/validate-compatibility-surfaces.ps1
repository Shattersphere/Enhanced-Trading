param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string]$Message)
    $script:failures.Add($Message)
    Write-Host "FAIL: $Message"
}

function Add-Pass {
    param([string]$Message)
    Write-Host "PASS: $Message"
}

function Read-Text {
    param([string]$RelativePath)
    $path = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure "$RelativePath is missing"
        return ""
    }
    return Get-Content -LiteralPath $path -Raw
}

function Assert-Contains {
    param(
        [string]$RelativePath,
        [string]$Text,
        [string]$Needle
    )
    if ($Text.Contains($Needle)) {
        Add-Pass "$RelativePath contains $Needle"
    } else {
        Add-Failure "$RelativePath must contain $Needle"
    }
}

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
    'const val TRADE_FAILURE_STEP: String = "wp.debug.failTradeStep"',
    'const val SHIP_CATALOG: String = "wp.debug.shipCatalog"',
    'const val SHIP_CATALOG_VIEW: String = "wp.debug.shipCatalogView"',
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

$stockConfig = Read-Text "src/main/kotlin/weaponsprocurement/config/StockReviewConfig.kt"
foreach ($needle in @(
    'CONFIG_PATH = CompatibilityIds.ConfigFiles.STOCK_REVIEW',
    'json.optJSONObject("desiredDefaults")',
    'json.optJSONObject("perWeapon")',
    'json.optJSONObject("perItem")',
    'optBoolean(sources, "includeCurrentMarketStorage", true)',
    'optBoolean(sources, "includeBlackMarket", true)'
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
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-jar-classes.ps1 -JarPath .\jars\enhanced-trading.jar -Label Repo'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-compatibility-surfaces.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-fixer-persistence-contracts.ps1'
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-trade-rollback-contracts.ps1'
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
