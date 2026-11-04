package pacote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RepositorioMensagens {
    private final Path arquivo;
    private final ArrayList<String> mensagens;
    private boolean precisaSeparador;
    private boolean gravacaoBloqueada;

    public RepositorioMensagens(Path arquivo) throws IOException {
        this.arquivo = arquivo;
        Path pasta = arquivo.toAbsolutePath().getParent();
        Files.createDirectories(pasta);

        // CREATE não substitui nem trunca um histórico que já existe.
        try (FileChannel canal = FileChannel.open(arquivo,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
        }
        mensagens = new ArrayList<>(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        if (!mensagens.isEmpty() && mensagens.get(0).startsWith("\uFEFF")) {
            mensagens.set(0, mensagens.get(0).substring(1));
        }

        // Um TXT antigo pode terminar sem quebra de linha.
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.READ)) {
            if (canal.size() > 0) {
                ByteBuffer ultimo = ByteBuffer.allocate(1);
                canal.position(canal.size() - 1);
                canal.read(ultimo);
                byte caractere = ultimo.array()[0];
                precisaSeparador = caractere != '\n' && caractere != '\r';
            }
        }
    }

    public synchronized void adicionarMensagem(String mensagem) throws IOException {
        if (gravacaoBloqueada) {
            throw new IOException("Gravação bloqueada após falha ao desfazer uma escrita. Verifique o TXT antes de reiniciar.");
        }

        String quebra = System.lineSeparator();
        byte[] dados = ((precisaSeparador ? quebra : "") + mensagem + quebra)
                .getBytes(StandardCharsets.UTF_8);
        List<String> novasMensagens = new ArrayList<>();
        // Mantém a divisão por linhas já utilizada no envio do histórico antigo.
        try (BufferedReader leitor = new BufferedReader(new StringReader(mensagem + quebra))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                novasMensagens.add(linha);
            }
        }

        long tamanhoOriginal = Files.size(arquivo);
        boolean iniciouEscrita = false;
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
            canal.position(tamanhoOriginal);
            ByteBuffer buffer = ByteBuffer.wrap(dados);
            iniciouEscrita = true;
            while (buffer.hasRemaining()) {
                canal.write(buffer);
            }
            canal.force(true);
        } catch (IOException e) {
            if (iniciouEscrita) {
                // Desfaz somente o acréscimo da operação que falhou.
                try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
                    canal.truncate(tamanhoOriginal);
                    canal.force(true);
                } catch (IOException erroAoDesfazer) {
                    gravacaoBloqueada = true;
                    e.addSuppressed(erroAoDesfazer);
                }
            }
            throw e;
        }

        mensagens.addAll(novasMensagens);
        precisaSeparador = false;
    }

    public synchronized List<String> consultarMensagens(int indice) {
        return new ArrayList<>(mensagens.subList(indice, mensagens.size()));
    }
}
