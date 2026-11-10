'use strict';
const form = document.getElementById('compositor');
const texto = document.getElementById('texto');
const recurso = document.getElementById('recurso');
const previa = document.getElementById('previa');
const imagem = document.getElementById('imagemPrevia');
const nome = document.getElementById('nomePrevia');
const erro = document.getElementById('erro');
let selecionado = null, carregando = false, enviando = false;
let inicio = 0, fim = 0;
for (const evento of ['select', 'keyup', 'click', 'input', 'blur']) texto.addEventListener(evento, () => {
    inicio = texto.selectionStart; fim = texto.selectionEnd;
});
function remover() {
    selecionado = null; recurso.value = ''; carregando = false;
    imagem.removeAttribute('src'); imagem.hidden = true; previa.hidden = true;
    document.querySelectorAll('.escolha').forEach(b => b.setAttribute('aria-pressed', 'false'));
}
document.getElementById('remover').addEventListener('click', remover);
document.querySelectorAll('.escolha').forEach(botao => botao.addEventListener('click', () => {
    if (enviando) return;
    const r = botao.dataset;
    if (r.categoria === 'unicode') {
        if (texto.value.length - (fim-inicio) + r.valor.length > texto.maxLength) {
            erro.textContent = 'Limite de 150 caracteres atingido.'; return;
        }
        // https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement/setRangeText
        texto.setRangeText(r.valor, inicio, fim, 'end');
        inicio = fim = texto.selectionEnd; texto.focus(); return;
    }
    remover(); selecionado = r; recurso.value = r.id; carregando = true;
    botao.setAttribute('aria-pressed', 'true'); previa.hidden = false;
    nome.textContent = r.nome + (r.categoria.startsWith('figurinha-') ? ' — envio independente; rascunho preservado' : '');
    imagem.width = Number(r.largura); imagem.height = Number(r.altura); imagem.alt = r.nome;
    imagem.onload = () => { carregando = false; imagem.hidden = false; };
    imagem.onerror = () => { remover(); erro.textContent = 'Imagem indisponível. Selecione novamente.'; };
    imagem.src = '/figurinhas/' + r.valor;
}));
form.addEventListener('submit', async evento => {
    evento.preventDefault();
    if (enviando || carregando || (!texto.value.trim() && !selecionado)) return;
    enviando = true; erro.textContent = '';
    const original = texto.value;
    const figurinha = selecionado && selecionado.categoria.startsWith('figurinha-');
    const dados = new URLSearchParams(new FormData(form));
    const controles = Array.from(form.querySelectorAll('button,input'), c => [c, c.disabled]);
    controles.forEach(([c]) => c.disabled = true);
    try {
        // Um POST por ação explícita; falhas mantêm o estado e não são repetidas.
        // https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
        const resposta = await fetch(form.action, {method:'POST', body:dados, redirect:'error'});
        if (resposta.status !== 204) throw new Error(await resposta.text());
        if (!figurinha && texto.value === original) texto.value = '';
        remover(); inicio = fim = texto.value.length;
    } catch (e) {
        erro.textContent = e.message || 'Envio não confirmado. Confira o histórico antes de reenviar.';
    } finally {
        enviando = false;
        controles.forEach(([c, desabilitado]) => c.disabled = desabilitado);
        texto.focus();
    }
});
