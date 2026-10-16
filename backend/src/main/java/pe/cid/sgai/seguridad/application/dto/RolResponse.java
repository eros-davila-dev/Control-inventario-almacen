package pe.cid.sgai.seguridad.application.dto;

import java.util.List;

public record RolResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean esSistema,
        boolean activo,
        long usuariosAsignados,
        List<PermisoResponse> permisos
) {
}
