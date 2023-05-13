$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath
Set-Location $scriptDir
Write-Output $PWD

# START OF SCRIPT

k6 run ./script.js

