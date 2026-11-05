<%@page import="com.mycompany.chat_cliente_web.ChatWeb"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    if (!ChatWeb.perfilValido(session.getAttribute("nickname"), session.getAttribute("cor"), session.getAttribute("avatar"))) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    
    
    <body>
        
        <img src="/figurinhas/imagens/<%=ChatWeb.html(session.getAttribute("avatar"))%>" width="50" height="50">
        <font size="+2" color="<%=ChatWeb.html(session.getAttribute("cor"))%>">
            <b><%=ChatWeb.html(session.getAttribute("nickname"))%></b></font>
        
    </body>
</html>
