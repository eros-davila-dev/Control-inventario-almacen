package pe.cid.sgai.seguridad.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.cid.sgai.shared.exception.CodigoError;
import pe.cid.sgai.shared.exception.NegocioException;

import java.util.Optional;
import java.util.Set;

/** Acceso tipado al usuario autenticado. */
public final class ContextoSeguridad {

    private ContextoSeguridad() {
    }

    public static Optional<UsuarioAutenticado> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    public static UsuarioAutenticado requerido() {
        return usuarioActual().orElseThrow(() -> new NegocioException(CodigoError.CREDENCIALES_INVALIDAS,
                "No hay una sesion activa"));
    }

    public static Long idUsuarioActual() {
        return requerido().id();
    }

    public static String username() {
        return usuarioActual().map(UsuarioAutenticado::username).orElse("sistema");
    }

    public static Set<Long> almacenesPermitidos() {
        return requerido().almacenIds();
    }

    /** Verifica el alcance por almacen (HU-14). */
    public static void verificarAlmacen(Long almacenId) {
        UsuarioAutenticado usuario = requerido();
        if (!usuario.puedeOperarAlmacen(almacenId)) {
            throw new NegocioException(CodigoError.ALMACEN_NO_AUTORIZADO,
                    "No tiene asignado el almacen solicitado").con("almacenId", almacenId);
        }
    }
}
