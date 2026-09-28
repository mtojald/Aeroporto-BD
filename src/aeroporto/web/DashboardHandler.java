package aeroporto.web;

import aeroporto.dao.DashboardDAO;

import java.sql.SQLException;

public class DashboardHandler extends CrudHandler {

    private final DashboardDAO dao = new DashboardDAO();

    public DashboardHandler() {
        super("/api/dashboard");
    }

    @Override
    protected Object listar() throws SQLException {
        return dao.montar();
    }
}