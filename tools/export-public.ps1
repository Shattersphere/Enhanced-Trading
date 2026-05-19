param(
    [string]$OutputPath = (Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) "build") "public-export"),
    [switch]$NoClean
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$resolvedRepoRoot = [System.IO.Path]::GetFullPath($repoRoot)
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$resolvedBuildRoot = [System.IO.Path]::GetFullPath((Join-Path $repoRoot "build"))

if ($resolvedOutput -eq $resolvedRepoRoot) {
    throw "Refusing to export public files over the private repo root."
}
if ($resolvedOutput -eq $resolvedBuildRoot) {
    throw "Refusing to export public files over the repo build root."
}
if (-not $resolvedOutput.StartsWith($resolvedBuildRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to export outside the repo build directory: $resolvedOutput"
}
if (-not $NoClean -and (Test-Path -LiteralPath $resolvedOutput)) {
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$includeFiles = @(
    ".github/workflows/sanity.yml",
    ".gitignore",
    "build.ps1",
    "build.gradle.kts",
    "CHANGELOG.md",
    "CONFIG.md",
    "gradle.properties",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradlew",
    "gradlew.bat",
    "mod_info.json",
    "PACKAGING.md",
    "README.md",
    "settings.gradle.kts",
    "data/campaign/rules.csv",
    "data/config/LunaSettings.csv",
    "data/config/enhanced_trading_market_blacklist.json",
    "data/config/enhanced_trading_stock.json",
    "tools/deploy-live-mod.ps1",
    "tools/lib/Deploy.Common.ps1",
    "tools/analyze-trade-rollback-diagnostics.ps1",
    "tools/validate-doc-links.ps1",
    "tools/validate-gui-button-style.ps1",
    "tools/validate-live-gui-classes.ps1"
)

function Copy-RepoFile {
    param([string]$RelativePath)
    $source = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Missing export source: $RelativePath"
    }
    $target = Join-Path $resolvedOutput $RelativePath
    $targetDir = Split-Path -Parent $target
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
}

function Get-RelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )
    $base = [System.IO.Path]::GetFullPath($BasePath).TrimEnd('\', '/')
    $full = [System.IO.Path]::GetFullPath($FullPath)
    if (-not $full.StartsWith($base + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path '$full' is not under '$base'."
    }
    return $full.Substring($base.Length + 1)
}

foreach ($file in $includeFiles) {
    Copy-RepoFile -RelativePath $file
}

$publicWorkflow = Join-Path $resolvedOutput ".github/workflows/sanity.yml"
if (Test-Path -LiteralPath $publicWorkflow) {
    $workflowText = Get-Content -LiteralPath $publicWorkflow -Raw
    $workflowText = $workflowText.Replace(" -IncludePrivateDocs", "")
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Set up Java for jar inspection\r?\n        uses: actions/setup-java@v5\r?\n        with:\r?\n          distribution: temurin\r?\n          java-version: '17'\r?\n",
        "`r`n"
    )
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Validate committed jar stale classes\r?\n        shell: pwsh\r?\n        run: .*?validate-jar-classes.*?\r?\n",
        "`r`n"
    )
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Validate public export boundary\r?\n        shell: pwsh\r?\n        run: .*?export-public\.ps1.*?\r?\n",
        "`r`n"
    )
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Validate Kotlin migration boundaries\r?\n        shell: pwsh\r?\n        run: .*?validate-kotlin-migration\.ps1.*?\r?\n",
        "`r`n"
    )
    Set-Content -LiteralPath $publicWorkflow -Value $workflowText -NoNewline
}
$publicDeploy = Join-Path $resolvedOutput "tools/deploy-live-mod.ps1"
if (Test-Path -LiteralPath $publicDeploy) {
    $deployText = Get-Content -LiteralPath $publicDeploy -Raw
    $deployText = [regex]::Replace(
        $deployText,
        "(?ms)function Assert-DeployJarBoundary \{.*?\r?\n\}\r?\n\r?\nfunction Assert-DeployRoot",
        @'
function Assert-DeployJarBoundary {
    param([string]$BaseRoot)

    $jarPath = Join-Path $BaseRoot "jars\enhanced-trading.jar"
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "Deploy jar not found: $jarPath"
    }
}

function Assert-DeployRoot
'@
    )
    Set-Content -LiteralPath $publicDeploy -Value $deployText -NoNewline
}
$publicBuildWrapper = Join-Path $resolvedOutput "build.ps1"
@'
param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipClean
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$gradleWrapper = Join-Path $PSScriptRoot "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Missing Gradle wrapper at '$gradleWrapper'."
}

$gradleArgs = @("--no-daemon", "-PstarsectorDir=$StarsectorDir")
if (-not $SkipClean) {
    $gradleArgs += "clean"
}
$gradleArgs += "buildMod"

& $gradleWrapper @gradleArgs
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE."
}

$jarPath = Join-Path $PSScriptRoot "jars\enhanced-trading.jar"
Write-Host "Built $jarPath"
'@ | Set-Content -LiteralPath $publicBuildWrapper -NoNewline
$publicDocValidator = Join-Path $resolvedOutput "tools/validate-doc-links.ps1"
@'
param(
    [string[]]$Paths = @(
        "README.md",
        "PACKAGING.md",
        "CONFIG.md",
        "CHANGELOG.md"
    )
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$badLinks = @()
$missingFiles = @()

foreach ($path in $Paths) {
    $fullPath = Join-Path $repoRoot $path
    if (-not (Test-Path -LiteralPath $fullPath)) {
        $missingFiles += $path
        continue
    }

    $content = Get-Content -LiteralPath $fullPath -Raw
    if ($content -match '\]\((?:/[A-Za-z]:|[A-Za-z]:\\|file://)') {
        $badLinks += $path
    }
}

if ($missingFiles.Count -gt 0) {
    throw "Documentation files missing: $($missingFiles -join ', ')"
}

if ($badLinks.Count -gt 0) {
    throw "Documentation contains local filesystem links: $($badLinks -join ', ')"
}

Write-Host "Documentation link validation passed."
'@ | Set-Content -LiteralPath $publicDocValidator -NoNewline

$srcRoot = Join-Path $repoRoot "src"
$sources = Get-ChildItem -LiteralPath $srcRoot -Recurse -File |
    Where-Object {
        $_.Extension -in @(".java", ".kt")
    }
foreach ($source in $sources) {
    $relative = (Get-RelativePath -BasePath $repoRoot -FullPath $source.FullName).Replace("\", "/")
    Copy-RepoFile -RelativePath $relative
}

$leakTerms = @(
    "AGENTS.md",
    ".agent/",
    ".agent\",
    "HANDOVER.md",
    "PLANS.md",
    "LESSONS.md",
    "D:\Sean Mods",
    "C:\Games\Starsector",
    "starfarer_obf",
    "CargoStackView",
    "bytecode",
    "patched badge",
    "patched cargo-cell",
    "WeaponsProcurementBadgeHelper",
    "WeaponsProcurementBadgeConfig",
    "WeaponsProcurementCountUpdater"
)

$scanFiles = Get-ChildItem -LiteralPath $resolvedOutput -Recurse -File |
    Where-Object {
        $_.Extension -in @(".java", ".kt", ".ps1", ".md", ".csv", ".json", ".yml", ".yaml", ".txt", ".kts", ".gradle", ".properties")
    }
$leaks = @()
foreach ($file in $scanFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($term in $leakTerms) {
        if ($text.IndexOf($term, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
            $relative = Get-RelativePath -BasePath $resolvedOutput -FullPath $file.FullName
            $leaks += "$relative contains '$term'"
        }
    }
}
if ($leaks.Count -gt 0) {
    throw "Public export leak scan failed:`n$($leaks -join "`n")"
}

Write-Host "Exported public Enhanced Trading source to $resolvedOutput"
