$repoRoot = Split-Path -Parent $PSScriptRoot
$legacyGuiDir = Join-Path $repoRoot "src\weaponsprocurement\gui"
$kotlinGuiDir = Join-Path $repoRoot "src\main\kotlin\weaponsprocurement\ui"
$controlsCandidates = @(
    (Join-Path $legacyGuiDir "WimGuiControls.java"),
    (Join-Path $kotlinGuiDir "WimGuiControls.kt")
)
$controlsPath = $controlsCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

if (-not $controlsPath) {
    throw "WimGuiControls source not found. Checked: $($controlsCandidates -join ', ')"
}

$sourceFiles = @()
foreach ($dir in @($legacyGuiDir, $kotlinGuiDir)) {
    if (Test-Path -LiteralPath $dir) {
        $sourceFiles += @(Get-ChildItem -Path $dir -Recurse -File -Include *.java,*.kt)
    }
}

$checkboxHits = @($sourceFiles | Select-String -Pattern "addAreaCheckbox|addCheckbox" -SimpleMatch)
if ($checkboxHits.Count -gt 0) {
    throw "WP GUI must not use checkbox-backed buttons/toggles. Hits:`n$($checkboxHits -join "`n")"
}

$directButtonHits = @($sourceFiles |
    Where-Object { $_.FullName -ne $controlsPath } |
    Select-String -Pattern ".addButton(" -SimpleMatch)
if ($directButtonHits.Count -gt 0) {
    throw "WP GUI buttons must route through WimGuiControls.addButton. Hits:`n$($directButtonHits -join "`n")"
}

$controlsText = Get-Content -LiteralPath $controlsPath -Raw
if ($controlsText -notmatch "dimForIdle\(idle\)") {
    throw "WimGuiControls.addButton must dim the inner button fill from the idle color."
}
if ($controlsText -notmatch "(Color|val) hover = .*colors(\?|\.)\.hover|val hover = colors\?\.hover") {
    throw "WimGuiControls.addButton must keep hover color separate from the dimmed inner idle fill."
}

$stockReviewStylePath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewStyle.kt"
$stockReviewListModelPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListModel.kt"
$stockReviewReviewModelPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewReviewListModel.kt"
$stockReviewListSectionPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListSection.kt"
$stockReviewListEmptyRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListEmptyRows.kt"
$stockReviewItemTypeSectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemTypeSections.kt"
$stockReviewStockCategorySectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewStockCategorySections.kt"
$stockReviewTradeGroupSectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeGroupSections.kt"
$stockReviewItemRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemRows.kt"
$stockReviewItemInfoRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemInfoRows.kt"
$stockReviewRowLayoutPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowLayout.kt"
$stockReviewDetailRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewDetailRows.kt"
$stockReviewSourceAllocationRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewSourceAllocationRows.kt"
$stockReviewCellGroupPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewCellGroup.kt"
$stockReviewTradeCellsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeRowCells.kt"
$stockReviewTradeSummaryRendererPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeSummaryRenderer.kt"
$stockReviewTradeSummaryFieldsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeSummaryFields.kt"
$stockReviewTooltipPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewItemTooltip.kt"
$stockReviewItemInfoFieldsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemInfoFields.kt"
$stockReviewActionControlsPath = Join-Path $kotlinGuiDir "stockreview\controls\StockReviewActionControls.kt"
$stockReviewRowSpecPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowSpec.kt"
$stockReviewListRowPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListRow.kt"
$stockReviewFooterSpecPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFooterSpec.kt"
$stockReviewFooterButtonsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFooterButtons.kt"
$stockReviewItemTypeHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemTypeHeadingRows.kt"
$stockReviewStockCategoryHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewStockCategoryHeadingRows.kt"
$stockReviewTradeGroupHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeGroupHeadingRows.kt"
$stockReviewFilterHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFilterHeadingRows.kt"
$stockReviewItemDetailHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemDetailHeadingRows.kt"
$stockReviewFilterRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFilterRows.kt"
$stockReviewFilterGroupSectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFilterGroupSections.kt"
$stockReviewLegacyHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewHeadingRows.kt"
$stockReviewActionRowRendererPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewActionRowRenderer.kt"
$stockReviewActionRowButtonsPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewActionRowButtons.kt"

foreach ($requiredPath in @($stockReviewStylePath, $stockReviewListModelPath, $stockReviewReviewModelPath, $stockReviewListSectionPath, $stockReviewListEmptyRowsPath, $stockReviewItemTypeSectionsPath, $stockReviewStockCategorySectionsPath, $stockReviewTradeGroupSectionsPath, $stockReviewItemRowsPath, $stockReviewItemInfoRowsPath, $stockReviewRowLayoutPath, $stockReviewDetailRowsPath, $stockReviewSourceAllocationRowsPath, $stockReviewCellGroupPath, $stockReviewTradeCellsPath, $stockReviewTradeSummaryRendererPath, $stockReviewTradeSummaryFieldsPath, $stockReviewTooltipPath, $stockReviewItemInfoFieldsPath, $stockReviewActionControlsPath, $stockReviewRowSpecPath, $stockReviewListRowPath, $stockReviewFooterSpecPath, $stockReviewFooterButtonsPath, $stockReviewItemTypeHeadingRowsPath, $stockReviewStockCategoryHeadingRowsPath, $stockReviewTradeGroupHeadingRowsPath, $stockReviewFilterHeadingRowsPath, $stockReviewItemDetailHeadingRowsPath, $stockReviewFilterRowsPath, $stockReviewFilterGroupSectionsPath, $stockReviewActionRowRendererPath, $stockReviewActionRowButtonsPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required stock-review UI source missing: $requiredPath"
    }
}
if (Test-Path -LiteralPath $stockReviewLegacyHeadingRowsPath) {
    throw "StockReviewHeadingRows must stay split into focused heading owners."
}

$stockReviewSourceRoot = Join-Path $kotlinGuiDir "stockreview"
$stockReviewSourceFiles = @(Get-ChildItem -Path $stockReviewSourceRoot -Recurse -File -Include *.kt)
$legacyHeadingReferenceHits = @($stockReviewSourceFiles | Select-String -Pattern "StockReviewHeadingRows" -SimpleMatch)
if ($legacyHeadingReferenceHits.Count -gt 0) {
    throw "Stock-review heading callers must use focused heading owners. Hits:`n$($legacyHeadingReferenceHits -join "`n")"
}
$directStockReviewActionButtonFactoryHits = @($stockReviewSourceFiles |
    Where-Object { $_.FullName -ne $stockReviewActionControlsPath } |
    Select-String -Pattern "WimGuiSemanticButtonFactory<StockReviewAction>" -SimpleMatch)
if ($directStockReviewActionButtonFactoryHits.Count -gt 0) {
    throw "Stock-review action buttons must route through StockReviewActionButtonFactory. Hits:`n$($directStockReviewActionButtonFactoryHits -join "`n")"
}

$directStockReviewActionCellHits = @($stockReviewSourceFiles |
    Where-Object { $_.FullName -ne $stockReviewActionControlsPath } |
    Select-String -Pattern "WimGuiRowCell.standardAction" -SimpleMatch)
if ($directStockReviewActionCellHits.Count -gt 0) {
    throw "Stock-review action cells must route through StockReviewActionCells. Hits:`n$($directStockReviewActionCellHits -join "`n")"
}

$styleText = Get-Content -LiteralPath $stockReviewStylePath -Raw
if ($styleText -notmatch "const val SHOW_WIDTH_TEST_ROWS = true") {
    throw "Stock-review worst-case debug rows must remain visible."
}
if ($styleText -notmatch "const val ROW_ICON_INDENT = ACTION_BUTTON_HEIGHT \+ BUTTON_GAP" -or
    $styleText -notmatch "const val WEAPON_INDENT = ROW_ICON_INDENT") {
    throw "Stock-review weapon indent must match the rendered row icon footprint."
}
$rowLayoutText = Get-Content -LiteralPath $stockReviewRowLayoutPath -Raw
if ($styleText -notmatch "const val STOCK_CELL_WIDTH = 148f" -or
    $rowLayoutText -notmatch "StockReviewStyle\.STOCK_CELL_WIDTH" -or
    $rowLayoutText -notmatch "fun review\(\): StockReviewRowLayout") {
    throw "Stock-review storage cells must be wide enough for the capped worst-case storage label in trade and review screens."
}

$listModelText = Get-Content -LiteralPath $stockReviewListModelPath -Raw
$listSectionText = Get-Content -LiteralPath $stockReviewListSectionPath -Raw
$listEmptyRowsText = Get-Content -LiteralPath $stockReviewListEmptyRowsPath -Raw
$itemTypeSectionsText = Get-Content -LiteralPath $stockReviewItemTypeSectionsPath -Raw
$stockCategorySectionsText = Get-Content -LiteralPath $stockReviewStockCategorySectionsPath -Raw
$tradeGroupSectionsText = Get-Content -LiteralPath $stockReviewTradeGroupSectionsPath -Raw
$reviewModelText = Get-Content -LiteralPath $stockReviewReviewModelPath -Raw
$itemTypeHeadingRowsText = Get-Content -LiteralPath $stockReviewItemTypeHeadingRowsPath -Raw
$stockCategoryHeadingRowsText = Get-Content -LiteralPath $stockReviewStockCategoryHeadingRowsPath -Raw
$tradeGroupHeadingRowsText = Get-Content -LiteralPath $stockReviewTradeGroupHeadingRowsPath -Raw
$filterHeadingRowsText = Get-Content -LiteralPath $stockReviewFilterHeadingRowsPath -Raw
$itemDetailHeadingRowsText = Get-Content -LiteralPath $stockReviewItemDetailHeadingRowsPath -Raw
$filterRowsText = Get-Content -LiteralPath $stockReviewFilterRowsPath -Raw
$filterGroupSectionsText = Get-Content -LiteralPath $stockReviewFilterGroupSectionsPath -Raw
if ($itemTypeSectionsText -notmatch "class StockReviewItemTypeSection" -or
    $itemTypeSectionsText -notmatch "object StockReviewItemTypeSections" -or
    $itemTypeSectionsText -notmatch "StockReviewItemTypeSection\(StockItemType\.WEAPON, false\)" -or
    $itemTypeSectionsText -notmatch "StockReviewItemTypeSection\(StockItemType\.WING, true\)" -or
    $itemTypeSectionsText -notmatch "StockReviewStockCategorySections\.ORDERED" -or
    $itemTypeSectionsText -notmatch "StockReviewItemTypeHeadingRows\.itemType" -or
    $listModelText -notmatch "StockReviewItemTypeSections\.ORDERED" -or
    $listModelText -match "StockItemType\.WEAPON" -or
    $listModelText -match "StockItemType\.WING" -or
    $listModelText -match "StockReviewHeadingRows\.itemType" -or
    $listModelText -match "StockReviewStockCategorySections\.ORDERED") {
    throw "Stock-review item-type order, top gaps, headings, and category composition must live in StockReviewItemTypeSections."
}
if (-not $stockCategorySectionsText.Contains('" [$typeLabel: $itemTypes, "') -or
    -not $stockCategorySectionsText.Contains('"Selling: ${maxOf(0, selling)}, "') -or
    $stockCategorySectionsText.Contains('"Selling: ${maxOf(0, selling)} | "')) {
    throw "Stock-review category headings must use comma-separated type/selling/buying summaries."
}
if ($stockCategorySectionsText -notmatch "class StockReviewStockCategorySection" -or
    $stockCategorySectionsText -notmatch "object StockReviewStockCategorySections" -or
    $stockCategorySectionsText -notmatch "StockCategory\.NO_STOCK, StockReviewStyle\.NO_STOCK, false, true" -or
    $stockCategorySectionsText -notmatch "StockCategory\.INSUFFICIENT, StockReviewStyle\.INSUFFICIENT, true, false" -or
    $stockCategorySectionsText -notmatch "StockCategory\.SUFFICIENT, StockReviewStyle\.SUFFICIENT, true, false" -or
    $stockCategorySectionsText -notmatch "StockReviewFilters\.matches" -or
    $stockCategorySectionsText -notmatch "includesWorstCaseRow && StockItemType\.WEAPON == itemType" -or
    $stockCategorySectionsText -notmatch "StockReviewStockCategoryHeadingRows\.stockCategory" -or
    $itemTypeSectionsText -notmatch "StockReviewStockCategorySections\.ORDERED" -or
    $listModelText -match "StockCategory\.NO_STOCK" -or
    $listModelText -match "categoryHeading" -or
    $listModelText -match "filteredRecords") {
    throw "Stock-review stock-category order, filters, headings, colors, top gaps, and debug-row policy must live in StockReviewStockCategorySections."
}
if ($tradeGroupSectionsText -notmatch "class StockReviewTradeGroupSection" -or
    $tradeGroupSectionsText -notmatch "object StockReviewTradeGroupSections" -or
    $tradeGroupSectionsText -notmatch "StockReviewTradeGroupSection\(StockReviewTradeGroup\.BUYING, false, true\)" -or
    $tradeGroupSectionsText -notmatch "StockReviewTradeGroupSection\(StockReviewTradeGroup\.SELLING, true, false\)" -or
    $tradeGroupSectionsText -notmatch "StockReviewListSection\.builder\(groupTrades\)" -or
    $tradeGroupSectionsText -notmatch "StockReviewTradeGroupHeadingRows\.reviewGroup" -or
    $tradeGroupSectionsText -notmatch "includeWorstCaseRow\(includesWorstCaseRow\)" -or
    $tradeGroupSectionsText -notmatch "fun tradesFrom" -or
    $tradeGroupSectionsText -notmatch "class StockReviewTradeGroupRows" -or
    $tradeGroupSectionsText -notmatch "fun allEmpty" -or
    $reviewModelText -notmatch "StockReviewTradeGroupSections\.build" -or
    $reviewModelText -match "StockReviewTradeGroup\.BUYING" -or
    $reviewModelText -match "StockReviewTradeGroup\.SELLING" -or
    $reviewModelText -match "reviewTradesForGroup" -or
    $reviewModelText -match "StockReviewListSection\.builder") {
    throw "Stock-review review-group order, trade splitting, top gaps, headings, and debug-row policy must live in StockReviewTradeGroupSections."
}
$filterListModelText = Get-Content -LiteralPath (Join-Path $kotlinGuiDir "stockreview\state\StockReviewFilterListModel.kt") -Raw
if ($filterRowsText -notmatch "object StockReviewFilterRows" -or
    $filterRowsText -notmatch "fun addActive" -or
    $filterRowsText -notmatch "fun available" -or
    $filterRowsText -notmatch "StockReviewListRow\.filter" -or
    $filterRowsText -notmatch "StockReviewTooltips\.filter" -or
    $filterGroupSectionsText -notmatch "class StockReviewFilterGroupSection" -or
    $filterGroupSectionsText -notmatch "object StockReviewFilterGroupSections" -or
    $filterGroupSectionsText -notmatch "fun addGroups" -or
    $filterGroupSectionsText -notmatch "fun shouldShow" -or
    $filterGroupSectionsText -notmatch "StockSourceMode\.FIXERS" -or
    $filterGroupSectionsText -notmatch "StockReviewFilters\.activeInGroup" -or
    $filterGroupSectionsText -notmatch "StockReviewFilterHeadingRows\.filterGroup" -or
    $filterGroupSectionsText -notmatch "StockReviewFilterRows\.available" -or
    $filterListModelText -notmatch "StockReviewFilterRows\.addActive" -or
    $filterListModelText -notmatch "StockReviewFilterGroupSections\.addGroups" -or
    $filterListModelText -match "StockReviewListRow\.filter" -or
    $filterListModelText -match "StockReviewFilterHeadingRows" -or
    $filterListModelText -match "StockReviewTooltips" -or
    $filterListModelText -match "StockSourceMode\.FIXERS" -or
    $filterListModelText -match "activeInGroup") {
    throw "Stock-review filter rows, group visibility, active rows, and available rows must route through StockReviewFilterRows and StockReviewFilterGroupSections."
}
$itemRowsText = Get-Content -LiteralPath $stockReviewItemRowsPath -Raw
if ($itemRowsText -notmatch "\.indent\(layout\.itemIndent\)" -or
    $itemRowsText -notmatch "StockReviewRowIcon\.item\(record\)" -or
    $rowLayoutText -notmatch "StockReviewStyle\.WEAPON_INDENT") {
    throw "Main stock-review item rows must start at the category indent; the icon supplies the next visual indent."
}

$itemInfoRowsText = Get-Content -LiteralPath $stockReviewItemInfoRowsPath -Raw
if ($itemRowsText -notmatch "\.indent\(layout\.itemIndent\)" -or
    $itemRowsText -notmatch "StockReviewRowIcon\.item\(record\)" -or
    $rowLayoutText -notmatch "fun review\(\): StockReviewRowLayout") {
    throw "Review stock-review item rows must use the same icon-indent model as the main trade screen."
}
if ($listSectionText -notmatch "class StockReviewListSection" -or
    $listSectionText -notmatch "SHOW_WIDTH_TEST_ROWS && includeWorstCaseRow" -or
    $listSectionText -notmatch "StockReviewItemRows\.addWorstCaseRow\(rows, layout\)" -or
    $listSectionText -notmatch "fun addHeading" -or
    $itemTypeSectionsText -notmatch "StockReviewListSection\.addHeading" -or
    $stockCategorySectionsText -notmatch "StockReviewListSection\.builder\(records\)" -or
    $tradeGroupSectionsText -notmatch "StockReviewListSection\.builder\(groupTrades\)" -or
    $tradeGroupSectionsText -notmatch "\.includeWorstCaseRow\(includesWorstCaseRow\)" -or
    $rowLayoutText -notmatch "fun review\(\): StockReviewRowLayout") {
    throw "Stock-review main/review list sections must route heading, expansion, and worst-case rows through StockReviewListSection."
}
if ($listEmptyRowsText -notmatch "object StockReviewListEmptyRows" -or
    $listEmptyRowsText -notmatch "No trades are planned" -or
    $listEmptyRowsText -notmatch "All rows are hidden by the active filters" -or
    $listModelText -notmatch "StockReviewListEmptyRows\.main" -or
    $reviewModelText -notmatch "StockReviewListEmptyRows\.review" -or
    $listModelText -match "No local weapon or wing stock" -or
    $reviewModelText -match "No trades are planned") {
    throw "Stock-review empty-state rows must be declared in StockReviewListEmptyRows."
}

$tradeCellsText = Get-Content -LiteralPath $stockReviewTradeCellsPath -Raw
$detailRowsText = Get-Content -LiteralPath $stockReviewDetailRowsPath -Raw
$sourceAllocationRowsText = Get-Content -LiteralPath $stockReviewSourceAllocationRowsPath -Raw
$cellGroupText = Get-Content -LiteralPath $stockReviewCellGroupPath -Raw
$itemInfoFieldsText = Get-Content -LiteralPath $stockReviewItemInfoFieldsPath -Raw
$tradeSummaryRendererText = Get-Content -LiteralPath $stockReviewTradeSummaryRendererPath -Raw
$tradeSummaryFieldsText = Get-Content -LiteralPath $stockReviewTradeSummaryFieldsPath -Raw
$actionControlsText = Get-Content -LiteralPath $stockReviewActionControlsPath -Raw
$rowSpecText = Get-Content -LiteralPath $stockReviewRowSpecPath -Raw
$listRowText = Get-Content -LiteralPath $stockReviewListRowPath -Raw
$footerSpecText = Get-Content -LiteralPath $stockReviewFooterSpecPath -Raw
$footerButtonsText = Get-Content -LiteralPath $stockReviewFooterButtonsPath -Raw
$actionRowRendererText = Get-Content -LiteralPath $stockReviewActionRowRendererPath -Raw
$actionRowButtonsText = Get-Content -LiteralPath $stockReviewActionRowButtonsPath -Raw
if ($actionControlsText -notmatch "class StockReviewActionRef" -or
    $actionControlsText -notmatch "class StockReviewButtonDefinition" -or
    $actionControlsText -notmatch "StockReviewActionGuards\.requireGroup" -or
    $rowSpecText -notmatch "fun action\(value: StockReviewActionRef\?\)" -or
    $listRowText -notmatch "StockReviewActionRef\.rowExpansion" -or
    $listRowText -notmatch "StockReviewActionRef\.filters" -or
    $listRowText -notmatch "StockReviewActionRef\.scroll" -or
    $itemRowsText -notmatch "StockReviewActionRef\.debugMode") {
    throw "Stock-review row actions must use group-checked StockReviewActionRef/StockReviewActionControls helpers."
}
if ($footerSpecText -match "StockReviewActionGroup" -or
    $footerSpecText -match "BUTTON_FACTORY\.button" -or
    $footerSpecText -notmatch "StockReviewFooterButtons\.tradeLeft" -or
    $footerSpecText -notmatch "StockReviewFooterButtons\.reviewLeft" -or
    $footerButtonsText -notmatch "StockReviewButtonDefinition" -or
    $footerButtonsText -notmatch "PURCHASE_ALL" -or
    $footerButtonsText -notmatch "RESET_ALL") {
    throw "Stock-review footer buttons must be declared in StockReviewFooterButtons, not inline in StockReviewFooterSpec."
}
if ($actionRowRendererText -match "StockReviewActionGroup" -or
    $actionRowRendererText -match "BUTTON_FACTORY\.button" -or
    $actionRowRendererText -notmatch "StockReviewActionRowButtons\.build" -or
    $actionRowButtonsText -notmatch "StockReviewButtonDefinition" -or
    $actionRowButtonsText -notmatch "BLACK_MARKET" -or
    $actionRowButtonsText -notmatch "WeaponsProcurementConfig\.isDebugShipCatalogViewEnabled") {
    throw "Stock-review action-row buttons must be declared in StockReviewActionRowButtons, not inline in StockReviewActionRowRenderer."
}
if ($itemTypeHeadingRowsText -notmatch "object StockReviewItemTypeHeadingRows" -or
    $itemTypeHeadingRowsText -notmatch "fun itemType" -or
    $stockCategoryHeadingRowsText -notmatch "object StockReviewStockCategoryHeadingRows" -or
    $stockCategoryHeadingRowsText -notmatch "fun stockCategory" -or
    $tradeGroupHeadingRowsText -notmatch "object StockReviewTradeGroupHeadingRows" -or
    $tradeGroupHeadingRowsText -notmatch "fun reviewGroup" -or
    $filterHeadingRowsText -notmatch "object StockReviewFilterHeadingRows" -or
    $filterHeadingRowsText -notmatch "fun filterGroup" -or
    $itemDetailHeadingRowsText -notmatch "object StockReviewItemDetailHeadingRows" -or
    $itemDetailHeadingRowsText -notmatch "fun basicInfo" -or
    $itemDetailHeadingRowsText -notmatch "fun advancedInfo" -or
    $itemDetailHeadingRowsText -notmatch "fun itemLabel" -or
    $itemTypeSectionsText -notmatch "StockReviewItemTypeHeadingRows\.itemType" -or
    $stockCategorySectionsText -notmatch "StockReviewStockCategoryHeadingRows\.stockCategory" -or
    $tradeGroupSectionsText -notmatch "StockReviewTradeGroupHeadingRows\.reviewGroup" -or
    $itemInfoRowsText -notmatch "StockReviewItemDetailHeadingRows\.basicInfo" -or
    $itemInfoRowsText -notmatch "StockReviewItemDetailHeadingRows\.advancedInfo" -or
    $itemRowsText -notmatch "StockReviewItemDetailHeadingRows\.itemLabel" -or
    $itemRowsText -match "WimGuiToggleHeading" -or
    $itemRowsText -match "fun infoSectionKey" -or
    $itemRowsText -match "::info::" -or
    $listModelText -match "StockReviewGroupRows" -or
    $reviewModelText -match "StockReviewGroupRows") {
    throw "Stock-review heading labels/actions/tooltips must route through focused heading owners."
}
if ($itemInfoFieldsText -notmatch "object StockReviewItemInfoFields" -or
    $itemInfoFieldsText -notmatch "StockReviewDetailRows\.itemInfo" -or
    $itemInfoFieldsText -notmatch "WEAPON_BASIC_FIELDS" -or
    $itemInfoFieldsText -notmatch "WING_BASIC_FIELDS" -or
    $itemInfoFieldsText -notmatch "WEAPON_ADVANCED_FIELDS" -or
    $itemInfoFieldsText -notmatch "Damage/Sec \(sustained\)" -or
    $itemInfoFieldsText -notmatch "EMP/Second \(sustained\)" -or
    $itemInfoRowsText -notmatch "StockReviewItemInfoFields\.basic\(record\)" -or
    $itemInfoRowsText -notmatch "StockReviewItemInfoFields\.advanced\(record\)" -or
    $itemInfoRowsText -match "addDataRow" -or
    $itemInfoRowsText -match "addPositiveDataRow" -or
    $itemInfoRowsText -match "fun dataRow" -or
    $itemInfoRowsText -match "isMeaningful" -or
    $itemInfoRowsText -match "isPositiveValue") {
    throw "Stock-review Basic/Advanced detail fields must be declared in StockReviewItemInfoFields."
}
if ($detailRowsText -notmatch "object StockReviewDetailRows" -or
    $detailRowsText -notmatch "fun itemInfo" -or
    $detailRowsText -notmatch "fun sourceAllocation" -or
    $sourceAllocationRowsText -notmatch "object StockReviewSourceAllocationRows" -or
    $sourceAllocationRowsText -notmatch "Purchase Source" -or
    $sourceAllocationRowsText -notmatch "Unavailable" -or
    $sourceAllocationRowsText -notmatch "StockReviewDetailRows\.sourceAllocation" -or
    $itemRowsText -notmatch "StockReviewSourceAllocationRows\.add" -or
    $itemRowsText -match "addSourceAllocationRows" -or
    $itemRowsText -match "sourceLabel" -or
    $itemRowsText -match "allocationSummary") {
    throw "Stock-review source-allocation rows must route through StockReviewSourceAllocationRows and StockReviewDetailRows."
}
if ($tradeSummaryRendererText -notmatch "StockReviewTradeSummaryFields\.build" -or
    $tradeSummaryRendererText -match "addSummaryRow" -or
    $tradeSummaryRendererText -match "tariffsPaidLabel" -or
    $tradeSummaryFieldsText -notmatch "class StockReviewTradeSummaryField" -or
    $tradeSummaryFieldsText -notmatch "object StockReviewTradeSummaryFields" -or
    $tradeSummaryFieldsText -notmatch "Tariffs Paid" -or
    $tradeSummaryFieldsText -notmatch '\[\$percent%\]' -or
    $tradeSummaryFieldsText -notmatch "totalBuyCost") {
    throw "Stock-review footer summary row definitions must live in StockReviewTradeSummaryFields."
}
if ($cellGroupText -notmatch "Debug Worst-Case Suzuki-Clapteryon Thermal Prokector") {
    throw "Stock-review worst-case weapon debug row label is missing."
}
if ($cellGroupText -notmatch "Storage: 99\+ \[-99\+\]" -or
    $cellGroupText -notmatch "MAX_UNIT_PRICE = 99999" -or
    $cellGroupText -notmatch "MAX_TRANSACTION_CREDITS = 999999" -or
    $tradeCellsText -notmatch "StockReviewCellGroup\.debugPriceLabel\(\)" -or
    $tradeCellsText -notmatch "StockReviewCellGroup\.debugPlanLabel\(\)") {
    throw "Stock-review worst-case weapon debug row must exercise capped storage, price, and plan labels."
}
if ($itemRowsText -notmatch "\.indent\(layout\.itemIndent\)" -or
    $rowLayoutText -notmatch "StockReviewStyle\.WEAPON_INDENT") {
    throw "Stock-review worst-case weapon debug row must use the same icon-indent model as real weapon rows."
}

$tooltipText = Get-Content -LiteralPath $stockReviewTooltipPath -Raw
if ($tooltipText -match "CodexDataV2|setCodexEntry|F10") {
    throw "Stock-review item tooltips must not attach the broken Codex/F10 footer."
}
if ($tooltipText -notmatch "WimGuiPanelPlugin\(TOOLTIP_BACKGROUND, TOOLTIP_BORDER\)" -or
    $tooltipText -notmatch "TOOLTIP_BACKGROUND = Color\(0, 0, 0, 255\)") {
    throw "Stock-review weapon tooltip must render an opaque custom background."
}

Write-Host "WP GUI button style validation passed."
