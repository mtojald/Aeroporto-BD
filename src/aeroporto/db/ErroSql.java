package aeroporto.db;

import java.sql.SQLException;

/** Traduz os códigos de erro do MySQL para mensagens legíveis na interface. */
public final class ErroSql {

    public static final int DUPLICADO = 1062;
    public static final int REFERENCIADO = 1451;
    public static final int REFERENCIA_INEXISTENTE = 1452;
    public static final int CHECK_VIOLADO = 3819;

    private ErroSql() {}

    public static boolean ehFalhaDeConexao(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("08");
    }

    public static int status(SQLException e) {
        if (ehFalhaDeConexao(e)) return 503;
        return switch (e.getErrorCode()) {
            case DUPLICADO, REFERENCIADO -> 409;
            default -> 400;
        };
    }

    public static String mensagem(SQLException e) {
        if (ehFalhaDeConexao(e)) {
            return "Não foi possível conectar ao MySQL. Verifique se o servidor está rodando "
                    + "e se o config.properties está correto.";
        }
        return switch (e.getErrorCode()) {
            case DUPLICADO -> "Já existe um registro com essa chave.";
            case REFERENCIADO -> "Operação bloqueada: existem outros registros que dependem deste.";
            case REFERENCIA_INEXISTENTE -> "Referência inválida: o registro relacionado não existe.";
            case CHECK_VIOLADO -> "Restrição do banco violada: " + e.getMessage();
            case 1048 -> "Campo obrigatório não informado: " + e.getMessage();
            case 1292, 1366 -> "Valor em formato inválido: " + e.getMessage();
            case 1406 -> "Valor maior que o permitido para o campo: " + e.getMessage();
            case 1049 -> "O banco 'aeroporto' não existe. Rode database/migrations.sql primeiro.";
            case 1146 -> "Tabela não encontrada. Rode database/migrations.sql primeiro.";
            default -> "Erro no banco (" + e.getErrorCode() + "): " + e.getMessage();
        };
    }
}
