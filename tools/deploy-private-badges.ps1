param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipCorePatchRefresh,
    [switch]$Status,
    [switch]$QueuedWorker,
    [string]$SourceProject = "",
    [string]$DeployAttemptedAt = "",
    [int]$PollSeconds = 5
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Deploy.Common.ps1")

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $repoRoot "jars\weapons-procurement.jar"
$buildScript = Join-Path $repoRoot "build.ps1"
$deployScript = Join-Path $PSScriptRoot "deploy-live-mod.ps1"
$patcherScript = Join-Path $PSScriptRoot "cargo-stack-view-patcher.ps1"
$patchValidator = Join-Path $PSScriptRoot "validate-cargo-stack-view-patch.ps1"
$badgeValidator = Join-Path $PSScriptRoot "validate-total-badges.ps1"
$stateRoot = Join-Path $repoRoot ".agent-deploy"
$stateFile = Join-Path $stateRoot "deploy-private-badges.json"
$logFile = Join-Path $stateRoot "deploy-private-badges.log"
$obfJar = Join-Path $StarsectorDir "starsector-core\starfarer_obf.jar"
$deployStatusFile = Join-Path $repoRoot "Deploy Status.txt"

function Get-CorePatchBlocker {
    if ((Test-Path -LiteralPath $obfJar) -and -not (Test-FileReplaceable -Path $obfJar)) {
        return $obfJar
    }
    return ""
}

function Read-DeployState {
    return Read-DeployStateFile -Path $stateFile
}

function Write-DeployState {
    param(
        [int]$ProcessId,
        [string]$Phase
    )
    if (-not (Test-Path -LiteralPath $stateRoot)) {
        New-Item -ItemType Directory -Force -Path $stateRoot | Out-Null
    }
    [pscustomobject]@{
        Pid = $ProcessId
        ScriptPath = $PSCommandPath
        RepoRoot = $repoRoot
        StarsectorDir = $StarsectorDir
        DeployName = "deploy-private-badges"
        Target = $obfJar
        Phase = $Phase
        UpdatedAt = (Get-Date).ToString("o")
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
}

function Stop-OlderQueuedPrivateDeploy {
    Stop-QueuedDeployFromStateFile `
        -Path $stateFile `
        -CurrentScriptPath $PSCommandPath `
        -CurrentProcessId $PID `
        -NotOwnedMessage "Previous private badge deploy pid={pid} is alive but is not this deploy script. Leaving it alone." `
        -CancelledMessage "Cancelled older queued private badge deploy pid={pid}."
}

function Start-QueuedPrivateBadgeDeploy {
    Stop-OlderQueuedPrivateDeploy
    Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "blocked, waiting..."
    $powerShellPath = (Get-Process -Id $PID).Path
    $deployAttemptedAtValue = (Get-Date).ToString("o")
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $PSCommandPath,
        "-StarsectorDir", $StarsectorDir,
        "-QueuedWorker",
        "-SourceProject", $repoRoot,
        "-DeployAttemptedAt", $deployAttemptedAtValue,
        "-PollSeconds", ([string]$PollSeconds)
    )
    if ($SkipCorePatchRefresh) {
        $args += "-SkipCorePatchRefresh"
    }
    $argumentLine = ($args | ForEach-Object { ConvertTo-ProcessArgument -Value $_ }) -join " "
    $process = Start-MinimizedNoActivateProcess -FilePath $powerShellPath -ArgumentList $argumentLine
    Write-DeployState -ProcessId $process.Id -Phase "queued"
    Write-DeployLog "Queued private badge deploy pid=$($process.Id) target=$obfJar."
    Write-Host "Private badge deploy queued: core jar is locked. Minimized visible worker pid=$($process.Id) will rebuild, patch, and deploy after the lock clears."
}

function Write-PrivateBadgeDeployStatus {
    Write-Host "Private badge deploy status only; no files were modified."
    Write-Host "Source project: $repoRoot"
    Write-Host "Starsector directory: $StarsectorDir"
    Write-Host "Core jar target: $obfJar"
    Write-Host "Deploy state file: $stateFile"
    Write-Host "Deploy status file: $deployStatusFile"

    $state = Read-DeployState
    if ($null -eq $state) {
        Write-Host "Private badge queue state: none"
    } else {
        Write-Host "Private badge queue state: present"
        Write-Host "Private badge queue active: $(Test-QueuedDeployWorkerActive -State $state)"
        Write-Host "Private badge queue phase: $($state.Phase)"
        Write-Host "Private badge queue pid: $($state.Pid)"
        Write-Host "Private badge queue updated: $($state.UpdatedAt)"
    }

    if (Test-Path -LiteralPath $deployStatusFile) {
        Write-Host "Deploy Status.txt:"
        Get-Content -LiteralPath $deployStatusFile | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "Deploy Status.txt: missing"
    }

    $blocker = Get-CorePatchBlocker
    if ([string]::IsNullOrWhiteSpace($blocker)) {
        Write-Host "Current core patch blocker: none"
    } else {
        Write-Host "Current core patch blocker: $blocker"
    }
}

$deployCommit = Get-DeployCommit -RepoRoot $repoRoot
if ($Status) {
    if ($QueuedWorker) {
        throw "Status mode is a foreground diagnostic; do not combine it with -QueuedWorker."
    }
    Write-PrivateBadgeDeployStatus
    exit 0
}

trap {
    Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "error" -Message $_.Exception.Message
    $state = Read-DeployState
    if ($null -ne $state -and $state.Pid -eq $PID) {
        $state.Phase = "failed"
        $state.UpdatedAt = (Get-Date).ToString("o")
        $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
    }
    break
}

if ($QueuedWorker) {
    $deployAttemptedAtValue = if ([string]::IsNullOrWhiteSpace($DeployAttemptedAt)) { (Get-Date).ToString("o") } else { $DeployAttemptedAt }
    $sourceProjectValue = if ([string]::IsNullOrWhiteSpace($SourceProject)) { $repoRoot } else { $SourceProject }
    Write-Host "Source project: $sourceProjectValue"
    Write-Host "Time of attempted deploy: $deployAttemptedAtValue"
    Write-DeployState -ProcessId $PID -Phase "waiting"
    $lastBlocker = ""
    while ($true) {
        $blocker = Get-CorePatchBlocker
        if ([string]::IsNullOrWhiteSpace($blocker)) {
            break
        }
        if ($blocker -ne $lastBlocker) {
            Write-DeployLog "Waiting for core patch blocker: $blocker"
            $lastBlocker = $blocker
        }
        Start-Sleep -Seconds $PollSeconds
    }
    Write-DeployState -ProcessId $PID -Phase "running"
} else {
    Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "initialised" -Reset
}

function Assert-PrivateBadgeJar {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Private badge jar not found: $Path"
    }

    $entries = Get-ZipEntryNames -Path $Path
    $required = @(
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper.class",
        "weaponsprocurement/internal/WeaponsProcurementBadgeConfig.class",
        "weaponsprocurement/internal/WeaponsProcurementCountUpdater.class",
        "weaponsprocurement/plugins/WeaponsProcurementPrivateBadgeBootstrap.class"
    )

    $missing = @($required | Where-Object { $entries -notcontains $_ })
    if ($missing.Count -gt 0) {
        throw "Jar is missing private badge bridge classes: $($missing -join ', ')"
    }

    Assert-PrivateBadgeBootstrapClass -Path $Path

    $kotlinOnlyHelperEntries = @(
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper`$Companion.class"
    )
    $stale = @($kotlinOnlyHelperEntries | Where-Object { $entries -contains $_ })
    if ($stale.Count -gt 0) {
        throw "Jar contains Kotlin-compiled embedded helper entries that are unsafe for the core classloader: $($stale -join ', ')"
    }
}

function Assert-PrivateBadgeBootstrapClass {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $zip.GetEntry("weaponsprocurement/plugins/WeaponsProcurementPrivateBadgeBootstrap.class")
        if ($null -eq $entry) {
            throw "Jar is missing private badge bootstrap class."
        }
        $stream = $entry.Open()
        try {
            $memory = New-Object System.IO.MemoryStream
            try {
                $stream.CopyTo($memory)
                $classText = [System.Text.Encoding]::ASCII.GetString($memory.ToArray())
                if ($classText.IndexOf("weaponsprocurement/internal/WeaponsProcurementCountUpdater", [System.StringComparison]::Ordinal) -lt 0) {
                    throw "Private badge bootstrap does not reference WeaponsProcurementCountUpdater; public no-op bootstrap may have been packaged."
                }
            } finally {
                $memory.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

function Invoke-RequiredScript {
    param([string[]]$Arguments)

    & powershell @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: powershell $($Arguments -join ' ')"
    }
}

function Invoke-RequiredScriptWithOutput {
    param([string[]]$Arguments)

    $output = @(& powershell @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "Command failed with exit code $exitCode`: powershell $($Arguments -join ' ')"
    }
    return $output
}

function Restore-CleanRepoJar {
    Invoke-RequiredScript -Arguments @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $buildScript,
        "-StarsectorDir", $StarsectorDir
    )
}

Invoke-RequiredScript -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $buildScript,
    "-StarsectorDir", $StarsectorDir,
    "-PrivateBadge"
)

Assert-PrivateBadgeJar -Path $jarPath

if (-not $SkipCorePatchRefresh) {
    $backupJar = "$obfJar.wp_backup"
    if (-not (Test-Path -LiteralPath $obfJar)) {
        throw "Could not find starfarer_obf.jar at '$obfJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
    }
    $blocker = Get-CorePatchBlocker
    if (-not [string]::IsNullOrWhiteSpace($blocker)) {
        if ($QueuedWorker) {
            throw "Queued private badge deploy woke up but core jar is still locked: $blocker"
        }
        Start-QueuedPrivateBadgeDeploy
        Restore-CleanRepoJar
        exit 0
    }
    if (Test-Path -LiteralPath $backupJar) {
        Invoke-RequiredScript -Arguments @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", $patcherScript,
            "-Mode", "Restore",
            "-StarsectorDir", $StarsectorDir
        )
    }

    Invoke-RequiredScript -Arguments @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $patcherScript,
        "-Mode", "Patch",
        "-StarsectorDir", $StarsectorDir
    )
}

Invoke-RequiredScript -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $patchValidator,
    "-StarsectorDir", $StarsectorDir
)

$deployOutput = Invoke-RequiredScriptWithOutput -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $deployScript,
    "-StarsectorDir", $StarsectorDir,
    "-AllowPrivateBadgeJar"
)

$deployQueued = (($deployOutput | Out-String) -match "Deploy queued:")
if ($deployQueued) {
    Write-Host "Private patched-badge build and patch validation passed; live deploy is queued until Starsector releases the target files."
} else {
    Invoke-RequiredScript -Arguments @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $badgeValidator,
        "-StarsectorDir", $StarsectorDir
    )

    Write-Host "Private patched-badge build, patch, deploy, and validation completed."
    Write-DeployStatus -Path $deployStatusFile -Commit $deployCommit -Status "succeeded"
}

Restore-CleanRepoJar

Write-DeployState -ProcessId $PID -Phase "completed"
