package aeroporto.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Serve os arquivos da pasta web/ (HTML, CSS, JS). */
public class StaticHandler implements HttpHandler {

    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "ico", "image/x-icon");

    private final Path raiz;

    public StaticHandler(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (caminho.endsWith("/")) caminho += "index.html";
        Path arquivo = raiz.resolve(caminho.substring(1)).normalize();

        // impede acesso fora da pasta web/ (ex.: /../config.properties)
        if (!arquivo.startsWith(raiz) || !Files.isRegularFile(arquivo)) {
            Http.enviar(ex, 404, "text/plain; charset=utf-8",
                        "Não encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String nome = arquivo.getFileName().toString();
        String ext = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase();
        Http.enviar(ex, 200, TIPOS.getOrDefault(ext, "application/octet-stream"), Files.readAllBytes(arquivo));
    }
}
