package aeroporto.web;

import aeroporto.dao.DashboardDAO;
import aeroporto.db.ErroSql;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** API do dashboard com filtros cruzados por status, companhia e período. */
public class DashboardHandler implements HttpHandler {

    private final DashboardDAO dao = new DashboardDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            Http.enviarJson(ex, 405, Map.of("erro", "Método não permitido."));
            return;
        }
        try {
            Map<String, String> parametros = Http.lerParametros(ex.getRequestURI().getRawQuery());
            String inicio = parametros.get("inicio");
            String fim = parametros.get("fim");
            if (inicio != null && !inicio.isBlank()) java.time.LocalDate.parse(inicio);
            if (fim != null && !fim.isBlank()) java.time.LocalDate.parse(fim);
            if (inicio != null && fim != null && !inicio.isBlank() && !fim.isBlank() && inicio.compareTo(fim) > 0) {
                Http.enviarJson(ex, 400, Map.of("erro", "A data inicial deve ser anterior à data final."));
                return;
            }
            Object resultado = dao.montar(parametros.get("status"), parametros.get("cia"), inicio, fim, parametros);
            Http.enviarJson(ex, 200, resultado);
        } catch (DateTimeParseException e) {
            Http.enviarJson(ex, 400, Map.of("erro", "Informe as datas no formato válido."));
        } catch (SQLException e) {
            System.err.println("[SQL] " + e.getErrorCode() + " " + e.getMessage());
            Http.enviarJson(ex, ErroSql.status(e), Map.of("erro", ErroSql.mensagem(e)));
        } catch (Exception e) {
            Http.enviarJson(ex, 500, Map.of("erro", "Não foi possível carregar o dashboard."));
        }
    }
}
