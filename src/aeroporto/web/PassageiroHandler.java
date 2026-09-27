package aeroporto.web;

import aeroporto.dao.PassageiroDAO;
import aeroporto.model.Passageiro;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** API REST de /api/passageiros. */
public class PassageiroHandler extends CrudHandler {

    private final PassageiroDAO dao = new PassageiroDAO();

    public PassageiroHandler() {
        super("/api/passageiros");
    }

    @Override
    protected Object listar() throws SQLException {
        return dao.listar();
    }

    @Override
    protected String inserir(Map<String, String> campos) throws SQLException {
        Passageiro p = ler(new Campos(campos).obrigatorio("cpf", "CPF"), campos);
        dao.inserir(p);
        return "Passageiro " + p.primeiroNome() + " cadastrado.";
    }

    @Override
    protected String alterar(String cpf, Map<String, String> campos) throws SQLException {
        Passageiro p = ler(cpf, campos);
        dao.alterar(p);
        return "Passageiro " + p.primeiroNome() + " atualizado.";
    }

    @Override
    protected String excluir(String cpf) throws SQLException {
        dao.excluir(somenteDigitos(cpf));
        return "Passageiro excluído.";
    }

    private Passageiro ler(String cpfBruto, Map<String, String> valores) {
        Campos c = new Campos(valores);
        String cpf = somenteDigitos(cpfBruto);
        if (cpf.length() != 11) throw ApiException.invalido("O CPF deve ter 11 dígitos.");

        String dataNasci = c.data("data_nasci", "Data de nascimento", false);
        if (dataNasci != null && LocalDate.parse(dataNasci).isAfter(LocalDate.now())) {
            throw ApiException.invalido("A data de nascimento não pode estar no futuro.");
        }
        String email = c.texto("email", "E-mail", false, 100);
        if (email != null && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw ApiException.invalido("E-mail inválido.");
        }

        // telefones separados por vírgula, ";" ou quebra de linha
        String telefonesBrutos = c.opcional("telefones");
        List<String> telefones = telefonesBrutos == null ? List.of()
                : Arrays.stream(telefonesBrutos.split("[,;\\n]"))
                        .map(t -> t.replaceAll("[^0-9+]", ""))
                        .filter(t -> !t.isEmpty())
                        .distinct()
                        .toList();
        for (String t : telefones) {
            if (t.length() > 20) throw ApiException.invalido("Telefone muito longo: " + t);
        }

        return new Passageiro(
                cpf,
                c.texto("primeiro_nome", "Nome", true, 50),
                c.texto("sobrenome", "Sobrenome", true, 50),
                dataNasci,
                email,
                telefones);
    }

    private static String somenteDigitos(String s) {
        return s.replaceAll("\\D", "");
    }
}
