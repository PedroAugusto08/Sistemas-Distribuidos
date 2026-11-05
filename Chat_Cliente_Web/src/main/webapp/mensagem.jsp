<%@page import="com.mycompany.chat_cliente_web.RelatorErrosWeb"%>
<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    if (!ChatWeb.perfilValido(session.getAttribute("nickname"), session.getAttribute("cor"), session.getAttribute("avatar"))) {
        response.sendRedirect("index.jsp");
        return;
    }
    String erro = "";
    String texto = "";
    String emoji = "";
    if ("POST".equals(request.getMethod()) && request.getParameter("btnEnviar") != null) {
        texto = request.getParameter("txtmsg");
        emoji = request.getParameter("cbEmoji");
        boolean mensagemValidada = false;
        try {
            String mensagem = ChatWeb.montarMensagem(session.getAttribute("nickname"), session.getAttribute("cor"),
                    session.getAttribute("avatar"), texto, emoji);
            mensagemValidada = true;
            ChatWeb.enviar(application.getInitParameter("urlMiddleware"), mensagem);
            RelatorErrosWeb.padrao().recuperar("mensagem.jsp", "enviarMensagem", request.getRemoteAddr());
            // Redireciona após sucesso: atualizar o iframe não repete o POST.
            response.sendRedirect("mensagem.jsp");
            return;
        } catch (IllegalArgumentException e) {
            if (mensagemValidada) {
                RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                        "mensagem.jsp", "enviarMensagem", request.getRemoteAddr(), e);
                erro = "Serviço Web indisponível.";
            } else {
                erro = "Mensagem ou perfil inválido.";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            erro = "Envio interrompido. A confirmação não chegou; confira a conversa antes de reenviar.";
        } catch (java.io.IOException e) {
            RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "mensagem.jsp", "enviarMensagem", request.getRemoteAddr(), e);
            erro = "Envio não confirmado. Confira a conversa antes de reenviar; o serviço pode estar indisponível.";
        } catch (RuntimeException e) {
            RelatorErrosWeb.padrao().registrar(application.getInitParameter("urlColetorErros"),
                    "mensagem.jsp", "enviarMensagem", request.getRemoteAddr(), e);
            throw e;
        }
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Mensagem</title>
    </head>
    <body>
        <b style="color:red"><%=ChatWeb.html(erro)%></b>
        <form name="formMensagem" method="post" action="mensagem.jsp">
            <label for="txtmsg">Mensagem: </label>
            <input type="text" name="txtmsg" size="200" maxlength="150" value="<%=ChatWeb.html(texto)%>">

            <label for="cbEmoji">Emoji:</label>
            <select name="cbEmoji" id="cbEmoji">
                <option value="" <%= "".equals(emoji) ? "selected" : "" %>>Selecione</option>
                <option value="emojis/coracao.gif" <%= "emojis/coracao.gif".equals(emoji) ? "selected" : "" %>>&#129505;</option>
                <option value="emojis/risada.gif" <%= "emojis/risada.gif".equals(emoji) ? "selected" : "" %>>&#128514;</option>
                <option value="emojis/choro.gif" <%= "emojis/choro.gif".equals(emoji) ? "selected" : "" %>>&#128557;</option>
            </select>
            <input type="submit" name="btnEnviar" value="Enviar">
        </form>
    </body>
</html>
