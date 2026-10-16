package pe.cid.sgai.shared.exception;

public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String entidad, Object id) {
        super(CodigoError.NO_ENCONTRADO, "No existe %s con identificador %s".formatted(entidad, id));
        con("entidad", entidad);
        con("id", id);
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(CodigoError.NO_ENCONTRADO, mensaje);
    }
}
