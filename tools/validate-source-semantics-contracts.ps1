param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

$compatibilityIds = Read-Text "src/main/kotlin/weaponsprocurement/CompatibilityIds.kt"
Assert-Contains "CompatibilityIds.kt" $compatibilityIds 'const val FIXERS_MARKET_SUBMARKET_ID: String = "wp_fixers_market"'

$sourceMode = Read-Text "src/main/kotlin/weaponsprocurement/stock/item/StockSourceMode.kt"
foreach ($needle in @(
    'LOCAL("Local", false)',
    'SECTOR("Sector Market", true)',
    'FIXERS("Fixer''s Market", true)',
    'fun isRemote(): Boolean = remote',
    'fun supportsBlackMarketToggle(): Boolean = this != FIXERS',
    'if (this == SECTOR) return WeaponsProcurementConfig.isSectorMarketEnabled()',
    'if (this == FIXERS) return WeaponsProcurementConfig.isFixersMarketEnabled()',
    'return true'
)) {
    Assert-Contains "StockSourceMode.kt source-mode contract" $sourceMode $needle
}

$snapshot = Read-Text "src/main/kotlin/weaponsprocurement/stock/item/WeaponStockSnapshot.kt"
Assert-Contains "WeaponStockSnapshot.kt" $snapshot 'private val sourceMode: StockSourceMode = sourceMode ?: StockSourceMode.LOCAL'
Assert-Contains "WeaponStockSnapshot.kt" $snapshot 'if (StockSourceMode.FIXERS == sourceMode) return GlobalWeaponMarketService.FIXERS_MARKET_NAME'
Assert-Contains "WeaponStockSnapshot.kt" $snapshot 'if (StockSourceMode.SECTOR == sourceMode) return GlobalWeaponMarketService.SECTOR_MARKET_NAME'

$snapshotBuilder = Read-Text "src/main/kotlin/weaponsprocurement/stock/item/WeaponStockSnapshotBuilder.kt"
Assert-Contains "WeaponStockSnapshotBuilder.kt" $snapshotBuilder 'if (!resolvedSourceMode.isEnabled)'
Assert-Contains "WeaponStockSnapshotBuilder.kt" $snapshotBuilder 'resolvedSourceMode = StockSourceMode.LOCAL'
Assert-Contains "WeaponStockSnapshotBuilder.kt" $snapshotBuilder 'if (StockSourceMode.FIXERS == resolvedSourceMode) false else includeBlackMarket'
Assert-Order "WeaponStockSnapshotBuilder marketStock dispatch" $snapshotBuilder @(
    'if (StockSourceMode.FIXERS == sourceMode) {',
    'return globalWeaponMarketService.collectFixersWeaponStock(sector)',
    'if (StockSourceMode.SECTOR == sourceMode) {',
    'return globalWeaponMarketService.collectSectorWeaponStock(sector, includeBlackMarket)',
    'return marketStockService.collectCurrentMarketItemStock(market, includeBlackMarket)'
)

$globalMarket = Read-Text "src/main/kotlin/weaponsprocurement/stock/market/GlobalWeaponMarketService.kt"
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'val VIRTUAL_SUBMARKET_ID: String = CompatibilityIds.Markets.FIXERS_MARKET_SUBMARKET_ID'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'val SECTOR_MARKET_NAME: String = "Sector Market"'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'val FIXERS_MARKET_NAME: String = "Fixer''s Market"'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'val VIRTUAL_STOCK: Int = 999'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'WeaponsProcurementConfig.sectorMarketPriceMultiplier()'
Assert-Contains "GlobalWeaponMarketService.kt" $globalMarket 'WeaponsProcurementConfig.fixersMarketPriceMultiplier()'
$sectorSection = Get-Section $globalMarket 'private fun buildSectorWeaponStock(' 'private fun buildFixersWeaponStock('
foreach ($needle in @(
    'WeaponMarketBlacklist.load()',
    'marketStockService.collectCurrentMarketItemStock(market, includeBlackMarket)',
    'if (blacklist.isBannedFromSector(itemKey)) continue',
    'source.marketId',
    'source.marketName',
    'source.submarketId',
    'source.submarketName',
    'markedUpPrice(source.baseUnitPrice, priceMultiplier)',
    'source.baseUnitPrice',
    'source.isPurchasable()'
)) {
    Assert-Contains "GlobalWeaponMarketService.kt Sector Market contract" $sectorSection $needle
}
$fixerSection = Get-Section $globalMarket 'private fun buildFixersWeaponStock(' 'private fun addLiveObservedReferences('
foreach ($needle in @(
    'val persistentObserved = observedCatalog.observedItems(sector, blacklist)',
    'addLiveObservedReferences(sector, references, blacklist)',
    'addTheoreticalCandidates(sector, references, blacklist, persistentObserved)',
    'VIRTUAL_SUBMARKET_ID',
    'FIXERS_MARKET_NAME',
    'VIRTUAL_STOCK',
    'markedUpPrice(reference.baseUnitPrice, priceMultiplier)',
    'reference.baseUnitPrice',
    'reference.unitCargoSpace',
    'true',
    'FixerCatalogMetadata.create(reference.rarity, reference.source)'
)) {
    Assert-Contains "GlobalWeaponMarketService.kt Fixer Market contract" $fixerSection $needle
}
Assert-NotContains "GlobalWeaponMarketService.kt Fixer Market contract" $fixerSection 'source.marketId'
Assert-NotContains "GlobalWeaponMarketService.kt Fixer Market contract" $fixerSection 'source.submarketId'

$marketStock = Read-Text "src/main/kotlin/weaponsprocurement/stock/market/MarketStockService.kt"
foreach ($needle in @(
    'val builder = MarketStockBuilder()',
    'StockSubmarketAccess.isTradeEligible(submarket, includeBlackMarket)',
    'StockSubmarketAccess.isNonTradeSubmarket(submarketId)',
    'Submarkets.SUBMARKET_BLACK == submarketId',
    'StockItemStacks.isPurchasableItemStack(submarket, stack, itemType)',
    'builder.add(',
    'return builder.build()',
    'purchasableTotals(byItemKey)'
)) {
    Assert-Contains "MarketStockService.kt real cargo stock contract" $marketStock $needle
}
foreach ($needle in @(
    'val metadata = stock.getFixerCatalogMetadata(id)',
    'add(id, sources[i], metadata)'
)) {
    Assert-Contains "MarketStockService.kt builder merge metadata contract" $marketStock $needle
}

$submarketAccess = Read-Text "src/main/kotlin/weaponsprocurement/stock/market/StockSubmarketAccess.kt"
foreach ($needle in @(
    'if (isNonTradeSubmarket(id)) return false',
    'if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK == id) return false',
    'if (submarket.cargoNullOk == null) return false',
    'Submarkets.SUBMARKET_STORAGE == submarketId',
    'Submarkets.LOCAL_RESOURCES == submarketId',
    '!plugin.isHidden && plugin.isEnabled(OpenMarketCoreUi)'
)) {
    Assert-Contains "StockSubmarketAccess.kt trade-eligible source contract" $submarketAccess $needle
}

$marketSources = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockPurchaseMarketSources.kt"
$localSources = Get-Section $marketSources 'fun collectLocalSources(' 'fun collectSectorSources('
foreach ($needle in @(
    'MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)',
    'val cargo = submarket.cargoNullOk ?: continue',
    'StockItemStacks.isPurchasableItemStack(submarket, stack, itemType)',
    'StockPurchaseSource(',
    'submarket,',
    'cargo,',
    'StockItemStacks.unitPrice(submarket, stack)',
    'StockItemStacks.baseUnitPrice(stack)'
)) {
    Assert-Contains "StockPurchaseMarketSources.kt local source contract" $localSources $needle
}
$sectorSources = Get-Section $marketSources 'fun collectSectorSources(' 'fun sellTarget('
foreach ($needle in @(
    'if (!stock.isPurchasable() || stock.count <= 0) continue',
    'val market = findMarket(sector, stock.marketId)',
    'val submarket = market?.getSubmarket(stock.submarketId)',
    'if (!MarketStockService.isTradeSubmarket(submarket, true)) continue',
    'val cargo = submarket?.cargoNullOk',
    'val stack = StockItemCargo.itemStack(cargo, itemType, itemId)',
    'val liveAvailable = if (stack == null) 0 else Math.round(stack.size)',
    'val available = Math.min(stock.count, liveAvailable)',
    'StockPurchaseSource(',
    'market,',
    'submarket,',
    'cargo,',
    'stock.unitPrice',
    'stock.baseUnitPrice',
    'StockItemStacks.unitCargoSpace(stack)'
)) {
    Assert-Contains "StockPurchaseMarketSources.kt sector source rehydration contract" $sectorSources $needle
}

$purchaseService = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockPurchaseService.kt"
Assert-Contains "StockPurchaseService.kt" $purchaseService 'return StockPurchaseExecutor.buyFromFixersMarket(LOG, cargo, itemType, checkedItemId, requestedQuantity, totalCost)'
Assert-Contains "StockPurchaseService.kt" $purchaseService 'val sources = StockPurchaseMarketSources.collectSectorSources(sector, itemType, checkedItemId, stockSources)'
Assert-Contains "StockPurchaseService.kt" $purchaseService '"No sector-market stock is available."'
Assert-Contains "StockPurchaseService.kt" $purchaseService '"Sector-market stock changed before confirmation. Reopen the review and try again."'
Assert-Contains "StockPurchaseService.kt" $purchaseService '" from the sector market"'
Assert-Contains "StockPurchaseService.kt" $purchaseService '"buy from sector market"'
Assert-Contains "StockPurchaseService.kt" $purchaseService 'val sources = StockPurchaseMarketSources.collectLocalSources('
Assert-Contains "StockPurchaseService.kt" $purchaseService '"buy from local market"'

$executor = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockPurchaseExecutor.kt"
$fixerExecutor = Get-Section $executor 'fun buyFromFixersMarket(' 'fun buyPlan('
Assert-Contains "StockPurchaseExecutor.kt Fixer execution contract" $fixerExecutor 'StockItemCargo.addItem(playerCargo, itemType, itemId, quantity)'
Assert-Contains "StockPurchaseExecutor.kt Fixer execution contract" $fixerExecutor 'playerCargo.credits.subtract(totalCost.toFloat())'
Assert-Contains "StockPurchaseExecutor.kt Fixer execution contract" $fixerExecutor '" from the fixer''s market for "'
Assert-NotContains "StockPurchaseExecutor.kt Fixer execution contract" $fixerExecutor 'StockItemCargo.removeItem(sourceCargo'
Assert-NotContains "StockPurchaseExecutor.kt Fixer execution contract" $fixerExecutor 'StockMarketTransactionReporter.reportItemTransaction'
$buyPlanExecutor = Get-Section $executor 'fun buyPlan(' 'private fun buyPlanStillAvailable('
Assert-Contains "StockPurchaseExecutor.kt real-source buy contract" $buyPlanExecutor 'StockItemCargo.removeItem(sourceCargo, itemType, itemId, line.quantity)'
Assert-Contains "StockPurchaseExecutor.kt real-source buy contract" $buyPlanExecutor 'StockItemCargo.addItem(playerCargo, itemType, itemId, checkedPlan.totalQuantity)'
Assert-Contains "StockPurchaseExecutor.kt real-source buy contract" $buyPlanExecutor 'flushTransactionReports(log, reportLines)'

$executionController = Read-Text "src/main/kotlin/weaponsprocurement/ui/stockreview/trade/StockReviewExecutionController.kt"
foreach ($needle in @(
    'val sourceMode = snapshot?.getSourceMode() ?: StockSourceMode.LOCAL',
    'if (trade.isSell()) {',
    'if (sourceMode != null && sourceMode.isRemote())',
    'return purchaseService.sellItemToMarket(sector, market, record.itemType, record.itemId, -trade.quantity, false)',
    'if (StockSourceMode.FIXERS == sourceMode) {',
    'return purchaseService.buyItemFromFixersMarket(',
    'virtualUnitPrice(trade.itemKey)',
    'virtualUnitCargoSpace(trade.itemKey)',
    'if (StockSourceMode.SECTOR == sourceMode) {',
    'return purchaseService.buyItemFromSectorSources(',
    'stockSources(trade.itemKey, trade.submarketId)',
    'return purchaseService.buyItemFromLocalSource(',
    'return purchaseService.buyCheapestItem('
)) {
    Assert-Contains "StockReviewExecutionController.kt source dispatch contract" $executionController $needle
}

$rebalancer = Read-Text "src/main/kotlin/weaponsprocurement/ui/stockreview/trade/StockReviewLocalMarketRebalancer.kt"
Assert-Contains "StockReviewLocalMarketRebalancer.kt" $rebalancer 'previousSnapshot?.getSourceMode() != StockSourceMode.LOCAL'
Assert-Contains "StockReviewLocalMarketRebalancer.kt" $rebalancer 'currentSnapshot?.getSourceMode() != StockSourceMode.LOCAL'
Assert-Contains "StockReviewLocalMarketRebalancer.kt" $rebalancer 'return copyTrades(previousTrades)'

if ($failures.Count -gt 0) {
    throw "Source semantics contract validation failed with $($failures.Count) failure(s)."
}

Write-Host "Source semantics contract validation passed."
