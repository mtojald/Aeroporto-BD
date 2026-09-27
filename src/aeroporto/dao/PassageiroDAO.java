package aeroporto.dao;

import aeroporto.db.Database;
import aeroporto.db.ErroSql;
import aeroporto.db.Linhas;
import aeroporto.model.Passageiro;
import aeroporto.web.ApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** CRUD da tabela Passageiro (e da tabela filha Passageiro_Telefone). */
public class PassageiroDAO {

    public List<Map<String, Object>> listar() throws SQLException {
        String sql = """
                SELECT p.cpf, p.primeiro_nome, p.sobrenome, p.data_nasci, p.email,
                       GROUP_CONCAT(t.telefone ORDER BY t.telefone SEPARATOR ', ') AS telefones,
                       (SELECT COUNT(*) FROM Bilhete b WHERE b.fk_Passageiro_cpf = p.cpf) AS qtd_bilhetes
                FROM Passageiro p
                LEFT JOIN Passageiro_Telefone t ON t.cpf = p.cpf
                GROUP BY p.cpf, p.primeiro_nome, p.sobrenome, p.data_nasci, p.email
                ORDER BY p.primeiro_nome, p.sobrenome
                """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return Linhas.de(rs);
        }
    }

    public void inserir(Passageiro p) throws SQLException {
        String sql = """
                INSERT INTO Passageiro (cpf, primeiro_nome, sobrenome, data_nasci, email)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false); // passageiro + telefones numa única transação
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, p.cpf());
                ps.setString(2, p.primeiroNome());
                ps.setString(3, p.sobrenome());
                ps.setString(4, p.dataNasci());
                ps.setString(5, p.email());
                ps.executeUpdate();
                inserirTelefones(con, p.cpf(), p.telefones());
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == ErroSql.DUPLICADO) {
                    throw new ApiException(409, "Já existe um passageiro com o CPF " + p.cpf() + ".");
                }
                throw e;
            }
        }
    }

    public void alterar(Passageiro p) throws SQLException {
        String sql = """
                UPDATE Passageiro
                SET primeiro_nome = ?, sobrenome = ?, data_nasci = ?, email = ?
                WHERE cpf = ?
                """;
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, p.primeiroNome());
                ps.setString(2, p.sobrenome());
                ps.setString(3, p.dataNasci());
                ps.setString(4, p.email());
                ps.setString(5, p.cpf());
                if (ps.executeUpdate() == 0) {
                    throw ApiException.naoEncontrado("Passageiro com CPF " + p.cpf() + " não encontrado.");
                }
                // telefones: substitui a lista inteira
                try (PreparedStatement del = con.prepareStatement(
                        "DELETE FROM Passageiro_Telefone WHERE cpf = ?")) {
                    del.setString(1, p.cpf());
                    del.executeUpdate();
                }
                inserirTelefones(con, p.cpf(), p.telefones());
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void excluir(String cpf) throws SQLException {
        // Passageiro_Telefone é removido em cascata (ON DELETE CASCADE)
        String sql = "DELETE FROM Passageiro WHERE cpf = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cpf);
            if (ps.executeUpdate() == 0) {
                throw ApiException.naoEncontrado("Passageiro com CPF " + cpf + " não encontrado.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == ErroSql.REFERENCIADO) {
                throw new ApiException(409,
                        "Este passageiro possui bilhetes comprados e não pode ser excluído. "
                        + "Exclua os bilhetes dele antes.");
            }
            throw e;
        }
    }

    private void inserirTelefones(Connection con, String cpf, List<String> telefones) throws SQLException {
        if (telefones.isEmpty()) return;
        String sql = "INSERT INTO Passageiro_Telefone (cpf, telefone) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String tel : telefones) {
                ps.setString(1, cpf);
                ps.setString(2, tel);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
