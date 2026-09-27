package aeroporto.model;

import java.util.List;

/** Linha da tabela Passageiro + seus telefones (atributo multivalorado em Passageiro_Telefone). */
public record Passageiro(
        String cpf,
        String primeiroNome,
        String sobrenome,
        String dataNasci,   // yyyy-MM-dd ou null
        String email,       // pode ser null
        List<String> telefones) {
}
