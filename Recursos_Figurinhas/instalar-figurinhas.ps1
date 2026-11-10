param(
    [Parameter(Mandatory=$true)][string]$GlassFishHome,
    [string]$Dominio = "domain1",
    [Parameter(Mandatory=$true)][string]$CatalogoJar,
    [string]$Java = "java",
    # Atualizações deliberadas: caminho relativo=SHA256 ATUAL esperado no destino.
    # Inclua também catalogo.tsv quando trocar de distribuição. Não remove arquivos.
    [string[]]$AutorizarSubstituicoes = @()
)
$ErrorActionPreference = "Stop"
$origem = Join-Path $PSScriptRoot "figurinhas"
$docroot = Join-Path $GlassFishHome "domains/$Dominio/docroot"
$destino = Join-Path $docroot "figurinhas"
if (!(Test-Path -LiteralPath $origem -PathType Container)) { throw "Origem ausente: $origem" }
if (!(Test-Path -LiteralPath $docroot -PathType Container)) { throw "Docroot ausente: $docroot" }

# Nenhuma escrita antes da validação integral da distribuição e do plano de destino.
& $Java -cp $CatalogoJar chat.catalogo.Catalogo $origem
if ($LASTEXITCODE -ne 0) { throw "Catalogo ou recursos invalidos; nada publicado." }
$previstos = @('imagens/avatar.png','imagens/menino.png','imagens/menina.png')
$linhas = [IO.File]::ReadAllLines((Join-Path $origem 'catalogo.tsv'), [Text.Encoding]::UTF8)
foreach ($linha in $linhas | Select-Object -Skip 1) {
    $colunas = $linha.Split([char]9)
    if ($colunas[1] -ne 'unicode') { $previstos += $colunas[3] }
}
$previstos += 'catalogo.tsv' # manifesto por último
if (@($previstos | Select-Object -Unique).Count -ne $previstos.Count) { throw 'Caminhos repetidos.' }
$autorizados = @{}
foreach ($item in $AutorizarSubstituicoes) {
    if ($item -notmatch '^([^=]+)=([A-Fa-f0-9]{64})$') { throw 'Autorizacao exige caminho=SHA256 atual.' }
    $relativo=$Matches[1]; $hash=$Matches[2]
    if ($previstos -cnotcontains $relativo -or $autorizados.ContainsKey($relativo)) { throw "Autorizacao invalida: $relativo" }
    $autorizados[$relativo]=$hash
}
function Verificar-Caminho([string]$caminho) {
    # Recusa junções/symlinks, inclusive em ancestrais, e conflitos diretório/arquivo.
    $atual = [IO.Path]::GetFullPath($caminho)
    while ($atual) {
        if (Test-Path -LiteralPath $atual) {
            if ((Get-Item -LiteralPath $atual -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw "Link/juncao nao permitido: $atual"
            }
        }
        $pai = Split-Path $atual -Parent
        if ($pai -eq $atual) { break }
        $atual=$pai
    }
}
$plano = @()
foreach ($relativo in $previstos) {
    $fonte=Join-Path $origem $relativo; $alvo=Join-Path $destino $relativo
    # A origem já foi resolvida/contida pela validação Java; placeholders OneDrive são legítimos.
    # No destino não aceitamos redirecionamentos por links/junções.
    Verificar-Caminho $alvo
    if (!(Test-Path -LiteralPath $fonte -PathType Leaf)) { throw "Fonte ausente: $relativo" }
    $pai=Split-Path $alvo -Parent
    while ($pai) {
        if ((Test-Path -LiteralPath $pai) -and !(Test-Path -LiteralPath $pai -PathType Container)) { throw "Ancestral nao e diretorio: $pai" }
        $proximo=Split-Path $pai -Parent
        if ($proximo -eq $pai) { break }; $pai=$proximo
    }
    $hash=(Get-FileHash -LiteralPath $fonte -Algorithm SHA256).Hash
    $existente=$null
    if (Test-Path -LiteralPath $alvo) {
        if (!(Test-Path -LiteralPath $alvo -PathType Leaf)) { throw "Destino nao e arquivo: $relativo" }
        $existente=(Get-FileHash -LiteralPath $alvo -Algorithm SHA256).Hash
        if ($existente -ne $hash -and (!$autorizados.ContainsKey($relativo) -or $autorizados[$relativo] -ne $existente)) {
            throw "Conflito: $relativo. Nada publicado. Autorize explicitamente com caminho=SHA256 atual: $existente"
        }
    }
    if ($autorizados.ContainsKey($relativo) -and $autorizados[$relativo] -ne $existente) { throw "Precondicao de atualizacao divergente: $relativo" }
    $plano += [PSCustomObject]@{Relativo=$relativo; Fonte=$fonte; Alvo=$alvo; Hash=$hash; Existente=$existente}
}
# Segunda conferência antes de escrever: detecta mudanças ocorridas durante a pré-validação.
foreach ($item in $plano) {
    if ((Get-FileHash -LiteralPath $item.Fonte).Hash -ne $item.Hash) { throw 'Origem alterada durante validacao.' }
    $atual=$null
    if (Test-Path -LiteralPath $item.Alvo -PathType Leaf) { $atual=(Get-FileHash -LiteralPath $item.Alvo).Hash }
    if ($atual -ne $item.Existente) { throw 'Destino alterado durante validacao.' }
}
foreach ($item in $plano) {
    if ($item.Existente -eq $item.Hash) { continue }
    New-Item -ItemType Directory -Force -Path (Split-Path $item.Alvo -Parent) | Out-Null
    Copy-Item -LiteralPath $item.Fonte -Destination $item.Alvo -Force
    if ((Get-FileHash -LiteralPath $item.Alvo).Hash -ne $item.Hash) { throw "Falha de verificacao: $($item.Relativo)" }
}
Write-Host "Instalacao concluida: $($plano.Count) arquivos previstos verificados; extras preservados/ignorados."
Write-Host "Destino: $destino"
