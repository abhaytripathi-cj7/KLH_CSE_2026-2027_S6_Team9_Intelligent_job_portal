$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out | Out-Null
$javaFiles = @(Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
Write-Host 'Compiling Java project...'
& javac --add-modules jdk.httpserver -encoding UTF-8 -d out $javaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ''
Write-Host 'Running full project self-test...'
& java --add-modules jdk.httpserver -cp out com.dsa.jobportal.SelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ''
Write-Host 'Starting console edition...'
& java --add-modules jdk.httpserver -cp out com.dsa.jobportal.Main
