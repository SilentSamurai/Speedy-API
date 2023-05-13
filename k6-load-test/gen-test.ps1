$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath
Set-Location $scriptDir
Write-Output $PWD

# START OF SCRIPT

openapi-generator-cli generate

#if(Test-Path 'tests/script.js'){
#    Remove-Item 'tests' -Recurse -Force
#}
