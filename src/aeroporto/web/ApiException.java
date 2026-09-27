package aeroporto.web;

/** Erro de validação/negócio que deve chegar ao usuário com um status HTTP específico. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public static ApiException invalido(String mensagem) {
        return new ApiException(400, mensagem);
    }

    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(404, mensagem);
    }

    public int status() {
        return status;
    }
}
