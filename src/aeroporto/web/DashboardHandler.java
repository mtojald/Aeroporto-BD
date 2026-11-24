package aeroporto.web;

import aeroporto.dao.DashboardDAO;

import com.sun.net.httpserver.HttpExchange;

import java.sql.SQLException;

public class DashboardHandler extends CrudHandler {

    private final DashboardDAO dao = new DashboardDAO();

    public DashboardHandler() {
        super("/api/dashboard");
    }

    @Override
    protected Object listar(HttpExchange ex) throws SQLException {
        Campos filtros = new Campos(Http.lerParametros(ex));
        String inicio = filtros.data("inicio", "Data inicial", false);
        String fim = filtros.data("fim", "Data final", false);
        if (inicio != null && fim != null && inicio.compareTo(fim) > 0) {
            throw ApiException.invalido("A data inicial deve ser anterior ou igual à data final.");
        }
        return dao.montar(inicio, fim, filtros.texto("companhia", "Companhia", false, 10),
                          filtros.texto("status", "Status", false, 20));
    }
}