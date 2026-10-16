package pe.cid.sgai.despacho.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cid.sgai.despacho.domain.Prioridad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RequisicionRequest(
        @NotNull(message = "Seleccione el almacen solicitante") Long almacenSolicitanteId,
        @NotNull(message = "Seleccione el almacen que atiende") Long almacenAtencionId,
        @NotBlank(message = "Indique el centro de costo") @Size(max = 40) String centroCosto,
        @NotBlank(message = "Indique el area solicitante") @Size(max = 120) String area,
        Prioridad prioridad,
        LocalDate fechaRequerida,
        @Size(max = 500) String observacion,
        @NotEmpty(message = "Agregue al menos un producto") @Valid List<LineaRequisicion> lineas
) {

    public record LineaRequisicion(
            @NotNull Long productoId,
            @NotNull @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidadSolicitada,
            @Size(max = 300) String observacion
    ) {
    }
}
