$ErrorActionPreference = "Stop"

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

function Get-StableHash {
    param([string]$Value)

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Value.ToLowerInvariant())
        $hash = $sha.ComputeHash($bytes)
        return (($hash | ForEach-Object { $_.ToString("x2") }) -join "").Substring(0, 16)
    } finally {
        $sha.Dispose()
    }
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

function Read-DeployStateFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    try {
        return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Test-QueuedDeployWorkerActive {
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

function Stop-QueuedDeployFromStateFile {
    param(
        [string]$Path,
        [string]$CurrentScriptPath,
        [int]$CurrentProcessId,
        [string]$NotOwnedMessage,
        [string]$CancelledMessage
    )

    $state = Read-DeployStateFile -Path $Path
    if ($null -eq $state -or $null -eq $state.Pid) {
        return
    }
    $oldPid = [int]$state.Pid
    if ($oldPid -eq $CurrentProcessId) {
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
    if ($commandLine.IndexOf($CurrentScriptPath, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
        Write-Host ($NotOwnedMessage.Replace("{pid}", [string]$oldPid))
        return
    }
    Stop-Process -Id $oldPid -Force
    $message = $CancelledMessage.Replace("{pid}", [string]$oldPid).Replace("{path}", $Path)
    Write-DeployLog $message
}

function Get-ManifestHash {
    param([string[]]$Lines)

    $text = ($Lines | Sort-Object) -join "`n"
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
        $hash = $sha.ComputeHash($bytes)
        return (($hash | ForEach-Object { $_.ToString("x2") }) -join "")
    } finally {
        $sha.Dispose()
    }
}

function Get-ZipEntryNames {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
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
    $workingDirectory = Split-Path -Parent $FilePath

    $startupInfo = New-Object QueuedDeploy.STARTUPINFO
    $startupInfo.cb = [Runtime.InteropServices.Marshal]::SizeOf([type][QueuedDeploy.STARTUPINFO])
    $startupInfo.dwFlags = 0x00000001
    $startupInfo.wShowWindow = 7
    $processInfo = New-Object QueuedDeploy.PROCESS_INFORMATION
    $created = [QueuedDeploy.NativeMethods]::CreateProcess($FilePath, $commandLine, [IntPtr]::Zero, [IntPtr]::Zero, $false, 0x00000010, [IntPtr]::Zero, $workingDirectory, [ref]$startupInfo, [ref]$processInfo)
    if (-not $created) {
        $errorCode = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
        throw "Failed to start queued deploy worker without activating focus. Win32 error: $errorCode"
    }

    [void][QueuedDeploy.NativeMethods]::CloseHandle($processInfo.hThread)
    [void][QueuedDeploy.NativeMethods]::CloseHandle($processInfo.hProcess)
    return [pscustomobject]@{ Id = [int]$processInfo.dwProcessId }
}
