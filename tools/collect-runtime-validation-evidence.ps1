param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [string]$LogPath = "",
    [switch]$RequireDeployParity,
    [switch]$RequireLiveJar,
    [switch]$RequireRollbackPass,
    [string[]]$ExpectFailureStep = @(
        "after-source-removal",
        "after-player-cargo-remove",
        "after-player-cargo-add",
        "after-target-cargo-add",
        "after-credit-mutation"
    ),
    [switch]$AnalyzeShipCatalog,
    [string[]]$ExpectShipHull = @(),
    [switch]$AllowMissingShipTargets
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

function Write-Gate {
    param(
        [string]$Status,
        [string]$Message
    )
    Write-Host ("{0}: {1}" -f $Status, $Message)
}

function ConvertTo-PowerShellLiteral {
    param([string]$Value)

    return "'$($Value.Replace("'", "''"))'"
}

function ConvertTo-PowerShellArrayLiteral {
    param([string[]]$Values)

    $quoted = @()
    foreach ($value in $Values) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $quoted += (ConvertTo-PowerShellLiteral -Value $value)
        }
    }

    return "@($($quoted -join ", "))"
}

function Invoke-ValidationTool {
    param(
        [string]$Label,
        [string]$ScriptName,
        [string[]]$Arguments = @(),
        [switch]$Required
    )
    $scriptPath = Join-Path $PSScriptRoot $ScriptName
    if (-not (Test-Path -LiteralPath $scriptPath)) {
        $message = "Missing validation tool: $ScriptName"
        if ($Required) {
            Write-Gate -Status "FAIL" -Message $message
            throw "Runtime validation evidence collection failed."
        }
        Write-Gate -Status "SKIP" -Message $message
        return
    }
    Write-Gate -Status "INFO" -Message "Running $Label"
    & powershell -NoProfile -ExecutionPolicy Bypass -File $scriptPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        $message = "$Label failed with exit code $LASTEXITCODE"
        if ($Required) {
            Write-Gate -Status "FAIL" -Message $message
            throw "Runtime validation evidence collection failed."
        }
        Write-Gate -Status "SKIP" -Message $message
        return
    }
    if ($Required) {
        Write-Gate -Status "PASS" -Message "$Label completed."
    } else {
        Write-Gate -Status "INFO" -Message "$Label completed without being required."
    }
}

function Invoke-ValidationCommand {
    param(
        [string]$Label,
        [string]$Command,
        [switch]$Required
    )

    Write-Gate -Status "INFO" -Message "Running $Label"
    & powershell -NoProfile -ExecutionPolicy Bypass -Command $Command
    if ($LASTEXITCODE -ne 0) {
        $message = "$Label failed with exit code $LASTEXITCODE"
        if ($Required) {
            Write-Gate -Status "FAIL" -Message $message
            throw "Runtime validation evidence collection failed."
        }

        Write-Gate -Status "SKIP" -Message $message
        return
    }

    if ($Required) {
        Write-Gate -Status "PASS" -Message "$Label completed."
    } else {
        Write-Gate -Status "INFO" -Message "$Label completed without being required."
    }
}

Write-Host "Runtime validation evidence report"
Write-Host "Repo: $repoRoot"
Write-Host "StarsectorDir: $(if ([string]::IsNullOrWhiteSpace($StarsectorDir)) { '<unset>' } else { $StarsectorDir })"
Write-Host "LogPath: $(if ([string]::IsNullOrWhiteSpace($LogPath)) { '<default>' } else { $LogPath })"

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    if ($RequireDeployParity -or $RequireLiveJar) {
        Write-Gate -Status "FAIL" -Message "Set STARSECTOR_DIRECTORY or pass -StarsectorDir for required deploy/live jar proof."
        throw "Runtime validation evidence collection failed."
    }
    Write-Gate -Status "SKIP" -Message "Deploy status, deploy parity, and live jar proof require STARSECTOR_DIRECTORY."
} else {
    Invoke-ValidationTool -Label "deploy status" -ScriptName "deploy-live-mod.ps1" -Arguments @(
        "-StarsectorDir", $StarsectorDir,
        "-Status"
    )
    if ($RequireDeployParity) {
        Invoke-ValidationTool -Label "deploy parity" -ScriptName "deploy-live-mod.ps1" -Arguments @(
            "-StarsectorDir", $StarsectorDir,
            "-CheckOnly",
            "-RequireCurrent"
        ) -Required
    } else {
        Write-Gate -Status "SKIP" -Message "Deploy parity not required. Pass -RequireDeployParity to fail on stale live files."
    }
    if ($RequireLiveJar) {
        Invoke-ValidationTool -Label "live jar class/hash validation" -ScriptName "validate-live-gui-classes.ps1" -Arguments @(
            "-StarsectorDir", $StarsectorDir
        ) -Required
    } else {
        Write-Gate -Status "SKIP" -Message "Live jar class/hash validation not required. Pass -RequireLiveJar to enforce it."
    }
}

$rollbackScript = Join-Path $PSScriptRoot "analyze-trade-rollback-diagnostics.ps1"
$rollbackCommand = "& $(ConvertTo-PowerShellLiteral -Value $rollbackScript)"
if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
    $rollbackCommand += " -LogPath $(ConvertTo-PowerShellLiteral -Value $LogPath)"
}
if ($RequireRollbackPass) {
    $expectedSteps = @()
    foreach ($step in $ExpectFailureStep) {
        if (-not [string]::IsNullOrWhiteSpace($step)) {
            $expectedSteps += $step
        }
    }
    if ($expectedSteps.Count -gt 0) {
        $rollbackCommand += " -ExpectFailureStep $(ConvertTo-PowerShellArrayLiteral -Values $expectedSteps)"
    }
    $rollbackCommand += " -RequirePass"
}
Invoke-ValidationCommand -Label "rollback diagnostic analysis" -Command $rollbackCommand -Required:$RequireRollbackPass

if ($AnalyzeShipCatalog -or $ExpectShipHull.Count -gt 0) {
    $shipScript = Join-Path $PSScriptRoot "analyze-ship-catalog-diagnostics.ps1"
    $shipCommand = "& $(ConvertTo-PowerShellLiteral -Value $shipScript)"
    if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
        $shipCommand += " -LogPath $(ConvertTo-PowerShellLiteral -Value $LogPath)"
    }
    $expectedHulls = @()
    foreach ($hull in $ExpectShipHull) {
        if (-not [string]::IsNullOrWhiteSpace($hull)) {
            $expectedHulls += $hull
        }
    }
    if ($expectedHulls.Count -gt 0) {
        $shipCommand += " -ExpectHull $(ConvertTo-PowerShellArrayLiteral -Values $expectedHulls)"
    }
    if ($AllowMissingShipTargets) {
        $shipCommand += " -AllowMissingTargets"
    }
    Invoke-ValidationCommand -Label "ship catalog diagnostic analysis" -Command $shipCommand
} else {
    Write-Gate -Status "SKIP" -Message "Ship catalog diagnostics not requested. Pass -AnalyzeShipCatalog or -ExpectShipHull."
}

Write-Gate -Status "INFO" -Message "Manual proof still required for F8 UI, Luna UI, campaign mutation, local ship buy/sell, and rollback scenarios not represented in the supplied log."
Write-Gate -Status "PASS" -Message "Runtime validation evidence collection completed."
