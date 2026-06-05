param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [string]$ShatterLibDir = $env:SHATTER_LIB_DIRECTORY,
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
if (-not [string]::IsNullOrWhiteSpace($ShatterLibDir)) {
    $gradleArgs += "-PshatterLibDir=$ShatterLibDir"
}
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
