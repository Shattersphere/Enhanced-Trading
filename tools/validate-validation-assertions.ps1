param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]
. (Join-Path $PSScriptRoot "lib\Validation.Assertions.ps1")

function Invoke-AssertionCase {
    param(
        [string]$Label,
        [scriptblock]$ScriptBlock,
        [int]$ExpectedFailureCount,
        [string[]]$ExpectedOutput = @(),
        [string[]]$ForbiddenOutput = @()
    )

    $script:failures.Clear()
    $output = @(& $ScriptBlock 6>&1 | ForEach-Object { $_.ToString() })
    $text = $output -join "`n"

    if ($script:failures.Count -eq $ExpectedFailureCount) {
        Write-Host "PASS: $Label failure count is $ExpectedFailureCount"
    } else {
        throw "$Label expected $ExpectedFailureCount failure(s) but saw $($script:failures.Count). Output: $text"
    }

    foreach ($needle in $ExpectedOutput) {
        if ($text.Contains($needle)) {
            Write-Host "PASS: $Label output contains $needle"
        } else {
            throw "$Label output missing $needle. Output: $text"
        }
    }

    foreach ($needle in $ForbiddenOutput) {
        if ($text.Contains($needle)) {
            throw "$Label output must not contain $needle. Output: $text"
        }
        Write-Host "PASS: $Label output avoids $needle"
    }
}

Invoke-AssertionCase `
    -Label "Assert-ObjectKeys valid object" `
    -ScriptBlock { Assert-ObjectKeys "valid object" ([pscustomobject]@{ allowed = "yes" }) @("allowed") } `
    -ExpectedFailureCount 0 `
    -ExpectedOutput @("PASS: valid object has only supported keys")

Invoke-AssertionCase `
    -Label "Assert-ObjectKeys unsupported key" `
    -ScriptBlock { Assert-ObjectKeys "bad object" ([pscustomobject]@{ unexpected = "no" }) @("allowed") } `
    -ExpectedFailureCount 1 `
    -ExpectedOutput @("FAIL: bad object has unsupported key 'unexpected'") `
    -ForbiddenOutput @("PASS: bad object has only supported keys")

Invoke-AssertionCase `
    -Label "Assert-StringArray valid values" `
    -ScriptBlock { Assert-StringArray "valid string array" ([pscustomobject]@{ items = @("alpha", "beta") }) "items" } `
    -ExpectedFailureCount 0 `
    -ExpectedOutput @(
        "PASS: items entry 'alpha' is a non-empty string",
        "PASS: items entry 'beta' is a non-empty string",
        "PASS: items is a string list with 2 entries"
    )

Invoke-AssertionCase `
    -Label "Assert-StringArray bad value" `
    -ScriptBlock { Assert-StringArray "bad string array" ([pscustomobject]@{ items = @("alpha", "") }) "items" } `
    -ExpectedFailureCount 1 `
    -ExpectedOutput @("FAIL: items entries must be non-empty strings") `
    -ForbiddenOutput @("PASS: items is a string list with 2 entries")

Write-Host "Validation assertion helper validation passed."
