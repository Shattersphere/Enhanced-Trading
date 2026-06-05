param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

$shipDir = "src/main/kotlin/weaponsprocurement/ui/stockreview/ships"
$snapshotBuilder = Read-Text "$shipDir/StockReviewShipSnapshotBuilder.kt"
$executionController = Read-Text "$shipDir/StockReviewShipExecutionController.kt"
$pendingTrade = Read-Text "$shipDir/StockReviewPendingShipTrade.kt"
$pendingTrades = Read-Text "$shipDir/StockReviewPendingShipTrades.kt"
$tradeController = Read-Text "$shipDir/StockReviewShipTradeController.kt"

Assert-Contains "StockReviewShipSnapshotBuilder.kt local-only contract" $snapshotBuilder "Builds the local-only ship trade snapshot"
Assert-Contains "StockReviewShipSnapshotBuilder.kt local-only contract" $snapshotBuilder "Remote ship trading needs separate source semantics before being added."
Assert-Contains "StockReviewShipSnapshotBuilder.kt market guard" $snapshotBuilder "if (market == null) {"
Assert-Contains "StockReviewShipSnapshotBuilder.kt market guard" $snapshotBuilder "return StockReviewShipSnapshot.EMPTY"
Assert-Contains "StockReviewShipSnapshotBuilder.kt buy source contract" $snapshotBuilder "for (submarket in market.submarketsCopy.orEmpty())"
Assert-Contains "StockReviewShipSnapshotBuilder.kt buy source contract" $snapshotBuilder "if (!StockSubmarketAccess.isTradeEligible(submarket, includeBlackMarket)) continue"
Assert-Contains "StockReviewShipSnapshotBuilder.kt buy source contract" $snapshotBuilder "val members = submarket?.cargoNullOk?.mothballedShips?.membersListCopy ?: continue"
Assert-Contains "StockReviewShipSnapshotBuilder.kt buy source contract" $snapshotBuilder "addBuyRecord(buyRecords, member, submarket)"
Assert-Contains "StockReviewShipSnapshotBuilder.kt sell source contract" $snapshotBuilder "val sellTarget = sellTarget(market, includeBlackMarket)"
Assert-Contains "StockReviewShipSnapshotBuilder.kt sell source contract" $snapshotBuilder "val members = playerFleet?.fleetData?.membersListCopy.orEmpty()"
Assert-Contains "StockReviewShipSnapshotBuilder.kt sell source contract" $snapshotBuilder "addSellRecord(sellRecords, member, sellTarget)"
Assert-NotContains "StockReviewShipSnapshotBuilder.kt local-only contract" $snapshotBuilder "StockSourceMode.SECTOR"
Assert-NotContains "StockReviewShipSnapshotBuilder.kt local-only contract" $snapshotBuilder "StockSourceMode.FIXERS"
Assert-NotContains "StockReviewShipSnapshotBuilder.kt local-only contract" $snapshotBuilder "ObservedShipStockIndex"

$buyRecord = Get-Section $snapshotBuilder "private fun addBuyRecord(" "private fun addSellRecord("
foreach ($needle in @(
    "if (!isTradeableShip(member)) return",
    'val id = member?.id?.takeIf { it.isNotBlank() } ?: return',
    'StockReviewShipPricing.buyQuote(member, submarket)',
    '"B|${submarket.specId}|$id"',
    "StockReviewShipTradeSide.BUY",
    "submarket.specId",
    "submarket.nameOneLine"
)) {
    Assert-Contains "StockReviewShipSnapshotBuilder.kt buy record contract" $buyRecord $needle
}

$sellRecord = Get-Section $snapshotBuilder "private fun addSellRecord(" "private fun addDebugRecord("
foreach ($needle in @(
    "if (!isTradeableShip(member)) return",
    "if (member?.isFlagship == true) return",
    'val id = member?.id?.takeIf { it.isNotBlank() } ?: return',
    'StockReviewShipPricing.sellQuote(member, submarket)',
    '"S|$id"',
    "StockReviewShipTradeSide.SELL",
    "submarket.specId",
    "submarket.nameOneLine"
)) {
    Assert-Contains "StockReviewShipSnapshotBuilder.kt sell record contract" $sellRecord $needle
}

$sellTarget = Get-Section $snapshotBuilder "private fun sellTarget(" "private fun isTradeableShip("
Assert-Order "StockReviewShipSnapshotBuilder.kt sell target priority" $sellTarget @(
    "if (includeBlackMarket) {",
    "it?.specId == Submarkets.SUBMARKET_BLACK",
    "it?.specId == Submarkets.SUBMARKET_OPEN",
    "it?.specId == Submarkets.GENERIC_MILITARY",
    "return submarkets.firstOrNull { StockSubmarketAccess.isTradeEligible(it, includeBlackMarket) }"
)

$tradeable = Get-Section $snapshotBuilder "private fun isTradeableShip(" "`n}"
foreach ($needle in @(
    "if (member == null) return false",
    "if (member.isFighterWing) return false",
    "if (member.isStation) return false",
    "return member.type != null"
)) {
    Assert-Contains "StockReviewShipSnapshotBuilder.kt tradeable filter" $tradeable $needle
}

Assert-Contains "StockReviewShipExecutionController.kt exact-member contract" $executionController "Confirms queued exact-member ship trades."
Assert-Contains "StockReviewShipExecutionController.kt exact-member contract" $executionController "source/player list at confirm time or the trade fails cleanly"
Assert-Contains "StockReviewShipExecutionController.kt host contract" $executionController "fun market(): MarketAPI?"
Assert-Contains "StockReviewShipExecutionController.kt host contract" $executionController "fun playerFleet(): CampaignFleetAPI?"
Assert-Contains "StockReviewShipExecutionController.kt host contract" $executionController 'host.postMessage("Ship trades require an active market and player fleet.")'

$confirm = Get-Section $executionController "fun confirmPendingShipTrades()" "private fun executeBuy("
foreach ($needle in @(
    "val trades = pendingTrades.asList()",
    "val market = host.market()",
    "val playerFleet = host.playerFleet()",
    "executeBuy(market, playerFleet, trade, failures)",
    "executeSell(market, playerFleet, trade, failures)",
    "pendingTrades.reset(trade.recordKey)",
    "host.refreshVanillaCargoScreen()",
    "host.rebuildSnapshot()",
    'host.postMessage("Ship trades completed.")',
    'host.postMessage("Some ship trades could not be completed: ${failures.first()}")'
)) {
    Assert-Contains "StockReviewShipExecutionController.kt confirmation contract" $confirm $needle
}

$executeBuy = Get-Section $executionController "private fun executeBuy(" "private fun executeSell("
Assert-Order "StockReviewShipExecutionController.kt buy mutation order" $executeBuy @(
    "val source = findSubmarket(market, trade.submarketId)",
    "val member = findMember(source?.cargoNullOk?.mothballedShips?.membersListCopy, trade.memberId)",
    'failures.add("${trade.memberName} is no longer for sale.")',
    "val credits = playerFleet.cargo?.credits",
    'failures.add("not enough credits for ${trade.memberName}.")',
    "source.cargo.mothballedShips.removeFleetMember(member)",
    "playerFleet.fleetData.addFleetMember(member)",
    "credits.subtract(trade.unitPrice.toFloat())",
    "reportShipTransaction(market, source, member, trade.unitPrice, true)"
)

$executeSell = Get-Section $executionController "private fun executeSell(" "private fun findSubmarket("
Assert-Order "StockReviewShipExecutionController.kt sell mutation order" $executeSell @(
    "val target = findSubmarket(market, trade.submarketId)",
    "val member = findMember(playerFleet.fleetData?.membersListCopy, trade.memberId)",
    'failures.add("${trade.memberName} is no longer available to sell.")',
    "playerFleet.fleetData.removeFleetMember(member)",
    "target.cargo.mothballedShips.addFleetMember(member)",
    "playerFleet.cargo.credits.add(trade.unitPrice.toFloat())",
    "reportShipTransaction(market, target, member, trade.unitPrice, false)"
)

Assert-Contains "StockReviewShipExecutionController.kt submarket lookup" $executionController "return market.submarketsCopy?.firstOrNull { it?.specId == submarketId }"
Assert-Contains "StockReviewShipExecutionController.kt member lookup" $executionController "members?.firstOrNull { it?.id == memberId }"
Assert-Contains "StockReviewShipExecutionController.kt transaction reporting" $executionController "PlayerMarketTransaction(market, submarket, tradeMode(submarket))"
Assert-Contains "StockReviewShipExecutionController.kt transaction reporting" $executionController "PlayerMarketTransaction.ShipSaleInfo(member, unitPrice.toFloat())"
Assert-Contains "StockReviewShipExecutionController.kt transaction reporting" $executionController "plugin.reportPlayerMarketTransaction(transaction)"
Assert-Contains "StockReviewShipExecutionController.kt transaction reporting" $executionController "LOG.warn(`"WP_STOCK_REVIEW ship transaction report failed for"
Assert-NotContains "StockReviewShipExecutionController.kt local-only contract" $executionController "Global.getSector()"
Assert-NotContains "StockReviewShipExecutionController.kt local-only contract" $executionController "StockSourceMode.SECTOR"
Assert-NotContains "StockReviewShipExecutionController.kt local-only contract" $executionController "StockSourceMode.FIXERS"
Assert-NotContains "StockReviewShipExecutionController.kt local-only contract" $executionController "ObservedShipStockIndex"

foreach ($needle in @(
    "@JvmField val recordKey: String",
    "@JvmField val memberId: String",
    "@JvmField val side: StockReviewShipTradeSide",
    "@JvmField val submarketId: String?",
    "fun isBuy(): Boolean = side.isBuy()",
    "fun copy(): StockReviewPendingShipTrade =",
    "val memberId = record.member.id ?: return null",
    "if (memberId.isBlank()) return null",
    "record.price.finalCredits",
    "record.price.baseCredits",
    "record.price.tariffCredits"
)) {
    Assert-Contains "StockReviewPendingShipTrade.kt pending trade contract" $pendingTrade $needle
}

foreach ($needle in @(
    "private val tradesByKey = LinkedHashMap<String, StockReviewPendingShipTrade>()",
    "fun asList(): List<StockReviewPendingShipTrade> = immutableCopy(tradesByKey.values)",
    "tradesByKey[trade.recordKey] = trade.copy()",
    "val trade = StockReviewPendingShipTrade.fromRecord(record) ?: return false",
    "if (tradesByKey.remove(trade.recordKey) == null)",
    "tradesByKey[trade.recordKey] = trade",
    "fun reset(recordKey: String?): Boolean",
    "return false",
    "tradesByKey.remove(recordKey)",
    "return true",
    "Collections.unmodifiableList(result)"
)) {
    Assert-Contains "StockReviewPendingShipTrades.kt pending collection contract" $pendingTrades $needle
}

foreach ($needle in @(
    "val record = host.shipSnapshot().getRecord(action.getItemKey())",
    'host.postMessage("That ship is no longer available.")',
    "if (pendingTrades.toggle(record))",
    "if (pendingTrades.reset(action.getItemKey()))",
    "host.requestContentRebuild()"
)) {
    Assert-Contains "StockReviewShipTradeController.kt stale selection contract" $tradeController $needle
}

if ($failures.Count -gt 0) {
    throw "Ship trading contract validation failed with $($failures.Count) failure(s)."
}

Write-Host "Ship trading contract validation passed."
