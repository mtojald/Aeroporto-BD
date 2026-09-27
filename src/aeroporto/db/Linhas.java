package aeroporto.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converte um ResultSet em uma lista de linhas (coluna -> valor), pronta para virar JSON. */
public final class Linhas {

    private Linhas() {}

    public static List<Map<String, Object>> de(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        List<Map<String, Object>> linhas = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                // getString evita conversões de fuso horário em DATE/TIME
                linha.put(meta.getColumnLabel(i), rs.getString(i));
            }
            linhas.add(linha);
        }
        return linhas;
    }
}
