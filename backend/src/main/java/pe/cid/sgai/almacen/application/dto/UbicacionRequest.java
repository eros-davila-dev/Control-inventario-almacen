package pe.cid.sgai.almacen.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.almacen.domain.TipoUbicacion;

public record UbicacionRequest(
        @NotNull(message = "Seleccione el almacen") Long almacenId,
        @NotBlank(message = "El codigo es obligatorio") @Size(max = 30) String codigo,
        @Size(max = 150) String descripcion,
        @Size(max = 30) String zona,
        @Size(max = 30) String rack,
        @Size(max = 30) String nivel,
        TipoUbicacion tipoUbicacion,
        Boolean activo
) {
}
