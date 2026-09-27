package aeroporto.web;

import java.util.Collection;
import java.util.Map;

/** Serializador JSON mínimo (sem dependências externas). */
public final class Json {

    private Json() {}

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(sb, valor);
        return sb.toString();
    }

    private static void escrever(StringBuilder sb, Object valor) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escrever(sb, e.getValue());
            }
            sb.append('}');
        } else if (valor instanceof Collection<?> lista) {
            sb.append('[');
            boolean primeiro = true;
            for (Object item : lista) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escrever(sb, item);
            }
            sb.append(']');
        } else {
            texto(sb, valor.toString());
        }
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
