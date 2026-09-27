package aeroporto.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Utilitários para ler requisições e enviar respostas com o HttpServer do JDK. */
public final class Http {

    private Http() {}

    /** Lê um corpo application/x-www-form-urlencoded (o que o front envia com URLSearchParams). */
    public static Map<String, String> lerFormulario(HttpExchange ex) throws IOException {
        String corpo;
        try (InputStream in = ex.getRequestBody()) {
            corpo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> campos = new HashMap<>();
        if (corpo.isBlank()) return campos;
        for (String par : corpo.split("&")) {
            int i = par.indexOf('=');
            String chave = i < 0 ? par : par.substring(0, i);
            String valor = i < 0 ? "" : par.substring(i + 1);
            campos.put(URLDecoder.decode(chave, StandardCharsets.UTF_8),
                       URLDecoder.decode(valor, StandardCharsets.UTF_8).trim());
        }
        return campos;
    }

    public static void enviarJson(HttpExchange ex, int status, Object corpo) throws IOException {
        enviar(ex, status, "application/json; charset=utf-8",
               Json.escrever(corpo).getBytes(StandardCharsets.UTF_8));
    }

    public static void enviar(HttpExchange ex, int status, String tipo, byte[] corpo) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, corpo.length == 0 ? -1 : corpo.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(corpo);
        }
    }
}
