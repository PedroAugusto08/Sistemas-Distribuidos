<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("UTF-8");
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
<!DOCTYPE html>
<html>
    <head>
        <title>Sala de conversa</title>
    </head>
    <body>
        <iframe src="topo.jsp" width="100%" height="100"></iframe>
        <iframe src="conversa.jsp" width="100%" height="480"></iframe>
        <iframe src="mensagem.jsp" width="100%" height="360"></iframe>
    </body>
</html>
