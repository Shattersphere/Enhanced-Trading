if ($null -eq $script:failures) {
    $script:failures = New-Object System.Collections.Generic.List[string]
}

function Add-Failure {
    param([string]$Message)
    $script:failures.Add($Message)
    Write-Host "FAIL: $Message"
}

function Add-Pass {
    param([string]$Message)
    Write-Host "PASS: $Message"
}

function Resolve-RepoPath {
    param([string]$RelativePath)
    if ([string]::IsNullOrWhiteSpace($script:repoRoot)) {
        throw "Validation helper requires `$repoRoot to be set before dot-sourcing."
    }
    Join-Path $script:repoRoot $RelativePath
}

function Read-Text {
    param([string]$RelativePath)
    $path = Resolve-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure "$RelativePath is missing"
        return ""
    }
    return Get-Content -LiteralPath $path -Raw
}

function Assert-Contains {
    param(
        [string]$Label,
        [string]$Content,
        [string]$Expected
    )
    if ($Content.Contains($Expected)) {
        Add-Pass "$Label contains $Expected"
    } else {
        Add-Failure "$Label missing $Expected"
    }
}

function Assert-NotContains {
    param(
        [string]$Label,
        [string]$Content,
        [string]$Unexpected
    )
    if ($Content.Contains($Unexpected)) {
        Add-Failure "$Label must not contain $Unexpected"
    } else {
        Add-Pass "$Label avoids $Unexpected"
    }
}

function Assert-NotMatch {
    param(
        [string]$Label,
        [string]$Content,
        [string]$Pattern
    )
    if ($Content -match $Pattern) {
        Add-Failure "$Label must not match $Pattern"
    } else {
        Add-Pass "$Label avoids $Pattern"
    }
}

function Assert-Equal {
    param(
        [string]$Label,
        [object]$Actual,
        [object]$Expected
    )
    if ($Actual -eq $Expected) {
        Add-Pass "$Label is $Expected"
    } else {
        Add-Failure "$Label expected $Expected but was $Actual"
    }
}

function Assert-NumberRange {
    param(
        [string]$Label,
        [object]$Value,
        [double]$Min,
        [double]$Max
    )
    if ($null -eq $Value -or -not ($Value -is [int] -or $Value -is [long] -or $Value -is [double] -or $Value -is [decimal])) {
        Add-Failure "$Label must be numeric"
        return
    }
    $number = [double]$Value
    if ($number -lt $Min -or $number -gt $Max) {
        Add-Failure "$Label must be between $Min and $Max"
    } else {
        Add-Pass "$Label is within $Min..$Max"
    }
}

function Assert-Boolean {
    param(
        [string]$Label,
        [object]$Value
    )
    if ($Value -is [bool]) {
        Add-Pass "$Label is Boolean"
    } else {
        Add-Failure "$Label must be Boolean"
    }
}

function Assert-ObjectKeys {
    param(
        [string]$Label,
        [object]$Object,
        [string[]]$AllowedKeys
    )
    if ($null -eq $Object) {
        Add-Failure "$Label is missing"
        return
    }
    foreach ($key in $Object.PSObject.Properties.Name) {
        if ($AllowedKeys -notcontains $key) {
            Add-Failure "$Label has unsupported key '$key'"
        }
    }
    Add-Pass "$Label has only supported keys"
}

function Assert-StringArray {
    param(
        [string]$Label,
        [object]$Object,
        [string]$PropertyName
    )
    if ($null -eq $Object -or -not $Object.PSObject.Properties.Name.Contains($PropertyName)) {
        Add-Failure "$Label missing $PropertyName"
        return
    }
    $values = @($Object.$PropertyName)
    foreach ($value in $values) {
        if ($value -is [string] -and -not [string]::IsNullOrWhiteSpace($value)) {
            Add-Pass "$PropertyName entry '$value' is a non-empty string"
        } else {
            Add-Failure "$PropertyName entries must be non-empty strings"
        }
    }
    Add-Pass "$PropertyName is a string list with $($values.Count) entr$(if ($values.Count -eq 1) { 'y' } else { 'ies' })"
}

function Assert-Order {
    param(
        [string]$Label,
        [string]$Content,
        [string[]]$ExpectedOrder
    )
    $lastIndex = -1
    foreach ($needle in $ExpectedOrder) {
        $index = $Content.IndexOf($needle, [System.StringComparison]::Ordinal)
        if ($index -lt 0) {
            Add-Failure "$Label missing ordered token $needle"
            return
        }
        if ($index -le $lastIndex) {
            Add-Failure "$Label token $needle appears out of order"
            return
        }
        $lastIndex = $index
    }
    Add-Pass "$Label keeps required order"
}

function Assert-RegexCount {
    param(
        [string]$Label,
        [string]$Content,
        [string]$Pattern,
        [int]$ExpectedCount
    )
    $count = @([regex]::Matches($Content, $Pattern)).Count
    if ($count -eq $ExpectedCount) {
        Add-Pass "$Label has $ExpectedCount match(es) for $Pattern"
    } else {
        Add-Failure "$Label expected $ExpectedCount match(es) for $Pattern but found $count"
    }
}

function Get-Section {
    param(
        [string]$Content,
        [string]$StartToken,
        [string]$EndToken
    )
    $start = $Content.IndexOf($StartToken, [System.StringComparison]::Ordinal)
    if ($start -lt 0) {
        Add-Failure "missing section start $StartToken"
        return ""
    }
    $end = $Content.IndexOf($EndToken, $start + $StartToken.Length, [System.StringComparison]::Ordinal)
    if ($end -lt 0) {
        Add-Failure "missing section end $EndToken"
        return $Content.Substring($start)
    }
    return $Content.Substring($start, $end - $start)
}
