param([int]$Port = 0)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host 'ERROR: javac was not found. Install JDK 17 or later and restart VS Code.' -ForegroundColor Red
    exit 1
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host 'ERROR: java was not found. Install JDK 17 or later and restart VS Code.' -ForegroundColor Red
    exit 1
}

$javaVersion = (& cmd.exe /d /c "java -version 2>&1" | Select-Object -First 1)
Write-Host "Using $javaVersion" -ForegroundColor DarkGray
New-Item -ItemType Directory -Force out | Out-Null
$javaFiles = @(Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })

Write-Host 'Compiling Java + DSA engine + local web server...' -ForegroundColor Cyan
& javac --add-modules jdk.httpserver -encoding UTF-8 -d out $javaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ''
Write-Host 'Running full project self-test...' -ForegroundColor Cyan
& java --add-modules jdk.httpserver -cp out com.dsa.jobportal.SelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ''
Write-Host 'Starting NovaHire Web Edition...' -ForegroundColor Green
if ($Port -gt 0) {
    & java --add-modules jdk.httpserver -cp out com.dsa.jobportal.web.WebMain $Port
} else {
    & java --add-modules jdk.httpserver -cp out com.dsa.jobportal.web.WebMain
}
