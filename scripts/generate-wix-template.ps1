param(
    [string]$ProjectRoot = ".",
    [string]$TempDir = "target\jpackage-temp-windows-msi",
    [string]$OutputDir = "output\windows",
    [string]$InputDir = "target\jpackage-input",
    [string]$AppName = "Dataverse Downloader",
    [string]$MainJar = "dataverse-downloader.jar",
    [string]$MainClass = "de.unibn.hrz.dataverse.downloader.App",
    [string]$Vendor = "University of Bonn",
    [string]$IconPath = "packaging\windows\dataverse.ico"
)

$ErrorActionPreference = "Stop"

function Get-JPackageVersionFromPom {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PomPath
    )

    if (!(Test-Path $PomPath)) {
        throw "pom.xml not found: $PomPath"
    }

    [xml]$pom = Get-Content -Path $PomPath

    $versionNode = $pom.project.version
    if ([string]::IsNullOrWhiteSpace($versionNode)) {
        throw "Could not read <version> from pom.xml"
    }

    $rawVersion = $versionNode.Trim()

    if ($rawVersion -match '^(\d+)\.(\d+)\.(\d+)') {
        return "$($matches[1]).$($matches[2]).$($matches[3])"
    }

    throw "Project version '$rawVersion' is not a jpackage-compatible numeric version."
}

Set-Location $ProjectRoot

$PomPath = Join-Path (Get-Location) "pom.xml"
$AppVersion = Get-JPackageVersionFromPom -PomPath $PomPath

Write-Host "Project root: $(Get-Location)"
Write-Host "Resolved app version from pom.xml: $AppVersion"
Write-Host "Preparing temp directory: $TempDir"

if (Test-Path $TempDir) {
    Remove-Item -Recurse -Force $TempDir
}
New-Item -ItemType Directory -Path $TempDir | Out-Null

if (!(Test-Path $InputDir)) {
    throw "Input directory not found: $InputDir. Run 'mvn package' first."
}

$mainJarPath = Join-Path $InputDir $MainJar
if (!(Test-Path $mainJarPath)) {
    throw "Main JAR not found: $mainJarPath"
}

if (!(Test-Path $IconPath)) {
    throw "Icon not found: $IconPath"
}

Write-Host "Running jpackage to generate MSI temp sources..."

& jpackage `
  --type msi `
  --dest $OutputDir `
  --input $InputDir `
  --name $AppName `
  --main-jar $MainJar `
  --main-class $MainClass `
  --app-version $AppVersion `
  --vendor $Vendor `
  --icon $IconPath `
  --win-menu `
  --win-shortcut `
  --java-options "-Dfile.encoding=UTF-8" `
  --temp $TempDir `
  --verbose

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}

Write-Host "Searching for generated main.wxs..."

$mainWxs = Get-ChildItem -Path $TempDir -Recurse -Filter "main.wxs" | Select-Object -First 1

if ($null -eq $mainWxs) {
    throw "Could not find generated main.wxs in $TempDir"
}

$targetDir = "packaging\windows\jpackage-resources"
$targetFile = Join-Path $targetDir "main.wxs"

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
Copy-Item -Force $mainWxs.FullName $targetFile

Write-Host ""
Write-Host "Generated WiX template copied to:"
Write-Host "  $targetFile"
Write-Host ""
Write-Host "Original generated file:"
Write-Host "  $($mainWxs.FullName)"
Write-Host ""
Write-Host "App version used:"
Write-Host "  $AppVersion"
Write-Host ""
Write-Host "Next step: edit packaging\windows\jpackage-resources\main.wxs only if you want to regenerate from scratch."