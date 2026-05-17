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
$stockReviewItemRowsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewItemRows.kt"
$stockReviewRowLayoutPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowLayout.kt"
$stockReviewCellGroupPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewCellGroup.kt"
$stockReviewTradeCellsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeRowCells.kt"
$stockReviewTooltipPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewItemTooltip.kt"
$stockReviewActionControlsPath = Join-Path $kotlinGuiDir "stockreview\controls\StockReviewActionControls.kt"
$stockReviewRowSpecPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewRowSpec.kt"
$stockReviewListRowPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewListRow.kt"
$stockReviewFooterSpecPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFooterSpec.kt"
$stockReviewFooterButtonsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewFooterButtons.kt"
$stockReviewActionRowRendererPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewActionRowRenderer.kt"
$stockReviewActionRowButtonsPath = Join-Path $kotlinGuiDir "stockreview\rendering\StockReviewActionRowButtons.kt"

foreach ($requiredPath in @($stockReviewStylePath, $stockReviewListModelPath, $stockReviewReviewModelPath, $stockReviewItemRowsPath, $stockReviewRowLayoutPath, $stockReviewCellGroupPath, $stockReviewTradeCellsPath, $stockReviewTooltipPath, $stockReviewActionControlsPath, $stockReviewRowSpecPath, $stockReviewListRowPath, $stockReviewFooterSpecPath, $stockReviewFooterButtonsPath, $stockReviewActionRowRendererPath, $stockReviewActionRowButtonsPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required stock-review UI source missing: $requiredPath"
    }
}

$stockReviewSourceRoot = Join-Path $kotlinGuiDir "stockreview"
$stockReviewSourceFiles = @(Get-ChildItem -Path $stockReviewSourceRoot -Recurse -File -Include *.kt)
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
if (-not $listModelText.Contains('" [$typeLabel: $itemTypes, "') -or
    -not $listModelText.Contains('"Selling: ${maxOf(0, selling)}, "') -or
    $listModelText.Contains('"Selling: ${maxOf(0, selling)} | "')) {
    throw "Stock-review category headings must use comma-separated type/selling/buying summaries."
}
$itemRowsText = Get-Content -LiteralPath $stockReviewItemRowsPath -Raw
if ($itemRowsText -notmatch "\.indent\(layout\.itemIndent\)" -or
    $itemRowsText -notmatch "StockReviewRowIcon\.item\(record\)" -or
    $rowLayoutText -notmatch "StockReviewStyle\.WEAPON_INDENT") {
    throw "Main stock-review item rows must start at the category indent; the icon supplies the next visual indent."
}

$reviewModelText = Get-Content -LiteralPath $stockReviewReviewModelPath -Raw
if ($itemRowsText -notmatch "\.indent\(layout\.itemIndent\)" -or
    $itemRowsText -notmatch "StockReviewRowIcon\.item\(record\)" -or
    $rowLayoutText -notmatch "fun review\(\): StockReviewRowLayout") {
    throw "Review stock-review item rows must use the same icon-indent model as the main trade screen."
}
if ($reviewModelText -notmatch "SHOW_WIDTH_TEST_ROWS && StockReviewTradeGroup\.BUYING == tradeGroup" -or
    $reviewModelText -notmatch "StockReviewItemRows\.addWorstCaseRow\(rows, layout\)" -or
    $rowLayoutText -notmatch "fun review\(\): StockReviewRowLayout") {
    throw "Review stock-review worst-case debug row must be visible in the buying group and exercise capped storage labels."
}

$tradeCellsText = Get-Content -LiteralPath $stockReviewTradeCellsPath -Raw
$cellGroupText = Get-Content -LiteralPath $stockReviewCellGroupPath -Raw
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
