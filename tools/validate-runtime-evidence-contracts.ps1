param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

$collectorPath = Join-Path $PSScriptRoot "collect-runtime-validation-evidence.ps1"

function ConvertTo-PowerShellLiteral {
    param([string]$Value)

    return "'$($Value.Replace("'", "''"))'"
}

function Invoke-CollectorCommand {
    param(
        [string]$Label,
        [string]$Command,
        [int]$ExpectedExitCode = 0,
        [string[]]$ExpectedOutput = @()
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(
            & powershell -NoProfile -ExecutionPolicy Bypass -Command $Command 2>&1 |
                ForEach-Object { $_.ToString() }
        )
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $text = $output -join "`n"

    if ($exitCode -eq $ExpectedExitCode) {
        Add-Pass "$Label exit code is $ExpectedExitCode"
    } else {
        Add-Failure "$Label expected exit code $ExpectedExitCode but was $exitCode"
    }

    foreach ($needle in $ExpectedOutput) {
        if ($text.Contains($needle)) {
            Add-Pass "$Label output contains $needle"
        } else {
            Add-Failure "$Label output missing $needle"
        }
    }
}

function New-TempLogPath {
    param([string]$Name)

    return Join-Path ([System.IO.Path]::GetTempPath()) $Name
}

$collector = Read-Text "tools/collect-runtime-validation-evidence.ps1"
foreach ($needle in @(
    "[switch]`$RequireDeployParity",
    "[switch]`$RequireLiveJar",
    "[switch]`$RequireRollbackPass",
    "[string[]]`$ExpectFailureStep",
    "[string[]]`$ExpectShipHull",
    "Manual proof still required for F8 UI, Luna UI, campaign mutation, local ship buy/sell",
    "Invoke-ValidationCommand -Label `"rollback diagnostic analysis`"",
    "Invoke-ValidationCommand -Label `"ship catalog diagnostic analysis`"",
    "ConvertTo-PowerShellArrayLiteral -Values `$expectedSteps",
    "ConvertTo-PowerShellArrayLiteral -Values `$expectedHulls",
    "`$shipRequired = `$ExpectShipHull.Count -gt 0",
    "Invoke-ValidationCommand -Label `"ship catalog diagnostic analysis`" -Command `$shipCommand -Required:`$shipRequired"
)) {
    Assert-Contains "collect-runtime-validation-evidence.ps1 contract" $collector $needle
}

$shipAnalyzer = Read-Text "tools/analyze-ship-catalog-diagnostics.ps1"
foreach ($needle in @(
    "function Get-RequiredIntField",
    "Ship catalog summary missing required field",
    "Ship catalog summary field",
    "is not an integer"
)) {
    Assert-Contains "analyze-ship-catalog-diagnostics.ps1 contract" $shipAnalyzer $needle
}

$collectorLiteral = ConvertTo-PowerShellLiteral -Value $collectorPath

Invoke-CollectorCommand `
    -Label "optional runtime evidence collection" `
    -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral" `
    -ExpectedExitCode 0 `
    -ExpectedOutput @(
        "SKIP: Deploy status, deploy parity, and live jar proof require STARSECTOR_DIRECTORY.",
        "INFO: rollback diagnostic analysis completed without being required.",
        "INFO: Manual proof still required for F8 UI, Luna UI, campaign mutation, local ship buy/sell",
        "PASS: Runtime validation evidence collection completed."
    )

Invoke-CollectorCommand `
    -Label "required rollback evidence missing log" `
    -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral -RequireRollbackPass" `
    -ExpectedExitCode 1 `
    -ExpectedOutput @(
        "FAIL: No log path supplied. Pass -LogPath or set STARSECTOR_DIRECTORY.",
        "FAIL: rollback diagnostic analysis failed with exit code 1",
        "Runtime validation evidence collection failed."
    )

$rollbackLog = New-TempLogPath "wp-runtime-evidence-rollback-contract.log"
$shipLog = New-TempLogPath "wp-runtime-evidence-ship-contract.log"
try {
    @(
        "WP_STOCK_REVIEW_ROLLBACK status=PASS operation=buy item=W:test quantity=1 failedStep=after-source-removal restoredCargos=2 failedCargos=0 creditsRestored=true countsRestored=true creditsBefore=100 creditsAtFailure=50 creditsAfterRollback=100 touched=player",
        "WP_STOCK_REVIEW_ROLLBACK status=PASS operation=buy item=W:test quantity=1 failedStep=after-player-cargo-remove restoredCargos=2 failedCargos=0 creditsRestored=true countsRestored=true creditsBefore=100 creditsAtFailure=50 creditsAfterRollback=100 touched=player",
        "WP_STOCK_REVIEW_ROLLBACK status=PASS operation=buy item=W:test quantity=1 failedStep=after-player-cargo-add restoredCargos=2 failedCargos=0 creditsRestored=true countsRestored=true creditsBefore=100 creditsAtFailure=50 creditsAfterRollback=100 touched=player",
        "WP_STOCK_REVIEW_ROLLBACK status=PASS operation=buy item=W:test quantity=1 failedStep=after-target-cargo-add restoredCargos=2 failedCargos=0 creditsRestored=true countsRestored=true creditsBefore=100 creditsAtFailure=50 creditsAfterRollback=100 touched=player",
        "WP_STOCK_REVIEW_ROLLBACK status=PASS operation=buy item=W:test quantity=1 failedStep=after-credit-mutation restoredCargos=2 failedCargos=0 creditsRestored=true countsRestored=true creditsBefore=100 creditsAtFailure=50 creditsAfterRollback=100 touched=player"
    ) | Set-Content -LiteralPath $rollbackLog -Encoding UTF8

    $rollbackLiteral = ConvertTo-PowerShellLiteral -Value $rollbackLog
    Invoke-CollectorCommand `
        -Label "required rollback evidence with expected steps" `
        -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral -LogPath $rollbackLiteral -RequireRollbackPass" `
        -ExpectedExitCode 0 `
        -ExpectedOutput @(
            "PASS: rollback diagnostic analysis completed.",
            "operation=buy",
            "PASS: Runtime validation evidence collection completed."
        )

    @(
        "WP_STOCK_REVIEW_ROLLBACK status=PASS operation=buy item=W:test quantity=1 failedStep=after-source-removal creditsRestored=true countsRestored=true touched=player"
    ) | Set-Content -LiteralPath $rollbackLog -Encoding UTF8

    Invoke-CollectorCommand `
        -Label "required rollback evidence rejects malformed records" `
        -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral -LogPath $rollbackLiteral -RequireRollbackPass -ExpectFailureStep @('after-source-removal')" `
        -ExpectedExitCode 1 `
        -ExpectedOutput @(
            "Rollback diagnostic record missing required field",
            "Runtime validation evidence collection failed."
        )

    @(
        "WP_STOCK_REVIEW_ROLLBACK status=MAYBE operation=buy item=W:test quantity=1 failedStep=after-source-removal restoredCargos=2 failedCargos=0 creditsRestored=true countsRestored=true creditsBefore=100 creditsAtFailure=50 creditsAfterRollback=100 touched=player"
    ) | Set-Content -LiteralPath $rollbackLog -Encoding UTF8

    Invoke-CollectorCommand `
        -Label "required rollback evidence rejects unsupported status" `
        -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral -LogPath $rollbackLiteral -RequireRollbackPass -ExpectFailureStep @('after-source-removal')" `
        -ExpectedExitCode 1 `
        -ExpectedOutput @(
            "Rollback diagnostic record has unsupported status 'MAYBE'",
            "Runtime validation evidence collection failed."
        )

    @(
        "WP_SHIP_CATALOG_DIAG PASS summary observedHullTypes=0 theoreticalHullTypes=2 common=2 uncommon=0 rare=0 veryRare=0",
        "WP_SHIP_CATALOG_DIAG theoretical hull=hull_alpha",
        "WP_SHIP_CATALOG_DIAG theoretical hull=hull_beta"
    ) | Set-Content -LiteralPath $shipLog -Encoding UTF8

    $shipLiteral = ConvertTo-PowerShellLiteral -Value $shipLog
    Invoke-CollectorCommand `
        -Label "ship catalog evidence expected hulls" `
        -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral -LogPath $shipLiteral -ExpectShipHull @('hull_alpha', 'hull_beta')" `
        -ExpectedExitCode 0 `
        -ExpectedOutput @(
            "PASS: Expected hull 'hull_alpha' appeared in diagnostic detail.",
            "PASS: Expected hull 'hull_beta' appeared in diagnostic detail.",
            "PASS: Runtime validation evidence collection completed."
        )

    @(
        "WP_SHIP_CATALOG_DIAG PASS summary observedHullTypes=0 theoreticalHullTypes=2 common=2 uncommon=0 rare=0",
        "WP_SHIP_CATALOG_DIAG theoretical hull=hull_alpha"
    ) | Set-Content -LiteralPath $shipLog -Encoding UTF8

    Invoke-CollectorCommand `
        -Label "ship catalog evidence rejects malformed summary" `
        -Command "`$env:STARSECTOR_DIRECTORY = ''; & $collectorLiteral -LogPath $shipLiteral -ExpectShipHull @('hull_alpha')" `
        -ExpectedExitCode 1 `
        -ExpectedOutput @(
            "Ship catalog summary missing required field 'veryRare'.",
            "FAIL: ship catalog diagnostic analysis failed with exit code 1",
            "Runtime validation evidence collection failed."
        )
} finally {
    foreach ($path in @($rollbackLog, $shipLog)) {
        if (Test-Path -LiteralPath $path) {
            Remove-Item -LiteralPath $path -Force
        }
    }
}

if ($failures.Count -gt 0) {
    throw "Runtime evidence contract validation failed with $($failures.Count) failure(s)."
}

Write-Host "Runtime evidence contract validation passed."
