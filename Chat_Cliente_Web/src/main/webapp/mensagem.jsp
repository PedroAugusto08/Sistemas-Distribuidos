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
<% response.sendRedirect("chat.jsp"); %>
