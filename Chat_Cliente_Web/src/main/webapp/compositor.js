'use strict';
const form = document.getElementById('compositor');
const texto = document.getElementById('texto');
const erro = document.getElementById('erro');
const seletor = document.getElementById('seletor');
const abrir = document.getElementById('abrir-seletor');
const abas = Array.from(document.querySelectorAll('[role=tab]'));
let inicio = 0, fim = 0, direcao = 'none', enviando = false;
const enviarBotao = document.getElementById('enviar');
const imagens = Array.from(document.querySelectorAll('.escolha')).filter(c => c.dataset.categoria !== 'unicode');
const imagensIndisponiveis = new Set(imagens.filter(c => c.disabled));
function atualizarControlesConexao() {
    const bloqueado = conexaoWeb !== 'disponivel';
    enviarBotao.disabled = enviando || bloqueado;
    imagens.forEach(c => { c.disabled = enviando || bloqueado || imagensIndisponiveis.has(c); });
}
atualizarControlesConexao();
for (const evento of ['select', 'keyup', 'click', 'input', 'blur']) texto.addEventListener(evento, () => {
    inicio = texto.selectionStart; fim = texto.selectionEnd; direcao = texto.selectionDirection;
});
function fechar(focar) {
    seletor.hidden = true; abrir.setAttribute('aria-expanded', 'false');
    if (focar) { texto.focus(); texto.setSelectionRange(inicio, fim, direcao); }
}
abrir.addEventListener('click', () => {
    const aberto = seletor.hidden;
    seletor.hidden = !aberto; abrir.setAttribute('aria-expanded', String(aberto));
    if (aberto) abas.find(a => a.getAttribute('aria-selected') === 'true').focus();
    else { texto.focus(); texto.setSelectionRange(inicio, fim, direcao); }
});
document.addEventListener('click', e => { if (!seletor.hidden && !e.target.closest('.seletor-area')) fechar(false); });
document.addEventListener('keydown', e => { if (e.key === 'Escape' && !seletor.hidden) { e.preventDefault(); fechar(true); } });
function selecionarAba(aba) {
    for (const item of abas) {
        const ativa = item === aba;
        item.setAttribute('aria-selected', String(ativa)); item.tabIndex = ativa ? 0 : -1;
        document.getElementById(item.getAttribute('aria-controls')).hidden = !ativa;
    }
}
abas.forEach((aba, i) => {
    aba.addEventListener('click', () => selecionarAba(aba));
    aba.addEventListener('keydown', e => {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) return;
        e.preventDefault();
        const alvo = e.key === 'Home' ? 0 : e.key === 'End' ? abas.length - 1
            : (i + (e.key === 'ArrowRight' ? 1 : -1) + abas.length) % abas.length;
        selecionarAba(abas[alvo]); abas[alvo].focus();
    });
});
async function enviar(id) {
    if (enviando || conexaoWeb !== 'disponivel' || (!id && !texto.value.trim())) return;
    enviando = true; erro.textContent = '';
    const original = texto.value, posInicio = inicio, posFim = fim, posDirecao = direcao;
    const dados = new URLSearchParams({txtmsg: id ? '' : original, recurso: id || '', catalogo: form.elements.catalogo.value});
    const controles = Array.from(form.querySelectorAll('button'), c => [c, c.disabled]);
    controles.forEach(([c]) => { c.disabled = true; });
    if (id) fechar(false);
    try {
        const resposta = await fetch(form.action, {method: 'POST', body: dados, redirect: 'error'});
        if (resposta.status === 401) definirConexaoWeb('sessao');
        else if (resposta.status === 503) definirConexaoWeb('indisponivel');
        if (resposta.status !== 204) throw new Error(await resposta.text());
        if (!id && texto.value === original) { texto.value = ''; inicio = fim = 0; direcao = 'none'; }
    } catch (e) {
        if (e instanceof TypeError && conexaoWeb !== 'sessao') definirConexaoWeb('indisponivel');
        erro.textContent = e instanceof TypeError || !e.message
            ? 'Envio não confirmado. Confira o histórico antes de reenviar.' : e.message;
    } finally {
        enviando = false; controles.forEach(([c, estado]) => { c.disabled = estado; });
        atualizarControlesConexao();
        if (id && texto.value === original) { inicio = posInicio; fim = posFim; direcao = posDirecao; }
        texto.focus(); texto.setSelectionRange(inicio, fim, direcao);
    }
}
document.querySelectorAll('.escolha').forEach(botao => botao.addEventListener('click', () => {
    if (enviando) return;
    const r = botao.dataset;
    if (r.categoria !== 'unicode') { enviar(r.id); return; }
    if (texto.value.length - (fim - inicio) + r.valor.length > texto.maxLength) {
        erro.textContent = 'Limite de 150 caracteres atingido.'; return;
    }
    texto.setRangeText(r.valor, inicio, fim, 'end');
    inicio = fim = texto.selectionEnd; direcao = 'none'; erro.textContent = ''; fechar(true);
}));
form.addEventListener('submit', e => { e.preventDefault(); enviar(''); });
