$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath
Set-Location $scriptDir

# npm i docsify-cli -g
# docsify init ./docs


if (Get-Command "docsify" -ErrorAction SilentlyContinue)
{
    Invoke-Expression 'docsify serve docs'
}
else
{
    Write-Host "docsify not installed."
}