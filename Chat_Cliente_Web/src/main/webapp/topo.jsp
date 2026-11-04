<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    
    <%
        String urlFigurinhas = application.getInitParameter("urlFigurinhas");
    %>
    
    <body>
        
        <img src="<%=urlFigurinhas%>/imagens/<%=session.getAttribute("avatar")%>" width="50" height="50">
        <font size="+2" color="<%=session.getAttribute("cor")%>">
            <b><%=session.getAttribute("nickname")%></b></font>
        
    </body>
</html>
