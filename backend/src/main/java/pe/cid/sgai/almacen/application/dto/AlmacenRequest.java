package pe.cid.sgai.almacen.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.almacen.domain.TipoAlmacen;

public record AlmacenRequest(
        @NotBlank(message = "El codigo es obligatorio") @Size(max = 20) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @NotBlank(message = "Indique la sede") @Size(max = 120) String sede,
        @Size(max = 250) String direccion,
        @NotNull(message = "Seleccione el tipo de almacen") TipoAlmacen tipoAlmacen,
        Long responsableId,
        @Size(max = 40) String telefono,
        Boolean permiteNegativo,
        Boolean activo
) {
}
