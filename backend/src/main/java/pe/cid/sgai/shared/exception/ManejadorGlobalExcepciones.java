package pe.cid.sgai.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Traduce toda excepcion a ProblemDetail (RFC 7807) con un codigo de negocio estable.
 * Nunca expone stack traces ni mensajes internos (RNF-01).
 */
@Slf4j
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final String BASE_TIPO = "https://sgai.cid.pe/errores/";

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail negocio(NegocioException ex, HttpServletRequest req) {
        log.debug("Error de negocio {} en {}: {}", ex.getCodigo(), req.getRequestURI(), ex.getMessage());
        ProblemDetail pd = base(ex.getCodigo().estado(), ex.getCodigo(), ex.getMessage(), req);
        ex.getDetalles().forEach(pd::setProperty);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errores.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION,
                "Revise los datos enviados", req);
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail restriccion(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION,
                "Revise los datos enviados", req);
        pd.setProperty("errores", ex.getConstraintViolations().stream().collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(),
                v -> v.getMessage() == null ? "valor invalido" : v.getMessage(),
                (a, b) -> a)));
        return pd;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ProblemDetail peticionIlegible(Exception ex, HttpServletRequest req) {
        return base(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION,
                "La peticion no tiene el formato esperado", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacion de integridad en {}: {}", req.getRequestURI(), raiz(ex));
        return base(HttpStatus.CONFLICT, CodigoError.DUPLICADO,
                "La operacion rompe una restriccion de integridad: verifique duplicados o referencias", req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concurrencia(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return base(HttpStatus.CONFLICT, CodigoError.CONCURRENCIA,
                CodigoError.CONCURRENCIA.mensajePorDefecto(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail autenticacion(AuthenticationException ex, HttpServletRequest req) {
        return base(HttpStatus.UNAUTHORIZED, CodigoError.CREDENCIALES_INVALIDAS,
                CodigoError.CREDENCIALES_INVALIDAS.mensajePorDefecto(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acceso(AccessDeniedException ex, HttpServletRequest req) {
        return base(HttpStatus.FORBIDDEN, CodigoError.PERMISO_DENEGADO,
                CodigoError.PERMISO_DENEGADO.mensajePorDefecto(), req);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail noEncontrado(NoHandlerFoundException ex, HttpServletRequest req) {
        return base(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, "Recurso no encontrado", req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Error inesperado [{}] en {} {}", traceId, req.getMethod(), req.getRequestURI(), ex);
        ProblemDetail pd = base(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO,
                "Ocurrio un error inesperado. Informe el codigo " + traceId + " a soporte.", req);
        pd.setProperty("traceId", traceId);
        return pd;
    }

    private ProblemDetail base(HttpStatus estado, CodigoError codigo, String detalle, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setTitle(codigo.mensajePorDefecto());
        pd.setType(URI.create(BASE_TIPO + codigo.name().toLowerCase()));
        pd.setProperty("codigo", codigo.name());
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("ruta", req.getRequestURI());
        return pd;
    }

    private String raiz(Throwable ex) {
        Throwable actual = ex;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual.getMessage();
    }
}
