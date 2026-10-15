<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head><title>Sala de conversa</title></head>
    <body>
        <iframe src="topo.jsp" width="100%" height="100"></iframe>
        <iframe src="conversa.jsp" width="100%" height="480"></iframe>
        <iframe src="mensagem.jsp" width="100%" height="140"></iframe>
        
        <%
            /*out.print("<b>Nickname: </b>" + request.getParameter("txtNickname") + "<br><b> Cor: </b>" +
                    request.getParameter("radCor") + "<br><b> Avatar: </b>" + request.getParameter("radAvatar"));*/
            
            session.setAttribute("nickname", request.getParameter("txtNickname"));
            session.setAttribute("cor", request.getParameter("radCor"));
            session.setAttribute("avatar", request.getParameter("radAvatar"));
            
            /*out.print("<br><br> Nickname sessão: " + session.getAttribute("nickname")+
                    "<br> Cor sessão: " + session.getAttribute("cor")+
                    "<br> Avatar sessão: " + session.getAttribute("avaar"));*/
        %>
    </body>
</html>