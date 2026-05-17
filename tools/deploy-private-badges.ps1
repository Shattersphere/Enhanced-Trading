param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipCorePatchRefresh,
    [switch]$QueuedWorker,
    [int]$PollSeconds = 5
)

$ErrorActionPreference = "Stop"

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

function Write-DeployLog {
    param([string]$Message)
    $line = "$(Get-Date -Format o) $Message"
    if (-not (Test-Path -LiteralPath $stateRoot)) {
        New-Item -ItemType Directory -Force -Path $stateRoot | Out-Null
    }
    Add-Content -LiteralPath $logFile -Value $line
}

function Test-FileReplaceable {
    param([string]$Path)

    $stream = $null
    try {
        $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        return $true
    } catch {
        return $false
    } finally {
        if ($stream -ne $null) {
            $stream.Close()
        }
    }
}

function Get-CorePatchBlocker {
    if ((Test-Path -LiteralPath $obfJar) -and -not (Test-FileReplaceable -Path $obfJar)) {
        return $obfJar
    }
    return ""
}

function Read-DeployState {
    if (-not (Test-Path -LiteralPath $stateFile)) {
        return $null
    }
    try {
        return Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
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
        Target = $obfJar
        Phase = $Phase
        UpdatedAt = (Get-Date).ToString("o")
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
}

function Stop-OlderQueuedPrivateDeploy {
    $state = Read-DeployState
    if ($null -eq $state -or $null -eq $state.Pid) {
        return
    }
    $oldPid = [int]$state.Pid
    if ($oldPid -eq $PID) {
        return
    }
    $oldProcess = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
    if ($null -eq $oldProcess) {
        return
    }
    $commandLine = ""
    try {
        $commandLine = [string](Get-CimInstance Win32_Process -Filter "ProcessId = $oldPid" -ErrorAction Stop).CommandLine
    } catch {
    }
    if ($commandLine.IndexOf($PSCommandPath, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
        Write-Host "Previous private badge deploy pid=$oldPid is alive but is not this deploy script. Leaving it alone."
        return
    }
    Stop-Process -Id $oldPid -Force
    Write-DeployLog "Cancelled older queued private badge deploy pid=$oldPid."
}

function ConvertTo-ProcessArgument {
    param([string]$Value)
    if ($null -eq $Value) {
        return '""'
    }
    return '"' + ($Value -replace '"', '\"') + '"'
}

function Start-QueuedPrivateBadgeDeploy {
    Stop-OlderQueuedPrivateDeploy
    $powerShellPath = (Get-Process -Id $PID).Path
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $PSCommandPath,
        "-StarsectorDir", $StarsectorDir,
        "-QueuedWorker",
        "-PollSeconds", ([string]$PollSeconds)
    )
    if ($SkipCorePatchRefresh) {
        $args += "-SkipCorePatchRefresh"
    }
    $argumentLine = ($args | ForEach-Object { ConvertTo-ProcessArgument -Value $_ }) -join " "
    $process = Start-Process -FilePath $powerShellPath -ArgumentList $argumentLine -WindowStyle Hidden -PassThru
    Write-DeployState -ProcessId $process.Id -Phase "queued"
    Write-DeployLog "Queued private badge deploy pid=$($process.Id) target=$obfJar."
    Write-Host "Private badge deploy queued: core jar is locked. Hidden worker pid=$($process.Id) will rebuild, patch, and deploy after the lock clears."
}

if ($QueuedWorker) {
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
}

function Get-JarEntries {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
}

function Assert-PrivateBadgeJar {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Private badge jar not found: $Path"
    }

    $entries = Get-JarEntries -Path $Path
    $required = @(
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper.class",
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper`$Companion.class",
        "weaponsprocurement/internal/WeaponsProcurementBadgeConfig.class",
        "weaponsprocurement/internal/WeaponsProcurementCountUpdater.class"
    )

    $missing = @($required | Where-Object { $entries -notcontains $_ })
    if ($missing.Count -gt 0) {
        throw "Jar is missing private badge bridge classes: $($missing -join ', ')"
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
}

Restore-CleanRepoJar

Write-DeployState -ProcessId $PID -Phase "completed"
