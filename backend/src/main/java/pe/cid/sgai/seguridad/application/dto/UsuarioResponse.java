package pe.cid.sgai.seguridad.application.dto;

import java.time.Instant;
import java.util.Set;

public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String dni,
        String cargo,
        String telefono,
        boolean activo,
        boolean debeCambiarPassword,
        Instant ultimoAcceso,
        Set<String> roles,
        Set<String> permisos,
        Set<Long> almacenIds
) {
}
