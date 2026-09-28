package aeroporto.web;

import aeroporto.dao.ConsultaDAO;

import java.sql.SQLException;

/* devolve texto + resultado. */
public class ConsultaHandler extends CrudHandler {

    private final ConsultaDAO dao = new ConsultaDAO();

    public ConsultaHandler() {
        super("/api/consultas");
    }

    @Override
    protected Object listar() throws SQLException {
        return dao.executarTodas();
    }
}