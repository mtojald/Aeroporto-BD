package aeroporto.dao;

import aeroporto.db.Database;
import aeroporto.db.Linhas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardDAO {

    private static final String RECORTE = """
            WITH voos AS (
                SELECT v.*, a.fk_CompanhiaAerea_cod_cia AS companhia, a.capac_passageiros
                FROM Voo v
                JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                WHERE v.data_voo >= COALESCE(?, v.data_voo)
                  AND v.data_voo <= COALESCE(?, v.data_voo)
                  AND (? IS NULL OR a.fk_CompanhiaAerea_cod_cia = ?)
                  AND (? IS NULL OR v.status = ?)
            ), bilhetes AS (
                SELECT b.* FROM Bilhete b JOIN voos v ON v.num_voo = b.fk_Voo_num_voo
            )
            """;

    public Map<String, Object> montar(String inicio, String fim, String companhia, String status) throws SQLException {
        Map<String, Object> dados = new LinkedHashMap<>();
        String[] filtros = {inicio, fim, companhia, companhia, status, status};
        boolean filtrado = inicio != null || fim != null || companhia != null || status != null;
        try (Connection con = Database.getConnection()) {
            // As séries e os indicadores devem enxergar o mesmo estado do banco.
            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            con.setReadOnly(true);
            con.setAutoCommit(false);
            dados.put("indicadores", umaLinha(con, """
                    SELECT (SELECT COUNT(*) FROM voos) AS voos,
                           (SELECT %s) AS passageiros,
                           (SELECT %s) AS aeronaves,
                           (SELECT COUNT(*) FROM bilhetes) AS bilhetes,
                           (SELECT COALESCE(SUM(preco), 0) FROM bilhetes) AS receita,
                           (SELECT ROUND(AVG(preco), 2) FROM bilhetes) AS precoMedio,
                           (SELECT ROUND(100 * AVG(CASE WHEN status = 'Cancelado' THEN 1 ELSE 0 END), 2)
                            FROM voos) AS cancelamentos
                    """.formatted(
                        filtrado ? "COUNT(DISTINCT fk_Passageiro_cpf) FROM bilhetes" : "COUNT(*) FROM Passageiro",
                        filtrado ? "COUNT(DISTINCT fk_Aeronave_cod_aeronave) FROM voos" : "COUNT(*) FROM Aeronave"),
                    filtros));

            dados.put("voosPorStatus", serie(con, """
                    SELECT COALESCE(status, 'Sem status') AS rotulo, COUNT(*) AS valor
                    FROM voos GROUP BY status ORDER BY valor DESC, rotulo
                    """, filtros));

            dados.put("receitaPorCompanhia", serie(con, """
                    SELECT c.nome AS rotulo, COALESCE(SUM(b.preco), 0) AS valor
                    FROM CompanhiaAerea c
                    JOIN voos v ON v.companhia = c.cod_cia
                    LEFT JOIN bilhetes b ON b.fk_Voo_num_voo = v.num_voo
                    GROUP BY c.cod_cia, c.nome ORDER BY valor DESC, rotulo
                    """, filtros));

            dados.put("voosPorDia", serie(con, """
                    SELECT DATE_FORMAT(data_voo, '%d/%m/%Y') AS rotulo, COUNT(*) AS valor
                    FROM voos GROUP BY data_voo ORDER BY data_voo
                    """, filtros));

            dados.put("ocupacaoPorVoo", serie(con, """
                    SELECT CONCAT('Voo ', v.num_voo) AS rotulo,
                           ROUND(100 * COUNT(b.cod_bilhete) / v.capac_passageiros, 2) AS valor
                    FROM voos v
                    LEFT JOIN bilhetes b ON b.fk_Voo_num_voo = v.num_voo
                    WHERE v.capac_passageiros > 0
                    GROUP BY v.num_voo, v.capac_passageiros ORDER BY v.num_voo
                    """, filtros));

            dados.put("bilhetesPorClasse", serie(con, """
                    SELECT COALESCE(classe, 'Sem classe') AS rotulo, COUNT(*) AS valor
                    FROM bilhetes GROUP BY classe ORDER BY valor DESC, rotulo
                    """, filtros));

            List<Double> precos = new ArrayList<>();
            try (PreparedStatement ps = preparar(con, "SELECT preco FROM bilhetes WHERE preco IS NOT NULL ORDER BY preco", filtros);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) precos.add(rs.getDouble(1));
            }
            dados.put("estatisticasPreco", estatisticas(precos));
            dados.put("histogramaPreco", histograma(precos));
            try (PreparedStatement ps = con.prepareStatement("SELECT cod_cia, nome FROM CompanhiaAerea ORDER BY nome, cod_cia");
                 ResultSet rs = ps.executeQuery()) {
                dados.put("companhias", Linhas.de(rs));
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT DISTINCT status FROM Voo WHERE status IS NOT NULL AND status <> '' ORDER BY status");
                 ResultSet rs = ps.executeQuery()) {
                dados.put("statusDisponiveis", Linhas.de(rs));
            }
            con.commit();
        }
        return dados;
    }

    private PreparedStatement preparar(Connection con, String sql, String[] filtros) throws SQLException {
        PreparedStatement ps = con.prepareStatement(RECORTE + sql);
        for (int i = 0; i < filtros.length; i++) ps.setString(i + 1, filtros[i]);
        return ps;
    }

    // Leitura de resultados
    private Map<String, Object> umaLinha(Connection con, String sql, String[] filtros) throws SQLException {
        try (PreparedStatement ps = preparar(con, sql, filtros); ResultSet rs = ps.executeQuery()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            if (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    linha.put(rs.getMetaData().getColumnLabel(i), rs.getBigDecimal(i));
                }
            }
            return linha;
        }
    }

    //Consulta com colunas
    private Map<String, Object> serie(Connection con, String sql, String[] filtros) throws SQLException {
        List<String> rotulos = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        try (PreparedStatement ps = preparar(con, sql, filtros); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rotulos.add(rs.getString("rotulo"));
                valores.add(rs.getBigDecimal("valor"));
            }
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("rotulos", rotulos);
        s.put("valores", valores);
        return s;
    }

    //Lista ordenada
    private Map<String, Object> estatisticas(List<Double> x) {
        Map<String, Object> e = new LinkedHashMap<>();
        int n = x.size();
        e.put("n", n);
        if (n == 0) return e;

        double soma = 0;
        for (double v : x) soma += v;
        double media = soma / n;

        double somaQuadrados = 0;
        for (double v : x) somaQuadrados += (v - media) * (v - media);
        double variancia = n > 1 ? somaQuadrados / (n - 1) : 0;
        Map<Double, Integer> frequencias = new LinkedHashMap<>();
        for (double v : x) frequencias.merge(v, 1, Integer::sum);
        int maiorFrequencia = Collections.max(frequencias.values());
        List<Double> modas = new ArrayList<>();
        if (maiorFrequencia > 1) {
            frequencias.forEach((preco, frequencia) -> {
                if (frequencia == maiorFrequencia) modas.add(preco);
            });
        }

        double mediana = n % 2 == 1 ? x.get(n / 2) : (x.get(n / 2 - 1) + x.get(n / 2)) / 2;

        e.put("media", arredondar(media));
        e.put("mediana", arredondar(mediana));
        e.put("modas", modas);
        e.put("variancia", n > 1 ? arredondar(variancia) : null);
        e.put("desvioPadrao", n > 1 ? arredondar(Math.sqrt(variancia)) : null);
        e.put("minimo", arredondar(Collections.min(x)));
        e.put("maximo", arredondar(Collections.max(x)));
        return e;
    }


     // Histograma com k classes de mesma amplitude.
     // k = arredondamento para cima de raiz(n) (regra da raiz), limitado entre 3 e 8.
    private Map<String, Object> histograma(List<Double> x) {
        List<String> rotulos = new ArrayList<>();
        List<Integer> frequencias = new ArrayList<>();
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("rotulos", rotulos);
        h.put("valores", frequencias);
        if (x.isEmpty()) return h;

        int k = Math.max(3, Math.min(8, (int) Math.ceil(Math.sqrt(x.size()))));
        double min = Collections.min(x), max = Collections.max(x);
        double amplitude = (max - min) / k;
        if (amplitude == 0) {
            rotulos.add(String.format(java.util.Locale.forLanguageTag("pt-BR"), "R$ %.2f", min));
            frequencias.add(x.size());
            return h;
        }

        int[] contagem = new int[k];
        for (double v : x) {
            int classe = (int) ((v - min) / amplitude);
            contagem[Math.min(classe, k - 1)]++; // o valor máximo cai na última classe
        }
        for (int i = 0; i < k; i++) {
            double de = min + i * amplitude;
            double ate = i == k - 1 ? max : min + (i + 1) * amplitude;
            rotulos.add(String.format(java.util.Locale.forLanguageTag("pt-BR"), "R$ %.2f – %.2f", de, ate));
            frequencias.add(contagem[i]);
        }
        return h;
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}