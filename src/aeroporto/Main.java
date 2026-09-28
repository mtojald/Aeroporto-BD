package aeroporto;

import aeroporto.db.Database;
import aeroporto.db.ErroSql;
import aeroporto.web.OpcoesHandler;
import aeroporto.web.PassageiroHandler;
import aeroporto.web.StaticHandler;
import aeroporto.web.VooHandler;
import aeroporto.web.ConsultaHandler;
import aeroporto.web.DashboardHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(Database.config("server.port", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
        // API (JSON)
        server.createContext("/api/passageiros", new PassageiroHandler());
        server.createContext("/api/voos", new VooHandler());
        server.createContext("/api/opcoes", new OpcoesHandler());
        server.createContext("/api/consultas", new ConsultaHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        // Interface (HTML/CSS/JS)
        server.createContext("/", new StaticHandler(Path.of("web")));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Servidor rodando em http://localhost:" + porta);
        testarConexao();
    }

    private static void testarConexao() {
        try (Connection con = Database.getConnection()) {
            System.out.println("Conectado ao MySQL: " + con.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("AVISO: " + ErroSql.mensagem(e));
            System.err.println("       Detalhe: " + e.getMessage());
        }
    }
}
