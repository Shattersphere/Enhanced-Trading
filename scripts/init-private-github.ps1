<#
Create/connect this local repo to a private GitHub repo under an explicit owner.

Safe default: dry-run. Real run requires both -Run and -ConfirmPrivateRemote
matching the exact Owner/RepoName. This script requires an existing local commit,
a clean working tree, and does not stage or commit files.
#>
[CmdletBinding()]
param(
    [string]$Owner = "",
    [string]$RepoName,
    [string]$Description = "",
    [switch]$Run,
    [string]$ConfirmPrivateRemote = ""
)

$ErrorActionPreference = "Stop"

function Fail($Message) {
    Write-Error $Message
    exit 1
}

function Slugify($Name) {
    $slug = $Name.Trim().ToLowerInvariant()
    $slug = $slug -replace "[^a-z0-9._-]+", "-"
    $slug = $slug -replace "^-+", ""
    $slug = $slug -replace "-+$", ""
    if ([string]::IsNullOrWhiteSpace($slug)) { Fail "Could not derive a repo slug. Pass -RepoName." }
    return $slug
}

if ([string]::IsNullOrWhiteSpace($Owner)) {
    Fail "-Owner is required. Example: .\scripts\init-private-github.ps1 -Owner my-org -RepoName my-project"
}

$repoRoot = (Get-Location).Path
if (-not $RepoName) {
    $RepoName = Slugify((Split-Path -Leaf $repoRoot))
} else {
    $RepoName = Slugify($RepoName)
}

$target = "$Owner/$RepoName"
Write-Host "Repo root: $repoRoot"
Write-Host "Target GitHub repo: $target"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Fail "git is not available on PATH." }
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { Fail "GitHub CLI 'gh' is not available on PATH." }

& git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) { Fail "Not inside a Git repo. Initialize and commit locally first." }

& git rev-parse --verify HEAD *> $null
if ($LASTEXITCODE -ne 0) { Fail "No local commit exists. Commit intentional files before creating remote." }

$porcelain = (& git status --porcelain)
if ($porcelain) { Fail "Working tree is not clean. Commit, revert, or intentionally leave remote creation for later." }

& gh auth status
if ($LASTEXITCODE -ne 0) { Fail "GitHub CLI is not authenticated. Run: gh auth login" }

& git status --short --branch

$remoteUrl = ""
try { $remoteUrl = (& git remote get-url origin 2>$null) } catch { $remoteUrl = "" }
if ($remoteUrl) {
    Write-Host "origin already exists: $remoteUrl"
    Write-Host "No remote will be created. Verify it is the intended private repo."
    exit 0
}

$cmd = @("gh", "repo", "create", $target, "--private", "--source=.", "--remote=origin", "--push")
if ($Description.Trim()) { $cmd += @("--description", $Description) }

if ($Run) {
    if ($ConfirmPrivateRemote -ne $target) {
        Fail "Refusing real run. Pass -ConfirmPrivateRemote '$target' to confirm private repo creation."
    }
    $extra = @()
    if ($Description.Trim()) { $extra += @("--description", $Description) }
    & gh repo create $target --private --source=. --remote=origin --push @extra
} else {
    Write-Host "DRY RUN: would run: $($cmd -join ' ')"
    Write-Host "Real run requires: -Run -ConfirmPrivateRemote '$target'"
}
