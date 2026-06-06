param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

$steps = @(
    "after-source-removal",
    "after-player-cargo-remove",
    "after-player-cargo-add",
    "after-target-cargo-add",
    "after-credit-mutation"
)

$compatibilityIds = Read-Text "src/main/kotlin/weaponsprocurement/CompatibilityIds.kt"
Assert-Contains "CompatibilityIds.kt" $compatibilityIds 'const val TRADE_FAILURE_STEP: String = "wp.debug.failTradeStep"'

$config = Read-Text "src/main/kotlin/weaponsprocurement/config/WeaponsProcurementConfig.kt"
Assert-Contains "WeaponsProcurementConfig.kt" $config 'const val KEY_DEBUG_TRADE_FAILURE_STEP: String = CompatibilityIds.Diagnostics.TRADE_FAILURE_STEP'
Assert-Contains "WeaponsProcurementConfig.kt" $config 'System.setProperty(KEY_DEBUG_TRADE_FAILURE_STEP, debugTradeFailureStep)'
Assert-Contains "WeaponsProcurementConfig.kt" $config 'fun debugTradeFailureStep(): String = normalizeDebugTradeFailureStep(System.getProperty(KEY_DEBUG_TRADE_FAILURE_STEP, ""))'
Assert-Contains "WeaponsProcurementConfig.kt" $config 'if (value.isEmpty() || value.equals("none", ignoreCase = true))'
Assert-Contains "WeaponsProcurementConfig.kt" $config 'if (value == "*")'
foreach ($step in $steps) {
    Assert-Contains "WeaponsProcurementConfig.kt accepted failure step" $config "`"$step`""
}
Assert-Contains "WeaponsProcurementConfig.kt" $config 'LOG.warn("WP_CONFIG ignored unknown debug trade failure step: $value")'

$luna = Read-Text "data/config/LunaSettings.csv"
if ($luna.Contains("wp.debug.failTradeStep")) {
    Add-Failure "LunaSettings.csv must not expose wp.debug.failTradeStep"
} else {
    Add-Pass "LunaSettings.csv does not expose wp.debug.failTradeStep"
}

$executor = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockPurchaseExecutor.kt"
$rollbackJournal = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockPurchaseRollbackJournal.kt"
$transactionReporter = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockMarketTransactionReporter.kt"
foreach ($step in $steps) {
    $constantName = "FAIL_" + $step.ToUpperInvariant().Replace("-", "_")
    Assert-Contains "StockPurchaseExecutor.kt failure constant" $executor "private const val $constantName = `"$step`""
}
Assert-RegexCount "StockPurchaseExecutor.kt maybeFail call sites" $executor '\bmaybeFail\(FAIL_' 8
Assert-Contains "StockPurchaseExecutor.kt" $executor 'val requested = WeaponsProcurementConfig.debugTradeFailureStep()'
Assert-Contains "StockPurchaseExecutor.kt" $executor 'if (step.equals(requested, ignoreCase = true) || "*" == requested)'
Assert-Contains "StockPurchaseExecutor.kt" $executor 'throw RuntimeException("WP debug forced trade failure at $step")'

$sellToMarket = Get-Section $executor 'fun sellToMarket(' 'fun buyFromFixersMarket('
$buyFromFixersMarket = Get-Section $executor 'fun buyFromFixersMarket(' 'fun buyPlan('
$buyPlan = Get-Section $executor 'fun buyPlan(' 'private fun buyPlanStillAvailable('

Assert-Order "sellToMarket rollback sequence" $sellToMarket @(
    'journal.recordCargo(playerCargo, "player cargo")',
    'journal.recordCargo(target.cargo, "sell target " + marketLabel(market, target.submarket))',
    'StockItemCargo.removeItem(playerCargo, itemType, itemId, quantity)',
    'maybeFail(FAIL_AFTER_PLAYER_CARGO_REMOVE)',
    'playerCargo.credits.add(credits.toFloat())',
    'maybeFail(FAIL_AFTER_CREDIT_MUTATION)',
    'StockItemCargo.addItem(target.cargo, itemType, itemId, quantity)',
    'maybeFail(FAIL_AFTER_TARGET_CARGO_ADD)',
    'StockMarketTransactionReporter.reportItemTransaction('
)

Assert-Order "buyFromFixersMarket rollback sequence" $buyFromFixersMarket @(
    'journal.recordCargo(playerCargo, "player cargo")',
    'StockItemCargo.addItem(playerCargo, itemType, itemId, quantity)',
    'maybeFail(FAIL_AFTER_PLAYER_CARGO_ADD)',
    'playerCargo.credits.subtract(totalCost.toFloat())',
    'maybeFail(FAIL_AFTER_CREDIT_MUTATION)',
    'StockPurchaseChecks.addCampaignMessage(message)'
)

Assert-Order "buyPlan rollback sequence" $buyPlan @(
    'journal.recordCargo(playerCargo, "player cargo")',
    'journal.recordCargo(line.source.cargo, "buy source " + sourceLabel(line.source, fallbackMarket))',
    'StockItemCargo.removeItem(sourceCargo, itemType, itemId, line.quantity)',
    'maybeFail(FAIL_AFTER_SOURCE_REMOVAL)',
    'reportLines.add(',
    'StockItemCargo.addItem(playerCargo, itemType, itemId, checkedPlan.totalQuantity)',
    'maybeFail(FAIL_AFTER_PLAYER_CARGO_ADD)',
    'playerCargo.credits.subtract(checkedPlan.totalCost.toFloat())',
    'maybeFail(FAIL_AFTER_CREDIT_MUTATION)',
    'flushTransactionReports(log, reportLines)'
)

foreach ($needle in @(
    'journal?.rollback(itemType, itemId) ?: RollbackReport.none()',
    '"WP_STOCK_REVIEW_ROLLBACK status=" + report.status',
    '" failedStep=" + failureStepToken()',
    '" restoredCargos=" + report.restoredCargos',
    '" failedCargos=" + report.failedCargos',
    '" creditsRestored=" + report.creditsRestored',
    '" countsRestored=" + report.countsRestored',
    '" creditsBefore=" + formatFloat(report.creditsBefore)',
    '" creditsAtFailure=" + formatFloat(report.creditsAtFailure)',
    '" creditsAfterRollback=" + formatFloat(report.creditsAfterRollback)',
    '" touched=" + report.touchedSummary()',
    'for (ch in raw)'
)) {
    Assert-Contains "StockPurchaseExecutor.kt rollback contract" $executor $needle
}

foreach ($needle in @(
    'internal class MutationJournal(',
    'fun recordCargo(cargo: CargoAPI?, label: String)',
    'fun rollback(itemType: StockItemType, itemId: String): RollbackReport',
    'StockItemCargo.reconcileItemCount(snapshot.cargo, itemType, itemId, snapshot.itemCountBefore)',
    'playerCargo.credits.set(creditsBefore)',
    'get() = if (failedCargos == 0 && creditsRestored && countsRestored) "PASS" else "FAIL"',
    'fun touchedSummary(): String',
    'for (ch in label)'
)) {
    Assert-Contains "StockPurchaseRollbackJournal.kt rollback contract" $rollbackJournal $needle
}

$checks = Read-Text "src/main/kotlin/weaponsprocurement/trade/execution/StockPurchaseChecks.kt"
foreach ($needle in @(
    'fun canMutateCredits(credits: Long): StockPurchaseService.PurchaseResult?',
    'TradeMoney.canExecuteCreditMutation(credits)',
    'fun canCompletePurchase(',
    'if (totalSpace < 0f || totalSpace.isNaN() || totalSpace.isInfinite())',
    'StockPurchaseService.PurchaseResult.failure("Order cargo space is invalid.")',
    'validation = canAfford(playerCargo, totalCost)',
    'return validation ?: hasCargoSpace(playerCargo, totalSpace)'
)) {
    Assert-Contains "StockPurchaseChecks.kt credit/cargo guard" $checks $needle
}

$plan = Read-Text "src/main/kotlin/weaponsprocurement/trade/plan/StockPurchasePlan.kt"
foreach ($needle in @(
    'val lineCost = TradeMoney.lineTotal(source.unitPrice, quantity)',
    'if (lineCost < 0L) continue',
    'val lineSpace = source.unitCargoSpace * quantity',
    'if (lineSpace < 0f || lineSpace.isNaN() || lineSpace.isInfinite()) continue',
    'lines.add(StockPurchaseLine(source, quantity))',
    'remaining -= quantity',
    'totalCost = TradeMoney.safeAdd(totalCost, lineCost)',
    'totalSpace += lineSpace'
)) {
    Assert-Contains "StockPurchasePlan.kt invalid plan-source guard" $plan $needle
}
Assert-Order "StockPurchasePlan.kt invalid plan-source guard" $plan @(
    'val lineCost = TradeMoney.lineTotal(source.unitPrice, quantity)',
    'if (lineCost < 0L) continue',
    'val lineSpace = source.unitCargoSpace * quantity',
    'if (lineSpace < 0f || lineSpace.isNaN() || lineSpace.isInfinite()) continue',
    'lines.add(StockPurchaseLine(source, quantity))',
    'remaining -= quantity',
    'totalCost = TradeMoney.safeAdd(totalCost, lineCost)',
    'totalSpace += lineSpace'
)

$quoteBook = Read-Text "src/main/kotlin/weaponsprocurement/ui/stockreview/trade/StockReviewQuoteBook.kt"
foreach ($needle in @(
    'val cost = TradeMoney.lineTotal(stock.unitPrice, quantity)',
    'if (cost < 0L) continue',
    'val baseCost = TradeMoney.lineTotal(stock.baseUnitPrice, quantity)',
    'if (baseCost < 0L) continue',
    'val cargo = stock.unitCargoSpace * quantity',
    'if (cargo < 0f || cargo.isNaN() || cargo.isInfinite()) continue',
    'totalBaseCost = TradeMoney.safeAdd(totalBaseCost, baseCost)',
    'totalCargo += cargo'
)) {
    Assert-Contains "StockReviewQuoteBook.kt invalid quote-source guard" $quoteBook $needle
}
Assert-Contains "StockReviewQuoteBook.kt fallback cargo guard" $quoteBook 'if (stock.unitCargoSpace > 0f && !stock.unitCargoSpace.isNaN() && !stock.unitCargoSpace.isInfinite())'
Assert-Contains "StockReviewQuoteBook.kt fallback cargo guard" $quoteBook 'val reference = StockItemStacks.referenceUnitCargoSpace(record.itemType, record.itemId)'
Assert-Order "StockReviewQuoteBook.kt fallback cargo guard" $quoteBook @(
    'var foundStockCargo = false',
    'foundStockCargo = true',
    'if (!foundStockCargo)',
    'val reference = StockItemStacks.referenceUnitCargoSpace(record.itemType, record.itemId)',
    'result = reference'
)
Assert-Order "StockReviewQuoteBook.kt invalid quote-source guard" $quoteBook @(
    'val cost = TradeMoney.lineTotal(stock.unitPrice, quantity)',
    'if (cost < 0L) continue',
    'val baseCost = TradeMoney.lineTotal(stock.baseUnitPrice, quantity)',
    'if (baseCost < 0L) continue',
    'val cargo = stock.unitCargoSpace * quantity',
    'if (cargo < 0f || cargo.isNaN() || cargo.isInfinite()) continue',
    'totalCost = TradeMoney.safeAdd(totalCost, cost)',
    'totalBaseCost = TradeMoney.safeAdd(totalBaseCost, baseCost)',
    'totalQuantity += quantity',
    'totalCargo += cargo',
    'allocations.add(StockReviewSellerAllocation(stock.displaySourceName, stock.sourceId, quantity, cost))'
)

$money = Read-Text "src/main/kotlin/weaponsprocurement/trade/plan/TradeMoney.kt"
foreach ($needle in @(
    'MAX_EXECUTABLE_CREDITS: Long = 2147483647L',
    'if (unitPrice < 0 || quantity < 0) return -1L',
    'if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE',
    'return credits in 0L..MAX_EXECUTABLE_CREDITS'
)) {
    Assert-Contains "TradeMoney.kt" $money $needle
}

foreach ($needle in @(
    'catch (t: Throwable)',
    '// Transaction callbacks are best-effort; cargo mutation has already succeeded.',
    'log.warn("WP_STOCK_REVIEW transaction report failed for $itemId at ${submarket.specId}", t)'
)) {
    Assert-Contains "StockMarketTransactionReporter.kt post-commit report guard" $transactionReporter $needle
}

$analyzer = Read-Text "tools/analyze-trade-rollback-diagnostics.ps1"
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'Select-String -LiteralPath $resolvedLog -Pattern "WP_STOCK_REVIEW_ROLLBACK"'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer '$passCount = @($records | Where-Object { $_.status -eq "PASS" }).Count'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer '$failCount = @($records | Where-Object { $_.status -eq "FAIL" }).Count'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'Rollback diagnostic record missing required field'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'Rollback diagnostic record has unsupported status'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'function Test-IntegerText'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'function Test-FloatText'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'function Test-BooleanText'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'Rollback diagnostic field'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'is not an integer'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'is not a boolean'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'is not a number'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'Missing passing rollback diagnostic for failure step'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'operation=$($record.operation)'
Assert-Contains "analyze-trade-rollback-diagnostics.ps1" $analyzer 'operation={1}'
Assert-NotMatch "analyze-trade-rollback-diagnostics.ps1 output field" $analyzer '\$record\.op(?!eration)'
foreach ($field in @("status", "operation", "item", "quantity", "failedStep", "restoredCargos", "failedCargos", "creditsRestored", "countsRestored", "creditsBefore", "creditsAtFailure", "creditsAfterRollback", "touched")) {
    Assert-Contains "analyze-trade-rollback-diagnostics.ps1 required field" $analyzer "`"$field`""
}
foreach ($field in @("failedStep", "operation", "item", "quantity", "creditsRestored", "countsRestored", "touched")) {
    Assert-Contains "analyze-trade-rollback-diagnostics.ps1 output field" $analyzer "`$record.$field"
}

$configDocs = Read-Text "CONFIG.md"
foreach ($step in $steps) {
    Assert-Contains "CONFIG.md rollback accepted step" $configDocs $step
}
Assert-Contains "CONFIG.md" $configDocs 'Leave the property unset, empty, or `none` for normal play and public packages.'
Assert-Contains "CONFIG.md" $configDocs 'WP_STOCK_REVIEW_ROLLBACK'

if ($failures.Count -gt 0) {
    throw "Trade rollback contract validation failed with $($failures.Count) failure(s)."
}

Write-Host "Trade rollback contract validation passed."
