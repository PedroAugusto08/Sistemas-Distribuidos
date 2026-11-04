# Histórico do Middleware

O projeto usa Java 21 e não precisa de dependências adicionais. Execute a classe
`pacote.run` no NetBeans. O `exec.mainClass` já existente no POM aponta para um
nome de classe inexistente; ele não foi alterado nesta tarefa. A classe
`pacote.Chat_Middleware_Service` contém somente o exemplo “Hello World”.

## Funcionamento

`run` inicializa o repositório antes de criar o menu. O construtor do painel
também garante essa inicialização quando ele é executado diretamente. `Util`
mantém uma única instância: chamadas seguintes não recarregam o arquivo.
Se o carregamento UTF-8 falhar, um erro é apresentado e os serviços não são
disponibilizados. A pasta e o TXT são criados se ainda não existirem, sem
substituir um histórico existente.

`RepositorioMensagens` mantém um `ArrayList<String>`. Seus métodos sincronizados
serializam gravações e consultas: primeiro o acréscimo UTF-8 é escrito, confirmado
com `force(true)` e fechado; depois as linhas entram na RAM. Uma consulta devolve
uma cópia a partir do índice solicitado. Isso mantém a divisão por linhas que
o envio antigo já usava, inclusive para mensagens com quebras de linha.

Se uma escrita falhar, a mensagem não entra na RAM. O repositório tenta restaurar
o tamanho anterior, removendo somente o acréscimo incompleto. Se essa recuperação
falhar, novas gravações ficam bloqueadas e o erro de recuperação acompanha a
exceção original. A recepção registra a falha de persistência no console e continua
aguardando outras mensagens, sem encerrar o socket servidor. Essa proteção trata
falhas de E/S reportadas pelo Java; não é uma transação contra interrupção abrupta
do processo ou falta de energia durante o acréscimo.

Cada conexão de envio começa no índice zero e mantém seu próprio contador.
A cada 200 ms consulta apenas a RAM, enviando o recorte ainda não transmitido.
As escritas na rede ocorrem fora do bloqueio do repositório. Desativar o serviço
mantém a instância e o histórico, envia o aviso de manutenção existente e fecha
as conexões. A reconexão recebe novamente o histórico completo.

O TXT deve ser usado exclusivamente por esta instância do Middleware enquanto
ela estiver ativa. Edições externas não são recarregadas. Um TXT antigo que não
seja UTF-8 gera erro de carregamento; não há conversão automática dos dados.

## Validação no Windows, sem usar o histórico real

No WSL desta tarefa, `java`, `javac` e `mvn` não estão no PATH. A compilação e
os testes de execução precisam ser realizados com o JDK 21 no Windows.

1. No NetBeans, selecione o JDK 21 e use **Build**, sem **Clean**. Com Maven
   disponível, a alternativa é `mvn -f Chat_Middleware_Service/pom.xml compile`.
2. Para testar sem acessar `C:\Dados\RepositorioDesktop.txt`, use uma sessão
   JShell do JDK 21 com `--class-path Chat_Middleware_Service/target/classes`.
   Crie um histórico fictício assim:

   ```java
   import java.nio.file.*;
   import java.nio.charset.*;
   import pacote.*;
   Path pasta = Files.createTempDirectory("chat-historico-");
   Path arquivo = pasta.resolve("historico.txt");
   Files.writeString(arquivo, "<b>Histórico: olá</b><br>", StandardCharsets.UTF_8);
   Util.PathRepDesktop = arquivo.toString();
   Util.inicializarRepositorio();
   var repositorio = Util.obterRepositorio();
   repositorio.consultarMensagens(0);
   Util.inicializarRepositorio();
   System.out.println(repositorio == Util.obterRepositorio()); // true
   repositorio.adicionarMensagem("Nova mensagem<br>");
   Files.readAllLines(arquivo, StandardCharsets.UTF_8);
   repositorio.consultarMensagens(1); // somente a nova mensagem
   var copia = repositorio.consultarMensagens(0);
   copia.clear();
   repositorio.consultarMensagens(0); // histórico intacto
   ```

3. Na mesma sessão, simule falha de persistência **somente no arquivo temporário**:

   ```java
   Path salvo = pasta.resolve("salvo.txt");
   Files.move(arquivo, salvo);
   try { repositorio.adicionarMensagem("Não deve entrar na RAM"); }
   catch (java.io.IOException e) { System.out.println("Falha esperada: " + e); }
   repositorio.consultarMensagens(0); // continua com duas mensagens
   Files.move(salvo, arquivo);
   ```

4. Ainda nessa sessão, execute `new menuServidor();` e ative o serviço Desktop
   no painel. Não execute simultaneamente outro Middleware nas mesmas portas.
   Conecte dois clientes Desktop locais: ambos devem receber as duas mensagens.
   Envie mensagens pelos dois e confira ordem, ausência de duplicações e o TXT
   temporário. Um terceiro cliente deve receber todo o histórico. O cliente Web
   local também deve exibir o histórico ao abrir `conversa.jsp`.
5. Desative o serviço pelo painel: confira o aviso de manutenção. Reative:
   os clientes Desktop devem reconectar e receber o histórico uma única vez na
   conversa reconstruída. A instância retornada por `Util.obterRepositorio()`
   deve continuar sendo a mesma.
6. Para comprovar que o envio não relê o TXT, com os clientes conectados renomeie
   temporariamente **apenas o TXT fictício** como no passo 3 e abra outro cliente:
   o histórico deve chegar pela RAM. Restaure o nome antes de novos envios.
7. Encerre essa sessão. Para testar erro de inicialização, em uma nova sessão
   atribua a `Util.PathRepDesktop` o caminho de um diretório temporário e execute
   `pacote.run.main(new String[0])`: deve aparecer erro, sem abrir os serviços.
   Para testar criação, use outra sessão com um caminho temporário inexistente.

Esses são procedimentos de validação pendentes; não foram executados no WSL.
Não provoque falha de disco, altere permissões ou renomeie o histórico real para
testar o caminho de recuperação de escrita parcial.
