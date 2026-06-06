# Builds a compact recursive source archive for review agents without modifying source files.
[CmdletBinding()]
param(
    [string] $RepoRoot,
    [string] $OutputDir,
    [int] $MaxFileSizeMb = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Join-Path $scriptRoot '..'
}

$repoRootPath = (Resolve-Path -LiteralPath $RepoRoot).Path.TrimEnd('\', '/')
if ($MaxFileSizeMb -lt 1) {
    throw 'MaxFileSizeMb must be at least 1.'
}

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $repoRootPath 'zips'
}

$outputPath = if (Test-Path -LiteralPath $OutputDir) {
    (Resolve-Path -LiteralPath $OutputDir).Path
} else {
    (New-Item -ItemType Directory -Path $OutputDir -Force).FullName
}

function ConvertTo-SafeFileNamePart {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Value
    )

    $safeValue = $Value.Trim()
    foreach ($invalidChar in [System.IO.Path]::GetInvalidFileNameChars()) {
        $safeValue = $safeValue.Replace($invalidChar, '-')
    }

    $safeValue = $safeValue -replace '\s+', ' '
    $safeValue = $safeValue.Trim(' ', '.', '-')
    if ($safeValue.Length -gt 80) {
        $safeValue = $safeValue.Substring(0, 80).Trim(' ', '.', '-')
    }

    if ([string]::IsNullOrWhiteSpace($safeValue)) {
        return 'Unnamed Mod'
    }

    return $safeValue
}

function Get-ModName {
    $fallbackName = Split-Path -Leaf $repoRootPath
    $modInfoPath = Join-Path $repoRootPath 'mod_info.json'
    if (Test-Path -LiteralPath $modInfoPath -PathType Leaf) {
        $modInfoText = Get-Content -Raw -LiteralPath $modInfoPath
        try {
            $modInfo = $modInfoText | ConvertFrom-Json
            if ($modInfo.PSObject.Properties.Name -contains 'name' -and
                -not [string]::IsNullOrWhiteSpace([string] $modInfo.name)) {
                return ConvertTo-SafeFileNamePart -Value ([string] $modInfo.name)
            }
        } catch {
            $nameMatch = [regex]::Match($modInfoText, '(?m)"name"\s*:\s*"((?:\\.|[^"\\])*)"')
            if ($nameMatch.Success -and -not [string]::IsNullOrWhiteSpace($nameMatch.Groups[1].Value)) {
                return ConvertTo-SafeFileNamePart -Value ([regex]::Unescape($nameMatch.Groups[1].Value))
            }

            Write-Warning ("Could not read mod name from {0}; using repo folder name. {1}" -f $modInfoPath, $_.Exception.Message)
        }
    }

    return ConvertTo-SafeFileNamePart -Value $fallbackName
}

$modName = Get-ModName
$zipPath = Join-Path $outputPath ("{0} Code.zip" -f $modName)

$safeRepoName = Split-Path -Leaf $repoRootPath
$safeRepoName = $safeRepoName -replace '[^A-Za-z0-9._-]', '-'
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("review-code-{0}-{1}" -f $safeRepoName, ([guid]::NewGuid().ToString('N')))
$null = New-Item -ItemType Directory -Path $stagingRoot -Force

# Recursive-by-default keeps the zipper current as the repo grows. These
# exclusions should describe generated/local/noisy material, not source layout.
$excludedDirectoryNames = @(
    '.git',
    '.gradle',
    '.gradle-agent',
    '.gradle-codex-build',
    '.gradle-home',
    '.gradle-run-home',
    '.kotlin',
    '.agent-deploy',
    '.idea',
    '.run',
    '.vscode',
    'build',
    'dist',
    'jars',
    'node_modules',
    'out',
    'release',
    'releases',
    'target',
    'zips',
    'Public Release',
    'debug text'
)

$excludedDirectoryRelativePaths = @(
    '.agent/staging',
    '.agent/tmp',
    '.agent/archive/history',
    '.agent/archive/retired-plans'
)

$excludedFilePatterns = @(
    '.env',
    '.env.*',
    '*.7z',
    '*.bmp',
    '*.class',
    '*.gif',
    '*.jar',
    '*.jks',
    '*.jpeg',
    '*.jpg',
    '*.log',
    '*.png',
    '*.p12',
    '*.pem',
    '*.pfx',
    '*.psd',
    '*.rar',
    '*.keystore',
    '*.temp',
    '*.tmp',
    '*.webp',
    '*.xcf',
    '*.zip',
    'Deploy Status.txt',
    'id_ed25519',
    'id_rsa',
    'local.properties'
)

function Get-RelativePath {
    param(
        [Parameter(Mandatory = $true)]
        [string] $FullPath
    )

    $normalizedPath = $FullPath.TrimEnd('\', '/')
    if ($normalizedPath.Length -lt $repoRootPath.Length -or
        -not $normalizedPath.StartsWith($repoRootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is outside repo root: $FullPath"
    }

    return $normalizedPath.Substring($repoRootPath.Length).TrimStart('\', '/')
}

function Test-ExcludedDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.DirectoryInfo] $Directory
    )

    foreach ($excludedName in $excludedDirectoryNames) {
        if ($Directory.Name -ieq $excludedName) {
            return $true
        }
    }

    $relativeDirectoryPath = (Get-RelativePath -FullPath $Directory.FullName).Replace('\', '/')
    foreach ($excludedRelativePath in $excludedDirectoryRelativePaths) {
        if ($relativeDirectoryPath.Equals($excludedRelativePath, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    return $false
}

function Test-ExcludedFilePattern {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileInfo] $File
    )

    if ($File.Name -like '*.example' -or
        $File.Name -like '*.sample' -or
        $File.Name -like '*.template') {
        return $false
    }

    foreach ($pattern in $excludedFilePatterns) {
        if ($File.Name -like $pattern) {
            return $true
        }
    }

    return $false
}

function Copy-ReviewFile {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileInfo] $File,
        [Parameter(Mandatory = $true)]
        [string] $RelativePath
    )

    $targetPath = Join-Path $stagingRoot $RelativePath
    $targetParent = Split-Path -Parent $targetPath
    if ($targetParent) {
        $null = New-Item -ItemType Directory -Path $targetParent -Force
    }

    Copy-Item -LiteralPath $File.FullName -Destination $targetPath -Force
}

function Write-ReviewArchiveNotes {
    $notesPath = Join-Path $stagingRoot 'REVIEW_ARCHIVE_NOTES.txt'
    $lines = @(
        'Review archive generated by scripts/create_review_code_zip.ps1.',
        '',
        'This archive is source-first review context, not a release package.',
        'Generated zips, build output, deploy staging, logs, local secrets, jars, and image-heavy assets are omitted by default.',
        'Some included config files may reference omitted runtime assets. Use the full repo or a curated release package for asset-existence, install, deploy, or visual review.',
        'Repo-specific copies may add narrow exceptions for review-critical assets or checked-in binaries.',
        ("Max file size: {0} MB" -f $MaxFileSizeMb)
    )

    Set-Content -LiteralPath $notesPath -Value $lines -Encoding ASCII
}

function New-PortableReviewZip {
    param(
        [Parameter(Mandatory = $true)]
        [string] $SourceRoot,
        [Parameter(Mandatory = $true)]
        [string] $DestinationPath
    )

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    if (Test-Path -LiteralPath $DestinationPath) {
        Remove-Item -LiteralPath $DestinationPath -Force
    }

    $sourceRootPath = (Resolve-Path -LiteralPath $SourceRoot).Path.TrimEnd('\', '/')
    $fileStream = [System.IO.File]::Open(
        $DestinationPath,
        [System.IO.FileMode]::CreateNew,
        [System.IO.FileAccess]::ReadWrite,
        [System.IO.FileShare]::None
    )
    $archive = [System.IO.Compression.ZipArchive]::new(
        $fileStream,
        [System.IO.Compression.ZipArchiveMode]::Create
    )
    try {
        Get-ChildItem -LiteralPath $sourceRootPath -File -Recurse -Force |
            Sort-Object FullName |
            ForEach-Object {
                $relativePath = $_.FullName.Substring($sourceRootPath.Length).TrimStart('\', '/')
                $entryName = $relativePath.Replace('\', '/')
                if ($entryName.Contains('\')) {
                    throw "Non-portable zip entry path generated: $entryName"
                }
                [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                    $archive,
                    $_.FullName,
                    $entryName,
                    [System.IO.Compression.CompressionLevel]::Optimal
                ) | Out-Null
            }
    } finally {
        $archive.Dispose()
        $fileStream.Dispose()
    }
}

function Assert-PortableZipEntries {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ZipFile
    )

    Add-Type -AssemblyName System.IO.Compression

    $fileStream = [System.IO.File]::Open(
        $ZipFile,
        [System.IO.FileMode]::Open,
        [System.IO.FileAccess]::Read,
        [System.IO.FileShare]::Read
    )
    $archive = [System.IO.Compression.ZipArchive]::new(
        $fileStream,
        [System.IO.Compression.ZipArchiveMode]::Read
    )
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.Contains('\')) {
                throw "Review archive contains non-portable backslash entry path: $($entry.FullName)"
            }
        }
    } finally {
        $archive.Dispose()
        $fileStream.Dispose()
    }
}

$maxFileSizeBytes = [int64] $MaxFileSizeMb * 1MB
$includedCount = 0
$skippedDirectories = 0
$skippedByPattern = 0
$skippedBySize = 0

try {
    $directoriesToScan = New-Object 'System.Collections.Generic.List[string]'
    $directoriesToScan.Add($repoRootPath)

    while ($directoriesToScan.Count -gt 0) {
        $currentDirectory = $directoriesToScan[$directoriesToScan.Count - 1]
        $directoriesToScan.RemoveAt($directoriesToScan.Count - 1)

        Get-ChildItem -LiteralPath $currentDirectory -Directory -Force -ErrorAction SilentlyContinue |
            ForEach-Object {
                if (Test-ExcludedDirectory -Directory $_) {
                    $script:skippedDirectories++
                } else {
                    $directoriesToScan.Add($_.FullName)
                }
            }

        Get-ChildItem -LiteralPath $currentDirectory -File -Force -ErrorAction SilentlyContinue |
            ForEach-Object {
                $relativePath = Get-RelativePath -FullPath $_.FullName

                if (Test-ExcludedFilePattern -File $_) {
                    $script:skippedByPattern++
                } elseif ($_.Length -gt $maxFileSizeBytes) {
                    $script:skippedBySize++
                } else {
                    Copy-ReviewFile -File $_ -RelativePath $relativePath
                    $script:includedCount++
                }
            }
    }

    Write-ReviewArchiveNotes
    $includedCount++

    $archiveItems = Get-ChildItem -LiteralPath $stagingRoot -Force
    if (-not $archiveItems) {
        throw 'No files were staged for the review archive. Check the exclusion lists and size cap.'
    }

    Get-ChildItem -LiteralPath $outputPath -File -Filter '*.zip' -ErrorAction SilentlyContinue |
        Remove-Item -Force

    New-PortableReviewZip -SourceRoot $stagingRoot -DestinationPath $zipPath
    Assert-PortableZipEntries -ZipFile $zipPath
    $zip = Get-Item -LiteralPath $zipPath
    $sizeMb = [math]::Round($zip.Length / 1MB, 2)
    Write-Host ("Created {0} ({1} MB)" -f $zip.FullName, $sizeMb)
    Write-Host ("Included {0} files. Skipped {1} directories, {2} files by pattern, {3} files by size > {4} MB." -f $includedCount, $skippedDirectories, $skippedByPattern, $skippedBySize, $MaxFileSizeMb)
} finally {
    if (Test-Path -LiteralPath $stagingRoot) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}
