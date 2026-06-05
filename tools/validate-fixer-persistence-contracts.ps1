param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

function Assert-PersistentCatalogAssignments {
    param([string]$Content)
    $assignments = @([regex]::Matches($Content, '(?:\b\w+\.)?persistentData\[PERSISTENT_KEY\]\s*=\s*([^\r\n]+)'))
    if ($assignments.Count -eq 0) {
        Add-Failure "FixerMarketObservedCatalog.kt has no persistentData[PERSISTENT_KEY] assignments"
        return
    }
    foreach ($assignment in $assignments) {
        $rightHandSide = $assignment.Groups[1].Value.Trim()
        if ($rightHandSide -ne "catalog") {
            Add-Failure "FixerMarketObservedCatalog.kt writes persistentData[PERSISTENT_KEY] to '$rightHandSide' instead of catalog"
        }
    }
    if ($assignments.Count -ne 2) {
        Add-Failure "FixerMarketObservedCatalog.kt expected 2 PERSISTENT_KEY assignment sites but found $($assignments.Count)"
    } else {
        Add-Pass "FixerMarketObservedCatalog.kt keeps exactly 2 PERSISTENT_KEY assignment sites"
    }
    Add-Pass "FixerMarketObservedCatalog.kt writes only sanitized catalog maps to PERSISTENT_KEY"
}

$compatibilityIds = Read-Text "src/main/kotlin/weaponsprocurement/CompatibilityIds.kt"
Assert-Contains "CompatibilityIds.kt" $compatibilityIds 'const val FIXER_OBSERVED_CATALOG_KEY: String = "weaponsProcurement.fixerObservedCatalog.v1"'
Assert-Contains "CompatibilityIds.kt" $compatibilityIds 'const val FIXER_OBSERVED_CATALOG_VALUE_SEPARATOR: String = "|"'

$catalog = Read-Text "src/main/kotlin/weaponsprocurement/stock/fixer/FixerMarketObservedCatalog.kt"
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'PERSISTENT_KEY = CompatibilityIds.Persistence.FIXER_OBSERVED_CATALOG_KEY'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'VALUE_SEPARATOR = CompatibilityIds.Persistence.FIXER_OBSERVED_CATALOG_VALUE_SEPARATOR'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'existing is Map<*, *>'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'existing is MutableMap<*, *> && containsOnlyStringEntries(existing)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'return existing as MutableMap<String, String>'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'sanitizedCatalog(sector, existing)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'val catalog = HashMap<String, String>()'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'persistentData[PERSISTENT_KEY] = catalog'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'sector.persistentData[PERSISTENT_KEY] = catalog'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'if (key is String && value is String)'
Assert-PersistentCatalogAssignments $catalog
Assert-NotMatch "FixerMarketObservedCatalog.kt" $catalog '\bdata\s+class\s+ObservedItem\b'
Assert-NotMatch "FixerMarketObservedCatalog.kt" $catalog '\bSerializable\b'

Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'FixerCatalogPolicy.isEligibleObservedItem(itemKey, blacklist)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'FixerCatalogPolicy.isSafeItem(itemKey)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'private fun decodePersistentItem(itemKey: String?, encoded: String?): ObservedItem?'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'if (!FixerCatalogPolicy.isSafeItem(itemKey)) return null'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'return decode(encoded)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'if (!FixerCatalogPolicy.isBanned(blacklist, itemKey))'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'Collections.unmodifiableMap(result)'

Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'Math.max(0, baseUnitPrice)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'sanitizeUnitCargoSpace(unitCargoSpace)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'split(VALUE_SEPARATOR, limit = 2)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'parts[0].trim().toInt()'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'parts[1].trim().toFloat()'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'if (!isFinite(unitCargoSpace)) return null'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'Math.max(0.01f, unitCargoSpace)'
Assert-Contains "FixerMarketObservedCatalog.kt" $catalog 'else 1f'

$policy = Read-Text "src/main/kotlin/weaponsprocurement/stock/fixer/FixerCatalogPolicy.kt"
Assert-Contains "FixerCatalogPolicy.kt" $policy 'fun isEligibleObservedItem(itemKey: String?, blacklist: WeaponMarketBlacklist?): Boolean'
Assert-Contains "FixerCatalogPolicy.kt" $policy 'return isSafeItem(itemKey) && !isBanned(blacklist, itemKey)'
Assert-Contains "FixerCatalogPolicy.kt" $policy 'fun isBanned(blacklist: WeaponMarketBlacklist?, itemKey: String?): Boolean'
Assert-Contains "FixerCatalogPolicy.kt" $policy 'blacklist.isBannedFromFixers(itemKey)'
Assert-Contains "FixerCatalogPolicy.kt" $policy 'fun isEligibleTheoreticalItem('
Assert-Contains "FixerCatalogPolicy.kt" $policy 'return militarySubmarket || !hasTag(itemKey, MILITARY_MARKET_ONLY_TAG)'
Assert-Contains "FixerCatalogPolicy.kt" $policy 'private const val MILITARY_MARKET_ONLY_TAG = "military_market_only"'
foreach ($tag in @(
    '"restricted"',
    '"no_dealer"',
    '"no_drop"',
    '"no_bp_drop"',
    '"omega"',
    '"remnant"',
    '"dweller"',
    '"threat"',
    '"hide_in_codex"',
    '"invisible_in_codex"',
    '"codex_unlockable"'
)) {
    Assert-Contains "FixerCatalogPolicy.kt spoiler denylist" $policy $tag
}

$updater = Read-Text "src/main/kotlin/weaponsprocurement/lifecycle/WeaponsProcurementFixerCatalogUpdater.kt"
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'class WeaponsProcurementFixerCatalogUpdater : EveryFrameScript'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'override fun runWhilePaused(): Boolean = true'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'if (!WeaponsProcurementConfig.isFixersMarketEnabled())'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'val added = catalog.observeSectorStock(sector, WeaponMarketBlacklist.load())'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'lastSuccessfulScanTimestamp = clock.timestamp'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'lastFailedScanTimestamp = clock.timestamp'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'clock.getElapsedDaysSince(lastSuccessfulScanTimestamp) < SCAN_INTERVAL_DAYS'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'clock.getElapsedDaysSince(lastFailedScanTimestamp) < FAILURE_RETRY_INTERVAL_DAYS'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'LOG.error("WP_FIXER_CATALOG scan failed", t)'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'private const val SCAN_INTERVAL_DAYS = 1f'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'private const val FAILURE_RETRY_INTERVAL_DAYS = 0.05f'
Assert-Contains "WeaponsProcurementFixerCatalogUpdater.kt" $updater 'private const val MAX_SCAN_LOGS = 10'

$globalMarket = Read-Text "src/main/kotlin/weaponsprocurement/stock/market/GlobalWeaponMarketService.kt"
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'val persistentObserved = observedCatalog.observedItems(sector, blacklist)'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'addTheoreticalCandidates(sector, references, blacklist, persistentObserved)'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'persistentObserved: Map<String, FixerMarketObservedCatalog.ObservedItem>'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'val persistent = persistentObserved[itemKey]'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'FixerCatalogSource.FACTION_CATALOG_OBSERVED_REFERENCE'

$docs = Read-Text "docs/CODEBASE_QUALITY_MODERNIZATION_PLAN.md"
Assert-Contains "CODEBASE_QUALITY_MODERNIZATION_PLAN.md" $docs 'Save/custom data: `weaponsProcurement.fixerObservedCatalog.v1`'
Assert-Contains "CODEBASE_QUALITY_MODERNIZATION_PLAN.md" $docs 'Fixer persistence'

$checks = Read-Text "docs/CHECKS.md"
Assert-Contains "CHECKS.md" $checks 'validate-fixer-persistence-contracts.ps1'

$sanity = Read-Text ".github/workflows/sanity.yml"
Assert-Contains ".github/workflows/sanity.yml" $sanity 'validate-fixer-persistence-contracts.ps1'

if ($failures.Count -gt 0) {
    throw "Fixer persistence contract validation failed with $($failures.Count) failure(s)."
}

Write-Host "Fixer persistence contract validation passed."
