<%@page import="com.mycompany.chat_cliente_web.RelatorErrosWeb"%>
<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    if (!ChatWeb.perfilValido(session.getAttribute("nickname"), session.getAttribute("cor"), session.getAttribute("avatar"))) {
        response.setStatus(401);
        out.print("Sessão expirada. Entre novamente.");
        return;
    }
    chat.catalogo.Catalogo catalogo = null;
    String erroCatalogo = "";
    try { catalogo = ChatWeb.catalogo(); }
    catch (IllegalStateException e) {
        erroCatalogo = "Catálogo embarcado inválido: reinstale a distribuição. Texto disponível.";
        application.log("Falha de integridade no catálogo embarcado", e);
    }
    String recursoSolicitado = "POST".equals(request.getMethod()) ? request.getParameter("recurso") : null;
    if (catalogo != null && (!"POST".equals(request.getMethod()) || recursoSolicitado != null && !recursoSolicitado.isEmpty())) {
        try { catalogo.conferirPublicacao(application.getInitParameter("urlCatalogoBase")); }
        catch (java.io.IOException e) { erroCatalogo = "Imagens indisponíveis: catálogo ausente ou incompatível."; }
    }
    if ("POST".equals(request.getMethod())) {
        response.setContentType("text/plain; charset=UTF-8");
        boolean mensagemValidada = false;
        try {
            String id = recursoSolicitado;
            if (id != null && !id.isEmpty() && (catalogo == null || !catalogo.assinatura().equals(request.getParameter("catalogo"))))
                throw new IllegalArgumentException("Catálogo alterado. Reabra o compositor.");
            if (id == null || !id.isEmpty() && (!erroCatalogo.isEmpty() || catalogo.selecionar(id).unicode()))
                throw new IllegalArgumentException("Recurso indisponível.");
            String mensagem = ChatWeb.montarMensagem(session.getAttribute("nickname"), session.getAttribute("cor"),
                    session.getAttribute("avatar"), request.getParameter("txtmsg"), id);
            mensagemValidada = true;
            ChatWeb.enviar(application.getInitParameter("urlMiddleware"), mensagem);
            RelatorErrosWeb.padrao().recuperar("mensagem.jsp", "enviarMensagem", request.getRemoteAddr());
            response.setStatus(204);
        } catch (IllegalArgumentException e) {
            if (mensagemValidada) {
                RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                        "mensagem.jsp", "enviarMensagem", request.getRemoteAddr(), e);
                response.setStatus(503); out.print("Serviço Web indisponível. Rascunho preservado.");
            } else {
                response.setStatus(400); out.print("Mensagem ou catálogo inválido. Rascunho preservado.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); response.setStatus(503);
            out.print("Envio interrompido. Confira o histórico antes de reenviar.");
        } catch (java.io.IOException e) {
            RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "mensagem.jsp", "enviarMensagem", request.getRemoteAddr(), e);
            response.setStatus(503); out.print("Envio não confirmado. Confira o histórico antes de reenviar.");
        } catch (RuntimeException e) {
            RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "mensagem.jsp", "enviarMensagem", request.getRemoteAddr(), e);
            throw e;
        }
        return;
    }
%>
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>Mensagem</title>
<style>
body {font-family:sans-serif;margin:8px} fieldset {display:inline-block;vertical-align:top;max-width:340px}
.catalogo {max-height:140px;overflow:auto;display:flex;flex-wrap:wrap;gap:4px}
button {cursor:pointer;padding:6px} button[aria-pressed=true] {outline:2px solid navy}
#previa {min-height:40px;display:flex;gap:10px;align-items:center} #previa img {object-fit:contain}
#previa[hidden] {display:none}
#erro {color:#a00000} input[type=text] {width:65%;max-width:650px}
</style></head><body>
<div id="erro" role="status"><%=ChatWeb.html(erroCatalogo)%></div>
<form id="compositor" method="post" action="mensagem.jsp">
<label for="texto">Mensagem:</label> <input id="texto" type="text" name="txtmsg" maxlength="150" autocomplete="off">
<input type="hidden" name="recurso" id="recurso" value="">
<input type="hidden" name="catalogo" value="<%=catalogo == null ? "" : catalogo.assinatura()%>">
<button type="submit" id="enviar">Enviar</button>
<div id="previa" hidden><img id="imagemPrevia" alt="" hidden><span id="nomePrevia"></span>
<button type="button" id="remover">Remover imagem</button></div>
<%
    String[] categorias = {"unicode", "emoji-gif", "figurinha-png", "figurinha-gif"};
    String[] nomes = {"Unicode", "Emojis GIF", "Figurinhas PNG", "Figurinhas GIF"};
    for (int i=0; i<categorias.length; i++) {
%><fieldset><legend><%=nomes[i]%></legend><div class="catalogo">
<% for (var r : catalogo == null ? java.util.List.<chat.catalogo.Catalogo.Recurso>of() : catalogo.recursos()) {
    if (!r.selecionavel() || !r.categoria().equals(categorias[i])) continue;
%><button type="button" class="escolha" aria-pressed="false" title="<%=ChatWeb.html(r.nome())%>"
 data-id="<%=r.id()%>" data-categoria="<%=r.categoria()%>" data-nome="<%=ChatWeb.html(r.nome())%>"
 data-valor="<%=ChatWeb.html(r.caminho())%>" data-largura="<%=r.larguraExibicao()%>" data-altura="<%=r.alturaExibicao()%>"
 <%=!r.unicode() && !erroCatalogo.isEmpty() ? "disabled" : ""%>><%=ChatWeb.html(r.nome())%></button>
<% } %></div></fieldset><% } %>
</form><script src="compositor.js"></script></body></html>
