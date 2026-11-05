package pacote;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public record RegistroErro(String identificacao, String ip, String classe, String metodo, String mensagem) {
    public String formatar(String servico) {
        String hora = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return hora + " | " + (servico == null ? "" : escapar(servico) + " | ")
                + escapar(identificacao) + " | " + escapar(ip) + " | "
                + escapar(classe) + ":" + escapar(metodo) + " | " + escapar(mensagem) + "\n\n";
    }

    public static RegistroErro de(String identificacao, String ip, String classe, String metodo, Exception erro) {
        String resumo = erro.getClass().getName() + ": "
                + (erro.getMessage() == null ? "Sem mensagem." : erro.getMessage());
        return new RegistroErro(identificacao, ip, classe, metodo, resumo);
    }

    private static String escapar(String valor) {
        if (valor == null || valor.isBlank()) return "-";
        StringBuilder texto = new StringBuilder();
        int limite = Math.min(valor.length(), 4096);
        if (limite < valor.length() && Character.isHighSurrogate(valor.charAt(limite - 1))
                && Character.isLowSurrogate(valor.charAt(limite))) limite--;
        for (int i = 0; i < limite; i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '\\' -> texto.append("\\\\");
                case '|' -> texto.append("\\u007c");
                case '\n' -> texto.append("\\n");
                case '\r' -> texto.append("\\r");
                default -> {
                    if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029') texto.append(' ');
                    else texto.append(c);
                }
            }
        }
        if (valor.length() > limite) texto.append(" [truncado]");
        return texto.toString();
    }
}
