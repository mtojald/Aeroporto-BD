package aeroporto.dao;

import aeroporto.db.Database;

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

    public Map<String, Object> montar(String status, String companhia, String inicio, String fim, Map<String, String> selecao) throws SQLException {
        status = normalizar(status);
        companhia = normalizar(companhia);
        inicio = normalizar(inicio);
        fim = normalizar(fim);
        Map<String, Object> dados = new LinkedHashMap<>();
        try (Connection con = Database.getConnection()) {
            StringBuilder filtro = new StringBuilder(" WHERE 1 = 1 ");
            if (status != null) filtro.append(" AND v.status = ? ");
            if (companhia != null) filtro.append(" AND c.cod_cia = ? ");
            if (inicio != null) filtro.append(" AND v.data_voo >= ? ");
            if (fim != null) filtro.append(" AND v.data_voo <= ? ");
            if (normalizar(selecao.get("voo")) != null) filtro.append(" AND v.num_voo = ").append(Integer.parseInt(selecao.get("voo")));
            if (normalizar(selecao.get("dia")) != null) filtro.append(" AND v.data_voo = DATE '").append(java.time.LocalDate.parse(selecao.get("dia"))).append("' ");
            if (normalizar(selecao.get("classe")) != null) {
                String hex = java.util.HexFormat.of().formatHex(selecao.get("classe").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                filtro.append(" AND EXISTS (SELECT 1 FROM Bilhete bx WHERE bx.fk_Voo_num_voo = v.num_voo AND bx.classe = CONVERT(X'").append(hex).append("' USING utf8mb4)) ");
            }
            if (normalizar(selecao.get("precoMin")) != null && normalizar(selecao.get("precoMax")) != null) {
                var min = new java.math.BigDecimal(selecao.get("precoMin"));
                var max = new java.math.BigDecimal(selecao.get("precoMax"));
                filtro.append(" AND EXISTS (SELECT 1 FROM Bilhete bx WHERE bx.fk_Voo_num_voo = v.num_voo AND bx.preco >= ").append(min.toPlainString()).append(" AND bx.preco <= ").append(max.toPlainString()).append(") ");
            }

            dados.put("indicadores", umaLinha(con, """
                    SELECT COUNT(DISTINCT v.num_voo) AS voos,
                           COUNT(DISTINCT b.fk_Passageiro_cpf) AS passageiros,
                           COUNT(DISTINCT v.fk_Aeronave_cod_aeronave) AS aeronaves,
                           COALESCE(SUM(b.preco), 0) AS receita
                    FROM Voo v
                    JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    LEFT JOIN Bilhete b ON b.fk_Voo_num_voo = v.num_voo
                    """ + filtro, status, companhia, inicio, fim));

            dados.put("voosPorStatus", serie(con, """
                    SELECT v.status AS rotulo, COUNT(DISTINCT v.num_voo) AS valor
                    FROM Voo v
                    JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    """ + filtro + " GROUP BY v.status ORDER BY valor DESC", status, companhia, inicio, fim));

            dados.put("receitaPorCompanhia", serie(con, """
                    SELECT c.cod_cia AS id, c.nome AS rotulo, COALESCE(SUM(b.preco), 0) AS valor
                    FROM Voo v
                    JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    LEFT JOIN Bilhete b ON b.fk_Voo_num_voo = v.num_voo
                    """ + filtro + " GROUP BY c.cod_cia, c.nome ORDER BY valor DESC", status, companhia, inicio, fim));

            dados.put("voosPorDia", serie(con, """
                    SELECT CAST(v.data_voo AS CHAR) AS id, DATE_FORMAT(v.data_voo, '%d/%m') AS rotulo, COUNT(DISTINCT v.num_voo) AS valor
                    FROM Voo v
                    JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    """ + filtro + " GROUP BY v.data_voo ORDER BY v.data_voo", status, companhia, inicio, fim));

            dados.put("ocupacaoPorVoo", serie(con, """
                    SELECT v.num_voo AS id, CONCAT('Voo ', v.num_voo) AS rotulo,
                           ROUND(100 * COUNT(b.cod_bilhete) / a.capac_passageiros, 2) AS valor
                    FROM Voo v
                    JOIN Aeronave a     ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    LEFT JOIN Bilhete b ON b.fk_Voo_num_voo = v.num_voo
                    """ + filtro + "\n" + """
                    GROUP BY v.num_voo, a.capac_passageiros
                    ORDER BY v.num_voo
                    """, status, companhia, inicio, fim));

            dados.put("bilhetesPorClasse", serie(con, """
                    SELECT COALESCE(b.classe, 'Sem classe') AS rotulo, COUNT(*) AS valor
                    FROM Bilhete b
                    JOIN Voo v ON v.num_voo = b.fk_Voo_num_voo
                    JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    """ + filtro + " GROUP BY b.classe ORDER BY valor DESC", status, companhia, inicio, fim));

            // estatística descritiva do preço dos bilhetes 
            List<Double> precos = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT b.preco
                    FROM Bilhete b
                    JOIN Voo v ON v.num_voo = b.fk_Voo_num_voo
                    JOIN Aeronave a ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    JOIN CompanhiaAerea c ON c.cod_cia = a.fk_CompanhiaAerea_cod_cia
                    """ + filtro + " ORDER BY b.preco")) {
                bindFiltros(ps, status, companhia, inicio, fim);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) precos.add(rs.getDouble(1));
                }
            }
            dados.put("estatisticasPreco", estatisticas(precos));
            dados.put("histogramaPreco", histograma(precos));
        }
        return dados;
    }

    // Leitura de resultados
    private Map<String, Object> umaLinha(Connection con, String sql, String status, String companhia, String inicio, String fim) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindFiltros(ps, status, companhia, inicio, fim);
            try (ResultSet rs = ps.executeQuery()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            if (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    linha.put(rs.getMetaData().getColumnLabel(i), rs.getBigDecimal(i));
                }
            }
            return linha;
            }
        }
    }

    private Map<String, Object> serie(Connection con, String sql, String status, String companhia, String inicio, String fim) throws SQLException {
        List<String> rotulos = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindFiltros(ps, status, companhia, inicio, fim);
            try (ResultSet rs = ps.executeQuery()) {
            boolean hasId;
            try { rs.findColumn("id"); hasId = true; } catch (SQLException ignored) { hasId = false; }
            while (rs.next()) {
                rotulos.add(rs.getString("rotulo"));
                valores.add(rs.getBigDecimal("valor"));
                if (hasId) ids.add(rs.getString("id"));
            }
            }
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("rotulos", rotulos);
        s.put("valores", valores);
        if (!ids.isEmpty()) s.put("ids", ids);
        return s;
    }

    private static void bindFiltros(PreparedStatement ps, String status, String companhia, String inicio, String fim) throws SQLException {
        int indice = 1;
        if (status != null) ps.setString(indice++, status);
        if (companhia != null) ps.setString(indice++, companhia);
        if (inicio != null) ps.setString(indice++, inicio);
        if (fim != null) ps.setString(indice, fim);
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    //Lista ordenada
    private Map<String, Object> estatisticas(List<Double> x) {
        Map<String, Object> e = new LinkedHashMap<>();
        int n = x.size();
        e.put("n", n);
        if (n == 0) return e;

        double soma = 0;
        Map<Double, Integer> frequencias = new LinkedHashMap<>();
        for (double v : x) soma += v;
        for (double v : x) frequencias.merge(v, 1, Integer::sum);
        double media = soma / n;

        double somaQuadrados = 0;
        for (double v : x) somaQuadrados += (v - media) * (v - media);
        double variancia = n > 1 ? somaQuadrados / (n - 1) : 0; // variância amostral
        double desvio = Math.sqrt(variancia);
        int maiorFrequencia = frequencias.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Double> modas = maiorFrequencia > 1
                ? frequencias.entrySet().stream().filter(e2 -> e2.getValue() == maiorFrequencia).map(Map.Entry::getKey).sorted().toList()
                : List.of();

        double mediana = n % 2 == 1 ? x.get(n / 2) : (x.get(n / 2 - 1) + x.get(n / 2)) / 2;

        e.put("media", arredondar(media));
        e.put("mediana", arredondar(mediana));
        e.put("moda", modas.stream().map(DashboardDAO::arredondar).toList());
        e.put("variancia", arredondar(variancia));
        e.put("desvioPadrao", arredondar(desvio));
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
        List<List<Double>> intervalos = new ArrayList<>();
        h.put("intervalos", intervalos);
        if (x.isEmpty()) return h;

        int k = Math.max(3, Math.min(8, (int) Math.ceil(Math.sqrt(x.size()))));
        double min = Collections.min(x), max = Collections.max(x);
        double amplitude = (max - min) / k;
        if (amplitude == 0) amplitude = 1;

        int[] contagem = new int[k];
        for (double v : x) {
            int classe = (int) ((v - min) / amplitude);
            contagem[Math.min(classe, k - 1)]++; // o valor máximo cai na última classe
        }
        for (int i = 0; i < k; i++) {
            intervalos.add(List.of(min + i * amplitude, i == k - 1 ? max : Math.nextDown(min + (i + 1) * amplitude)));
            long de = Math.round(min + i * amplitude);
            long ate = Math.round(min + (i + 1) * amplitude);
            rotulos.add("R$ " + de + " – " + ate);
            frequencias.add(contagem[i]);
        }
        return h;
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
