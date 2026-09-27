package aeroporto.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Ponto único de acesso ao MySQL via JDBC.
 * Lê as credenciais de config.properties (ou, se ele não existir, de config.properties.example).
 */
public final class Database {

    private static final Properties CONFIG = carregarConfig();

    private Database() {}

    private static Properties carregarConfig() {
        Properties props = new Properties();
        for (String nome : new String[] {"config.properties", "config.properties.example"}) {
            Path arquivo = Path.of(nome);
            if (Files.exists(arquivo)) {
                try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                    props.load(r);
                    System.out.println("Configuração carregada de " + nome);
                    return props;
                } catch (IOException e) {
                    throw new IllegalStateException("Erro ao ler " + nome, e);
                }
            }
        }
        System.out.println("Nenhum config.properties encontrado; usando valores padrão.");
        return props;
    }

    public static String config(String chave, String padrao) {
        return CONFIG.getProperty(chave, padrao).trim();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config("db.url", "jdbc:mysql://localhost:3306/aeroporto"),
                config("db.user", "root"),
                config("db.password", ""));
    }
}
