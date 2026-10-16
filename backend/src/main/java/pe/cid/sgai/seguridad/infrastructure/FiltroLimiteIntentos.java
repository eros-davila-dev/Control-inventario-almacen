package pe.cid.sgai.seguridad.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.cid.sgai.shared.config.PropiedadesSgai;
import pe.cid.sgai.shared.exception.CodigoError;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de peticiones al endpoint de login por IP (defensa contra fuerza bruta
 * distribuida sobre varios usuarios).
 */
@Slf4j
@Component
public class FiltroLimiteIntentos extends OncePerRequestFilter {

    private record Ventana(Instant inicio, AtomicInteger conteo) {
    }

    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final int maximoPorMinuto;

    public FiltroLimiteIntentos(PropiedadesSgai propiedades) {
        this.maximoPorMinuto = propiedades.seguridad().loginPorMinuto();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"/api/v1/auth/login".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = ipDe(request);
        Instant ahora = Instant.now();
        Ventana ventana = ventanas.compute(ip, (clave, actual) -> {
            if (actual == null || actual.inicio().plusSeconds(60).isBefore(ahora)) {
                return new Ventana(ahora, new AtomicInteger(1));
            }
            actual.conteo().incrementAndGet();
            return actual;
        });
        if (ventana.conteo().get() > maximoPorMinuto) {
            log.warn("Limite de intentos de login superado desde {}", ip);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("""
                    {"status":429,"codigo":"%s","title":"Demasiados intentos",
                    "detail":"Espere un minuto antes de volver a intentar"}"""
                    .formatted(CodigoError.DEMASIADAS_PETICIONES.name()));
            return;
        }
        chain.doFilter(request, response);
    }

    private String ipDe(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
