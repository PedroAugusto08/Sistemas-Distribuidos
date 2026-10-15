<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <title>Chat Distribuído Web 1.0</title>
    </head>

    <body>
        <br><br><br>
        <center>
            <form name="frmLogin" action="chat.jsp" method="post">
                <table border="1">
                    <tr>
                        <td colspan="2" bgcolor="crimson" align="center">
                            <font color="white" size="+2">Chat Distribuído 1.0</font>
                        </td>
                    </tr>
                    <tr>
                        <td align="right"><b>Nickname:</b></td>
                        <td>
                            <input type="text" name="txtNickname" maxlength="20" size="40">
                        </td>
                    </tr>
                    <tr>
                        <td align="right"><b>Cor:</b></td>
                        <td>
                            <input type="radio" name="radCor" value="navy" checked>
                            <font color="navy"><b>Azul</b></font>
                            
                            <input type="radio" name="radCor" value="tomato">
                            <font color="tomato"><b>Vermelho</b></font>
            
                            <input type="radio" name="radCor" value="black">
                            <font color="black"><b>Preto</b></font>
                        </td>
                    </tr>
                    <tr>
                        <td align="right"><b>Avatar: </b></td>
                        <td>
                            <input type="radio" name="radAvatar" value="menino.png" checked>
                            <img src="images/menino.png" width="30" height="30">
                 
                            <input type="radio" name="radAvatar" value="menina.png">
                            <img src="images/menina.png" width="30" height="30">

                            <input type="radio" name="radAvatar" value="avatar.png">
                            <img src="images/avatar.png" width="30" height="30">
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" align="center">
                            <input type="submit" name="btnEntrar" value="Entrar"> 
                        </td>
                    </tr>
                </table>
            </form>
        </center>
    </body>
</html>