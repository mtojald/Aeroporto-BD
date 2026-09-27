package aeroporto.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Leitura e validação dos campos enviados pelos formulários. */
public final class Campos {

    private final Map<String, String> valores;

    public Campos(Map<String, String> valores) {
        this.valores = valores;
    }

    /** Valor do campo, ou null se ausente/vazio. */
    public String opcional(String nome) {
        String v = valores.get(nome);
        return v == null || v.isBlank() ? null : v.trim();
    }

    public String obrigatorio(String nome, String rotulo) {
        String v = opcional(nome);
        if (v == null) throw ApiException.invalido("O campo \"" + rotulo + "\" é obrigatório.");
        return v;
    }

    public String texto(String nome, String rotulo, boolean obrigatorio, int tamanhoMax) {
        String v = obrigatorio ? obrigatorio(nome, rotulo) : opcional(nome);
        if (v != null && v.length() > tamanhoMax) {
            throw ApiException.invalido("\"" + rotulo + "\" deve ter no máximo " + tamanhoMax + " caracteres.");
        }
        return v;
    }

    public int inteiro(String nome, String rotulo) {
        return converterInteiro(obrigatorio(nome, rotulo), rotulo);
    }

    public Integer inteiroOpcional(String nome, String rotulo) {
        String v = opcional(nome);
        return v == null ? null : converterInteiro(v, rotulo);
    }

    public static int converterInteiro(String v, String rotulo) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw ApiException.invalido("\"" + rotulo + "\" deve ser um número inteiro.");
        }
    }

    /** Data no formato yyyy-MM-dd (o que o input type="date" envia). */
    public String data(String nome, String rotulo, boolean obrigatorio) {
        String v = obrigatorio ? obrigatorio(nome, rotulo) : opcional(nome);
        if (v == null) return null;
        try {
            return LocalDate.parse(v).toString();
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("\"" + rotulo + "\" não é uma data válida.");
        }
    }

    /** Hora HH:mm ou HH:mm:ss, devolvida como HH:mm:ss. */
    public String hora(String nome, String rotulo) {
        String v = opcional(nome);
        if (v == null) return null;
        try {
            LocalTime t = LocalTime.parse(v);
            return String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("\"" + rotulo + "\" não é uma hora válida.");
        }
    }
}
