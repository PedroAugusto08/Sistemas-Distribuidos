# Serviços de comunicação do chat

Execute `pacote.run` no NetBeans com JDK 21 ou superior. O painel também pode
ser executado diretamente. O `exec.mainClass` antigo dos POMs Desktop/Middleware
aponta para classes inexistentes; esta implementação não altera esses POMs.

## Arquitetura e configuração

Um único processo Middleware hospeda dois serviços independentes:

- Desktop: TCP 6662 recebe uma mensagem por conexão com `ObjectInputStream.readUTF()`;
  TCP 6661 transmite o histórico e as novidades com `ObjectOutputStream.writeUTF()`.
- Web: HTTP 8081, com GET e POST em `/mensagens`. Os JSPs fazem essas chamadas
  no servidor GlassFish; o navegador continua acessando o GlassFish em 8080.
- GlassFish continua hospedando JSPs e `/figurinhas`, inclusive quando o serviço
  Web do Middleware está desligado. Encerrar o GlassFish impede novos acessos às
  imagens também no Desktop, mas não encerra os sockets TCP do Middleware.

O bind HTTP padrão é `127.0.0.1`, adequado quando GlassFish e Middleware estão
na mesma máquina. São configuráveis pelas opções de VM do Middleware:

```text
-Dchat.web.bind=127.0.0.1 -Dchat.web.port=8081
```

No `Chat_Cliente_Web/src/main/webapp/WEB-INF/web.xml`, `urlMiddleware` contém
`http://127.0.0.1:8081/mensagens`. Se os processos estiverem em máquinas distintas,
configure um endereço específico acessível ao GlassFish e atualize esse parâmetro.
A API não tem autenticação: não deve ser exposta à Internet. Não há CORS habilitado;
requisições que trazem `Origin` são recusadas. Isso não substitui autenticação.

Não são necessárias dependências adicionais. O HTTP usa o módulo `jdk.httpserver`
do JDK completo; uma imagem Java reduzida precisa incluir esse módulo.

## Histórico compartilhado

`Util.inicializarRepositorio()` cria uma única instância de `RepositorioMensagens`
antes de disponibilizar os serviços. Alternar Desktop/Web não recria o repositório
nem relê o TXT. Os dois serviços recebem a mesma instância.

O repositório mantém `ArrayList<String>` com operações sincronizadas. Primeiro
escreve o acréscimo em UTF-8 e executa `force(true)`; somente depois atualiza a RAM.
Consultas devolvem cópias; escritas de rede ficam fora do bloqueio. Falhas de E/S
não acrescentam a mensagem à RAM. Se desfazer um acréscimo incompleto também
falhar, novas gravações ficam bloqueadas, conforme o comportamento anterior.

O formato do TXT e a divisão por linhas são preservados. O arquivo pertence
exclusivamente a esta instância do Middleware; edições externas não são recarregadas.
Não há garantia transacional contra falta de energia/interrupção abrupta.

## Concorrência e encerramento

`ServicoDesktop` coordena as duas portas: ambas são abertas antes de iniciar os
listeners; falha em qualquer uma libera a outra. Cada conexão é atendida em
thread virtual. Há limite de 128 conexões por listener. A recepção permite até
3 segundos de inatividade e 5 segundos totais para cabeçalho e mensagem.
Uma falha de cliente/persistência não encerra o listener.

O envio mantém índice por cliente e consulta a RAM a cada 200 ms. Um monitor
fecha escritas bloqueadas por mais de 5 segundos. Na parada, o aviso de manutenção
é melhor esforço, com espera total de até 500 ms; depois os sockets são fechados.
O Desktop tenta reconectar a cada segundo também em EOF/erros inesperados,
reconstruindo a conversa a partir do histórico completo. Envios Desktop não têm
ACK no protocolo existente: fechar o stream não confirma persistência.

O HTTP usa executor próprio de threads virtuais. O provedor recebe padrões de
5 segundos para requisição e resposta, 128 conexões e 8 KiB de cabeçalhos;
propriedades `-Dsun.net.httpserver.*`/`-Djdk.httpserver.*` explícitas têm precedência.
Esses limites são definidos antes da primeira criação de HttpServer na JVM.
Os valores `maxReqTime=5` e `maxRspTime=5` são em segundos na implementação
OpenJDK 21 (`ServerImpl.getTimeMillis`), apesar da documentação do módulo
mencionar milissegundos. Referência: [fonte oficial do OpenJDK 21](https://github.com/openjdk/jdk21u/blob/master/src/jdk.httpserver/share/classes/sun/net/httpserver/ServerImpl.java).
Os timers do provedor têm granularidade própria, portanto o fechamento pode
ocorrer depois do valor nominal. A parada HTTP concede até 1 segundo aos
atendimentos e fecha o listener; cada reativação cria outro HttpServer/executor.

O painel usa SwingWorker para iniciar/encerrar, desabilita o respectivo botão
durante a transição e só apresenta ativo após o bind/start bem-sucedido. Falha
inesperada de listener TCP encerra o par e atualiza o painel. Serviços distintos
não compartilham flags de parada nem executores. Callbacks antigos são ignorados
após troca de geração. Fechar o painel apenas o oculta/descarta, como antes;
não equivale a desligar os serviços.

Na recepção Desktop e no HTTP, fechar a rede é seguido de `shutdown()` e espera
por tarefas limitada a 3 segundos, sem `shutdownNow()` nem cancelamento dos
workers de persistência. Interromper esses workers poderia fechar o FileChannel
por interrupção e também impedir o rollback. Uma gravação em andamento pode
terminar depois da parada; sua thread continua até concluir, preservando a ordem
persistência → RAM. A reativação usa novos listeners/executores e a mesma instância
do repositório; não precisa aguardar a gravação antiga para reabrir as portas.
O envio Desktop conserva interrupção das suas tarefas, pois elas não persistem.
Uma operação de disco travada ainda pode reter o bloqueio compartilhado do
repositório e atrasar outras operações; não existe cancelamento seguro de `force(true)`. Os serviços compartilham a JVM, o disco e o bloqueio do repositório:
a independência é de comunicação/controle, não isolamento de falhas de processo.
No Java 21, E/S dentro dos métodos sincronizados do repositório pode reter uma
carrier thread; não se alterou a persistência já validada para otimizar esse caso.

## API HTTP e tratamento de entradas

- `GET /mensagens`: 200 com fragmento `text/html; charset=UTF-8`, histórico completo
  e `Cache-Control: no-store`. Nenhum cursor/sessão fica no Middleware.
- `POST /mensagens`: exige `Content-Type: text/plain; charset=UTF-8`, contendo o
  HTML montado pela aplicação. Responde 204 apenas depois de persistir e atualizar RAM.
- 400: vazio, UTF-8 malformado, controle inválido, HTML não permitido ou tamanho
  incompatível com `writeUTF` (UTF modificado, limite 65535 bytes).
- 413: corpo maior que 65535 bytes, inclusive com transferência chunked.
- 415: tipo/charset não suportado; 405: método inválido, com `Allow`; 404: caminho/query
  não suportado; 403: presença de `Origin`; 500: falha de persistência.
- Serviço parado: conexão recusada/encerrada, sem promessa de resposta 503.

`MensagemHtml` aceita somente as tags/atributos e imagens usados atualmente pelo
chat. Clientes escapam texto e nickname antes de gerar HTML; cores, avatares e
emojis Web são validados por listas. O mesmo filtro protege a exibição do
histórico legado sem modificar os dados gravados. HTML fora desse vocabulário
passa a aparecer como texto; ao adicionar novos recursos visuais, atualize a lista.
A filtragem usa um único Matcher sobre o texto original, reconhecendo tags na
posição corrente, sem buscar repetidamente `>` nem criar substrings de sufixos.
O custo é linear no tamanho da entrada, inclusive em sequências extensas de `<`.

### Limitação conhecida: mensagem histórica grande no Desktop

Antes de cada `writeUTF`, mede-se a representação já filtrada em UTF-8 modificado:
NUL ocupa 2 bytes e um emoji suplementar ocupa 6. O prefixo de tamanho não entra
no limite de 65.535 bytes. Se uma linha histórica exceder esse limite, o Desktop
recebe, nessa posição, um aviso explícito de limite de exibição. O contador avança
e as mensagens seguintes continuam na mesma conexão. O console registra o índice
base zero e o tamanho em bytes, sem imprimir o conteúdo da mensagem.

A mensagem original fica integralmente na RAM e no TXT. O serviço Web continua
retornando o conteúdo completo, sujeito ao filtro de segurança; nem o aviso nem
a versão filtrada substituem os dados armazenados. Isso é proteção para entradas
históricas excepcionais, não suporte a mensagens arbitrariamente grandes: não
há fragmentação/remontagem, mudança de protocolo nem relaxamento dos limites de
entrada. Um aviso pode reaparecer ao reconectar e reler o histórico.

Os JSPs preservam sessão e iframes. Um HttpClient reutilizável usa timeout de
conexão de 2 segundos e de requisição de 5 segundos. A conversa consulta a cada
2 segundos. O formulário preserva o texto em falhas e redireciona após sucesso,
evita repetir o POST ao atualizar o iframe e nunca reenvia automaticamente.
Em timeout, o POST pode ter sido persistido: conferir a conversa antes de reenviar.
Não se introduziu deduplicação/garantia de exactly-once.

## Teste automatizado sem dependências

Execute os comandos PowerShell abaixo na raiz do repositório, com JDK 21+.
Eles compilam fontes diretamente, sem Maven, não abrem telas, não acessam
`C:\Dados\RepositorioDesktop.txt` e utilizam portas livres e TXT temporário.
O teste imprime o caminho temporário para inspeção e preserva esse arquivo.

```powershell
$jdkChat = 'C:\Users\Pedro\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot' # ajuste para seu JDK 21
$javacChat = Join-Path $jdkChat 'bin\javac.exe'
$javaChat = Join-Path $jdkChat 'bin\java.exe'
$saidaChat = 'Chat_Middleware_Service\target\verificacao-servicos'
$fontesChat = @(Get-ChildItem 'Chat_Middleware_Service\src\main\java\pacote\*.java' | ForEach-Object FullName)
$fontesChat += 'Chat_Cliente_Web\src\main\java\com\mycompany\chat_cliente_web\ChatWeb.java'
$fontesChat += 'Chat_Middleware_Service\tests\ServicosIntegracaoTest.java'
$fontesChat += 'Chat_Middleware_Service\tests\CorrecoesAuditoriaTest.java'
& $javacChat --release 21 -encoding UTF-8 -d $saidaChat @fontesChat
if ($LASTEXITCODE -ne 0) { throw 'Compilação dos serviços falhou' }
& $javaChat '-Djava.awt.headless=true' -cp $saidaChat ServicosIntegracaoTest
if ($LASTEXITCODE -ne 0) { throw 'Teste dos serviços falhou' }
& $javaChat '-Djava.awt.headless=true' -cp $saidaChat CorrecoesAuditoriaTest
if ($LASTEXITCODE -ne 0) { throw 'Regressões pós-auditoria falharam' }

$fontesDesktop = @(Get-ChildItem 'Chat_Cliente_Desktop\src\main\java\pacote\*.java' | ForEach-Object FullName)
& $javacChat --release 21 -encoding UTF-8 -d 'Chat_Cliente_Desktop\target\verificacao-servicos' @fontesDesktop
if ($LASTEXITCODE -ne 0) { throw 'Compilação Desktop falhou' }
& $javacChat --release 17 -encoding UTF-8 -d 'Chat_Cliente_Web\target\verificacao-servicos' 'Chat_Cliente_Web\src\main\java\com\mycompany\chat_cliente_web\ChatWeb.java'
if ($LASTEXITCODE -ne 0) { throw 'Compilação do adaptador Web falhou' }
```

A suíte cobre falha de bind/rollback, HTTP inválido, limites, UTF-8, HTML,
stickers, timeouts TCP/HTTP, cliente sem leitura, aviso de manutenção, troca
TCP–HTTP, 40 envios simultâneos, falha/recuperação de persistência, consistência
RAM/TXT, independência e cinco ciclos de reativação.

Na correção pós-auditoria, ambas as suítes foram compiladas e executadas com
Temurin Java 21.0.12+8 do Windows, acessado pelo WSL. Os testes direcionados
reproduziram antes das correções: timeout do filtro adversarial, EOF no histórico
grande e interrupção do worker nas paradas Desktop e Web.

`CorrecoesAuditoriaTest` verifica 500 mil `<` com/sem `>` (até 5 segundos por caso),
contagem de bytes comparada ao DataOutputStream, renderização Swing de `<`, `>`
e `&`, expansão HTML acima do limite, emoji em UTF modificado, aviso e mensagens
seguintes, histórico Web completo e preservação do TXT. Uma subclasse de teste
pausa deterministicamente a chamada sincronizada de persistência antes da E/S
real; a parada deve retornar dentro do limite sem interromper o worker, inclusive
quando o serviço é reativado antes de a chamada terminar. Depois da liberação,
confere RAM/TXT e novas gravações por TCP e HTTP. Toda persistência usa arquivos
temporários; o teste não simula falha física de disco/energia.

A validação funcional anterior Java 21/GlassFish 8 foi informada pelo usuário.
Neste ajuste não se iniciou GlassFish nem se repetiu a demonstração visual; o
teste de texto usa HTMLEditorKit em modo headless.

Os cinco JSPs também foram traduzidos e seus servlets compilados offline com o
parser Jakarta já incluído no NetBeans, sem instalar dependências. Para repetir
nessa mesma instalação, após compilar `ChatWeb` acima:

```powershell
$netbeansChat = 'C:\Program Files\Apache NetBeans'
$cpJspChat = @(
    "$netbeansChat\enterprise\modules\ext\glassfish-jspparser-7.0.25.jar",
    "$netbeansChat\enterprise\modules\ext\servlet-jsp-api-6.1_3.0.jar",
    "$netbeansChat\enterprise\modules\ext\el-impl.jar",
    "$netbeansChat\enterprise\modules\ext\logging-api-1.0.4.jar",
    "$netbeansChat\extide\ant\lib\ant.jar"
) -join ';'
& $javaChat -cp $cpJspChat org.apache.jasper.JspC -uriroot 'Chat_Cliente_Web/src/main/webapp' -d 'Chat_Cliente_Web/target/verificacao-jsp' -classpath 'Chat_Cliente_Web/target/verificacao-servicos' -s mensagem.jsp conversa.jsp chat.jsp topo.jsp index.jsp
if ($LASTEXITCODE -ne 0) { throw 'Tradução JSP falhou' }
$fontesJspChat = @(Get-ChildItem 'Chat_Cliente_Web\target\verificacao-jsp\org\apache\jsp\*.java' | ForEach-Object FullName)
& $javacChat --release 17 -encoding UTF-8 -cp "$cpJspChat;Chat_Cliente_Web\target\verificacao-servicos" -d 'Chat_Cliente_Web/target/verificacao-jsp' @fontesJspChat
if ($LASTEXITCODE -ne 0) { throw 'Compilação dos servlets JSP falhou' }
```

Essa verificação valida tradução/scriptlets/imports, mas não substitui sessão,
renderização, carregamento de imagens e funcionamento no GlassFish 8 real.

## Regressão manual no Windows/GlassFish

1. Faça Build dos três projetos no NetBeans e publique o WAR atualizado no
   GlassFish. Não é necessário alterar os recursos de figurinhas.
2. Para demonstrar sem usar o TXT real, execute o Middleware pelo JShell:

   ```text
   jshell --class-path Chat_Middleware_Service/target/classes
   ```

   ```java
   import java.nio.file.*;
   import pacote.*;
   var pastaChat = Files.createTempDirectory("chat-demo-");
   Util.PathRepDesktop = pastaChat.resolve("historico.txt").toString();
   pacote.run.main(new String[0]);
   ```

3. Ative ambos os serviços. Entre em dois Desktops e duas sessões Web. Envie
   mensagens nos dois sentidos; confira acentos, emoji literal, carinhas, modos
   Fala/Grita/Xinga, todos os avatares e stickers. Digite `<script>` e tags no
   nickname/mensagem: devem aparecer como texto, sem execução.
4. Desative Desktop: confira manutenção/reconexão; Web continua enviando e
   consultando. Reative e confira reconstrução sem duplicações.
5. Desative Web: o formulário informa envio não confirmado, conversa mostra
   indisponibilidade, login/imagens continuam disponíveis e Desktop funciona.
   Reative: a conversa se recupera pelo refresh sem novo login.
6. Alterne repetidamente e rapidamente. Durante transições, somente o botão do
   serviço correspondente fica desabilitado; mover a janela continua possível.
7. Ocupe uma porta antes da ativação (na suíte, isso já é feito automaticamente)
   e confira que o painel não indica ativo nem deixa a outra porta TCP aberta.
8. Confira no TXT temporário as mensagens confirmadas. Testes de falha de
   persistência são responsabilidade da suíte; nunca remova/renomeie o TXT real.
9. Demonstre `GET http://127.0.0.1:8081/mensagens` separadamente. No navegador,
   as requisições continuam indo aos JSPs em 8080: o trecho 8081 é entre servidores.

Não foram iniciados GlassFish nem interfaces gráficas durante a validação automatizada.
