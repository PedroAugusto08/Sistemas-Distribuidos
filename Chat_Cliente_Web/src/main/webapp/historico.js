'use strict';
let anterior = '', primeira = true;
let apelidoAtual = null;
let conexaoWeb = 'conectando', versaoConexao = 0;
function definirConexaoWeb(estado) {
    conexaoWeb = estado;
    versaoConexao++;
    document.getElementById('estado').textContent = estado === 'disponivel' ? 'Histórico atualizado'
        : estado === 'sessao' ? 'Sessão expirada. Entre novamente.' : 'Web desconectado • reconectando...';
    if (typeof atualizarControlesConexao === 'function') atualizarControlesConexao();
}
const historico = document.getElementById('historico');
const rolagem = document.getElementById('rolagem');
const novas = document.getElementById('novas');
function pertoDoFim() { return rolagem.scrollHeight - rolagem.scrollTop - rolagem.clientHeight < 80; }
function irAoFim() { rolagem.scrollTop = rolagem.scrollHeight; novas.hidden = true; }
novas.addEventListener('click', irAoFim);
rolagem.addEventListener('scroll', () => { if (pertoDoFim()) novas.hidden = true; });
// O serviço HTTP separa as linhas do repositório com \n. Só agrupamos linhas
// com o cabeçalho completo produzido pelos clientes e o terminador conhecido.
const cabecalho = /^(<img src=['"]\/figurinhas\/imagens\/(?:menino|menina|avatar)\.png['"] width=['"](?:20|30)['"] height=['"](?:20|30)['"]>)(<font(?: size=['"][+]?[12]['"])? color=['"](?:#[0-9a-fA-F]{6}|navy|tomato|black|DarkRed|red)['"](?: size=['"][+]?[12]['"])?>(?:[^<]*|<b>[^<]*<\/b>)<\/font>)([\s\S]*<br\s*\/?>)$/i;
function formatarHistorico(html) {
    let resultado = '', legado = '';
    function concluirLegado() {
        if (legado) { resultado += '<div class="mensagens-legadas">' + legado + '</div>'; legado = ''; }
    }
    for (const linha of html.split(/(?<=\n)/)) {
        if (!linha) continue;
        const conteudo = linha.endsWith('\n') ? linha.slice(0, -1) : linha;
        const partes = conteudo.match(cabecalho);
        // Uma linha incompleta ou com outro avatar pode ser legado ambíguo.
        if (!linha.endsWith('\n') || !partes || /<img src=['"]\/figurinhas\/imagens\//i.test(partes[3])) {
            legado += linha;
            continue;
        }
        concluirLegado();
        let corpo = partes[3].replace(/<br\s*\/?>$/i, '')
            .replace(/^<br\s*\/?>\s*(?=<img src=['"]\/figurinhas\/stickers\/)/i, '');
        // Apenas o cabeçalho exato gerado pelo cliente Web garante o prefixo.
        const cabecalhoWeb = /^<img src='\/figurinhas\/imagens\/(?:menino|menina|avatar)\.png' width='30' height='30'>$/.test(partes[1])
            && /^<font size="\+1" color="(?:navy|tomato|black)"><b>[^<]*<\/b><\/font>$/.test(partes[2]);
        if (cabecalhoWeb && corpo.startsWith('Diz: ')) corpo = corpo.slice(5);
        resultado += '<article class="mensagem"><div class="mensagem-cabecalho">' + partes[1] + partes[2]
            + '</div><div class="mensagem-corpo">' + corpo + '</div></article>';
    }
    concluirLegado();
    return resultado;
}
function apelidoDoCabecalho(font) {
    const negrito = font.querySelector('b');
    if (negrito) return negrito.textContent; // Web: nome exato dentro de <b>.
    const nome = font.textContent;
    // Desktop acrescenta exatamente um espaço antes do apelido.
    return nome.startsWith(' ') ? nome.slice(1) : null;
}
function classificarMensagens(apelido) {
    historico.querySelectorAll('.mensagem').forEach(mensagem => {
        const font = mensagem.querySelector('.mensagem-cabecalho font');
        mensagem.classList.toggle('mensagem-propria', apelido !== null && font !== null
            && apelidoDoCabecalho(font) === apelido);
    });
}
function apelidoDaResposta(resposta) {
    const valor = resposta.headers.get('X-Chat-Nickname');
    if (valor === null) return null;
    try {
        return new TextDecoder('utf-8', {fatal: true}).decode(Uint8Array.from(atob(valor), c => c.charCodeAt(0)));
    } catch (_) { return null; }
}
async function atualizar() {
    const versao = versaoConexao;
    try {
        const resposta = await fetch('conversa.jsp?fragmento=1', {cache:'no-store', redirect:'error'});
        if (resposta.status === 401) {
            if (versao === versaoConexao) definirConexaoWeb('sessao');
            return;
        }
        if (!resposta.ok) throw new Error();
        const apelido = apelidoDaResposta(resposta);
        const html = await resposta.text();
        if (versao !== versaoConexao) return;
        const seguir = primeira || pertoDoFim(), posicao = rolagem.scrollTop;
        if (html !== anterior) {
            // Preserva os nós existentes e suas animações quando há somente acréscimo.
            if (html.startsWith(anterior) && (!anterior || anterior.endsWith('\n')))
                historico.insertAdjacentHTML('beforeend', formatarHistorico(html.slice(anterior.length)));
            else historico.innerHTML = formatarHistorico(html);
            if (seguir) irAoFim(); else { rolagem.scrollTop = posicao; novas.hidden = false; }
        }
        if (html !== anterior || apelido !== apelidoAtual) classificarMensagens(apelido);
        apelidoAtual = apelido;
        anterior = html; primeira = false;
        if (versao === versaoConexao) definirConexaoWeb('disponivel');
    } catch (e) {
        if (versao === versaoConexao && conexaoWeb !== 'sessao') definirConexaoWeb('indisponivel');
    } finally { setTimeout(atualizar, 2000); }
}
atualizar();
