package aeroporto.dao;

import aeroporto.db.Database;
import aeroporto.db.Linhas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Listas usadas para preencher os campos de seleção (chaves estrangeiras) dos formulários. */
public class OpcoesDAO {

    private static final Map<String, String> CONSULTAS = new LinkedHashMap<>();

    static {
        CONSULTAS.put("aeronaves", """
                SELECT a.cod_aeronave, a.modelo, c.nome AS companhia
                FROM Aeronave a
                JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                ORDER BY a.cod_aeronave
                """);
        CONSULTAS.put("aeroportos", """
                SELECT cod_iata, nome, cidade FROM Aeroporto ORDER BY cod_iata
                """);
        CONSULTAS.put("portoes", """
                SELECT num_portao, num_terminal FROM Portao ORDER BY num_terminal, num_portao
                """);
        CONSULTAS.put("pilotos", """
                SELECT p.fk_Funcionario_matricula AS matricula,
                       CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS nome, p.num_licenca
                FROM Piloto p
                JOIN Funcionario f ON f.matricula = p.fk_Funcionario_matricula
                ORDER BY nome
                """);
        CONSULTAS.put("voos", """
                SELECT num_voo, data_voo, fk_Aeroporto_origem AS origem, fk_Aeroporto_destino AS destino
                FROM Voo ORDER BY num_voo
                """);
    }

    public Map<String, List<Map<String, Object>>> listarTodas() throws SQLException {
        Map<String, List<Map<String, Object>>> resultado = new LinkedHashMap<>();
        try (Connection con = Database.getConnection()) {
            for (Map.Entry<String, String> c : CONSULTAS.entrySet()) {
                try (PreparedStatement ps = con.prepareStatement(c.getValue());
                     ResultSet rs = ps.executeQuery()) {
                    resultado.put(c.getKey(), Linhas.de(rs));
                }
            }
        }
        return resultado;
    }
}
