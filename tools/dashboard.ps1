$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'dashboard.py'
if (Get-Command py -ErrorAction SilentlyContinue) {
    & py -3 $scriptPath @args
} elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
    & python3 $scriptPath @args
} else {
    throw 'Instala Python 3 desde python.org y vuelve a abrir la terminal.'
}
exit $LASTEXITCODE
