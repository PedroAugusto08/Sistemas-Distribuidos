'use strict';
let anterior = '';
async function atualizar() {
    try {
        const resposta = await fetch('conversa.jsp?fragmento=1', {cache:'no-store', redirect:'error'});
        if (!resposta.ok) throw new Error('Histórico indisponível. Entre novamente se a sessão expirou.');
        const html = await resposta.text();
        const historico = document.getElementById('historico');
        // HTML recebido passa pela allowlist MensagemHtml no Middleware; não aceita HTML do formulário.
        // https://developer.mozilla.org/en-US/docs/Web/API/Element/insertAdjacentHTML
        // Preserva nós IMG já existentes: consultar o histórico não reinicia seus GIFs.
        if (html.startsWith(anterior)) historico.insertAdjacentHTML('beforeend', html.slice(anterior.length));
        else historico.innerHTML = html;
        anterior = html; document.getElementById('estado').textContent = '';
    } catch (e) { document.getElementById('estado').textContent = 'Histórico indisponível. Tentando novamente.'; }
    setTimeout(atualizar, 2000);
}
atualizar();
