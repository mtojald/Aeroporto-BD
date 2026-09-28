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

    public Map<String, Object> montar() throws SQLException {
        Map<String, Object> dados = new LinkedHashMap<>();
        try (Connection con = Database.getConnection()) {

            //  indicadores (cards)
            dados.put("indicadores", umaLinha(con, """
                    SELECT (SELECT COUNT(*) FROM Voo)        AS voos,
                           (SELECT COUNT(*) FROM Passageiro) AS passageiros,
                           (SELECT COUNT(*) FROM Aeronave)   AS aeronaves,
                           (SELECT COALESCE(SUM(preco), 0) FROM Bilhete) AS receita
                    """));

            // gráfico 1: voos por status 
            dados.put("voosPorStatus", serie(con, """
                    SELECT status AS rotulo, COUNT(*) AS valor
                    FROM Voo
                    GROUP BY status
                    ORDER BY valor DESC
                    """));

            // gráfico 2: receita por companhia 
            dados.put("receitaPorCompanhia", serie(con, """
                    SELECT c.nome AS rotulo, COALESCE(SUM(b.preco), 0) AS valor
                    FROM CompanhiaAerea c
                    LEFT JOIN Aeronave a ON a.fk_CompanhiaAerea_cod_cia = c.cod_cia
                    LEFT JOIN Voo v      ON v.fk_Aeronave_cod_aeronave = a.cod_aeronave
                    LEFT JOIN Bilhete b  ON b.fk_Voo_num_voo = v.num_voo
                    GROUP BY c.cod_cia, c.nome
                    ORDER BY valor DESC
                    """));

            //gráfico 3: voos por dia
            dados.put("voosPorDia", serie(con, """
                    SELECT DATE_FORMAT(data_voo, '%d/%m') AS rotulo, COUNT(*) AS valor
                    FROM Voo
                    GROUP BY data_voo
                    ORDER BY data_voo
                    """));

            // gráfico 4: ocupação (%) por voo 
            dados.put("ocupacaoPorVoo", serie(con, """
                    SELECT CONCAT('Voo ', v.num_voo) AS rotulo,
                           ROUND(100 * COUNT(b.cod_bilhete) / a.capac_passageiros, 2) AS valor
                    FROM Voo v
                    JOIN Aeronave a     ON a.cod_aeronave = v.fk_Aeronave_cod_aeronave
                    LEFT JOIN Bilhete b ON b.fk_Voo_num_voo = v.num_voo
                    GROUP BY v.num_voo, a.capac_passageiros
                    ORDER BY v.num_voo
                    """));

            // estatística descritiva do preço dos bilhetes 
            List<Double> precos = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT preco FROM Bilhete ORDER BY preco");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) precos.add(rs.getDouble(1));
            }
            dados.put("estatisticasPreco", estatisticas(precos));
            dados.put("histogramaPreco", histograma(precos));
        }
        return dados;
    }

    // Leitura de resultados
    private Map<String, Object> umaLinha(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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
    private Map<String, Object> serie(Connection con, String sql) throws SQLException {
        List<String> rotulos = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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
        double desvio = n > 1 ? Math.sqrt(somaQuadrados / (n - 1)) : 0; // desvio padrão amostral

        double mediana = n % 2 == 1 ? x.get(n / 2) : (x.get(n / 2 - 1) + x.get(n / 2)) / 2;

        e.put("media", arredondar(media));
        e.put("mediana", arredondar(mediana));
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