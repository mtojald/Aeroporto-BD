package aeroporto.web;

import aeroporto.db.Database;
import aeroporto.db.ErroSql;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

public class RecursosHandler implements HttpHandler {
    private static final Map<String, String> ARQUIVOS = Map.of(
        "views", "views.sql", "procedures", "Procedures.sql",
        "triggers", "Triggers.sql", "functions", "functions.sql");

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestURI().getPath().equals("/api/recursos")) {
                throw ApiException.naoEncontrado("Recurso não encontrado.");
            }
            if (ex.getRequestMethod().equals("GET")) {
                List<Object> catalogo = new ArrayList<>();
                for (String tipo : List.of("views", "procedures", "triggers", "functions")) {
                    String fonte = Files.readString(Path.of("database", ARQUIVOS.get(tipo)));
                    var matcher = Pattern.compile("(?is)CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:VIEW|PROCEDURE|TRIGGER|FUNCTION)\\s+(\\w+).*?" +
                        (tipo.equals("views") ? ";" : "END\\s*//")).matcher(fonte);
                    while (matcher.find()) {
                        catalogo.add(Map.of("tipo", tipo, "nome", matcher.group(1), "sql", matcher.group(), "arquivo", ARQUIVOS.get(tipo)));
                    }
                }
                Http.enviarJson(ex, 200, catalogo);
            } else if (ex.getRequestMethod().equals("POST")) {
                Http.enviarJson(ex, 200, executar(Http.lerFormulario(ex)));
            } else {
                throw new ApiException(405, "Método não permitido.");
            }
        } catch (ApiException e) {
            Http.enviarJson(ex, e.status(), Map.of("erro", e.getMessage()));
        } catch (SQLException e) {
            Http.enviarJson(ex, ErroSql.status(e), Map.of("erro", ErroSql.mensagem(e)));
        } catch (IOException e) {
            Http.enviarJson(ex, 500, Map.of("erro", "Não foi possível ler os arquivos SQL do projeto."));
        }
    }

    private Object executar(Map<String, String> valores) throws SQLException {
        Campos campos = new Campos(valores);
        String nome = campos.obrigatorio("nome", "Recurso");
        String sql = switch (nome) {
            case "vw_voo_completo" -> "SELECT * FROM vw_voo_completo";
            case "vw_passageiro_resumo" -> "SELECT * FROM vw_passageiro_resumo";
            case "atualizar_status_voo" -> "CALL atualizar_status_voo(?, ?)";
            case "verificar_conexoes_passageiros" -> "CALL verificar_conexoes_passageiros()";
            case "calcular_duracao_voo" -> "SELECT calcular_duracao_voo(?) AS duracao_minutos";
            case "calcular_taxa_ocupacao_voo" -> "SELECT calcular_taxa_ocupacao_voo(?) AS ocupacao";
            case "registrar_alteracao_status_voo", "verificar_manutencao_voo" ->
                "SELECT TRIGGER_NAME AS nome, EVENT_MANIPULATION AS evento, ACTION_TIMING AS momento, EVENT_OBJECT_TABLE AS tabela FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = ?";
            case "historico_status" -> "SELECT * FROM Log_Voo";
            default -> throw ApiException.invalido("Recurso não permitido.");
        };
        boolean temVoo = nome.equals("atualizar_status_voo") || nome.startsWith("calcular_");
        try (Connection con = Database.getConnection()) {
            Integer voo = temVoo ? campos.inteiro("voo", "Número do voo") : null;
            if (temVoo) {
                try (PreparedStatement ps = con.prepareStatement("SELECT num_voo FROM Voo WHERE num_voo = ?")) {
                    ps.setInt(1, voo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw ApiException.naoEncontrado("Voo não encontrado.");
                    }
                }
            }
            try (PreparedStatement ps = sql.startsWith("CALL") ? con.prepareCall(sql) : con.prepareStatement(sql)) {
                if (temVoo) ps.setInt(1, voo);
                if (nome.equals("atualizar_status_voo")) {
                    String status = campos.obrigatorio("status", "Status");
                    if (!List.of("Confirmado", "Cancelado").contains(status)) throw ApiException.invalido("Status inválido.");
                    ps.setString(2, status);
                }
                if ((nome.equals("registrar_alteracao_status_voo") || nome.equals("verificar_manutencao_voo"))) ps.setString(1, nome);
                List<Object> resultados = new ArrayList<>();
                boolean resultado = ps.execute();
                while (true) {
                    if (resultado) {
                        try (ResultSet rs = ps.getResultSet()) {
                            List<String> colunas = new ArrayList<>();
                            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) colunas.add(rs.getMetaData().getColumnLabel(i));
                            List<Object> linhas = new ArrayList<>();
                            while (rs.next()) {
                                List<String> linha = new ArrayList<>();
                                for (int i = 1; i <= colunas.size(); i++) linha.add(rs.getString(i));
                                linhas.add(linha);
                            }
                            resultados.add(Map.of("colunas", colunas, "linhas", linhas));
                        }
                    } else if (ps.getUpdateCount() == -1) break;
                    resultado = ps.getMoreResults();
                }
                Map<String, Object> resposta = new LinkedHashMap<>();
                resposta.put("mensagem", nome.equals("atualizar_status_voo") ? "Status do voo atualizado." : "Consulta concluída.");
                resposta.put("resultados", resultados);
                if (temVoo) resposta.put("voo", carregarContextoVoo(con, voo));
                return resposta;
            }
        }
    }

    private Map<String, Object> carregarContextoVoo(Connection con, int numero) throws SQLException {
        String sql = """
                SELECT v.num_voo, v.data_voo, v.hora_partida, v.hora_chegada, v.status,
                       v.fk_Aeroporto_origem AS cod_origem, ao.nome AS nome_origem, ao.cidade AS cidade_origem,
                       v.fk_Aeroporto_destino AS cod_destino, ad.nome AS nome_destino, ad.cidade AS cidade_destino,
                       a.capac_passageiros AS capacidade,
                       (SELECT COUNT(*) FROM Bilhete b WHERE b.fk_Voo_num_voo = v.num_voo) AS assentos_ocupados
                FROM Voo v
                JOIN Aeroporto ao ON ao.cod_iata = v.fk_Aeroporto_origem
                JOIN Aeroporto ad ON ad.cod_iata = v.fk_Aeroporto_destino
                JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                WHERE v.num_voo = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Map.of();
                Map<String, Object> voo = new LinkedHashMap<>();
                for (String coluna : List.of("num_voo", "data_voo", "hora_partida", "hora_chegada", "status",
                        "cod_origem", "nome_origem", "cidade_origem", "cod_destino", "nome_destino",
                        "cidade_destino", "capacidade", "assentos_ocupados")) {
                    voo.put(coluna, rs.getObject(coluna));
                }
                return voo;
            }
        }
    }
}
