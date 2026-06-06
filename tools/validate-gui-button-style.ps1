$repoRoot = Split-Path -Parent $PSScriptRoot
$legacyGuiDir = Join-Path $repoRoot "src\weaponsprocurement\gui"
$kotlinGuiDir = Join-Path $repoRoot "src\main\kotlin\weaponsprocurement\ui"
$controlsCandidates = @(
    (Join-Path $legacyGuiDir "WimGuiControls.java"),
    (Join-Path $kotlinGuiDir "WimGuiControls.kt")
)
$controlsPath = $controlsCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
$buttonSpecPath = Join-Path $kotlinGuiDir "WimGuiButtonSpec.kt"
$modalFooterPath = Join-Path $kotlinGuiDir "WimGuiModalFooter.kt"

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

$directButtonSpecConstructorHits = @($sourceFiles |
    Where-Object { $_.FullName -ne $buttonSpecPath } |
    Select-String -Pattern "new\s+WimGuiButtonSpec|WimGuiButtonSpec(?:<[^>]+>)?\s*\(")
if ($directButtonSpecConstructorHits.Count -gt 0) {
    throw "WimGuiButtonSpec construction must stay inside WimGuiButtonSpec.kt factories. Hits:`n$($directButtonSpecConstructorHits -join "`n")"
}

$buttonSpecsOfHits = @($sourceFiles | Select-String -Pattern "WimGuiButtonSpecs\.of\(")
if ($buttonSpecsOfHits.Count -ne 1 -or
    $buttonSpecsOfHits[0].Path -ne $modalFooterPath -or
    $buttonSpecsOfHits[0].Line -notmatch "WimGuiButtonSpecs\.of\(left\)") {
    throw "WimGuiButtonSpecs.of must remain limited to the known non-null modal-footer call unless a new nullable-vararg policy is added. Hits:`n$($buttonSpecsOfHits -join "`n")"
}

$controlsText = Get-Content -LiteralPath $controlsPath -Raw
if ($controlsText -notmatch "dimForIdle\(idle\)") {
    throw "WimGuiControls.addButton must dim the inner button fill from the idle color."
}
if ($controlsText -notmatch "(Color|val) hover = .*colors(\?|\.)\.hover|val hover = colors\?\.hover") {
    throw "WimGuiControls.addButton must keep hover color separate from the dimmed inner idle fill."
}

$stockReviewStylePath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewStyle.kt"
$weaponStockRecordPath = Join-Path $repoRoot "src\main\kotlin\weaponsprocurement\stock\item\WeaponStockRecord.kt"
$stockReviewListModelPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListModel.kt"
$stockReviewReviewModelPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewReviewListModel.kt"
$stockReviewListSectionPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListSection.kt"
$stockReviewListSourceSpecPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewListSourceSpec.kt"
$stockReviewListEmptyRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListEmptyRows.kt"
$stockReviewItemTypeSectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemTypeSections.kt"
$stockReviewStockCategorySectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewStockCategorySections.kt"
$stockReviewTradeGroupSectionsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeGroupSections.kt"
$stockReviewItemRowFramePath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemRowFrame.kt"
$stockReviewTradeItemRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeItemRows.kt"
$stockReviewReviewItemRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewReviewItemRows.kt"
$stockReviewWorstCaseItemRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewWorstCaseItemRows.kt"
$stockReviewSectionRowAppendersPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewSectionRowAppenders.kt"
$stockReviewItemInfoRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemInfoRows.kt"
$stockReviewRowLayoutPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowLayout.kt"
$stockReviewDetailRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewDetailRows.kt"
$stockReviewDetailRowSpecPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewDetailRowSpec.kt"
$stockReviewSourceAllocationRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewSourceAllocationRows.kt"
$stockReviewCellGroupPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewCellGroup.kt"
$stockReviewDebugCellGroupPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewDebugCellGroup.kt"
$stockReviewTradeCellsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeRowCells.kt"
$stockReviewColorDebugRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewColorDebugRows.kt"
$stockReviewShipCatalogDebugRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewShipCatalogDebugRows.kt"
$stockReviewTradeSummaryRendererPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeSummaryRenderer.kt"
$stockReviewTradeSummaryFieldsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeSummaryFields.kt"
$stockReviewTooltipPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewItemTooltip.kt"
$stockReviewWingTooltipLayoutBuilderPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewWingTooltipLayoutBuilder.kt"
$stockReviewWingTooltipRendererPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewWingTooltipRenderer.kt"
$stockReviewWeaponTooltipIconGridRendererPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewWeaponTooltipIconGridRenderer.kt"
$stockReviewWeaponTooltipTextRendererPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewWeaponTooltipTextRenderer.kt"
$stockReviewTooltipPanelPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewTooltipPanel.kt"
$stockReviewShipTooltipPath = Join-Path $kotlinGuiDir "stockreview\ships\StockReviewShipTooltip.kt"
$stockReviewItemInfoFieldsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemInfoFields.kt"
$stockReviewActionControlsPath = Join-Path $kotlinGuiDir "stockreview\controls\StockReviewActionControls.kt"
$stockReviewRowSpecPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowSpec.kt"
$stockReviewRowSpecsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowSpecs.kt"
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
$stockReviewLegacyItemRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemRows.kt"
$stockReviewLegacyHeadingRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewHeadingRows.kt"
$stockReviewActionRowRendererPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewActionRowRenderer.kt"
$stockReviewActionRowButtonsPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewActionRowButtons.kt"
$wimGuiTooltipPath = Join-Path $kotlinGuiDir "WimGuiTooltip.kt"

foreach ($requiredPath in @($stockReviewStylePath, $weaponStockRecordPath, $stockReviewListModelPath, $stockReviewReviewModelPath, $stockReviewListSectionPath, $stockReviewListSourceSpecPath, $stockReviewListEmptyRowsPath, $stockReviewItemTypeSectionsPath, $stockReviewStockCategorySectionsPath, $stockReviewTradeGroupSectionsPath, $stockReviewItemRowFramePath, $stockReviewTradeItemRowsPath, $stockReviewReviewItemRowsPath, $stockReviewWorstCaseItemRowsPath, $stockReviewSectionRowAppendersPath, $stockReviewItemInfoRowsPath, $stockReviewRowLayoutPath, $stockReviewDetailRowsPath, $stockReviewDetailRowSpecPath, $stockReviewSourceAllocationRowsPath, $stockReviewCellGroupPath, $stockReviewDebugCellGroupPath, $stockReviewTradeCellsPath, $stockReviewColorDebugRowsPath, $stockReviewShipCatalogDebugRowsPath, $stockReviewTradeSummaryRendererPath, $stockReviewTradeSummaryFieldsPath, $stockReviewTooltipPath, $stockReviewWingTooltipLayoutBuilderPath, $stockReviewWingTooltipRendererPath, $stockReviewWeaponTooltipIconGridRendererPath, $stockReviewWeaponTooltipTextRendererPath, $stockReviewTooltipPanelPath, $stockReviewShipTooltipPath, $stockReviewItemInfoFieldsPath, $stockReviewActionControlsPath, $stockReviewRowSpecPath, $stockReviewRowSpecsPath, $stockReviewListRowPath, $stockReviewFooterSpecPath, $stockReviewFooterButtonsPath, $stockReviewItemTypeHeadingRowsPath, $stockReviewStockCategoryHeadingRowsPath, $stockReviewTradeGroupHeadingRowsPath, $stockReviewFilterHeadingRowsPath, $stockReviewItemDetailHeadingRowsPath, $stockReviewFilterRowsPath, $stockReviewFilterGroupSectionsPath, $stockReviewActionRowRendererPath, $stockReviewActionRowButtonsPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required stock-review UI source missing: $requiredPath"
    }
}
if (Test-Path -LiteralPath $stockReviewLegacyItemRowsPath) {
    throw "StockReviewItemRows must stay split into trade, review, debug, and frame row owners."
}
if (Test-Path -LiteralPath $stockReviewLegacyHeadingRowsPath) {
    throw "StockReviewHeadingRows must stay split into focused heading owners."
}
if (-not (Test-Path -LiteralPath $wimGuiTooltipPath)) {
    throw "Required GUI tooltip source missing: $wimGuiTooltipPath"
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
if ($styleText -notmatch "fun showDebugUi\(\): Boolean = WeaponsProcurementConfig\.isDebugUiEnabled\(\)") {
    throw "Stock-review worst-case debug rows and debug controls must be gated by WeaponsProcurementConfig.isDebugUiEnabled()."
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
$listSourceSpecText = Get-Content -LiteralPath $stockReviewListSourceSpecPath -Raw
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
$sectionRowAppendersText = Get-Content -LiteralPath $stockReviewSectionRowAppendersPath -Raw
if ($listSourceSpecText -notmatch "when \(kind\)" -or
    $listSourceSpecText -notmatch "StockReviewListSourceKind\.TRADE" -or
    $listSourceSpecText -notmatch "StockReviewListModel\.build" -or
    $listSourceSpecText -notmatch "StockReviewReviewListModel\.build" -or
    $listSourceSpecText -notmatch "StockReviewFilterListModel\.build" -or
    $listSourceSpecText -notmatch "StockReviewColorDebugRows\.build" -or
    $listSourceSpecText -notmatch "StockReviewShipCatalogDebugRows\.build" -or
    $listSourceSpecText -match "builder:" -or
    $listSourceSpecText -match "invoke\(context\)" -or
    $listSourceSpecText -match "\{ context ->") {
    throw "Stock-review list sources must dispatch through explicit StockReviewListSourceKind handling, not lambda builders."
}
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
    $stockCategorySectionsText -notmatch "includesWorstCaseRow,\s*itemType,\s*state," -or
    $stockCategorySectionsText -notmatch "StockReviewStockCategoryHeadingRows\.stockCategory" -or
    $stockCategorySectionsText -notmatch "StockReviewListSection\.add" -or
    $stockCategorySectionsText -notmatch "StockReviewListSectionSpec" -or
    $stockCategorySectionsText -notmatch "StockReviewTradeRecordRowAppender" -or
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
    $tradeGroupSectionsText -notmatch "StockReviewListSection\.addHeading" -or
    $tradeGroupSectionsText -notmatch "StockReviewReviewItemGroup\.ORDERED" -or
    $tradeGroupSectionsText -notmatch "StockReviewReviewItemGroupHeadingRows\.reviewItemGroup" -or
    $tradeGroupSectionsText -notmatch "fun tradesForItemGroup" -or
    $tradeGroupSectionsText -notmatch "StockReviewReviewTradeRowAppender" -or
    $tradeGroupSectionsText -notmatch "StockReviewTradeGroupHeadingRows\.reviewGroup" -or
    $tradeGroupSectionsText -notmatch "includesWorstCaseRow" -or
    $tradeGroupSectionsText -notmatch "fun tradesFrom" -or
    $tradeGroupSectionsText -notmatch "class StockReviewTradeGroupRows" -or
    $tradeGroupSectionsText -notmatch "fun allEmpty" -or
    $reviewModelText -notmatch "StockReviewTradeGroupSections\.build" -or
    $reviewModelText -match "StockReviewTradeGroup\.BUYING" -or
    $reviewModelText -match "StockReviewTradeGroup\.SELLING" -or
    $reviewModelText -match "reviewTradesForGroup" -or
    $reviewModelText -match "StockReviewListSection\.") {
    throw "Stock-review review-group order, trade splitting, top gaps, headings, and debug-row policy must live in StockReviewTradeGroupSections."
}
if ($sectionRowAppendersText -notmatch "class StockReviewTradeRecordRowAppender" -or
    $sectionRowAppendersText -notmatch "class StockReviewReviewTradeRowAppender" -or
    $sectionRowAppendersText -notmatch "StockReviewSectionRowAppender<WeaponStockRecord>" -or
    $sectionRowAppendersText -notmatch "StockReviewSectionRowAppender<StockReviewPendingTrade>" -or
    $sectionRowAppendersText -notmatch "StockReviewTradeItemRows\.add" -or
    $sectionRowAppendersText -notmatch "StockReviewReviewItemRows\.add" -or
    $stockCategorySectionsText -match "itemAppender \{" -or
    $tradeGroupSectionsText -match "itemAppender \{" -or
    $listSectionText -match "class Builder" -or
    $listSectionText -match "fun builder" -or
    $listSectionText -match "invoke\(rows") {
    throw "Stock-review list sections must use named section specs and row appenders instead of generic builder lambdas."
}
$filterListModelText = Get-Content -LiteralPath (Join-Path $kotlinGuiDir "stockreview\state\StockReviewFilterListModel.kt") -Raw
if ($filterRowsText -notmatch "object StockReviewFilterRows" -or
    $filterRowsText -notmatch "fun addActive" -or
    $filterRowsText -notmatch "fun available" -or
    $filterRowsText -notmatch "StockReviewRowSpecs\.filter" -or
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
$itemRowFrameText = Get-Content -LiteralPath $stockReviewItemRowFramePath -Raw
$tradeItemRowsText = Get-Content -LiteralPath $stockReviewTradeItemRowsPath -Raw
$reviewItemRowsText = Get-Content -LiteralPath $stockReviewReviewItemRowsPath -Raw
$worstCaseItemRowsText = Get-Content -LiteralPath $stockReviewWorstCaseItemRowsPath -Raw
if ($itemRowFrameText -notmatch "StockReviewRowSpecs\.item\(label, cells, action, tooltip, tooltipCreator, layout\.itemIndent, icon\)" -or
    $tradeItemRowsText -notmatch "StockReviewRowIcon\.item\(record\)" -or
    $sectionRowAppendersText -notmatch "StockReviewTradeItemRows\.add" -or
    $rowLayoutText -notmatch "StockReviewStyle\.WEAPON_INDENT") {
    throw "Main stock-review item rows must start at the category indent; the icon supplies the next visual indent."
}

$itemInfoRowsText = Get-Content -LiteralPath $stockReviewItemInfoRowsPath -Raw
if ($itemRowFrameText -notmatch "StockReviewRowSpecs\.item\(label, cells, action, tooltip, tooltipCreator, layout\.itemIndent, icon\)" -or
    $reviewItemRowsText -notmatch "StockReviewRowIcon\.item\(record\)" -or
    $sectionRowAppendersText -notmatch "StockReviewReviewItemRows\.add" -or
    $rowLayoutText -notmatch "fun review\(\): StockReviewRowLayout") {
    throw "Review stock-review item rows must use the same icon-indent model as the main trade screen."
}
if ($listSectionText -notmatch "class StockReviewListSection" -or
    $listSectionText -notmatch "class StockReviewListSectionSpec" -or
    $listSectionText -notmatch "interface StockReviewSectionRowAppender" -or
    $listSectionText -notmatch "fun <T> add" -or
    $listSectionText -notmatch "StockReviewStyle\.showDebugUi\(\) && spec\.includeWorstCaseRow" -or
    $listSectionText -notmatch "StockReviewWorstCaseItemRows\.add\(rows, layout, spec\.worstCaseItemType \?: StockItemType\.WEAPON, spec\.state\)" -or
    $listSectionText -notmatch "fun addHeading" -or
    $itemTypeSectionsText -notmatch "StockReviewListSection\.addHeading" -or
    $stockCategorySectionsText -notmatch "StockReviewListSection\.add" -or
    $tradeGroupSectionsText -notmatch "StockReviewListSection\.add" -or
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
$detailRowSpecText = Get-Content -LiteralPath $stockReviewDetailRowSpecPath -Raw
$sourceAllocationRowsText = Get-Content -LiteralPath $stockReviewSourceAllocationRowsPath -Raw
$cellGroupText = Get-Content -LiteralPath $stockReviewCellGroupPath -Raw
$debugCellGroupText = Get-Content -LiteralPath $stockReviewDebugCellGroupPath -Raw
$colorDebugRowsText = Get-Content -LiteralPath $stockReviewColorDebugRowsPath -Raw
$shipCatalogDebugRowsText = Get-Content -LiteralPath $stockReviewShipCatalogDebugRowsPath -Raw
$itemInfoFieldsText = Get-Content -LiteralPath $stockReviewItemInfoFieldsPath -Raw
$weaponStockRecordText = Get-Content -LiteralPath $weaponStockRecordPath -Raw
$tradeSummaryRendererText = Get-Content -LiteralPath $stockReviewTradeSummaryRendererPath -Raw
$tradeSummaryFieldsText = Get-Content -LiteralPath $stockReviewTradeSummaryFieldsPath -Raw
$actionControlsText = Get-Content -LiteralPath $stockReviewActionControlsPath -Raw
$rowSpecText = Get-Content -LiteralPath $stockReviewRowSpecPath -Raw
$rowSpecsText = Get-Content -LiteralPath $stockReviewRowSpecsPath -Raw
$listRowText = Get-Content -LiteralPath $stockReviewListRowPath -Raw
$footerSpecText = Get-Content -LiteralPath $stockReviewFooterSpecPath -Raw
$footerButtonsText = Get-Content -LiteralPath $stockReviewFooterButtonsPath -Raw
$actionRowRendererText = Get-Content -LiteralPath $stockReviewActionRowRendererPath -Raw
$actionRowButtonsText = Get-Content -LiteralPath $stockReviewActionRowButtonsPath -Raw
if ($actionControlsText -notmatch "class StockReviewActionRef" -or
    $actionControlsText -notmatch "class StockReviewButtonDefinition" -or
    $actionControlsText -notmatch "fun interface StockReviewButtonValue" -or
    $actionControlsText -notmatch "fun <C> static\(" -or
    $actionControlsText -notmatch "fun <C> staticWithEnabled" -or
    $actionControlsText -notmatch "fun <C> dynamic" -or
    $actionControlsText -notmatch "fun <C> constant" -or
    $actionControlsText -notmatch "StockReviewActionGuards\.requireGroup" -or
    $rowSpecText -notmatch "fun create\(" -or
    $rowSpecText -match "class Builder" -or
    $rowSpecText -match "fun builder\(" -or
    $rowSpecsText -notmatch "object StockReviewRowSpecs" -or
    $rowSpecsText -notmatch "StockReviewRowSpec\.create" -or
    $rowSpecsText -match "StockReviewRowSpec\.builder" -or
    $rowSpecsText -notmatch "StockReviewActionRef\.rowExpansion" -or
    $rowSpecsText -notmatch "StockReviewActionRef\.filters" -or
    $rowSpecsText -notmatch "StockReviewActionRef\.scroll" -or
    $listRowText -match "fun category|fun filterHeading|fun item\(|fun form\(|fun empty\(|fun scroll\(|fun review" -or
    $worstCaseItemRowsText -notmatch "StockReviewActionRef\.rowExpansion") {
    throw "Stock-review row actions and row-shape factories must use StockReviewRowSpecs plus group-checked StockReviewActionRef helpers."
}
if ($footerSpecText -match "StockReviewActionGroup" -or
    $footerSpecText -match "BUTTON_FACTORY\.button" -or
    $footerSpecText -match "\{ context ->" -or
    $footerSpecText -match "invoke\(context\)" -or
    $footerSpecText -notmatch "enum class StockReviewFooterButtonSetKind" -or
    $footerSpecText -notmatch "buttonSetKind: StockReviewFooterButtonSetKind" -or
    $footerSpecText -notmatch "StockReviewFooterButtonSetKind\.TRADE" -or
    $footerSpecText -notmatch "StockReviewFooterButtonSetKind\.REVIEW" -or
    $footerSpecText -notmatch "StockReviewFooterButtons\.left" -or
    $footerSpecText -notmatch "StockReviewFooterButtons\.right" -or
    $footerButtonsText -notmatch "StockReviewButtonDefinition" -or
    $footerButtonsText -notmatch "object StockReviewFooterButtonPolicies" -or
    $footerButtonsText -notmatch "class StockReviewFooterButtonSet" -or
    $footerButtonsText -notmatch "StockReviewButtonDefinition\.staticWithEnabled" -or
    $footerButtonsText -notmatch "StockReviewButtonDefinition\.static" -or
    $footerButtonsText -match "StockReviewButtonDefinition\.alwaysEnabled" -or
    $footerButtonsText -match "context: StockReviewFooterContext ->" -or
    $footerButtonsText -notmatch "fun left\(" -or
    $footerButtonsText -notmatch "fun right\(" -or
    $footerButtonsText -notmatch "buttonSet\(kind\)" -or
    $footerButtonsText -notmatch "PURCHASE_ALL" -or
    $footerButtonsText -notmatch "RESET_ALL") {
    throw "Stock-review footer specs must use explicit button-set kinds, with button definitions declared in StockReviewFooterButtons."
}
if ($actionRowRendererText -match "StockReviewActionGroup" -or
    $actionRowRendererText -match "BUTTON_FACTORY\.button" -or
    $actionRowRendererText -notmatch "StockReviewActionRowButtons\.build\(" -or
    $actionRowRendererText -notmatch "modeSpec\.actionRowKind" -or
    $actionRowButtonsText -notmatch "StockReviewButtonDefinition" -or
    $actionRowButtonsText -notmatch "object StockReviewActionRowButtonPolicies" -or
    $actionRowButtonsText -notmatch "StockReviewButtonDefinition\.dynamic" -or
    $actionRowButtonsText -notmatch "StockReviewButtonDefinition\.static" -or
    $actionRowButtonsText -match "StockReviewButtonDefinition\.alwaysEnabled" -or
    $actionRowButtonsText -match "\{ context ->" -or
    $actionRowButtonsText -notmatch "class StockReviewActionRowButtonSet" -or
    $actionRowButtonsText -notmatch "buttonSet\(kind\)" -or
    $actionRowButtonsText -notmatch "StockReviewActionRowKind\.TRADE_CONTROLS" -or
    $actionRowButtonsText -notmatch "StockReviewActionRowKind\.NONE" -or
    $actionRowButtonsText -notmatch "BLACK_MARKET" -or
    $actionRowButtonsText -notmatch "WeaponsProcurementConfig\.isDebugShipCatalogViewEnabled") {
    throw "Stock-review action-row renderers must route through explicit action-row button sets."
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
    $tradeItemRowsText -notmatch "StockReviewItemDetailHeadingRows\.itemLabel" -or
    $reviewItemRowsText -notmatch "StockReviewItemDetailHeadingRows\.itemLabel" -or
    $tradeItemRowsText -match "WimGuiToggleHeading" -or
    $reviewItemRowsText -match "WimGuiToggleHeading" -or
    $tradeItemRowsText -match "fun infoSectionKey" -or
    $reviewItemRowsText -match "fun infoSectionKey" -or
    $tradeItemRowsText -match "::info::" -or
    $reviewItemRowsText -match "::info::" -or
    $listModelText -match "StockReviewGroupRows" -or
    $reviewModelText -match "StockReviewGroupRows") {
    throw "Stock-review heading labels/actions/tooltips must route through focused heading owners."
}
if ($itemInfoFieldsText -notmatch "object StockReviewItemInfoFields" -or
    $itemInfoFieldsText -notmatch "StockReviewDetailRows\.fromSpec" -or
    $itemInfoFieldsText -notmatch "StockReviewDetailRowSpec\.itemInfo" -or
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
if ($weaponStockRecordText -notmatch "wingHullSpec\(\)\?\.minCrew" -or
    $weaponStockRecordText -notmatch "wingHullSpec\(\)\?\.hitpoints" -or
    $weaponStockRecordText -notmatch "wingHullSpec\(\)\?\.armorRating" -or
    $weaponStockRecordText -notmatch "wingHullSpec\(\)\?\.engineSpec\?\.maxSpeed" -or
    $weaponStockRecordText -notmatch "wingHullSpec\(\)\?\.fluxCapacity" -or
    $weaponStockRecordText -notmatch "wingHullSpec\(\)\?\.fluxDissipation" -or
    $weaponStockRecordText -notmatch "positiveOneDecimalLabel\(wingHullSpec\(\)\?\.shieldSpec\?\.fluxPerDamageAbsorbed\)" -or
    $weaponStockRecordText -notmatch "positiveRoundedLabel\(wingHullSpec\(\)\?\.shieldSpec\?\.arc\)") {
    throw "Stock-review wing Advanced Info rows must source real fighter hull stats through WeaponStockRecord."
}
if ($detailRowSpecText -notmatch "class StockReviewDetailRowSpec" -or
    $detailRowSpecText -notmatch "fun itemInfo" -or
    $detailRowSpecText -notmatch "fun sourceAllocation" -or
    $detailRowsText -notmatch "object StockReviewDetailRows" -or
    $detailRowsText -notmatch "fun fromSpec" -or
    $detailRowsText -notmatch "fun itemInfo" -or
    $detailRowsText -notmatch "fun sourceAllocation" -or
    $sourceAllocationRowsText -notmatch "object StockReviewSourceAllocationRows" -or
    $sourceAllocationRowsText -notmatch "Purchase Source" -or
    $sourceAllocationRowsText -notmatch "Unavailable" -or
    $sourceAllocationRowsText -notmatch "StockReviewDetailRows\.fromSpec" -or
    $sourceAllocationRowsText -notmatch "StockReviewDetailRowSpec\.sourceAllocation" -or
    $reviewItemRowsText -notmatch "StockReviewSourceAllocationRows\.add" -or
    $reviewItemRowsText -match "addSourceAllocationRows" -or
    $reviewItemRowsText -match "sourceLabel" -or
    $reviewItemRowsText -match "allocationSummary") {
    throw "Stock-review source-allocation rows must route through StockReviewSourceAllocationRows and StockReviewDetailRows."
}
if ($tradeSummaryRendererText -notmatch "StockReviewTradeSummaryFields\.build" -or
    $tradeSummaryRendererText -match "addSummaryRow" -or
    $tradeSummaryRendererText -match "tariffsPaidLabel" -or
    $tradeSummaryFieldsText -notmatch "class StockReviewTradeSummaryField" -or
    $tradeSummaryFieldsText -notmatch "fun warning" -or
    $tradeSummaryFieldsText -notmatch "fun tariffsPaid" -or
    $tradeSummaryFieldsText -notmatch "fun creditsAvailable" -or
    $tradeSummaryFieldsText -notmatch "fun cargoSpaceAvailable" -or
    $tradeSummaryFieldsText -notmatch "object StockReviewTradeSummaryFields" -or
    $tradeSummaryFieldsText -notmatch "Tariffs Paid" -or
    $tradeSummaryFieldsText -notmatch '\[\$percent%\]' -or
    $tradeSummaryFieldsText -notmatch "totalBaseBuyCost") {
    throw "Stock-review footer summary row definitions must live in StockReviewTradeSummaryFields."
}
if ($cellGroupText -notmatch "Debug Worst-Case Suzuki-Clapteryon Thermal Prokector") {
    throw "Stock-review worst-case weapon debug row label is missing."
}
if ($cellGroupText -notmatch "Storage: 99\+ \[-99\+\]" -or
    $cellGroupText -notmatch "MAX_UNIT_PRICE = 99999" -or
    $cellGroupText -notmatch "MAX_TRANSACTION_CREDITS = 999999" -or
    $cellGroupText -notmatch "fun storageLabel" -or
    $cellGroupText -notmatch "fun unitPriceLabel" -or
    $cellGroupText -notmatch "fun planLabel" -or
    $tradeCellsText -notmatch "StockReviewCellGroup\.debugPrice\(layout\)" -or
    $tradeCellsText -notmatch "StockReviewCellGroup\.debugPlan\(layout\)" -or
    $tradeCellsText -notmatch "StockReviewCellGroup\.debugControlCells\(\)") {
    throw "Stock-review worst-case weapon debug row must exercise capped storage, price, and plan labels."
}
$rawTradeCellConstructionHits = @((Get-Item -LiteralPath $stockReviewTradeCellsPath) | Select-String -Pattern "WimGuiRowCell\.info|StockReviewActionCells\.standard")
if ($rawTradeCellConstructionHits.Count -gt 0) {
    throw "Stock-review trade/review row cells must route cell creation through StockReviewCellGroup. Hits:`n$($rawTradeCellConstructionHits -join "`n")"
}
$rawDiagnosticCellConstructionHits = @(
    (Get-Item -LiteralPath $stockReviewColorDebugRowsPath, $stockReviewShipCatalogDebugRowsPath) |
        Select-String -Pattern "WimGuiRowCell\.info|StockReviewActionCells\.standard|private fun info|private fun debugCell|private const val RARITY_WIDTH|private const val PRICE_WIDTH"
)
if ($rawDiagnosticCellConstructionHits.Count -gt 0) {
    throw "Stock-review diagnostic rows must route debug cell construction through StockReviewDebugCellGroup. Hits:`n$($rawDiagnosticCellConstructionHits -join "`n")"
}
if ($debugCellGroupText -notmatch "object StockReviewDebugCellGroup" -or
    $debugCellGroupText -notmatch "fun colorSampleInfo" -or
    $debugCellGroupText -notmatch "fun colorValueButton" -or
    $debugCellGroupText -notmatch "fun colorDelta" -or
    $debugCellGroupText -notmatch "fun shipRarityFill" -or
    $debugCellGroupText -notmatch "StockReviewActionGroup\.DEBUG_MODE" -or
    $debugCellGroupText -notmatch "StockReviewCellGroup\.infoCell" -or
    $colorDebugRowsText -notmatch "StockReviewDebugCellGroup\.colorSampleInfo" -or
    $colorDebugRowsText -notmatch "StockReviewDebugCellGroup\.colorValueButton" -or
    $colorDebugRowsText -notmatch "StockReviewDebugCellGroup\.colorDelta" -or
    $shipCatalogDebugRowsText -notmatch "StockReviewDebugCellGroup\.shipRarityFill" -or
    $shipCatalogDebugRowsText -notmatch "StockReviewDebugCellGroup\.shipPrice") {
    throw "Stock-review color and ship diagnostics must share StockReviewDebugCellGroup factories."
}
if ($itemRowFrameText -notmatch "StockReviewRowSpecs\.item\(label, cells, action, tooltip, tooltipCreator, layout\.itemIndent, icon\)" -or
    $worstCaseItemRowsText -notmatch "StockReviewItemRowFrame\.build" -or
    $rowLayoutText -notmatch "StockReviewStyle\.WEAPON_INDENT") {
    throw "Stock-review worst-case weapon debug row must use the same icon-indent model as real weapon rows."
}

$tooltipText = Get-Content -LiteralPath $stockReviewTooltipPath -Raw
$wingTooltipLayoutBuilderText = Get-Content -LiteralPath $stockReviewWingTooltipLayoutBuilderPath -Raw
$wingTooltipRendererText = Get-Content -LiteralPath $stockReviewWingTooltipRendererPath -Raw
$weaponTooltipIconGridRendererText = Get-Content -LiteralPath $stockReviewWeaponTooltipIconGridRendererPath -Raw
$weaponTooltipTextRendererText = Get-Content -LiteralPath $stockReviewWeaponTooltipTextRendererPath -Raw
$tooltipPanelText = Get-Content -LiteralPath $stockReviewTooltipPanelPath -Raw
$shipTooltipText = Get-Content -LiteralPath $stockReviewShipTooltipPath -Raw
$wimGuiTooltipText = Get-Content -LiteralPath $wimGuiTooltipPath -Raw
if ($tooltipText -match "CodexDataV2|setCodexEntry|F10") {
    throw "Stock-review item tooltips must not attach the broken Codex/F10 footer."
}
if ($tooltipText -notmatch "WimGuiPanelPlugin\(StockReviewTooltipPanel\.ITEM_BACKGROUND, StockReviewTooltipPanel\.ITEM_BORDER\)" -or
    $tooltipPanelText -notmatch "ITEM_BACKGROUND: Color = Color\(0, 0, 0, 255\)") {
    throw "Stock-review weapon tooltip must render an opaque custom background."
}
if ($tooltipText -notmatch "StockReviewWeaponTooltipIconGridRenderer\.addWeaponGrid" -or
    $tooltipText -notmatch "StockReviewWeaponTooltipIconGridRenderer\.addDamageTypeGrid" -or
    $tooltipText -notmatch "StockReviewWeaponTooltipIconGridRenderer\.addSpriteGrid") {
    throw "Stock-review weapon tooltip icon grids must route through StockReviewWeaponTooltipIconGridRenderer."
}
if ($tooltipText -notmatch "StockReviewWeaponTooltipTextRenderer\.addDescription" -or
    $tooltipText -notmatch "StockReviewWeaponTooltipTextRenderer\.addCustomSpecPara" -or
    $tooltipText -notmatch "StockReviewWeaponTooltipTextRenderer\.addDebugDescription" -or
    $weaponTooltipTextRendererText -notmatch "substituteFormatSpecifiers" -or
    $weaponTooltipTextRendererText -notmatch 'tooltipFormat\(value: String\?\): String = value\?\.replace\("%", "%%"\)' -or
    $weaponTooltipTextRendererText -notmatch "Description\.Type\.WEAPON") {
    throw "Stock-review weapon tooltip description and custom text must route through StockReviewWeaponTooltipTextRenderer."
}
if ($tooltipText -notmatch "StockReviewWingTooltipLayoutBuilder\.forDebugProfile" -or
    $tooltipText -notmatch "StockReviewWingTooltipLayoutBuilder\.forRecord" -or
    $wingTooltipLayoutBuilderText -notmatch "object StockReviewWingTooltipLayoutBuilder" -or
    $wingTooltipLayoutBuilderText -notmatch "fun forRecord" -or
    $wingTooltipLayoutBuilderText -notmatch "fun forDebugProfile" -or
    $wingTooltipLayoutBuilderText -notmatch "Description\.Type\.SHIP" -or
    $wingTooltipLayoutBuilderText -notmatch "wingArmamentsLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "StockReviewWingTooltipLayout") {
    throw "Stock-review wing tooltip layout construction must route through StockReviewWingTooltipLayoutBuilder."
}
if ($wingTooltipLayoutBuilderText -notmatch "record\.wingCrewPerFighterLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingHullIntegrityLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingArmorRatingLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingTopSpeedLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingFluxCapacityLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingFluxDissipationLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingShieldEfficiencyLabel" -or
    $wingTooltipLayoutBuilderText -notmatch "record\.wingShieldArcLabel") {
    throw "Stock-review wing tooltip rows must reuse WeaponStockRecord fighter stat labels."
}
if ($wimGuiTooltipText -notmatch "MAX_SCREEN_HEIGHT_FRACTION = 0\.95f" -or
    $wimGuiTooltipText -notmatch "fun maxTooltipHeight\(\): Float" -or
    $wimGuiTooltipText -notmatch "fun capHeight\(height: Float\): Float" -or
    $wimGuiTooltipText -notmatch "WimGuiText\.wrapToWidth" -or
    $tooltipPanelText -notmatch "WimGuiTooltip\.maxTooltipHeight\(\)" -or
    $tooltipPanelText -notmatch "WimGuiTooltip\.capHeight\(height\)" -or
    $tooltipPanelText -notmatch "fun addStatRow\(" -or
    $tooltipPanelText -notmatch "measuredTextWidth" -or
    $wingTooltipRendererText -notmatch "StockReviewTooltipPanel\.capHeight" -or
    $weaponTooltipTextRendererText -notmatch "WimGuiText\.wrapToWidth") {
    throw "Stock-review and generic GUI tooltips must share the 95-percent screen-height cap and measured wrapped truncation policy."
}
if ($shipTooltipText -match "data class TooltipLayout" -or
    $shipTooltipText -match "WimGuiText\.estimatedChars" -or
    $shipTooltipText -notmatch "StockReviewTooltipPanel\.addStatRow" -or
    $shipTooltipText -notmatch "panel\.position\.setSize\(WIDTH, StockReviewTooltipPanel\.capHeight\(max\(MIN_HEIGHT, finalY \+ PAD\)\)\)" -or
    $shipTooltipText -notmatch "WimGuiText\.wrapToWidth") {
    throw "Stock-review ship tooltip must use measured wrapping and post-render height sizing, not a separate estimated layout pass."
}
if (($tooltipText + $wingTooltipRendererText + $weaponTooltipIconGridRendererText) -match "private const val WING_LABEL_WIDTH|private const val GRID_LABEL_WIDTH" -or
    $wingTooltipRendererText -notmatch "MIN_LABEL_WIDTH" -or
    $weaponTooltipIconGridRendererText -notmatch "GRID_MIN_LABEL_WIDTH" -or
    $wingTooltipRendererText -notmatch "StockReviewTooltipPanel\.addStatRow" -or
    $weaponTooltipIconGridRendererText -notmatch "StockReviewTooltipPanel\.addStatRow") {
    throw "Stock-review weapon and wing tooltip stat rows must use measured dynamic label/value splits."
}

Write-Host "WP GUI button style validation passed."
