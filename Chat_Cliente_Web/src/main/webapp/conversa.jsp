<%@page import="com.mycompany.chat_cliente_web.RelatorErrosWeb"%>
<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'self'");
    if (!ChatWeb.perfilValido(session.getAttribute("nickname"), session.getAttribute("cor"), session.getAttribute("avatar"))) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta http-equiv="refresh" content="2;url=conversa.jsp">
        <title>Conversa</title>
    </head>
    <body>
        <%
            try {
                out.print(ChatWeb.consultar(application.getInitParameter("urlMiddleware")));
                RelatorErrosWeb.padrao().recuperar("conversa.jsp", "consultarHistorico", request.getRemoteAddr());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                out.print("<b style='color:red'>Consulta interrompida.</b>");
            } catch (java.io.IOException | IllegalArgumentException e) {
                RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "conversa.jsp", "consultarHistorico", request.getRemoteAddr(), e);
                out.print("<b style='color:red'>Serviço Web indisponível.</b>");
            } catch (RuntimeException e) {
                RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "conversa.jsp", "consultarHistorico", request.getRemoteAddr(), e);
                throw e;
            }
        %>
    </body>
</html>
