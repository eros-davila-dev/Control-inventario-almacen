package pe.cid.sgai.shared.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una operacion de servicio que debe quedar registrada en la pista de auditoria.
 * Ejemplo: {@code @Auditado(accion = "LIBERAR_LOTE", entidad = "lote")}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditado {

    String accion();

    String entidad();

    /** Registra los argumentos del metodo en el detalle (nunca usar con datos sensibles). */
    boolean incluirArgumentos() default true;
}
