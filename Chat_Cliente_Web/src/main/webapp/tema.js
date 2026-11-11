'use strict';
const botaoTema = document.querySelector('.tema');
try { if (localStorage.getItem('chat-tema') === 'claro') document.documentElement.dataset.tema = 'claro'; } catch (_) {}
botaoTema.addEventListener('click', () => {
    const claro = document.documentElement.dataset.tema !== 'claro';
    document.documentElement.dataset.tema = claro ? 'claro' : 'escuro';
    try { localStorage.setItem('chat-tema', claro ? 'claro' : 'escuro'); } catch (_) {}
});
