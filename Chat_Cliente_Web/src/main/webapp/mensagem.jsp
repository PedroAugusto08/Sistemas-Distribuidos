<%@page import="java.io.ObjectOutputStream"%>
<%@page import="java.net.Socket"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <%
            
            // Recursos visuais compartilhados via servidor Web, sem cópias por cliente.
            String urlFigurinhas = application.getInitParameter("urlFigurinhas");
            
            if (request.getParameter("btnEnviar") != null) {
                String msg = "";
                msg += "<img src='" + urlFigurinhas + "/imagens/" + session.getAttribute("avatar") + "' width='30' height='30'>";

                msg += "<font size=\"+1\" color=\"" + session.getAttribute("cor") + "\"><b>" + session.getAttribute("nickname")
                        + "</b></font>";

                msg += "Diz: " + request.getParameter("txtmsg");

                String emoji = request.getParameter("cbEmoji");

                if(emoji != null && !emoji.isBlank()){
                    msg += "<img src='" + urlFigurinhas + "/" + emoji + "' width='30' height='30'>";
                }

                msg += "<br>";

                Socket cliente = new Socket("127.0.0.1", 6662);
                ObjectOutputStream writer = new ObjectOutputStream(cliente.getOutputStream());

                writer.flush();
                writer.writeUTF(msg);
                writer.close();
                cliente.close();
            }

        %>

        <form name="formMensagem" method="post" action="mensagem.jsp">
            <label for="txtmsg">Mensagem: </label>
            <input type="text" name="txtmsg" size="200" maxlength="150">

            <label for="cbEmoji">Emoji:</label>
            <select name="cbEmoji" id="cbEmoji">
                <option value="">Selecione</option>
                <option value="emojis/coracao.gif">&#129505;</option>
                <option value="emojis/risada.gif">&#128514;</option>
                <option value="emojis/choro.gif">&#128557;</option>
            </select>
            <input type="submit" name="btnEnviar" value="Enviar">
        </form>
    </body>
</html>
