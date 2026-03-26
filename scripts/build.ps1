$ErrorActionPreference = "Stop"

function Get-AppVersionFromPom {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PomPath
    )

    if (!(Test-Path $PomPath)) {
        throw "pom.xml not found: $PomPath"
    }

    [xml]$pom = Get-Content -Path $PomPath
    $rawVersion = $pom.project.version

    if ([string]::IsNullOrWhiteSpace($rawVersion)) {
        throw "Could not read <version> from pom.xml"
    }

    $rawVersion = $rawVersion.Trim()

    if ($rawVersion -match '^(\d+)\.(\d+)\.(\d+)') {
        return "$($matches[1]).$($matches[2]).$($matches[3])"
    }

    throw "Project version '$rawVersion' is not a jpackage-compatible numeric version."
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
Set-Location $ProjectRoot

$AppVersion = Get-AppVersionFromPom -PomPath (Join-Path $ProjectRoot "pom.xml")

Write-Host "Detecting OS..."

$platform = [System.Environment]::OSVersion.Platform

if ($platform -eq "Win32NT") {
    $profile = "package-windows"
    $platformDir = "windows"
}
elseif ($platform -eq "Unix") {
    $uname = ""
    try { $uname = (uname) } catch {}

    if ($uname -match "Darwin") {
        $profile = "package-mac"
        $platformDir = "mac"
    }
    else {
        $profile = "package-linux"
        $platformDir = "linux"
    }
}
else {
    throw "Unsupported operating system: $platform"
}

if (Test-Path (Join-Path $ProjectRoot "mvnw.cmd")) {
    $mvnCmd = Join-Path $ProjectRoot "mvnw.cmd"
}
elseif (Test-Path (Join-Path $ProjectRoot "mvnw")) {
    $mvnCmd = Join-Path $ProjectRoot "mvnw"
}
else {
    $mvnCmd = "mvn"
}

$outputBase = Join-Path $ProjectRoot ("output\" + $platformDir)
$versionOutputDir = Join-Path $outputBase $AppVersion

Write-Host "Using app-image profile: $profile"
Write-Host "Resolved app version: $AppVersion"
Write-Host "Output directory: $versionOutputDir"

if (Test-Path $versionOutputDir) {
    Remove-Item -Recurse -Force $versionOutputDir
}
New-Item -ItemType Directory -Force -Path $versionOutputDir | Out-Null

& $mvnCmd clean package "-P$profile" "-Doutput.base=$versionOutputDir"

if ($LASTEXITCODE -ne 0) {
    throw "Build failed with exit code $LASTEXITCODE"
}

Write-Host ""
Write-Host "Build finished successfully."
Write-Host "Output written to: $versionOutputDir"