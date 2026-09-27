package aeroporto.dao;

import aeroporto.db.Database;
import aeroporto.db.ErroSql;
import aeroporto.db.Linhas;
import aeroporto.model.Voo;
import aeroporto.web.ApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/** CRUD da tabela Voo. */
public class VooDAO {

    public List<Map<String, Object>> listar() throws SQLException {
        String sql = """
                SELECT v.num_voo, v.data_voo, v.hora_partida, v.hora_chegada, v.status,
                       v.fk_Aeronave_cod_aeronave AS cod_aeronave, a.modelo,
                       c.nome AS companhia,
                       v.fk_Aeroporto_origem AS origem, v.fk_Aeroporto_destino AS destino,
                       v.fk_Portao_num_portao AS num_portao, v.fk_Portao_num_terminal AS num_terminal,
                       v.fk_Voo_conexao AS voo_conexao,
                       v.fk_Piloto_comandante AS piloto,
                       CONCAT(f.primeiro_nome, ' ', f.sobrenome) AS nome_piloto
                FROM Voo v
                JOIN Aeronave a       ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                JOIN Funcionario f    ON f.matricula = v.fk_Piloto_comandante
                ORDER BY v.data_voo, v.hora_partida, v.num_voo
                """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return Linhas.de(rs);
        }
    }

    public void inserir(Voo v) throws SQLException {
        String sql = """
                INSERT INTO Voo (data_voo, hora_partida, hora_chegada, status,
                                 fk_Aeronave_cod_aeronave, fk_Aeroporto_origem, fk_Aeroporto_destino,
                                 fk_Portao_num_portao, fk_Portao_num_terminal,
                                 fk_Voo_conexao, fk_Piloto_comandante, num_voo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, v);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == ErroSql.DUPLICADO) {
                throw new ApiException(409, "Já existe um voo com o número " + v.numVoo() + ".");
            }
            throw e;
        }
    }

    public void alterar(Voo v) throws SQLException {
        String sql = """
                UPDATE Voo
                SET data_voo = ?, hora_partida = ?, hora_chegada = ?, status = ?,
                    fk_Aeronave_cod_aeronave = ?, fk_Aeroporto_origem = ?, fk_Aeroporto_destino = ?,
                    fk_Portao_num_portao = ?, fk_Portao_num_terminal = ?,
                    fk_Voo_conexao = ?, fk_Piloto_comandante = ?
                WHERE num_voo = ?
                """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, v);
            if (ps.executeUpdate() == 0) {
                throw ApiException.naoEncontrado("Voo " + v.numVoo() + " não encontrado.");
            }
        }
    }

    public void excluir(int numVoo) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1) outros voos que apontam para este como conexão perdem a ligação
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Voo SET fk_Voo_conexao = NULL WHERE fk_Voo_conexao = ?")) {
                    ps.setInt(1, numVoo);
                    ps.executeUpdate();
                }
                // 2) remove o voo (a Escala é apagada em cascata; Bilhete bloqueia)
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM Voo WHERE num_voo = ?")) {
                    ps.setInt(1, numVoo);
                    if (ps.executeUpdate() == 0) {
                        throw ApiException.naoEncontrado("Voo " + numVoo + " não encontrado.");
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == ErroSql.REFERENCIADO) {
                    throw new ApiException(409, "O voo " + numVoo + " possui bilhetes vendidos e não pode "
                            + "ser excluído. Você pode alterar o status dele para 'Cancelado'.");
                }
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Mesma ordem de parâmetros no INSERT e no UPDATE (num_voo por último). */
    private void preencher(PreparedStatement ps, Voo v) throws SQLException {
        ps.setString(1, v.dataVoo());
        ps.setString(2, v.horaPartida());
        ps.setString(3, v.horaChegada());
        ps.setString(4, v.status());
        ps.setString(5, v.codAeronave());
        ps.setString(6, v.aeroportoOrigem());
        ps.setString(7, v.aeroportoDestino());
        ps.setInt(8, v.numPortao());
        ps.setInt(9, v.numTerminal());
        if (v.vooConexao() == null) ps.setNull(10, Types.INTEGER);
        else ps.setInt(10, v.vooConexao());
        ps.setInt(11, v.pilotoComandante());
        ps.setInt(12, v.numVoo());
    }
}
