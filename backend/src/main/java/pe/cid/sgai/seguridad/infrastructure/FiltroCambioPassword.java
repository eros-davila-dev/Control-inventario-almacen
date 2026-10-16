package pe.cid.sgai.seguridad.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.cid.sgai.shared.exception.CodigoError;

import java.io.IOException;
import java.util.Set;

/**
 * Mientras el usuario tenga la marca de cambio de contrasena obligatorio
 * (HU-03) solo puede consumir su perfil y el cambio de clave.
 */
@Component
@RequiredArgsConstructor
public class FiltroCambioPassword extends OncePerRequestFilter {

    private static final Set<String> RUTAS_PERMITIDAS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
            "/api/v1/perfil", "/api/v1/perfil/password");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        var usuario = ContextoSeguridad.usuarioActual().orElse(null);
        String ruta = request.getRequestURI();
        if (usuario != null && usuario.debeCambiarPassword() && !RUTAS_PERMITIDAS.contains(ruta)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"status":403,"codigo":"%s","title":"Cambio de contrasena requerido",
                    "detail":"Debe cambiar su contrasena antes de continuar","ruta":"%s"}"""
                    .formatted(CodigoError.PASSWORD_CHANGE_REQUIRED.name(), ruta));
            return;
        }
        chain.doFilter(request, response);
    }
}
