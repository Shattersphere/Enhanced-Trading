param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$NoClean,
    [switch]$CheckOnly,
    [switch]$Status,
    [switch]$CleanStaleStaging,
    [switch]$RequireCurrent,
    [switch]$QueuedWorker,
    [string]$StagingRoot = "",
    [string]$SourceProject = "",
    [string]$DeployAttemptedAt = "",
    [int]$PollSeconds = 5,
    [int]$StagingRetentionCount = 2,
    [int]$StagingMinAgeMinutes = 10
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Deploy.Common.ps1")

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$deployRoot = Join-Path $StarsectorDir "mods\Enhanced Trading"
$stateRoot = Join-Path $repoRoot ".agent-deploy"
$deployName = "deploy-live-mod"

if (-not (Test-Path -LiteralPath $repoRoot)) {
    throw "Repository root not found: $repoRoot"
}

$requiredItems = @(
    "data",
    "jars",
    "mod_info.json",
    "README.md",
    "CONFIG.md",
    "CHANGELOG.md",
    "PACKAGING.md"
)
$optionalItems = @(
    "graphics"
)
$items = @($requiredItems + $optionalItems)
$requiredShatterLibRuntimeClasses = @(
    "com/shattersphere/shatterlib/starsector/ui/tooltip/ShatterItemTooltipContext.class",
    "com/shattersphere/shatterlib/starsector/ui/tooltip/ShatterTooltipContextLine.class"
)

function Get-DeployScopeHash {
    $scope = "$($repoRoot.ToLowerInvariant())|$($deployRoot.ToLowerInvariant())|$($deployName.ToLowerInvariant())"
    return Get-StableHash -Value $scope
}

function Get-RelativeDeployItems {
    return $script:items
}

function Get-DeployItemManifest {
    param(
        [string]$Root,
        [switch]$SourceSide
    )

    $lines = New-Object System.Collections.Generic.List[string]
    foreach ($item in (Get-RelativeDeployItems)) {
        $path = Join-Path $Root $item
        $sourcePath = Join-Path $repoRoot $item
        $optional = $optionalItems -contains $item

        if ($optional -and -not (Test-Path -LiteralPath $sourcePath)) {
            if ($SourceSide -or -not (Test-Path -LiteralPath $path)) {
                continue
            }
        }
        if (-not (Test-Path -LiteralPath $path)) {
            $kind = if ($SourceSide) { "MISSING_SOURCE" } else { "MISSING_LIVE" }
            $lines.Add("$kind`t$item")
            continue
        }

        $deployItem = Get-Item -LiteralPath $path -Force
        if (-not $deployItem.PSIsContainer) {
            $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $deployItem.FullName).Hash
            $lines.Add("FILE`t$item`t$hash")
            continue
        }

        Get-ChildItem -LiteralPath $deployItem.FullName -File -Recurse -Force | Sort-Object FullName | ForEach-Object {
            $relativePath = $_.FullName.Substring($deployItem.FullName.Length).TrimStart("\", "/")
            $entryLabel = Join-Path $item $relativePath
            $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash
            $lines.Add("FILE`t$entryLabel`t$hash")
        }
    }
    return @($lines | Sort-Object)
}

function Get-DeployContentReport {
    $repoManifest = @(Get-DeployItemManifest -Root $repoRoot -SourceSide)
    $liveManifest = @(Get-DeployItemManifest -Root $deployRoot)
    $repoMissing = @($repoManifest | Where-Object { $_.StartsWith("MISSING_SOURCE`t", [System.StringComparison]::Ordinal) })
    $liveMissing = @($liveManifest | Where-Object { $_.StartsWith("MISSING_LIVE`t", [System.StringComparison]::Ordinal) })
    $repoHash = Get-ManifestHash -Lines $repoManifest
    $liveHash = Get-ManifestHash -Lines $liveManifest
    $diff = @(Compare-Object -ReferenceObject $repoManifest -DifferenceObject $liveManifest)

    $state = if ($repoMissing.Count -gt 0) {
        "unknown because required deploy source items are missing."
    } elseif ($liveMissing.Count -gt 0) {
        "stale because live deploy items are missing."
    } elseif ($repoHash -eq $liveHash) {
        "current because repo and live deploy manifests match."
    } else {
        "stale because repo and live deploy manifests differ."
    }

    [pscustomobject]@{
        RepoManifestHash = $repoHash
        LiveManifestHash = $liveHash
        RepoEntryCount = $repoManifest.Count
        LiveEntryCount = $liveManifest.Count
        RepoMissingCount = $repoMissing.Count
        LiveMissingCount = $liveMissing.Count
        DifferenceCount = $diff.Count
        LiveState = $state
    }
}

function Write-DeployContentReport {
    param([object]$Report)

    Write-Host "Repo deploy manifest SHA-256: $($Report.RepoManifestHash)"
    Write-Host "Live deploy manifest SHA-256: $($Report.LiveManifestHash)"
    Write-Host "Repo deploy manifest entries: $($Report.RepoEntryCount)"
    Write-Host "Live deploy manifest entries: $($Report.LiveEntryCount)"
    Write-Host "Repo missing deploy entries: $($Report.RepoMissingCount)"
    Write-Host "Live missing deploy entries: $($Report.LiveMissingCount)"
    Write-Host "Deploy manifest differences: $($Report.DifferenceCount)"
    Write-Host "The live deploy target is $($Report.LiveState)"
}

function Test-ModInfoContainsId {
    param(
        [string]$Candidate,
        [string]$ModId
    )

    $modInfo = Join-Path $Candidate "mod_info.json"
    if (-not (Test-Path -LiteralPath $modInfo)) {
        return $false
    }
    $text = Get-Content -LiteralPath $modInfo -Raw
    return [regex]::IsMatch($text, ('"id"\s*:\s*"' + [regex]::Escape($ModId) + '"'), [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

function Resolve-ShatterLibModRoot {
    $modsRoot = Join-Path $StarsectorDir "mods"
    if (-not (Test-Path -LiteralPath $modsRoot)) {
        return $null
    }

    $candidates = @(Get-ChildItem -LiteralPath $modsRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending)
    foreach ($candidate in $candidates) {
        if ($candidate.Name.Equals("Shatter Lib", [System.StringComparison]::OrdinalIgnoreCase)) {
            return $candidate.FullName
        }
    }
    foreach ($candidate in $candidates) {
        if ($candidate.Name.StartsWith("Shatter Lib-", [System.StringComparison]::OrdinalIgnoreCase)) {
            return $candidate.FullName
        }
    }
    foreach ($candidate in $candidates) {
        if (Test-ModInfoContainsId -Candidate $candidate.FullName -ModId "shatter_lib") {
            return $candidate.FullName
        }
    }
    return $null
}

function Get-ShatterLibRuntimeDependencyReport {
    $modRoot = Resolve-ShatterLibModRoot
    if ([string]::IsNullOrWhiteSpace($modRoot)) {
        return [pscustomobject]@{
            ModRoot = ""
            JarPath = ""
            MissingClasses = @($requiredShatterLibRuntimeClasses)
            LiveState = "stale because Shatter Lib is not installed under the Starsector mods directory."
        }
    }

    $jarPath = Join-Path $modRoot "jars\shatter-lib.jar"
    if (-not (Test-Path -LiteralPath $jarPath)) {
        return [pscustomobject]@{
            ModRoot = $modRoot
            JarPath = $jarPath
            MissingClasses = @($requiredShatterLibRuntimeClasses)
            LiveState = "stale because the installed Shatter Lib jar is missing."
        }
    }

    $entries = @(Get-ZipEntryNames -Path $jarPath)
    $missing = @($requiredShatterLibRuntimeClasses | Where-Object { $entries -notcontains $_ })
    $state = if ($missing.Count -eq 0) {
        "current because the installed Shatter Lib jar contains required Enhanced Trading API classes."
    } else {
        "stale because the installed Shatter Lib jar is missing required API classes: $($missing -join ', ')."
    }

    [pscustomobject]@{
        ModRoot = $modRoot
        JarPath = $jarPath
        MissingClasses = $missing
        LiveState = $state
    }
}

function Write-RuntimeDependencyReport {
    param([object]$Report)

    Write-Host "Runtime dependency Shatter Lib root: $(if ([string]::IsNullOrWhiteSpace($Report.ModRoot)) { 'missing' } else { $Report.ModRoot })"
    Write-Host "Runtime dependency Shatter Lib jar: $(if ([string]::IsNullOrWhiteSpace($Report.JarPath)) { 'missing' } else { $Report.JarPath })"
    Write-Host "Runtime dependency missing Shatter Lib API classes: $($Report.MissingClasses.Count)"
    Write-Host "The runtime dependency state is $($Report.LiveState)"
}

function Assert-RuntimeDependenciesCurrent {
    $report = Get-ShatterLibRuntimeDependencyReport
    if (!$report.LiveState.StartsWith("current", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Runtime dependencies are not current: $($report.LiveState)"
    }
}

function Assert-DeployJarBoundary {
    param([string]$BaseRoot)

    $jarPath = Join-Path $BaseRoot "jars\enhanced-trading.jar"
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "Deploy jar not found: $jarPath"
    }
    $privateTerms = @(
        "WeaponsProcurementBadgeHelper",
        "WeaponsProcurementBadgeConfig",
        "WeaponsProcurementCountUpdater",
        "weaponsprocurement/extensions/WeaponsProcurementExtensions",
        "weaponsprocurement/internal/WeaponsProcurementBadge",
        "weaponsprocurement/internal/WeaponsProcurementCountUpdater"
    )
    $privateEntries = @(Get-ZipEntryNames -Path $jarPath | Where-Object {
        $entry = $_
        $privateTerms | Where-Object { $entry.IndexOf($_, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 }
    })
    if ($privateEntries.Count -gt 0) {
        throw "Refusing deploy because the jar contains badge classes now owned by the standalone Weapon Badges mod. Entries: $($privateEntries -join ', ')"
    }
}

function Assert-DeployRoot {
    if ((Split-Path -Leaf $deployRoot) -ne "Enhanced Trading") {
        throw "Refusing to deploy to unexpected root: $deployRoot"
    }
}

function Get-DeployBlocker {
    foreach ($item in (Get-RelativeDeployItems)) {
        $target = Join-Path $deployRoot $item
        if (-not (Test-Path -LiteralPath $target)) {
            continue
        }

        $targetItem = Get-Item -LiteralPath $target -Force
        if ($targetItem.PSIsContainer) {
            $files = @(Get-ChildItem -LiteralPath $target -Recurse -File -Force -ErrorAction Stop)
        } else {
            $files = @($targetItem)
        }

        foreach ($file in $files) {
            if (-not (Test-FileReplaceable -Path $file.FullName)) {
                return $file.FullName
            }
        }
    }
    return ""
}

function Copy-DeployItem {
    param(
        [string]$Source,
        [string]$Target
    )

    $sourceItem = Get-Item -LiteralPath $Source
    if ($sourceItem.PSIsContainer) {
        New-Item -ItemType Directory -Force -Path $Target | Out-Null
        Copy-Item -Path (Join-Path $Source "*") -Destination $Target -Recurse -Force -ErrorAction Stop
    } else {
        $parent = Split-Path -Parent $Target
        if (-not (Test-Path -LiteralPath $parent)) {
            New-Item -ItemType Directory -Force -Path $parent | Out-Null
        }
        Copy-Item -LiteralPath $Source -Destination $Target -Force -ErrorAction Stop
    }
}

function Replace-DeployItem {
    param(
        [string]$Source,
        [string]$Target
    )

    if ($NoClean) {
        Copy-DeployItem -Source $Source -Target $Target
        return
    }

    $parent = Split-Path -Parent $Target
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    $leaf = Split-Path -Leaf $Target
    $tempTarget = Join-Path $parent (".wp-deploy-" + $leaf + "-" + [guid]::NewGuid().ToString("N"))
    $backupTarget = Join-Path $parent (".wp-deploy-backup-" + $leaf + "-" + [guid]::NewGuid().ToString("N"))
    try {
        Copy-DeployItem -Source $Source -Target $tempTarget
        if (Test-Path -LiteralPath $Target) {
            Move-Item -LiteralPath $Target -Destination $backupTarget -Force -ErrorAction Stop
        }
        Move-Item -LiteralPath $tempTarget -Destination $Target -Force -ErrorAction Stop
        if (Test-Path -LiteralPath $backupTarget) {
            Remove-Item -LiteralPath $backupTarget -Recurse -Force -ErrorAction SilentlyContinue
        }
    } catch {
        if ((-not (Test-Path -LiteralPath $Target)) -and (Test-Path -LiteralPath $backupTarget)) {
            Move-Item -LiteralPath $backupTarget -Destination $Target -Force
        }
        if (Test-Path -LiteralPath $tempTarget) {
            Remove-Item -LiteralPath $tempTarget -Recurse -Force -ErrorAction SilentlyContinue
        }
        throw
    }
}

function New-DeployStaging {
    $runId = (Get-Date -Format "yyyyMMddHHmmss") + "-" + [guid]::NewGuid().ToString("N")
    $stageRoot = Join-Path $stageRootBase $runId
    New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null

    foreach ($item in (Get-RelativeDeployItems)) {
        $source = Join-Path $repoRoot $item
        $target = Join-Path $stageRoot $item
        if (-not (Test-Path -LiteralPath $source)) {
            if ($optionalItems -contains $item) {
                continue
            }
            throw "Deploy source item not found: $source"
        }
        Copy-DeployItem -Source $source -Target $target
    }

    return $stageRoot
}

function Assert-SafeStagingDirectory {
    param([System.IO.DirectoryInfo]$Directory)

    $resolvedBase = [System.IO.Path]::GetFullPath($stageRootBase).TrimEnd("\", "/")
    $resolvedDirectory = [System.IO.Path]::GetFullPath($Directory.FullName).TrimEnd("\", "/")
    if (!$resolvedDirectory.StartsWith("$resolvedBase\", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean staging directory outside scoped staging root: $resolvedDirectory"
    }
}

function Get-ScopedStagingDirectories {
    if (-not (Test-Path -LiteralPath $stageRootBase)) {
        return @()
    }
    return @(Get-ChildItem -LiteralPath $stageRootBase -Directory -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending)
}

function Get-StaleStagingCandidates {
    param(
        [object]$State,
        [int]$RetentionCount,
        [int]$MinAgeMinutes
    )

    if ($RetentionCount -lt 0) {
        throw "StagingRetentionCount must be >= 0."
    }
    if ($MinAgeMinutes -lt 0) {
        throw "StagingMinAgeMinutes must be >= 0."
    }

    $activeStagingRoot = ""
    if ((Test-QueuedDeployWorkerActive -State $State) -and ![string]::IsNullOrWhiteSpace([string]$State.StagingRoot)) {
        $activeStagingRoot = [System.IO.Path]::GetFullPath([string]$State.StagingRoot).TrimEnd("\", "/")
    }

    $nowUtc = (Get-Date).ToUniversalTime()
    $inactive = New-Object System.Collections.Generic.List[System.IO.DirectoryInfo]
    foreach ($directory in @(Get-ScopedStagingDirectories)) {
        Assert-SafeStagingDirectory -Directory $directory
        $resolvedDirectory = [System.IO.Path]::GetFullPath($directory.FullName).TrimEnd("\", "/")
        if (![string]::IsNullOrWhiteSpace($activeStagingRoot) -and [string]::Equals($resolvedDirectory, $activeStagingRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            continue
        }
        $ageMinutes = ($nowUtc - $directory.LastWriteTimeUtc).TotalMinutes
        if ($ageMinutes -lt $MinAgeMinutes) {
            continue
        }
        [void]$inactive.Add($directory)
    }
    return @($inactive | Sort-Object LastWriteTimeUtc -Descending | Select-Object -Skip $RetentionCount)
}

function Remove-StaleStagingDirectories {
    param([System.IO.DirectoryInfo[]]$Candidates)

    $removed = 0
    foreach ($candidate in $Candidates) {
        Assert-SafeStagingDirectory -Directory $candidate
        Write-Host "Removing stale staging directory: $($candidate.FullName)"
        Remove-Item -LiteralPath $candidate.FullName -Recurse -Force
        $removed++
    }
    return $removed
}

function Read-DeployState {
    return Read-DeployStateFile -Path $stateFile
}

function Write-DeployState {
    param(
        [string]$RunId,
        [int]$ProcessId,
        [string]$StageRoot,
        [string]$Phase
    )
    if (-not (Test-Path -LiteralPath $stateRoot)) {
        New-Item -ItemType Directory -Force -Path $stateRoot | Out-Null
    }
    [pscustomobject]@{
        RunId = $RunId
        Pid = $ProcessId
        DeployName = $deployName
        DeployScopeHash = $deployScopeHash
        ScriptPath = $PSCommandPath
        RepoRoot = $repoRoot
        DeployRoot = $deployRoot
        StagingRoot = $StageRoot
        Phase = $Phase
        UpdatedAt = (Get-Date).ToString("o")
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
}

function Write-DeployQueueReport {
    param(
        [object]$ContentReport,
        [object]$RuntimeDependencyReport
    )

    if ($CleanStaleStaging) {
        Write-Host "Deploy status and stale staging cleanup."
    } else {
        Write-Host "Deploy status only; no files were modified."
    }
    Write-Host "Source project: $repoRoot"
    Write-Host "Deploy target: $deployRoot"
    Write-Host "Deploy name: $deployName"
    Write-Host "Deploy scope hash: $deployScopeHash"
    Write-Host "Deploy state file: $stateFile"
    Write-Host "Deploy status file: $deployStatusFile"

    $state = Read-DeployStateFile -Path $stateFile
    if ($null -eq $state) {
        Write-Host "Deploy queue state: none"
    } else {
        Write-Host "Deploy queue state: present"
        Write-Host "Deploy queue active: $(Test-QueuedDeployWorkerActive -State $state)"
        Write-Host "Deploy queue runId: $($state.RunId)"
        Write-Host "Deploy queue phase: $($state.Phase)"
        Write-Host "Deploy queue pid: $($state.Pid)"
        Write-Host "Deploy queue updated: $($state.UpdatedAt)"
        if (![string]::IsNullOrWhiteSpace([string]$state.StagingRoot)) {
            Write-Host "Deploy queue staging root: $($state.StagingRoot)"
        }
    }

    if (Test-Path -LiteralPath $deployStatusFile) {
        Write-Host "Deploy Status.txt:"
        Get-Content -LiteralPath $deployStatusFile | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "Deploy Status.txt: missing"
    }

    $blocker = Get-DeployBlocker
    if (![string]::IsNullOrWhiteSpace($blocker)) {
        Write-Host "Current deploy blocker: $blocker"
    } elseif ($null -ne $RuntimeDependencyReport -and !$RuntimeDependencyReport.LiveState.StartsWith("current", [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-Host "Current deploy blocker: runtime dependencies are not current: $($RuntimeDependencyReport.LiveState)"
    } else {
        Write-Host "Current deploy blocker: none"
    }

    $staging = @(Get-ScopedStagingDirectories)
    $candidates = @(Get-StaleStagingCandidates -State $state -RetentionCount $StagingRetentionCount -MinAgeMinutes $StagingMinAgeMinutes)
    Write-Host "Scoped staging directories: $($staging.Count)"
    Write-Host "Stale staging cleanup candidates: $($candidates.Count)"
    Write-Host "Stale staging retention count: $StagingRetentionCount"
    Write-Host "Stale staging minimum age minutes: $StagingMinAgeMinutes"
    if ($CleanStaleStaging) {
        $removed = Remove-StaleStagingDirectories -Candidates $candidates
        Write-Host "Stale staging directories removed: $removed"
    }

    Write-DeployContentReport -Report $ContentReport
}

function Publish-StagedDeploy {
    param([string]$StageRoot)

    Assert-DeployRoot
    Assert-DeployJarBoundary -BaseRoot $StageRoot
    Assert-RuntimeDependenciesCurrent
    New-Item -ItemType Directory -Force -Path $deployRoot | Out-Null

    foreach ($item in (Get-RelativeDeployItems)) {
        $source = Join-Path $StageRoot $item
        $target = Join-Path $deployRoot $item
        if (-not (Test-Path -LiteralPath $source)) {
            if (-not $NoClean -and ($optionalItems -contains $item) -and (Test-Path -LiteralPath $target)) {
                Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction Stop
            }
            continue
        }
        Replace-DeployItem -Source $source -Target $target
    }
}

function Stop-OlderQueuedDeployFromStateFile {
    param([string]$Path)

    Stop-QueuedDeployFromStateFile `
        -Path $Path `
        -CurrentScriptPath $PSCommandPath `
        -CurrentProcessId $PID `
        -NotOwnedMessage "Previous deploy pid={pid} is alive but is not this deploy script. Leaving it alone." `
        -CancelledMessage "Cancelled older queued deploy pid={pid} state={path}."
}

function Stop-OlderQueuedDeploy {
    Stop-OlderQueuedDeployFromStateFile -Path $stateFile
    if (![string]::Equals($legacyStateFile, $stateFile, [System.StringComparison]::OrdinalIgnoreCase)) {
        Stop-OlderQueuedDeployFromStateFile -Path $legacyStateFile
    }
}

function Start-QueuedDeploy {
    param([string]$StageRoot)

    Stop-OlderQueuedDeploy
    Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "blocked, waiting..."

    $runId = Split-Path -Leaf $StageRoot
    $deployAttemptedAtValue = (Get-Date).ToString("o")
    $powerShellPath = (Get-Process -Id $PID).Path
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $PSCommandPath,
        "-StarsectorDir", $StarsectorDir,
        "-QueuedWorker",
        "-StagingRoot", $StageRoot,
        "-SourceProject", $repoRoot,
        "-DeployAttemptedAt", $deployAttemptedAtValue,
        "-PollSeconds", ([string]$PollSeconds)
    )
    if ($NoClean) {
        $args += "-NoClean"
    }
    $argumentLine = ($args | ForEach-Object { ConvertTo-ProcessArgument -Value $_ }) -join " "
    $process = Start-MinimizedNoActivateProcess -FilePath $powerShellPath -ArgumentList $argumentLine
    Write-DeployState -RunId $runId -ProcessId $process.Id -StageRoot $StageRoot -Phase "queued"
    Write-DeployLog "Queued deploy runId=$runId pid=$($process.Id) target=$deployRoot stage=$StageRoot."
    Write-Host "Deploy queued: target is locked. Staged files at $StageRoot; minimized visible worker pid=$($process.Id) will publish after the lock clears."
}

$deployStatusFile = Join-Path $repoRoot "Deploy Status.txt"
$deployScopeHash = Get-DeployScopeHash
$stageRootBase = Join-Path $stateRoot "staged-$deployName-$deployScopeHash"
$stateFile = Join-Path $stateRoot "$deployName-$deployScopeHash.latest.json"
$legacyStateFile = Join-Path $stateRoot "deploy-live-mod.json"
$logFile = Join-Path $stateRoot "$deployName-$deployScopeHash.log"
$deployCommit = Get-DeployCommit -RepoRoot $repoRoot

if ($CheckOnly -and $Status) {
    throw "Use either -CheckOnly or -Status, not both."
}
if ($CleanStaleStaging -and -not $Status) {
    throw "Use -Status -CleanStaleStaging to clean stale deploy staging directories."
}
if (($CheckOnly -or $Status) -and $QueuedWorker) {
    throw "Status and check-only modes are foreground diagnostics; do not combine them with -QueuedWorker."
}
if ($CheckOnly -or $Status) {
    $contentReport = Get-DeployContentReport
    $runtimeDependencyReport = Get-ShatterLibRuntimeDependencyReport
    if ($CheckOnly) {
        Write-Host "Deploy check only; no files were modified."
        Write-DeployContentReport -Report $contentReport
    } else {
        Write-DeployQueueReport -ContentReport $contentReport -RuntimeDependencyReport $runtimeDependencyReport
    }
    Write-RuntimeDependencyReport -Report $runtimeDependencyReport
    if ($RequireCurrent -and !$contentReport.LiveState.StartsWith("current", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "The live deploy target is not current: $($contentReport.LiveState)"
    }
    if ($RequireCurrent -and !$runtimeDependencyReport.LiveState.StartsWith("current", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "The runtime dependencies are not current: $($runtimeDependencyReport.LiveState)"
    }
    exit 0
}

trap {
    if ((-not ($CheckOnly -or $Status)) -and ![string]::IsNullOrWhiteSpace($deployStatusFile)) {
        Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "error" -Message $_.Exception.Message
    }
    $state = $null
    if (![string]::IsNullOrWhiteSpace($stateFile)) {
        $state = Read-DeployStateFile -Path $stateFile
    }
    if ($null -ne $state -and $state.Pid -eq $PID) {
        $state.Phase = "failed"
        $state.UpdatedAt = (Get-Date).ToString("o")
        $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
    }
    break
}

if ($QueuedWorker) {
    if ([string]::IsNullOrWhiteSpace($StagingRoot) -or -not (Test-Path -LiteralPath $StagingRoot)) {
        throw "Queued deploy worker requires an existing -StagingRoot."
    }
    $deployAttemptedAtValue = if ([string]::IsNullOrWhiteSpace($DeployAttemptedAt)) { (Get-Date).ToString("o") } else { $DeployAttemptedAt }
    $sourceProjectValue = if ([string]::IsNullOrWhiteSpace($SourceProject)) { $repoRoot } else { $SourceProject }
    Write-Host "Source project: $sourceProjectValue"
    Write-Host "Time of attempted deploy: $deployAttemptedAtValue"
    Assert-RuntimeDependenciesCurrent
    $runId = Split-Path -Leaf $StagingRoot
    Write-DeployState -RunId $runId -ProcessId $PID -StageRoot $StagingRoot -Phase "waiting"
    $lastBlocker = ""
    while ($true) {
        $blocker = Get-DeployBlocker
        if ([string]::IsNullOrWhiteSpace($blocker)) {
            break
        }
        if ($blocker -ne $lastBlocker) {
            Write-DeployLog "Waiting for deploy blocker: $blocker"
            $lastBlocker = $blocker
        }
        Start-Sleep -Seconds $PollSeconds
    }
    Publish-StagedDeploy -StageRoot $StagingRoot
    Write-DeployState -RunId $runId -ProcessId $PID -StageRoot $StagingRoot -Phase "completed"
    Write-DeployLog "Completed queued deploy runId=$runId target=$deployRoot."
    Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "succeeded"
    exit 0
}

Assert-DeployJarBoundary -BaseRoot $repoRoot
Assert-RuntimeDependenciesCurrent
Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "initialised" -Reset
$stagedRoot = New-DeployStaging
$blockerPath = Get-DeployBlocker
if (-not [string]::IsNullOrWhiteSpace($blockerPath)) {
    Start-QueuedDeploy -StageRoot $stagedRoot
    exit 0
}

Publish-StagedDeploy -StageRoot $stagedRoot
$mode = if ($NoClean) { "copy-over" } else { "clean-sync" }
Write-Host "Deployed Enhanced Trading clean package files to $deployRoot ($mode)"
Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "succeeded"
