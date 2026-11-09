param(
    [string]$Task = ":app:assembleDebug",
    [switch]$NoDaemon = $true
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Join-Path (Split-Path -Parent $ScriptDir) "punjabi-beats"

Write-Host "=== Punjabi Beats Build ($Task) ===" -ForegroundColor Cyan
Write-Host "Project directory: $ProjectDir"

# 1. Check JAVA_HOME / Java
if (-not $env:JAVA_HOME) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        Write-Host "Using system Java: $($javaCmd.Source)"
    } else {
        Write-Error "JAVA_HOME is not set and java is not in PATH."
    }
} else {
    Write-Host "JAVA_HOME: $env:JAVA_HOME"
}

# 2. Check local.properties / Android SDK
$localProps = Join-Path $ProjectDir "local.properties"
if (-not (Test-Path $localProps)) {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) {
        $sdk = "$env:LOCALAPPDATA\Android\Sdk"
    }
    if ($sdk) {
        $sdkEscaped = $sdk.Replace("\", "/")
        "sdk.dir=$sdkEscaped" | Out-File -FilePath $localProps -Encoding ascii
        Write-Host "Created local.properties pointing to: $sdk" -ForegroundColor Green
    } else {
        Write-Warning "Android SDK not found. Please create punjabi-beats/local.properties with sdk.dir=<path to your Android SDK>"
    }
}

# 3. Run Gradlew
Push-Location $ProjectDir
try {
    $cmd = ".\gradlew.bat $Task"
    if ($NoDaemon) { $cmd += " --no-daemon" }
    Write-Host "Executing: $cmd" -ForegroundColor Yellow
    Invoke-Expression $cmd
} finally {
    Pop-Location
}

