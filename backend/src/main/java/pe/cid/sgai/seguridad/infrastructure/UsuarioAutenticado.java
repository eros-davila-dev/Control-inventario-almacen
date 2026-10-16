package pe.cid.sgai.seguridad.infrastructure;

import java.util.Set;

/**
 * Principal inmutable que viaja en el contexto de seguridad. Se arma desde los claims
 * del access token, por lo que la autorizacion no consulta la base en cada peticion.
 */
public record UsuarioAutenticado(
        Long id,
        String username,
        String nombreCompleto,
        Set<String> roles,
        Set<String> permisos,
        Set<Long> almacenIds,
        boolean debeCambiarPassword
) {

    public boolean esAdministrador() {
        return roles.contains("ADMIN");
    }

    public boolean tienePermiso(String codigo) {
        return permisos.contains(codigo);
    }

    /** Un rol con vision global no esta limitado a los almacenes asignados. */
    public boolean tieneVisionGlobal() {
        return roles.contains("ADMIN") || roles.contains("JEFE_ALMACEN") || roles.contains("AUDITOR");
    }

    public boolean puedeOperarAlmacen(Long almacenId) {
        return tieneVisionGlobal() || (almacenId != null && almacenIds.contains(almacenId));
    }
}
