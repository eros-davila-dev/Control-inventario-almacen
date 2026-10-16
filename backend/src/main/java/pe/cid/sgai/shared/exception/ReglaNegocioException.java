package pe.cid.sgai.shared.exception;

/** Violacion de una regla funcional del almacen (422). */
public class ReglaNegocioException extends NegocioException {

    public ReglaNegocioException(String mensaje) {
        super(CodigoError.REGLA_NEGOCIO, mensaje);
    }

    public ReglaNegocioException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
