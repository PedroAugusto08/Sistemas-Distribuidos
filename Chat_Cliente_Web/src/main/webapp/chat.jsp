<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    // Atualiza a sessão somente quando os dados vêm do login.
    String nickname = request.getParameter("txtNickname");

    if (nickname != null && !nickname.isBlank()) {
        session.setAttribute("nickname", nickname);
        session.setAttribute("cor", request.getParameter("radCor"));
        session.setAttribute("avatar", request.getParameter("radAvatar"));
    }

    // Impede acessar o chat sem uma sessão válida.
    if (session.getAttribute("nickname") == null
            || session.getAttribute("avatar") == null) {
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
        <iframe src="mensagem.jsp" width="100%" height="140"></iframe>
    </body>
</html>
