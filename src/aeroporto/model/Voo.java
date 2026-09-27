package aeroporto.model;

/** Linha da tabela Voo. */
public record Voo(
        int numVoo,
        String dataVoo,          // yyyy-MM-dd
        String horaPartida,      // HH:mm:ss ou null
        String horaChegada,      // HH:mm:ss ou null
        String status,
        String codAeronave,
        String aeroportoOrigem,
        String aeroportoDestino,
        int numPortao,
        int numTerminal,
        Integer vooConexao,      // null = sem conexão
        int pilotoComandante) {
}
