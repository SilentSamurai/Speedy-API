


# npm i docsify-cli -g
# docsify init ./docs


if (Get-Command "docsify" -ErrorAction SilentlyContinue) {
    Invoke-Expression 'docsify serve docs'
} else {
    Write-Host "docsify not installed."
}