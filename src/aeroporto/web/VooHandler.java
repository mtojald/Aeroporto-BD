package aeroporto.web;

import aeroporto.dao.VooDAO;
import aeroporto.model.Voo;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/** API REST de /api/voos. */
public class VooHandler extends CrudHandler {

    public static final Set<String> STATUS = Set.of(
            "Agendado", "Confirmado", "Embarcando", "Atrasado", "Cancelado", "Concluído");

    private final VooDAO dao = new VooDAO();

    public VooHandler() {
        super("/api/voos");
    }

    @Override
    protected Object listar() throws SQLException {
        return dao.listar();
    }

    @Override
    protected String inserir(Map<String, String> campos) throws SQLException {
        Voo v = ler(new Campos(campos).inteiro("num_voo", "Número do voo"), campos);
        dao.inserir(v);
        return "Voo " + v.numVoo() + " cadastrado.";
    }

    @Override
    protected String alterar(String id, Map<String, String> campos) throws SQLException {
        Voo v = ler(Campos.converterInteiro(id, "Número do voo"), campos);
        dao.alterar(v);
        return "Voo " + v.numVoo() + " atualizado.";
    }

    @Override
    protected String excluir(String id) throws SQLException {
        int num = Campos.converterInteiro(id, "Número do voo");
        dao.excluir(num);
        return "Voo " + num + " excluído.";
    }

    private Voo ler(int numVoo, Map<String, String> valores) {
        Campos c = new Campos(valores);
        if (numVoo <= 0) throw ApiException.invalido("O número do voo deve ser positivo.");

        String origem = c.obrigatorio("origem", "Origem");
        String destino = c.obrigatorio("destino", "Destino");
        if (origem.equals(destino)) {
            throw ApiException.invalido("Origem e destino devem ser aeroportos diferentes.");
        }

        String status = c.obrigatorio("status", "Status");
        if (!STATUS.contains(status)) throw ApiException.invalido("Status inválido: " + status);

        // o portão chega como "numPortao-numTerminal" (chave composta)
        String[] portao = c.obrigatorio("portao", "Portão").split("-");
        if (portao.length != 2) throw ApiException.invalido("Portão inválido.");

        Integer conexao = c.inteiroOpcional("voo_conexao", "Voo de conexão");
        if (conexao != null && conexao == numVoo) {
            throw ApiException.invalido("Um voo não pode ser conexão dele mesmo.");
        }

        return new Voo(
                numVoo,
                c.data("data_voo", "Data do voo", true),
                c.hora("hora_partida", "Hora de partida"),
                c.hora("hora_chegada", "Hora de chegada"),
                status,
                c.obrigatorio("aeronave", "Aeronave"),
                origem,
                destino,
                Campos.converterInteiro(portao[0], "Portão"),
                Campos.converterInteiro(portao[1], "Terminal"),
                conexao,
                c.inteiro("piloto", "Piloto comandante"));
    }
}
