package pe.cid.sgai.shared.audit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Registra automaticamente las operaciones anotadas con {@link Auditado},
 * tanto exitosas como fallidas, sin ensuciar los servicios de negocio.
 */
@Aspect
@Component
@Order(20)
@RequiredArgsConstructor
public class AuditoriaAspecto {

    private final AuditoriaService auditoriaService;

    @Pointcut("@annotation(auditado)")
    public void operacionAuditada(Auditado auditado) {
        // punto de corte declarativo
    }

    @Around(value = "operacionAuditada(auditado)", argNames = "pjp,auditado")
    public Object auditar(ProceedingJoinPoint pjp, Auditado auditado) throws Throwable {
        String detalle = auditado.incluirArgumentos() ? resumirArgumentos(pjp.getArgs()) : null;
        try {
            Object resultado = pjp.proceed();
            auditoriaService.registrar(auditado.accion(), auditado.entidad(),
                    extraerId(resultado), detalle, true);
            return resultado;
        } catch (Throwable ex) {
            auditoriaService.registrar(auditado.accion(), auditado.entidad(), null,
                    (detalle == null ? "" : detalle + " | ") + "error=" + ex.getMessage(), false);
            throw ex;
        }
    }

    private String resumirArgumentos(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return Arrays.stream(args)
                .map(a -> a == null ? "null" : enmascarar(a.toString()))
                .collect(Collectors.joining(" | "));
    }

    /** Nunca se escriben contrasenas ni tokens en la auditoria. */
    private String enmascarar(String valor) {
        return valor.replaceAll("(?i)(password|clave|token|secret)=[^,)\\]]*", "$1=***");
    }

    private Object extraerId(Object resultado) {
        if (resultado == null) {
            return null;
        }
        try {
            Method metodo = resultado.getClass().getMethod("id");
            return metodo.invoke(resultado);
        } catch (ReflectiveOperationException ignorada) {
            return null;
        }
    }
}
