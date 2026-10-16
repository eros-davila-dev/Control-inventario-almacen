package pe.cid.sgai.shared.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

/** Escritura y consulta de la pista de auditoria. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    /** Limites por defecto del rango de consulta: evitan parametros nulos sin tipo. */
    private static final Instant INICIO_DE_LOS_TIEMPOS = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant FIN_DE_LOS_TIEMPOS = Instant.parse("2999-12-31T23:59:59Z");

    private final AuditoriaLogRepository repository;

    /**
     * Registra en una transaccion independiente: si la operacion de negocio falla y
     * revierte, el intento queda registrado igualmente.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String entidad, Object entidadId, String detalle, boolean exitoso) {
        try {
            repository.save(AuditoriaLog.builder()
                    .usuario(usuarioActual())
                    .accion(accion)
                    .entidad(entidad)
                    .entidadId(entidadId == null ? null : String.valueOf(entidadId))
                    .detalle(recortar(detalle))
                    .ip(ipActual())
                    .userAgent(cabecera("User-Agent"))
                    .exitoso(exitoso)
                    .fecha(Instant.now())
                    .build());
        } catch (Exception ex) {
            log.error("No se pudo registrar auditoria de {} sobre {}", accion, entidad, ex);
        }
    }

    public void registrar(String accion, String entidad, Object entidadId, String detalle) {
        registrar(accion, entidad, entidadId, detalle, true);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaLog> buscar(String usuario, String entidad, String entidadId, String accion,
                                     Instant desde, Instant hasta, Pageable pageable) {
        return repository.buscar(vacioANulo(usuario), vacioANulo(entidad), vacioANulo(entidadId),
                vacioANulo(accion), desde == null ? INICIO_DE_LOS_TIEMPOS : desde,
                hasta == null ? FIN_DE_LOS_TIEMPOS : hasta, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaLog> historialDe(String entidad, Object entidadId) {
        return repository.findTop200ByEntidadAndEntidadIdOrderByFechaDesc(entidad, String.valueOf(entidadId));
    }

    public static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth.getName() == null ? "sistema" : auth.getName();
    }

    public static String ipActual() {
        HttpServletRequest req = peticion();
        if (req == null) {
            return null;
        }
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    public static String cabecera(String nombre) {
        HttpServletRequest req = peticion();
        if (req == null) {
            return null;
        }
        String valor = req.getHeader(nombre);
        return valor == null ? null : valor.substring(0, Math.min(valor.length(), 300));
    }

    private static HttpServletRequest peticion() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 4000 ? texto : texto.substring(0, 3997) + "...";
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
