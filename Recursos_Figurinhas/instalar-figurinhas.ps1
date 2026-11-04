
param(
    [Parameter(Mandatory=$true)]
    [string]$GlassFishHome,

    [string]$Dominio = "domain1"
)

$ErrorActionPreference = "Stop"

$origem = Join-Path $PSScriptRoot "figurinhas"
$docroot = Join-Path $GlassFishHome "domains\$Dominio\docroot"
$destino = Join-Path $docroot "figurinhas"

if (!(Test-Path -LiteralPath $origem -PathType Container)) {
    throw "Pasta de origem nao encontrada: $origem"
}

if (!(Test-Path -LiteralPath $docroot -PathType Container)) {
    throw "Docroot do GlassFish nao encontrado: $docroot"
}

$arquivos = @(Get-ChildItem -LiteralPath $origem -Recurse -File)

foreach ($arquivo in $arquivos) {
    $relativo = $arquivo.FullName.Substring($origem.Length).TrimStart('\')
    $alvo = Join-Path $destino $relativo

    New-Item -ItemType Directory -Force -Path (Split-Path $alvo -Parent) | Out-Null
    Copy-Item -LiteralPath $arquivo.FullName -Destination $alvo -Force

    if ((Get-FileHash $arquivo.FullName).Hash -ne (Get-FileHash $alvo).Hash) {
        throw "Falha ao verificar: $relativo"
    }
}

Write-Host "Instalacao concluida: $($arquivos.Count) arquivos copiados e verificados."
Write-Host "Destino: $destino"
