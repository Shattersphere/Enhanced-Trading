param(
    [string]$LogPath = "",
    [string[]]$ExpectFailureStep = @(),
    [switch]$RequirePass
)

$ErrorActionPreference = "Stop"

function Write-Gate {
    param(
        [string]$Status,
        [string]$Message
    )
    Write-Host ("{0}: {1}" -f $Status, $Message)
}

function Resolve-DefaultLogPath {
    if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
        return $LogPath
    }
    if (-not [string]::IsNullOrWhiteSpace($env:STARSECTOR_DIRECTORY)) {
        return (Join-Path $env:STARSECTOR_DIRECTORY "starsector-core\starsector.log")
    }
    return ""
}

function Read-Fields {
    param([string]$Line)
    $fields = @{}
    foreach ($match in [regex]::Matches($Line, '([A-Za-z][A-Za-z0-9]*)=([^ ]+)')) {
        $fields[$match.Groups[1].Value] = $match.Groups[2].Value
    }
    return $fields
}

$resolvedLog = Resolve-DefaultLogPath
if ([string]::IsNullOrWhiteSpace($resolvedLog)) {
    $message = "No log path supplied. Pass -LogPath or set STARSECTOR_DIRECTORY."
    if ($RequirePass -or $ExpectFailureStep.Count -gt 0) {
        Write-Gate -Status "FAIL" -Message $message
        throw "Trade rollback diagnostic analysis failed."
    }
    Write-Gate -Status "SKIP" -Message $message
    exit 0
}
if (-not (Test-Path -LiteralPath $resolvedLog)) {
    $message = "No log file found: $resolvedLog"
    if ($RequirePass -or $ExpectFailureStep.Count -gt 0) {
        Write-Gate -Status "FAIL" -Message $message
        throw "Trade rollback diagnostic analysis failed."
    }
    Write-Gate -Status "SKIP" -Message $message
    exit 0
}

$lines = @(Select-String -LiteralPath $resolvedLog -Pattern "WP_STOCK_REVIEW_ROLLBACK" | ForEach-Object { $_.Line })
if ($lines.Count -eq 0) {
    $message = "No WP_STOCK_REVIEW_ROLLBACK lines found in $resolvedLog"
    if ($RequirePass -or $ExpectFailureStep.Count -gt 0) {
        Write-Gate -Status "FAIL" -Message $message
        throw "Trade rollback diagnostic analysis failed."
    }
    Write-Gate -Status "SKIP" -Message $message
    exit 0
}

$records = @()
foreach ($line in $lines) {
    $records += [pscustomobject](Read-Fields -Line $line)
}

$failures = New-Object System.Collections.Generic.List[string]
$requiredFields = @(
    "status",
    "operation",
    "item",
    "quantity",
    "failedStep",
    "restoredCargos",
    "failedCargos",
    "creditsRestored",
    "countsRestored",
    "creditsBefore",
    "creditsAtFailure",
    "creditsAfterRollback",
    "touched"
)
foreach ($record in $records) {
    foreach ($field in $requiredFields) {
        if (-not $record.PSObject.Properties.Name.Contains($field) -or [string]::IsNullOrWhiteSpace([string]$record.$field)) {
            $failures.Add("Rollback diagnostic record missing required field '$field'.")
        }
    }
}
$passCount = @($records | Where-Object { $_.status -eq "PASS" }).Count
$failCount = @($records | Where-Object { $_.status -eq "FAIL" }).Count

if ($RequirePass -and $passCount -le 0) {
    $failures.Add("No passing rollback diagnostic record found.")
}
if ($failCount -gt 0) {
    $failed = $records | Where-Object { $_.status -eq "FAIL" } | Select-Object -First 5
    foreach ($record in $failed) {
        $failures.Add("Rollback failed: step=$($record.failedStep) item=$($record.item) operation=$($record.operation) touched=$($record.touched)")
    }
}

foreach ($step in $ExpectFailureStep) {
    if ([string]::IsNullOrWhiteSpace($step)) {
        continue
    }
    $matches = @($records | Where-Object { $_.failedStep -eq $step -and $_.status -eq "PASS" })
    if ($matches.Count -eq 0) {
        $failures.Add("Missing passing rollback diagnostic for failure step '$step'.")
    }
}

Write-Gate -Status "INFO" -Message "Rollback diagnostics: total=$($records.Count) pass=$passCount fail=$failCount log=$resolvedLog"
foreach ($record in ($records | Select-Object -Last 10)) {
    Write-Gate -Status $record.status -Message ("step={0} operation={1} item={2} quantity={3} creditsRestored={4} countsRestored={5} touched={6}" -f
        $record.failedStep,
        $record.operation,
        $record.item,
        $record.quantity,
        $record.creditsRestored,
        $record.countsRestored,
        $record.touched)
}

if ($failures.Count -gt 0) {
    foreach ($failure in $failures) {
        Write-Gate -Status "FAIL" -Message $failure
    }
    throw "Trade rollback diagnostic analysis failed."
}

Write-Gate -Status "PASS" -Message "Trade rollback diagnostic analysis completed."
