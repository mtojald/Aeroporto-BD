package aeroporto.web;

import aeroporto.db.ErroSql;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 * Base REST para uma tabela:
 *   GET    /api/xxx        -> listar()
 *   POST   /api/xxx        -> inserir(campos)
 *   PUT    /api/xxx/{id}   -> alterar(id, campos)
 *   DELETE /api/xxx/{id}   -> excluir(id)
 * Subclasses sobrescrevem só o que suportam; o resto responde 405.
 */
public abstract class CrudHandler implements HttpHandler {

    private final String caminhoBase;

    protected CrudHandler(String caminhoBase) {
        this.caminhoBase = caminhoBase;
    }

    protected Object listar() throws SQLException {
        throw naoSuportado();
    }

    protected String inserir(Map<String, String> campos) throws SQLException {
        throw naoSuportado();
    }

    protected String alterar(String id, Map<String, String> campos) throws SQLException {
        throw naoSuportado();
    }

    protected String excluir(String id) throws SQLException {
        throw naoSuportado();
    }

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            String id = extrairId(ex.getRequestURI().getRawPath());
            String metodo = ex.getRequestMethod();
            switch (metodo) {
                case "GET" -> {
                    semId(id);
                    Http.enviarJson(ex, 200, listar());
                }
                case "POST" -> {
                    semId(id);
                    Http.enviarJson(ex, 201, Map.of("mensagem", inserir(Http.lerFormulario(ex))));
                }
                case "PUT" -> Http.enviarJson(ex, 200, Map.of("mensagem", alterar(comId(id), Http.lerFormulario(ex))));
                case "DELETE" -> Http.enviarJson(ex, 200, Map.of("mensagem", excluir(comId(id))));
                default -> throw naoSuportado();
            }
        } catch (ApiException e) {
            Http.enviarJson(ex, e.status(), Map.of("erro", e.getMessage()));
        } catch (SQLException e) {
            System.err.println("[SQL] " + e.getErrorCode() + " " + e.getMessage());
            Http.enviarJson(ex, ErroSql.status(e), Map.of("erro", ErroSql.mensagem(e)));
        } catch (Exception e) {
            e.printStackTrace();
            Http.enviarJson(ex, 500, Map.of("erro", "Erro interno: " + e.getMessage()));
        }
    }

    /** "/api/voos" -> null, "/api/voos/101" -> "101". */
    private String extrairId(String caminho) {
        String resto = caminho.substring(caminhoBase.length());
        if (resto.isEmpty() || resto.equals("/")) return null;
        if (!resto.startsWith("/") || resto.indexOf('/', 1) >= 0) {
            throw ApiException.naoEncontrado("Rota não encontrada: " + caminho);
        }
        return URLDecoder.decode(resto.substring(1), StandardCharsets.UTF_8);
    }

    private static void semId(String id) {
        if (id != null) throw new ApiException(405, "Método não permitido para um registro específico.");
    }

    private static String comId(String id) {
        if (id == null) throw new ApiException(405, "Informe o registro na URL.");
        return id;
    }

    private static ApiException naoSuportado() {
        return new ApiException(405, "Método não suportado.");
    }
}
