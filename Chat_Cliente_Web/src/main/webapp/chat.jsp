<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'self'; form-action 'self'");
    // Atualiza a sessão somente quando os dados vêm do login.
    String nickname = request.getParameter("txtNickname");

    if (nickname != null) {
        if (!ChatWeb.perfilValido(nickname, request.getParameter("radCor"), request.getParameter("radAvatar"))) {
            response.sendRedirect("index.jsp");
            return;
        }
        session.setAttribute("nickname", nickname);
        session.setAttribute("cor", request.getParameter("radCor"));
        session.setAttribute("avatar", request.getParameter("radAvatar"));
    }

    // Impede acessar o chat sem uma sessão válida.
    if (!ChatWeb.perfilValido(session.getAttribute("nickname"), session.getAttribute("cor"), session.getAttribute("avatar"))) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<%
    chat.catalogo.Catalogo catalogo = null;
    String erroCatalogo = "";
    try { catalogo = ChatWeb.catalogo(); }
    catch (IllegalStateException e) {
        erroCatalogo = "Catálogo embarcado inválido. Texto disponível.";
        application.log("Falha de integridade no catálogo embarcado", e);
    }
    if (catalogo != null) try { catalogo.conferirPublicacao(application.getInitParameter("urlCatalogoBase")); }
    catch (java.io.IOException e) { erroCatalogo = "Imagens indisponíveis: catálogo ausente ou incompatível."; }
%>
<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Sala de conversa</title><link rel="stylesheet" href="chat.css">
<script defer src="tema.js"></script><script defer src="historico.js"></script><script defer src="compositor.js"></script></head>
<body class="chat-page"><main class="sala">
<header class="sala-cabecalho"><span class="marca" aria-hidden="true">◌</span><div class="sala-titulo"><h1>Sala de conversa</h1><span id="estado" role="status">Carregando histórico…</span></div>
<div class="perfil"><img src="/figurinhas/imagens/<%=ChatWeb.html(session.getAttribute("avatar"))%>" alt=""><span><%=ChatWeb.html(session.getAttribute("nickname"))%></span></div><button class="tema" type="button" aria-label="Alternar tema" title="Alternar tema">◐</button></header>
<section id="rolagem" class="conversa" aria-label="Histórico da conversa"><div class="conversa-interior"><p class="inicio-sala">Sala de conversa</p><div id="historico"></div></div></section>
<button id="novas" class="novas" type="button" hidden>Novas mensagens ↓</button>
<section class="area-compositor"><div id="erro" role="status"><%=ChatWeb.html(erroCatalogo)%></div>
<form id="compositor" method="post" action="mensagem.jsp"><div class="seletor-area">
<button type="button" id="abrir-seletor" class="icone" aria-label="Emojis e figurinhas" aria-expanded="false" aria-controls="seletor">☺</button>
<div id="seletor" class="seletor" hidden><div class="abas" role="tablist" aria-label="Emojis e figurinhas"><button type="button" role="tab" id="aba-emojis" aria-selected="true" aria-controls="emojis">Emojis</button><button type="button" role="tab" id="aba-figurinhas" aria-selected="false" aria-controls="figurinhas" tabindex="-1">Figurinhas</button></div>
<% for (String grupo : new String[]{"emojis", "figurinhas"}) { %>
<div id="<%=grupo%>" role="tabpanel" aria-labelledby="aba-<%=grupo%>" class="catalogo" <%=grupo.equals("figurinhas") ? "hidden" : ""%>>
<% for (var r : catalogo == null ? java.util.List.<chat.catalogo.Catalogo.Recurso>of() : catalogo.recursos()) {
    if (!r.selecionavel() || (grupo.equals("emojis") != (r.unicode() || r.categoria().equals("emoji-gif")))) continue;
%><button type="button" class="escolha" title="<%=ChatWeb.html(r.nome())%>" aria-label="<%=ChatWeb.html(r.nome())%>" data-id="<%=ChatWeb.html(r.id())%>" data-categoria="<%=ChatWeb.html(r.categoria())%>" data-valor="<%=ChatWeb.html(r.caminho())%>" <%=!r.unicode() && !erroCatalogo.isEmpty() ? "disabled" : ""%>><% if (r.unicode()) { %><span><%=ChatWeb.html(r.caminho())%></span><% } else { %><img src="/figurinhas/<%=ChatWeb.html(r.caminho())%>" alt=""><% } %></button><% } %></div><% } %></div></div>
<label class="sr-only" for="texto">Mensagem</label><input id="texto" type="text" name="txtmsg" maxlength="150" autocomplete="off" placeholder="Escreva uma mensagem"><input type="hidden" name="catalogo" value="<%=catalogo == null ? "" : catalogo.assinatura()%>"><button type="submit" id="enviar" class="enviar" aria-label="Enviar mensagem">➤</button></form></section></main></body></html>
