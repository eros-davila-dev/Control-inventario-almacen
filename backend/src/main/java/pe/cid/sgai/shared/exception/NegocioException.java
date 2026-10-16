package pe.cid.sgai.shared.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Excepcion base de negocio: siempre lleva un {@link CodigoError} consumible por el cliente. */
@Getter
public class NegocioException extends RuntimeException {

    private final CodigoError codigo;
    private final transient Map<String, Object> detalles = new LinkedHashMap<>();

    public NegocioException(CodigoError codigo) {
        this(codigo, codigo.mensajePorDefecto());
    }

    public NegocioException(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public NegocioException con(String clave, Object valor) {
        detalles.put(clave, valor);
        return this;
    }

    public static NegocioException de(CodigoError codigo, String mensaje, Object... args) {
        return new NegocioException(codigo, args.length == 0 ? mensaje : String.format(mensaje, args));
    }
}
