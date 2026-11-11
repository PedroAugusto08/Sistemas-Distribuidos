# Serviços de comunicação do chat

## Compilar e executar sem NetBeans

Instale JDK 21, Maven e um servidor GlassFish compatível com Jakarta EE. Na raiz,
mantenha `Chat_Catalogo`, `Chat_Swing` e `Recursos_Figurinhas` ao lado dos projetos:

```powershell
mvn -f Chat_Cliente_Desktop/pom.xml package
mvn -f Chat_Middleware_Service/pom.xml package
mvn -f Chat_Cliente_Web/pom.xml package
```

Os JARs em `target` têm Main-Class `pacote.frmLogin` e `pacote.run`. Publique
as imagens no docroot do domínio GlassFish com o instalador versionado:

```powershell
./Recursos_Figurinhas/instalar-figurinhas.ps1 -GlassFishHome '<GlassFish>' -Dominio '<dominio>' -CatalogoJar 'Chat_Middleware_Service/target/Chat_Middleware_Service-1.0-SNAPSHOT.jar'
```

Implante `Chat_Cliente_Web/target/Chat_Cliente_Web-1.0-SNAPSHOT.war` no GlassFish
com a ferramenta do servidor. Execute em terminais separados:

```powershell
java -jar Chat_Middleware_Service/target/Chat_Middleware_Service-1.0-SNAPSHOT.jar
java -jar Chat_Cliente_Desktop/target/Chat_Cliente_Desktop-1.0-SNAPSHOT.jar
```

Ative Desktop, Web e coletor no painel e abra o contexto do WAR no navegador.
Os padrões usam GlassFish em `localhost:8080`; ajuste `urlCatalogoBase` e
`urlMiddleware` em `WEB-INF/web.xml` antes do build quando necessário. Configure
`-Dchat.figurinhas.url` no Desktop para o mesmo servidor de imagens. O histórico
padrão é `C:\Dados\RepositorioDesktop.txt`; para uma demonstração isolada,
passe `-Dchat.history.path=<arquivo-temporario>` antes de `-jar` no Middleware.
Os logs podem ser isolados com `-Dchat.logs.dir=<pasta-temporaria>`.

## Arquitetura e configuração

Um único processo Middleware hospeda três serviços independentes:

- Desktop: TCP 6662 recebe uma mensagem por conexão com `ObjectInputStream.readUTF()`;
  TCP 6661 transmite o histórico e as novidades com `ObjectOutputStream.writeUTF()`.
- Web: HTTP 8081, com GET e POST em `/mensagens`. Os JSPs fazem essas chamadas
  no servidor GlassFish; o navegador continua acessando o GlassFish em 8080.
- Coletor de erros: HTTP 8082 com controle próprio, separado dos logs internos.
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
antes de disponibilizar Desktop/Web. O painel tenta carregar em segundo plano e
permanece disponível se falhar; cada ativação do chat pode tentar novamente.
O coletor não depende do repositório. Alternar Desktop/Web não recria o repositório
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

Os JSPs preservam a sessão; JavaScript consulta o histórico periodicamente. Um HttpClient reutilizável usa timeout de
conexão de 2 segundos e de requisição de 5 segundos. A conversa consulta a cada
2 segundos. O formulário preserva o texto em falhas, confirma o sucesso sem
repetir o POST e nunca reenvia automaticamente.
Em timeout, o POST pode ter sido persistido: conferir a conversa antes de reenviar.
Não se introduziu deduplicação/garantia de exactly-once.

## Testes automatizados locais

Com JDK 21 no Windows, os scripts locais `Chat_Catalogo/tests/build-m6.ps1` e
`Chat_Catalogo/tests/verificar-m6.ps1` constroem e testam em pastas temporárias.
Ajuste os caminhos do JDK e Maven indicados nos scripts para sua instalação. Os
scripts de teste são locais e não fazem parte da baseline versionada.

## Regressão manual no Windows/GlassFish

1. Compile os três projetos e publique o WAR e as figurinhas no GlassFish
   conforme o procedimento acima.
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

## Checklist de demonstração

- Com dois clientes Desktop e duas sessões Web, envie texto e imagens nos dois sentidos e confira o histórico após reconectar.
- Desative e reative Desktop e Web separadamente; confira que o outro serviço continua operando.
- Ative o coletor e provoque falhas controladas de cliente e serviço; confira os arquivos de log por tipo e serviço.
- Verifique o TXT temporário, a recarga do histórico e a disponibilidade das figurinhas pelo GlassFish.
