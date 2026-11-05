package com.mycompany.chat_cliente_web;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/** Comunicação servidor-servidor; nenhuma thread é criada pelos JSPs. */
public final class ChatWeb {
    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private static final Set<String> CORES = Set.of("navy", "tomato", "black");
    private static final Set<String> AVATARES = Set.of("menino.png", "menina.png", "avatar.png");
    private static final Set<String> EMOJIS = Set.of("", "emojis/coracao.gif", "emojis/risada.gif", "emojis/choro.gif");

    private ChatWeb() { }

    public static boolean perfilValido(Object nickname, Object cor, Object avatar) {
        return nickname instanceof String && !((String) nickname).isBlank()
                && ((String) nickname).length() <= 20
                && cor instanceof String && CORES.contains(cor)
                && avatar instanceof String && AVATARES.contains(avatar);
    }

    public static String html(Object valor) {
        return valor == null ? "" : valor.toString().replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String montarMensagem(Object nickname, Object cor, Object avatar, String texto, String emoji) {
        if (!perfilValido(nickname, cor, avatar) || texto == null || texto.length() > 150
                || (texto.isBlank() && (emoji == null || emoji.isEmpty()))
                || emoji == null || !EMOJIS.contains(emoji)) {
            throw new IllegalArgumentException("Mensagem ou perfil inválido.");
        }
        String mensagem = "<img src='/figurinhas/imagens/" + avatar + "' width='30' height='30'>"
                + "<font size=\"+1\" color=\"" + cor + "\"><b>" + html(nickname) + "</b></font>"
                + "Diz: " + html(texto);
        if (!emoji.isEmpty()) mensagem += "<img src='/figurinhas/" + emoji + "' width='30' height='30'>";
        return mensagem + "<br>";
    }

    private static HttpRequest.Builder requisicao(String endereco) {
        URI uri = URI.create(endereco);
        if (!"http".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Endereço do Middleware inválido.");
        }
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5));
    }

    public static String consultar(String endereco) throws IOException, InterruptedException {
        HttpResponse<String> resposta = CLIENTE.send(requisicao(endereco).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resposta.statusCode() != 200) throw new IOException("Serviço Web indisponível.");
        return resposta.body();
    }

    public static void enviar(String endereco, String mensagem) throws IOException, InterruptedException {
        HttpResponse<Void> resposta = CLIENTE.send(requisicao(endereco)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(mensagem, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.discarding());
        if (resposta.statusCode() != 204) throw new IOException("Envio não confirmado pelo Middleware.");
    }
}
