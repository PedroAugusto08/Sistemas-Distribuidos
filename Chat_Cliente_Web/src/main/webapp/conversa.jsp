<%@page import="java.io.ObjectInputStream"%>
<%@page import="java.io.EOFException"%>
<%@page import="java.net.Socket"%>
<%@page import="java.net.SocketTimeoutException"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta http-equiv="refresh" content="5;url=conversa.jsp">
        <title>Conversa</title>
    </head>
    <body>
        <%
            try(Socket cliente = new Socket("127.0.0.1", 6661)){
                cliente.setSoTimeout(1000);

                try(ObjectInputStream reader = new ObjectInputStream(cliente.getInputStream())){
                    while(true){
                        try{
                            String msg = reader.readUTF();
                            out.print(msg);
                        }catch(SocketTimeoutException | EOFException e){
                            break;
                        }
                    }
                }

            }catch(Exception e){
                out.print("<b style='color:red'>Servidor indisponível.</b>");
                e.printStackTrace();
            }
        %>
    </body>
</html>