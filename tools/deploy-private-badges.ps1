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

function Write-DeployLog {
    param([string]$Message)
    $line = "$(Get-Date -Format o) $Message"
    if (-not (Test-Path -LiteralPath $stateRoot)) {
        New-Item -ItemType Directory -Force -Path $stateRoot | Out-Null
    }
    Add-Content -LiteralPath $logFile -Value $line
}

function Get-DeployCommit {
    param([string]$RepoRoot)
    try {
        $commit = (& git -C $RepoRoot rev-parse --short HEAD 2>$null)
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($commit)) {
            return $commit.Trim()
        }
    } catch {
    }
    return "unknown"
}

function Get-DeployStatusTimestamp {
    return (Get-Date -Format "d/M/yy h:mmtt").ToLowerInvariant()
}

function Write-DeployStatus {
    param(
        [string]$Path,
        [string]$Commit,
        [string]$Status,
        [string]$Message = "",
        [switch]$Reset
    )

    $timestamp = Get-DeployStatusTimestamp
    if ($Status -eq "error") {
        $safeMessage = (($Message -replace "(\r\n|\n|\r)", " ") -replace "'", "''")
        $line = "$timestamp - Deploy $Commit error: '$safeMessage'"
    } else {
        $line = "$timestamp - Deploy $Commit $Status"
    }

    if ($Reset) {
        Set-Content -LiteralPath $Path -Value $line -Encoding UTF8
    } else {
        Add-Content -LiteralPath $Path -Value $line -Encoding UTF8
    }
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

function Test-QueuedPrivateDeployActive {
    param([object]$State)

    if ($null -eq $State -or $null -eq $State.Pid) {
        return $false
    }
    if ([string]::Equals([string]$State.Phase, "completed", [System.StringComparison]::OrdinalIgnoreCase)) {
        return $false
    }
    if ([string]::Equals([string]$State.Phase, "failed", [System.StringComparison]::OrdinalIgnoreCase)) {
        return $false
    }

    $process = Get-Process -Id ([int]$State.Pid) -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return $false
    }

    $scriptPath = [string]$State.ScriptPath
    if (![string]::IsNullOrWhiteSpace($scriptPath)) {
        $commandLine = ""
        try {
            $commandLine = [string](Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$State.Pid)" -ErrorAction Stop).CommandLine
        } catch {
        }
        if ($commandLine.IndexOf($scriptPath, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
            return $false
        }
    }
    return $true
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

function Start-MinimizedNoActivateProcess {
    param([string]$FilePath, [object]$ArgumentList)

    if (-not ("QueuedDeploy.NativeMethods" -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

namespace QueuedDeploy {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct STARTUPINFO {
        public UInt32 cb;
        public string lpReserved;
        public string lpDesktop;
        public string lpTitle;
        public UInt32 dwX;
        public UInt32 dwY;
        public UInt32 dwXSize;
        public UInt32 dwYSize;
        public UInt32 dwXCountChars;
        public UInt32 dwYCountChars;
        public UInt32 dwFillAttribute;
        public UInt32 dwFlags;
        public UInt16 wShowWindow;
        public UInt16 cbReserved2;
        public IntPtr lpReserved2;
        public IntPtr hStdInput;
        public IntPtr hStdOutput;
        public IntPtr hStdError;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct PROCESS_INFORMATION {
        public IntPtr hProcess;
        public IntPtr hThread;
        public UInt32 dwProcessId;
        public UInt32 dwThreadId;
    }

    public static class NativeMethods {
        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        public static extern bool CreateProcess(
            string lpApplicationName,
            string lpCommandLine,
            IntPtr lpProcessAttributes,
            IntPtr lpThreadAttributes,
            bool bInheritHandles,
            UInt32 dwCreationFlags,
            IntPtr lpEnvironment,
            string lpCurrentDirectory,
            ref STARTUPINFO lpStartupInfo,
            out PROCESS_INFORMATION lpProcessInformation);

        [DllImport("kernel32.dll", SetLastError = true)]
        public static extern bool CloseHandle(IntPtr hObject);
    }
}
'@
    }

    $argumentText = if ($ArgumentList -is [array]) { $ArgumentList -join " " } else { [string]$ArgumentList }
    $commandLine = '"' + $FilePath + '"'
    if (-not [string]::IsNullOrWhiteSpace($argumentText)) {
        $commandLine += " $argumentText"
    }

    $startupInfo = New-Object QueuedDeploy.STARTUPINFO
    $startupInfo.cb = [Runtime.InteropServices.Marshal]::SizeOf([type][QueuedDeploy.STARTUPINFO])
    $startupInfo.dwFlags = 0x00000001
    $startupInfo.wShowWindow = 7
    $processInfo = New-Object QueuedDeploy.PROCESS_INFORMATION
    $created = [QueuedDeploy.NativeMethods]::CreateProcess($null, $commandLine, [IntPtr]::Zero, [IntPtr]::Zero, $false, 0x00000010, [IntPtr]::Zero, $null, [ref]$startupInfo, [ref]$processInfo)
    if (-not $created) {
        $errorCode = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
        throw "Failed to start queued deploy worker without activating focus. Win32 error: $errorCode"
    }

    [void][QueuedDeploy.NativeMethods]::CloseHandle($processInfo.hThread)
    [void][QueuedDeploy.NativeMethods]::CloseHandle($processInfo.hProcess)
    return [pscustomobject]@{ Id = [int]$processInfo.dwProcessId }
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
        Write-Host "Private badge queue active: $(Test-QueuedPrivateDeployActive -State $state)"
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
        "weaponsprocurement/internal/WeaponsProcurementBadgeConfig.class",
        "weaponsprocurement/internal/WeaponsProcurementCountUpdater.class",
        "weaponsprocurement/plugins/WeaponsProcurementPrivateBadgeBootstrap.class"
    )

    $missing = @($required | Where-Object { $entries -notcontains $_ })
    if ($missing.Count -gt 0) {
        throw "Jar is missing private badge bridge classes: $($missing -join ', ')"
    }

    $kotlinOnlyHelperEntries = @(
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper`$Companion.class"
    )
    $stale = @($kotlinOnlyHelperEntries | Where-Object { $entries -contains $_ })
    if ($stale.Count -gt 0) {
        throw "Jar contains Kotlin-compiled embedded helper entries that are unsafe for the core classloader: $($stale -join ', ')"
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
