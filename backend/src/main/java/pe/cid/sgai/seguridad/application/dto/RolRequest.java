package pe.cid.sgai.seguridad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RolRequest(
        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(max = 60)
        @Pattern(regexp = "^[A-Z_]+$", message = "Use mayusculas y guion bajo, por ejemplo JEFE_ALMACEN")
        String nombre,

        @Size(max = 250)
        String descripcion,

        Boolean activo,

        @NotEmpty(message = "Seleccione al menos un permiso")
        Set<Long> permisoIds
) {
}
