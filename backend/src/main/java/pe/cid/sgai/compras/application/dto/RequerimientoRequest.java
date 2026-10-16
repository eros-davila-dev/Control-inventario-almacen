package pe.cid.sgai.compras.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RequerimientoRequest(
        @NotNull(message = "Seleccione el almacen") Long almacenId,
        @Size(max = 500) String justificacion,
        @NotEmpty(message = "Agregue al menos un producto") @Valid List<LineaRequerimiento> lineas
) {

    public record LineaRequerimiento(
            @NotNull Long productoId,
            @NotNull @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidadSolicitada,
            @Size(max = 300) String observacion
    ) {
    }
}
