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

function Resolve-RepoPath {
    param([string]$RelativePath)
    Join-Path $repoRoot $RelativePath
}

function Read-Text {
    param([string]$RelativePath)
    $path = Resolve-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure "$RelativePath is missing"
        return ""
    }
    return Get-Content -LiteralPath $path -Raw
}

function Assert-Contains {
    param(
        [string]$Label,
        [string]$Text,
        [string]$Needle
    )
    if ($Text.Contains($Needle)) {
        Add-Pass "$Label contains $Needle"
    } else {
        Add-Failure "$Label missing $Needle"
    }
}

function Assert-Equal {
    param(
        [string]$Label,
        [object]$Actual,
        [object]$Expected
    )
    if ($Actual -eq $Expected) {
        Add-Pass "$Label is $Expected"
    } else {
        Add-Failure "$Label expected $Expected but was $Actual"
    }
}

function Assert-NumberRange {
    param(
        [string]$Label,
        [object]$Value,
        [double]$Min,
        [double]$Max
    )
    if ($null -eq $Value -or -not ($Value -is [int] -or $Value -is [long] -or $Value -is [double] -or $Value -is [decimal])) {
        Add-Failure "$Label must be numeric"
        return
    }
    $number = [double]$Value
    if ($number -lt $Min -or $number -gt $Max) {
        Add-Failure "$Label must be between $Min and $Max"
    } else {
        Add-Pass "$Label is within $Min..$Max"
    }
}

function Assert-Boolean {
    param(
        [string]$Label,
        [object]$Value
    )
    if ($Value -is [bool]) {
        Add-Pass "$Label is Boolean"
    } else {
        Add-Failure "$Label must be Boolean"
    }
}

function Assert-ObjectKeys {
    param(
        [string]$Label,
        [object]$Object,
        [string[]]$AllowedKeys
    )
    if ($null -eq $Object) {
        Add-Failure "$Label is missing"
        return
    }
    foreach ($key in $Object.PSObject.Properties.Name) {
        if ($AllowedKeys -notcontains $key) {
            Add-Failure "$Label has unsupported key '$key'"
        }
    }
    Add-Pass "$Label has only supported keys"
}

function Assert-ItemOverrideBlock {
    param(
        [string]$Label,
        [object]$Block,
        [bool]$RequireTypedKeys
    )
    if ($null -eq $Block) {
        Add-Pass "$Label is absent"
        return
    }
    foreach ($entry in $Block.PSObject.Properties) {
        $itemKey = $entry.Name
        if ($RequireTypedKeys -and $itemKey -notmatch '^(W|F):[^:]+$') {
            Add-Failure "$Label entry '$itemKey' must use W: or F: typed item key"
        }
        $config = $entry.Value
        foreach ($key in $config.PSObject.Properties.Name) {
            if (@("desired", "ignored") -notcontains $key) {
                Add-Failure "$Label entry '$itemKey' has unsupported key '$key'"
            }
        }
        if ($config.PSObject.Properties.Name -contains "desired") {
            Assert-NumberRange "$Label entry '$itemKey' desired" $config.desired 0 999
        }
        if ($config.PSObject.Properties.Name -contains "ignored") {
            Assert-Boolean "$Label entry '$itemKey' ignored" $config.ignored
        }
    }
    Add-Pass "$Label override block is valid"
}

function Assert-StringArray {
    param(
        [string]$Label,
        [object]$Object,
        [string]$PropertyName
    )
    if ($null -eq $Object -or -not $Object.PSObject.Properties.Name.Contains($PropertyName)) {
        Add-Failure "$Label missing $PropertyName"
        return
    }
    $values = @($Object.$PropertyName)
    foreach ($value in $values) {
        if ($value -is [string] -and -not [string]::IsNullOrWhiteSpace($value)) {
            Add-Pass "$PropertyName entry '$value' is a non-empty string"
        } else {
            Add-Failure "$PropertyName entries must be non-empty strings"
        }
    }
    Add-Pass "$PropertyName is a string list with $($values.Count) entr$(if ($values.Count -eq 1) { 'y' } else { 'ies' })"
}

$lunaPath = Resolve-RepoPath "data/config/LunaSettings.csv"
$lunaRows = @(Import-Csv -LiteralPath $lunaPath)
$lunaIds = @($lunaRows | ForEach-Object { $_.fieldID })
$duplicates = @($lunaIds | Group-Object | Where-Object { $_.Count -gt 1 } | ForEach-Object { $_.Name })
if ($duplicates.Count -gt 0) {
    Add-Failure "LunaSettings.csv duplicate fieldID(s): $($duplicates -join ', ')"
} else {
    Add-Pass "LunaSettings.csv fieldIDs are unique"
}

$expectedLuna = @{
    "wp_header" = @{ fieldType = "Header"; defaultValue = "Settings"; minValue = ""; maxValue = "" }
    "wp_update_interval_seconds" = @{ fieldType = "Double"; defaultValue = "0.20"; minValue = "0.05"; maxValue = "2.00" }
    "wp_trade_hotkey" = @{ fieldType = "Keycode"; defaultValue = "66"; minValue = ""; maxValue = "" }
    "wp_enable_dialog_option" = @{ fieldType = "Boolean"; defaultValue = "false"; minValue = ""; maxValue = "" }
    "wp_enable_sector_market" = @{ fieldType = "Boolean"; defaultValue = "true"; minValue = ""; maxValue = "" }
    "wp_enable_fixers_market" = @{ fieldType = "Boolean"; defaultValue = "true"; minValue = ""; maxValue = "" }
    "wp_enable_fixers_market_tag_inference" = @{ fieldType = "Boolean"; defaultValue = "false"; minValue = ""; maxValue = "" }
    "wp_sector_market_price_multiplier" = @{ fieldType = "Double"; defaultValue = "3.00"; minValue = "1.00"; maxValue = "20.00" }
    "wp_fixers_market_price_multiplier" = @{ fieldType = "Double"; defaultValue = "5.00"; minValue = "1.00"; maxValue = "20.00" }
    "wp_desired_small_weapon_count" = @{ fieldType = "Double"; defaultValue = "16"; minValue = "0"; maxValue = "999" }
    "wp_desired_medium_weapon_count" = @{ fieldType = "Double"; defaultValue = "8"; minValue = "0"; maxValue = "999" }
    "wp_desired_large_weapon_count" = @{ fieldType = "Double"; defaultValue = "4"; minValue = "0"; maxValue = "999" }
    "wp_desired_fighter_wing_count" = @{ fieldType = "Double"; defaultValue = "4"; minValue = "0"; maxValue = "999" }
    "wp_enable_debug_ui" = @{ fieldType = "Boolean"; defaultValue = "false"; minValue = ""; maxValue = "" }
}

foreach ($id in $lunaIds) {
    if (-not $expectedLuna.ContainsKey($id)) {
        Add-Failure "LunaSettings.csv has unexpected fieldID '$id'"
    }
}
foreach ($id in $expectedLuna.Keys) {
    $row = $lunaRows | Where-Object { $_.fieldID -eq $id } | Select-Object -First 1
    if ($null -eq $row) {
        Add-Failure "LunaSettings.csv missing fieldID '$id'"
        continue
    }
    foreach ($field in @("fieldType", "defaultValue", "minValue", "maxValue")) {
        Assert-Equal "Luna $id $field" $row.$field $expectedLuna[$id][$field]
    }
}
if (@($lunaIds | Where-Object { $_ -like "wp.debug.*" }).Count -gt 0) {
    Add-Failure "Developer debug JVM properties must not be exposed as Luna settings"
} else {
    Add-Pass "Developer debug JVM properties are not Luna settings"
}

$compatibilitySource = Read-Text "src/main/kotlin/weaponsprocurement/CompatibilityIds.kt"
$lunaBlockMatch = [regex]::Match($compatibilitySource, '(?s)object Luna \{(.*?)\n    \}')
if (-not $lunaBlockMatch.Success) {
    Add-Failure "CompatibilityIds.kt missing Luna compatibility block"
    $sourceSettingIds = @()
} else {
    $sourceSettingIds = @([regex]::Matches($lunaBlockMatch.Groups[1].Value, 'const val [A-Z0-9_]+: String = "([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
}
$settingsSource = Read-Text "src/main/kotlin/weaponsprocurement/config/WeaponsProcurementConfig.kt"
foreach ($id in @(
    "SETTING_UPDATE_INTERVAL",
    "SETTING_ENABLE_DIALOG_OPTION",
    "SETTING_ENABLE_SECTOR_MARKET",
    "SETTING_ENABLE_FIXERS_MARKET",
    "SETTING_ENABLE_FIXERS_MARKET_TAG_INFERENCE",
    "SETTING_SECTOR_MARKET_PRICE_MULTIPLIER",
    "SETTING_FIXERS_MARKET_PRICE_MULTIPLIER",
    "SETTING_DESIRED_SMALL_WEAPON_COUNT",
    "SETTING_DESIRED_MEDIUM_WEAPON_COUNT",
    "SETTING_DESIRED_LARGE_WEAPON_COUNT",
    "SETTING_DESIRED_FIGHTER_WING_COUNT",
    "SETTING_TRADE_HOTKEY",
    "SETTING_ENABLE_DEBUG_UI"
)) {
    if ($settingsSource.Contains("$id = CompatibilityIds.Luna.")) {
        Add-Pass "WeaponsProcurementConfig $id reads CompatibilityIds.Luna"
    } else {
        Add-Failure "WeaponsProcurementConfig $id must read CompatibilityIds.Luna"
    }
}
$lunaSettingIds = @($lunaIds | Where-Object { $_ -ne "wp_header" } | Sort-Object -Unique)
foreach ($id in $sourceSettingIds) {
    if ($lunaSettingIds -notcontains $id) {
        Add-Failure "WeaponsProcurementConfig setting '$id' is not declared in LunaSettings.csv"
    }
}
foreach ($id in $lunaSettingIds) {
    if ($sourceSettingIds -notcontains $id) {
        Add-Failure "LunaSettings.csv field '$id' is not read by WeaponsProcurementConfig"
    }
}
Add-Pass "WeaponsProcurementConfig setting constants match LunaSettings.csv"

$stockJson = (Read-Text "data/config/enhanced_trading_stock.json") | ConvertFrom-Json
Assert-ObjectKeys "enhanced_trading_stock.json root" $stockJson @("display", "sources", "desiredDefaults", "perItem", "perWeapon")
Assert-ObjectKeys "enhanced_trading_stock.json display" $stockJson.display @("defaultSort")
if (@("NEED", "NAME", "PRICE", "RARITY", "COST", "PURCHASABLE", "FOR_SALE", "OWNED") -contains $stockJson.display.defaultSort) {
    Add-Pass "display.defaultSort is accepted by StockSortMode.fromConfig"
} else {
    Add-Failure "display.defaultSort '$($stockJson.display.defaultSort)' is not an accepted sort token"
}
Assert-ObjectKeys "enhanced_trading_stock.json sources" $stockJson.sources @("includeCurrentMarketStorage", "includeBlackMarket")
Assert-Boolean "sources.includeCurrentMarketStorage" $stockJson.sources.includeCurrentMarketStorage
Assert-Boolean "sources.includeBlackMarket" $stockJson.sources.includeBlackMarket
Assert-ObjectKeys "enhanced_trading_stock.json desiredDefaults" $stockJson.desiredDefaults @("smallWeapon", "mediumWeapon", "largeWeapon", "fighterWing")
Assert-NumberRange "desiredDefaults.smallWeapon" $stockJson.desiredDefaults.smallWeapon 0 999
Assert-NumberRange "desiredDefaults.mediumWeapon" $stockJson.desiredDefaults.mediumWeapon 0 999
Assert-NumberRange "desiredDefaults.largeWeapon" $stockJson.desiredDefaults.largeWeapon 0 999
Assert-NumberRange "desiredDefaults.fighterWing" $stockJson.desiredDefaults.fighterWing 0 999
Assert-ItemOverrideBlock "perItem" $stockJson.perItem $true
Assert-ItemOverrideBlock "perWeapon" $stockJson.perWeapon $false

$blacklistJson = (Read-Text "data/config/enhanced_trading_market_blacklist.json") | ConvertFrom-Json
Assert-ObjectKeys "enhanced_trading_market_blacklist.json root" $blacklistJson @("BANNED_FROM_SECTOR_MARKET", "BANNED_FROM_FIXERS_MARKET")
Assert-StringArray "enhanced_trading_market_blacklist.json" $blacklistJson "BANNED_FROM_SECTOR_MARKET"
Assert-StringArray "enhanced_trading_market_blacklist.json" $blacklistJson "BANNED_FROM_FIXERS_MARKET"

$stockItemTypeSource = Read-Text "src/main/kotlin/weaponsprocurement/stock/item/StockItemType.kt"
foreach ($needle in @(
    'WEAPON("Weapons", "Weapon", CompatibilityIds.StockItemKeys.WEAPON_PREFIX)',
    'WING("Wings", "Wing", CompatibilityIds.StockItemKeys.WING_PREFIX)',
    'fun key(itemId: String?): String = keyPrefix + (itemId ?: "")',
    'itemKey != null && itemKey.startsWith(WING.keyPrefix)',
    'if (itemKey == null) return null',
    'itemKey.substring(type.keyPrefix.length)',
    'return itemKey'
)) {
    Assert-Contains "StockItemType item-key contract" $stockItemTypeSource $needle
}

$blacklistSource = Read-Text "src/main/kotlin/weaponsprocurement/config/WeaponMarketBlacklist.kt"
foreach ($needle in @(
    'private const val CONFIG_PATH = CompatibilityIds.ConfigFiles.MARKET_BLACKLIST',
    'private const val SECTOR_KEY = CompatibilityIds.MarketBlacklist.SECTOR_KEY',
    'private const val FIXERS_KEY = CompatibilityIds.MarketBlacklist.FIXERS_KEY',
    'val rawId = StockItemType.rawId(itemKey)',
    'set.contains(normalize(itemKey)) || set.contains(normalize(rawId))',
    'val displayName = displayName(itemKey, rawId)',
    'set.contains(normalize(displayName))',
    'StockItemSpecs.wingSpec(rawId)?.wingName',
    'StockItemSpecs.weaponSpec(rawId)?.weaponName',
    'trimmed.lowercase(Locale.US)'
)) {
    Assert-Contains "WeaponMarketBlacklist matching contract" $blacklistSource $needle
}

$sortModeSource = Read-Text "src/main/kotlin/weaponsprocurement/stock/item/StockSortMode.kt"
foreach ($alias in @('"COST"', '"PURCHASABLE"', '"FOR_SALE"', '"OWNED"')) {
    if ($sortModeSource.Contains($alias)) {
        Add-Pass "StockSortMode keeps legacy alias $alias"
    } else {
        Add-Failure "StockSortMode must keep legacy alias $alias"
    }
}

$tradeMoneySource = Read-Text "src/main/kotlin/weaponsprocurement/trade/plan/TradeMoney.kt"
foreach ($needle in @(
    "MAX_EXECUTABLE_CREDITS: Long = 2147483647L",
    "if (unitPrice < 0 || quantity < 0) return -1L",
    "return credits in 0L..MAX_EXECUTABLE_CREDITS"
)) {
    if ($tradeMoneySource.Contains($needle)) {
        Add-Pass "TradeMoney contract contains $needle"
    } else {
        Add-Failure "TradeMoney contract missing $needle"
    }
}

if ($failures.Count -gt 0) {
    throw "Config contract validation failed with $($failures.Count) failure(s)."
}

Write-Host "Config contract validation passed."
