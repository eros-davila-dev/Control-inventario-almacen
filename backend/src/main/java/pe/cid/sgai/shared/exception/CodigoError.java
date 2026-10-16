package pe.cid.sgai.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogo unico de errores de negocio. El frontend traduce por codigo,
 * nunca por el texto del mensaje.
 */
public enum CodigoError {

    VALIDACION("Datos invalidos", HttpStatus.BAD_REQUEST),
    NO_ENCONTRADO("Recurso no encontrado", HttpStatus.NOT_FOUND),
    DUPLICADO("El registro ya existe", HttpStatus.CONFLICT),
    CONFLICTO("Conflicto con el estado actual del recurso", HttpStatus.CONFLICT),
    REGLA_NEGOCIO("La operacion no cumple una regla de negocio", HttpStatus.UNPROCESSABLE_ENTITY),
    ESTADO_INVALIDO("Transicion de estado no permitida", HttpStatus.CONFLICT),
    CONCURRENCIA("El registro fue modificado por otro usuario, vuelva a intentar", HttpStatus.CONFLICT),

    CREDENCIALES_INVALIDAS("Credenciales invalidas", HttpStatus.UNAUTHORIZED),
    CUENTA_BLOQUEADA("Cuenta bloqueada temporalmente por intentos fallidos", HttpStatus.UNAUTHORIZED),
    CUENTA_INACTIVA("La cuenta se encuentra inactiva", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALIDO("Token invalido o expirado", HttpStatus.UNAUTHORIZED),
    PASSWORD_CHANGE_REQUIRED("Debe cambiar su contrasena antes de continuar", HttpStatus.FORBIDDEN),
    PASSWORD_DEBIL("La contrasena no cumple la politica de seguridad", HttpStatus.BAD_REQUEST),
    PASSWORD_REUTILIZADA("No puede reutilizar sus contrasenas anteriores", HttpStatus.BAD_REQUEST),
    PERMISO_DENEGADO("No cuenta con permisos para esta operacion", HttpStatus.FORBIDDEN),
    ALMACEN_NO_AUTORIZADO("No tiene asignado el almacen de la operacion", HttpStatus.FORBIDDEN),
    SEGREGACION_FUNCIONES("Quien registra el documento no puede aprobarlo", HttpStatus.FORBIDDEN),
    DEMASIADAS_PETICIONES("Demasiados intentos, espere un momento", HttpStatus.TOO_MANY_REQUESTS),

    STOCK_INSUFICIENTE("Stock insuficiente para la operacion", HttpStatus.CONFLICT),
    LOTE_NO_DISPONIBLE("El lote no se encuentra disponible para despacho", HttpStatus.CONFLICT),
    LOTE_VENCIDO("El lote esta vencido", HttpStatus.CONFLICT),
    PRODUCTO_REQUIERE_LOTE("El producto exige registrar lote y vencimiento", HttpStatus.BAD_REQUEST),
    MOVIMIENTO_ANULADO("El movimiento ya se encuentra anulado", HttpStatus.CONFLICT),
    CANTIDAD_EXCEDE_PENDIENTE("La cantidad supera el saldo pendiente del documento", HttpStatus.CONFLICT),
    CALIDAD_RECHAZADA("La recepcion fue rechazada en el control de calidad", HttpStatus.UNPROCESSABLE_ENTITY),
    REGISTRO_SANITARIO_VENCIDO("El registro sanitario del producto esta vencido", HttpStatus.UNPROCESSABLE_ENTITY),
    VIDA_UTIL_INSUFICIENTE("La vida util remanente del lote es menor a la minima exigida", HttpStatus.UNPROCESSABLE_ENTITY),

    ERROR_INTERNO("Error interno del sistema", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String mensajePorDefecto;
    private final HttpStatus estado;

    CodigoError(String mensajePorDefecto, HttpStatus estado) {
        this.mensajePorDefecto = mensajePorDefecto;
        this.estado = estado;
    }

    public String mensajePorDefecto() {
        return mensajePorDefecto;
    }

    public HttpStatus estado() {
        return estado;
    }
}
