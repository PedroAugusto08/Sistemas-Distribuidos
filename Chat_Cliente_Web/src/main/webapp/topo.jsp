<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <!-- <img src="images/menino.png" width="50" height="50">--> 
        
        <%
           /*out.print("Nickname sessão: " + session.getAttribute("nickname")+
                    "<br> Cor sessão: " + session.getAttribute("cor")+
                    "<br> Avatar sessão: " + session.getAttribute("avaar"));*/
          
            /*out.print("<img src=\"images/" + session.getAttribute("avatar") +"\" width=\"50\" + height=\"50\">");*/
        %>
        
        <img src="images/<%=session.getAttribute("avatar")%>" width="50" height="50">
        <font size="+2" color="<%=session.getAttribute("cor")%>">
            <b><%=session.getAttribute("nickname")%></b></font>
        
    </body>
</html>
