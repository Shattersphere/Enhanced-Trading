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
$stockReviewTradeCellsPath = Join-Path $kotlinGuiDir "stockreview\rows\StockReviewTradeRowCells.kt"
$stockReviewTooltipPath = Join-Path $kotlinGuiDir "stockreview\tooltips\StockReviewItemTooltip.kt"

foreach ($requiredPath in @($stockReviewStylePath, $stockReviewListModelPath, $stockReviewReviewModelPath, $stockReviewTradeCellsPath, $stockReviewTooltipPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required stock-review UI source missing: $requiredPath"
    }
}

$styleText = Get-Content -LiteralPath $stockReviewStylePath -Raw
if ($styleText -notmatch "const val SHOW_WIDTH_TEST_ROWS = true") {
    throw "Stock-review worst-case debug rows must remain visible."
}
if ($styleText -notmatch "const val ROW_ICON_INDENT = ACTION_BUTTON_HEIGHT \+ BUTTON_GAP" -or
    $styleText -notmatch "const val WEAPON_INDENT = ROW_ICON_INDENT") {
    throw "Stock-review weapon indent must match the rendered row icon footprint."
}
if ($styleText -notmatch "const val STOCK_CELL_WIDTH = 156f" -or
    $styleText -notmatch "const val REVIEW_STOCK_CELL_WIDTH = STOCK_CELL_WIDTH") {
    throw "Stock-review storage cells must be wide enough for the capped worst-case storage label in trade and review screens."
}

$listModelText = Get-Content -LiteralPath $stockReviewListModelPath -Raw
if (-not $listModelText.Contains('" [$typeLabel: $itemTypes, "') -or
    -not $listModelText.Contains('"Selling: ${maxOf(0, selling)}, "') -or
    $listModelText.Contains('"Selling: ${maxOf(0, selling)} | "')) {
    throw "Stock-review category headings must use comma-separated type/selling/buying summaries."
}
if ($listModelText -notmatch "StockReviewStyle\.WEAPON_INDENT,\s*StockReviewRowIcon\.item\(record\)") {
    throw "Main stock-review item rows must start at the category indent; the icon supplies the next visual indent."
}

$reviewModelText = Get-Content -LiteralPath $stockReviewReviewModelPath -Raw
if ($reviewModelText -notmatch "StockReviewStyle\.WEAPON_INDENT,\s*StockReviewRowIcon\.item\(record\)") {
    throw "Review stock-review item rows must use the same icon-indent model as the main trade screen."
}

$tradeCellsText = Get-Content -LiteralPath $stockReviewTradeCellsPath -Raw
if ($tradeCellsText -notmatch "Debug Worst-Case Suzuki-Clapteryon Thermal Prokector") {
    throw "Stock-review worst-case weapon debug row label is missing."
}
if ($tradeCellsText -notmatch "Storage: 99\+ \[-99\+\]" -or
    $tradeCellsText -notmatch "Price: 99,999\+" -or
    $tradeCellsText -notmatch "Selling: 99\+ \[999,999\+") {
    throw "Stock-review worst-case weapon debug row must exercise capped storage, price, and plan labels."
}
if ($tradeCellsText -notmatch "StockReviewStyle\.WEAPON_INDENT") {
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
