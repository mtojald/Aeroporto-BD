package aeroporto.web;

import aeroporto.db.Database;
import aeroporto.db.Linhas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** CRUD das companhias e aeronaves. As instruções SQL são definidas por rota. */
public final class FrotaHandler extends CrudHandler {
    private final boolean aeronaves;

    public FrotaHandler(boolean aeronaves) {
        super(aeronaves ? "/api/aeronaves" : "/api/companhias");
        this.aeronaves = aeronaves;
    }

    @Override
    protected Object listar() throws SQLException {
        String sql = aeronaves ? """
                SELECT a.cod_aeronave AS codigo, a.modelo, a.capac_passageiros AS capacidade,
                       a.ano_fabricacao AS ano, c.cod_cia AS companhia, c.nome AS nome_companhia
                FROM Aeronave a
                JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                ORDER BY a.cod_aeronave
                """ : "SELECT cod_cia AS codigo, nome FROM CompanhiaAerea ORDER BY nome, cod_cia";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return Linhas.de(rs);
        }
    }

    @Override
    protected String inserir(Map<String, String> valores) throws SQLException {
        Campos c = new Campos(valores);
        String codigo = c.texto("codigo", "Código", true, 10);
        if (aeronaves) {
            executar("INSERT INTO Aeronave (modelo, capac_passageiros, ano_fabricacao, fk_CompanhiaAerea_cod_cia, cod_aeronave) VALUES (?, ?, ?, ?, ?)",
                     camposAeronave(c, codigo));
        } else {
            executar("INSERT INTO CompanhiaAerea (nome, cod_cia) VALUES (?, ?)",
                     c.texto("nome", "Nome", true, 100), codigo);
        }
        return "Registro cadastrado.";
    }

    @Override
    protected String alterar(String id, Map<String, String> valores) throws SQLException {
        Campos c = new Campos(valores);
        if (aeronaves) {
            executar("UPDATE Aeronave SET modelo = ?, capac_passageiros = ?, ano_fabricacao = ?, fk_CompanhiaAerea_cod_cia = ? WHERE cod_aeronave = ?",
                     camposAeronave(c, id));
        } else {
            executar("UPDATE CompanhiaAerea SET nome = ? WHERE cod_cia = ?",
                     c.texto("nome", "Nome", true, 100), id);
        }
        return "Registro atualizado.";
    }

    @Override
    protected String excluir(String id) throws SQLException {
        executar(aeronaves ? "DELETE FROM Aeronave WHERE cod_aeronave = ?"
                           : "DELETE FROM CompanhiaAerea WHERE cod_cia = ?", id);
        return "Registro excluído.";
    }

    private static Object[] camposAeronave(Campos c, String codigo) {
        Integer capacidade = c.inteiroOpcional("capacidade", "Capacidade");
        if (capacidade != null && (capacidade < 1 || capacidade > 299)) {
            throw ApiException.invalido("A capacidade deve estar entre 1 e 299 passageiros.");
        }
        return new Object[] {c.texto("modelo", "Modelo", false, 50), capacidade,
                             c.inteiroOpcional("ano", "Ano de fabricação"),
                             c.texto("companhia", "Companhia", true, 10), codigo};
    }

    private static void executar(String sql, Object... parametros) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            if (ps.executeUpdate() == 0) throw ApiException.naoEncontrado("Registro não encontrado.");
        }
    }
}
