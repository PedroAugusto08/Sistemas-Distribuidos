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
          if(request.getParameter("btnEnviar")!=null){
              String msg = "";
              msg += "<img src=\"images/" + session.getAttribute("avatar") + "\" width=\"30\" height=\"30\">";
              
              msg += "<font size=\"+1\" color=\"" + session.getAttribute("cor") + "\"><b>" + session.getAttribute("nickname") +
                      "</b></font>";
              
              msg += "Diz: " + request.getParameter("txtmsg");
              
              if(!request.getParameter("cbEmoji").equals("")){
                  msg += "<img src =\"" + request.getParameter("cbEmoji") + "\" width=\"30\" height=\"30\">";
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
            <label for="txtmsg" mensagem: </label>
            <input type="text" name="txtmsg" size="200" maxlength="150"]
                   <label for="cbemoji"> Emoji:</label>
            <select name="cbEmoji">
                <option value="">Selecione</option>
                <option value="/images/coracao.png">&#129505;</option>
                <option value="/images/risada.png">&#128514;</option>
                <option value="/images/choro.png">&#128557;</option>
                <option value="/images/joia.png">&#128077;</option>  
            </select>
            <input type="submit" name="btnEnviar" value="Enviar">
        </form>
    </body>
</html>
