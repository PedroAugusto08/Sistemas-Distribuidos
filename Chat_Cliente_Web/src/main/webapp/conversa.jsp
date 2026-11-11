<%@page import="com.mycompany.chat_cliente_web.RelatorErrosWeb"%>
<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; script-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'self'");
    if (!ChatWeb.perfilValido(session.getAttribute("nickname"), session.getAttribute("cor"), session.getAttribute("avatar"))) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<%
    if ("1".equals(request.getParameter("fragmento"))) {
        response.setContentType("text/plain; charset=UTF-8");
        // ASCII no cabeçalho, preservando apelidos Unicode e sem interpolar JavaScript.
        response.setHeader("X-Chat-Nickname", java.util.Base64.getEncoder().encodeToString(
                ((String) session.getAttribute("nickname")).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        try {
            out.print(ChatWeb.consultar(application.getInitParameter("urlMiddleware")));
            RelatorErrosWeb.padrao().recuperar("conversa.jsp", "consultarHistorico", request.getRemoteAddr());
        }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); response.setStatus(503); }
        catch (java.io.IOException | IllegalArgumentException e) {
            RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "conversa.jsp", "consultarHistorico", request.getRemoteAddr(), e);
            response.setStatus(503);
        } catch (RuntimeException e) {
            RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "conversa.jsp", "consultarHistorico", request.getRemoteAddr(), e);
            throw e;
        }
        return;
    }
%>
<% response.sendRedirect("chat.jsp"); %>
