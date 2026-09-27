package aeroporto.web;

import aeroporto.dao.OpcoesDAO;

import java.sql.SQLException;

/** GET /api/opcoes -> listas para os campos de seleção (aeronaves, aeroportos, portões...). */
public class OpcoesHandler extends CrudHandler {

    private final OpcoesDAO dao = new OpcoesDAO();

    public OpcoesHandler() {
        super("/api/opcoes");
    }

    @Override
    protected Object listar() throws SQLException {
        return dao.listarTodas();
    }
}
